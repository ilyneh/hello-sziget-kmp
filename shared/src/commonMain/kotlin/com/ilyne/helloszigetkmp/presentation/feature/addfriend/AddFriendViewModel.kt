package com.ilyne.helloszigetkmp.presentation.feature.addfriend

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ilyne.helloszigetkmp.core.auth.CurrentUserProvider
import com.ilyne.helloszigetkmp.core.repository.FriendRepository
import com.ilyne.helloszigetkmp.core.repository.UserRepository
import com.ilyne.helloszigetkmp.domain.model.User
import com.ilyne.helloszigetkmp.util.Logger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class FriendshipStatus { NONE, FRIEND, REQUEST_SENT, REQUEST_RECEIVED }

data class AddFriendUserItem(
    val user: User,
    val status: FriendshipStatus,
)

data class AddFriendUiState(
    val searchQuery: String = "",
    val results: List<AddFriendUserItem> = emptyList(),
    val status: Status = Status.Idle,
) {
    sealed class Status {
        data object Idle : Status()

        data class Error(
            val reason: AddFriendErrorReason,
        ) : Status()
    }
}

enum class AddFriendErrorReason {
    SEND_REQUEST_FAILED,
    ACCEPT_REQUEST_FAILED,
}

sealed class AddFriendIntent {
    data class SearchQueryChanged(
        val query: String,
    ) : AddFriendIntent()

    data class SendFriendRequest(
        val userId: String,
    ) : AddFriendIntent()

    data class AcceptFriendRequest(
        val userId: String,
    ) : AddFriendIntent()
}

class AddFriendViewModel(
    private val friendRepository: FriendRepository,
    private val userRepository: UserRepository,
    currentUserProvider: CurrentUserProvider,
    private val backgroundDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {
    private val searchQuery = MutableStateFlow("")

    private val _uiState = MutableStateFlow(AddFriendUiState())
    val uiState = _uiState.asStateFlow()

    // Nullable rather than CurrentUserProvider.currentUser (which throws): this ViewModel can be
    // constructed mid-navigation concurrently with a session invalidation clearing the provider
    // (e.g. a background token refresh failing right as the user taps into Add Friend). In that
    // case the screen is about to be replaced by Login anyway, so degrade gracefully instead of
    // crashing on construction.
    private val currentUserId: String? = currentUserProvider.currentUserOrNull?.id

    init {
        if (currentUserId == null) {
            Logger.e("AddFriendViewModel", "init: no current user, session was likely invalidated during navigation")
        }
        observeResults()
    }

    fun onIntent(intent: AddFriendIntent) {
        when (intent) {
            is AddFriendIntent.SearchQueryChanged -> {
                searchQuery.value = intent.query
                _uiState.update { it.copy(searchQuery = intent.query) }
            }

            is AddFriendIntent.SendFriendRequest -> {
                viewModelScope.launch {
                    val userId = currentUserId ?: return@launch
                    try {
                        friendRepository.sendFriendRequest(userId, intent.userId)
                        _uiState.update { it.copy(status = AddFriendUiState.Status.Idle) }
                        refreshFriendsFavoritedInBackground()
                    } catch (e: Exception) {
                        Logger.e("AddFriendViewModel", "Failed to send friend request", e)
                        _uiState.update {
                            it.copy(
                                status = AddFriendUiState.Status.Error(AddFriendErrorReason.SEND_REQUEST_FAILED),
                            )
                        }
                    }
                }
            }

            is AddFriendIntent.AcceptFriendRequest -> {
                viewModelScope.launch {
                    val userId = currentUserId ?: return@launch
                    try {
                        friendRepository.acceptFriendRequest(userId, intent.userId)
                        _uiState.update { it.copy(status = AddFriendUiState.Status.Idle) }
                        refreshFriendsFavoritedInBackground()
                    } catch (e: Exception) {
                        Logger.e("AddFriendViewModel", "Failed to accept friend request", e)
                        _uiState.update {
                            it.copy(
                                status = AddFriendUiState.Status.Error(AddFriendErrorReason.ACCEPT_REQUEST_FAILED),
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * Best-effort background sync so a newly accepted/requested friend's favorited artists show
     * up on the Schedule screen without waiting for its next pull-to-refresh. Fire-and-forget:
     * launched in its own coroutine so it never blocks or delays the success state above, and
     * failures are logged rather than surfaced, since this is not the primary operation.
     */
    private fun refreshFriendsFavoritedInBackground() {
        viewModelScope.launch {
            runCatching { friendRepository.refresh(force = true) }
                .onFailure { Logger.e("AddFriendViewModel", "Background friends refresh failed", it) }
        }
    }

    private fun observeResults() {
        viewModelScope.launch {
            combine(
                userRepository.observeUsers(),
                friendRepository.observeFriends(),
                friendRepository.observeSentFriendRequests(),
                friendRepository.observeFriendRequests(),
                searchQuery,
            ) { users, friends, sentRequests, receivedRequests, query ->
                val friendIds = friends.map { it.id }.toSet()
                val sentIds = sentRequests.map { it.id }.toSet()
                val receivedIds = receivedRequests.map { it.id }.toSet()

                users
                    .asSequence()
                    .filter { it.id != currentUserId }
                    .filter { it.id !in friendIds }
                    .filter { query.isBlank() || it.name.contains(query, ignoreCase = true) }
                    .map { user ->
                        val status = when (user.id) {
                            in sentIds -> FriendshipStatus.REQUEST_SENT
                            in receivedIds -> FriendshipStatus.REQUEST_RECEIVED
                            else -> FriendshipStatus.NONE
                        }
                        AddFriendUserItem(user = user, status = status)
                    }.toList()
            }.flowOn(backgroundDispatcher)
                .collect { results ->
                    _uiState.update { it.copy(results = results) }
                }
        }
    }
}

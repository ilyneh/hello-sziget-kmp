package com.ilyne.helloszigetkmp.presentation.feature.addfriend

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ilyne.helloszigetkmp.core.auth.CurrentUserProvider
import com.ilyne.helloszigetkmp.core.repository.FriendRepository
import com.ilyne.helloszigetkmp.core.repository.UserRepository
import com.ilyne.helloszigetkmp.domain.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
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
        data class Error(val message: String) : Status()
    }
}

sealed class AddFriendIntent {
    data class SearchQueryChanged(val query: String) : AddFriendIntent()
    data class SendFriendRequest(val userId: String) : AddFriendIntent()
    data class AcceptFriendRequest(val userId: String) : AddFriendIntent()
}

class AddFriendViewModel(
    private val friendRepository: FriendRepository,
    private val userRepository: UserRepository,
    currentUserProvider: CurrentUserProvider,
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")

    private val _uiState = MutableStateFlow(AddFriendUiState())
    val uiState = _uiState.asStateFlow()

    private val currentUserId: String = currentUserProvider.currentUser.id

    init {
        observeResults()
    }

    fun onIntent(intent: AddFriendIntent) {
        when (intent) {
            is AddFriendIntent.SearchQueryChanged -> {
                searchQuery.value = intent.query
                _uiState.update { it.copy(searchQuery = intent.query) }
            }
            is AddFriendIntent.SendFriendRequest -> viewModelScope.launch {
                try {
                    friendRepository.sendFriendRequest(currentUserId, intent.userId)
                    _uiState.update { it.copy(status = AddFriendUiState.Status.Idle) }
                } catch (e: Exception) {
                    _uiState.update {
                        it.copy(status = AddFriendUiState.Status.Error("Failed to send friend request"))
                    }
                }
            }
            is AddFriendIntent.AcceptFriendRequest -> viewModelScope.launch {
                try {
                    friendRepository.acceptFriendRequest(currentUserId, intent.userId)
                    _uiState.update { it.copy(status = AddFriendUiState.Status.Idle) }
                } catch (e: Exception) {
                    _uiState.update {
                        it.copy(status = AddFriendUiState.Status.Error("Failed to accept friend request"))
                    }
                }
            }
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
                    }
                    .toList()
            }.collect { results ->
                _uiState.update { it.copy(results = results) }
            }
        }
    }
}

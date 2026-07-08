package com.ilyne.helloszigetkmp.presentation.feature.addfriend

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ilyne.helloszigetkmp.data.repository.FriendRepository
import com.ilyne.helloszigetkmp.data.repository.UserRepository
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
)

sealed class AddFriendIntent {
    data class SearchQueryChanged(val query: String) : AddFriendIntent()
    data class SendFriendRequest(val userId: String) : AddFriendIntent()
    data class AcceptFriendRequest(val userId: String) : AddFriendIntent()
}

class AddFriendViewModel(
    private val friendRepository: FriendRepository,
    private val userRepository: UserRepository,
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")

    private val _uiState = MutableStateFlow(AddFriendUiState())
    val uiState = _uiState.asStateFlow()

    private var currentUserId: String? = null

    init {
        viewModelScope.launch {
            currentUserId = userRepository.getCurrentUser()?.id
            observeResults()
        }
    }

    fun onIntent(intent: AddFriendIntent) {
        when (intent) {
            is AddFriendIntent.SearchQueryChanged -> {
                searchQuery.value = intent.query
                _uiState.update { it.copy(searchQuery = intent.query) }
            }
            is AddFriendIntent.SendFriendRequest -> viewModelScope.launch {
                val currentUserId = currentUserId ?: return@launch
                friendRepository.sendFriendRequest(currentUserId, intent.userId)
            }
            is AddFriendIntent.AcceptFriendRequest -> viewModelScope.launch {
                val currentUserId = currentUserId ?: return@launch
                friendRepository.acceptFriendRequest(currentUserId, intent.userId)
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
                    .filter { query.isBlank() || it.name.contains(query, ignoreCase = true) }
                    .map { user ->
                        val status = when (user.id) {
                            in friendIds -> FriendshipStatus.FRIEND
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

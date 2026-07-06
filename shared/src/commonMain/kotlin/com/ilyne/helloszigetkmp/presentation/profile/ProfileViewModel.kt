package com.ilyne.helloszigetkmp.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ilyne.helloszigetkmp.data.repository.FriendRepository
import com.ilyne.helloszigetkmp.data.repository.UserRepository
import com.ilyne.helloszigetkmp.domain.model.User
import com.ilyne.helloszigetkmp.util.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


data class ProfileUiState(
    val name: String? = null,
    val friends: List<User> = emptyList(),
    val friendRequests: List<User> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

sealed class ProfileIntent {
    object AddFriend : ProfileIntent()

    data class AcceptFriendRequest(
        val friendId: String
    ) : ProfileIntent()

    data class DeclineFriendRequest(
        val friendId: String
    ) : ProfileIntent()

    data class ViewFriend(
        val friendId: String
    ) : ProfileIntent()
}

class ProfileViewModel(
    private val friendRepository: FriendRepository,
    private val userRepository: UserRepository,
) : ViewModel()  {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState = _uiState.asStateFlow()

    private lateinit var currentUser: User

    init {
        viewModelScope.launch {
            try {
                currentUser = userRepository.getCurrentUser() ?: run {
                    Logger.e("findme", "currentUser is null")
                    // need error handling
                    return@launch
                }

                _uiState.update {
                    it.copy(name = currentUser.name)
                }

                friendRepository.refresh()
            } catch (e: Exception) {

            }
        }
        observeFriends()
        observeFriendRequests()
    }

    fun onIntent(intent: ProfileIntent) {
        when (intent) {
            ProfileIntent.AddFriend -> TODO()
            is ProfileIntent.AcceptFriendRequest -> TODO()
            is ProfileIntent.DeclineFriendRequest -> TODO()
            is ProfileIntent.ViewFriend -> TODO()
        }
    }

    private fun observeFriends() {
        viewModelScope.launch {
            friendRepository.observeFriends().collect { friends ->
                _uiState.update { it.copy(friends = friends) }
            }
        }
    }

    private fun observeFriendRequests() {
        viewModelScope.launch {
            friendRepository.observeFriendRequests().collect { friendRequests ->
                _uiState.update { it.copy(friendRequests = friendRequests) }
            }
        }
    }
}

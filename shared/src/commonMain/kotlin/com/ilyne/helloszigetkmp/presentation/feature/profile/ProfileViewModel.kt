package com.ilyne.helloszigetkmp.presentation.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ilyne.helloszigetkmp.data.repository.ArtistRepository
import com.ilyne.helloszigetkmp.data.repository.FriendRepository
import com.ilyne.helloszigetkmp.data.repository.UserRepository
import com.ilyne.helloszigetkmp.data.sync.UsersSyncService
import com.ilyne.helloszigetkmp.domain.model.User
import com.ilyne.helloszigetkmp.domain.usecase.GetLikedArtistCountUseCase
import com.ilyne.helloszigetkmp.util.Logger
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


data class ProfileUiState(
    val name: String? = null,
    val friends: List<User> = emptyList(),
    val friendRequests: List<User> = emptyList(),
    val likedArtistCount: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null
)

sealed class ProfileEffect {
    data object NavigateToAddFriend : ProfileEffect()
    object Logout : ProfileEffect()
}

sealed class ProfileIntent {
    object AddFriend : ProfileIntent()
    object Logout: ProfileIntent()

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
    private val artistRepository: ArtistRepository,
    private val userRepository: UserRepository,
    private val usersSyncService: UsersSyncService,
    private val getLikedArtistCountUseCase: GetLikedArtistCountUseCase,
) : ViewModel()  {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<ProfileEffect>()
    val effects = _effects.asSharedFlow()

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

                if (usersSyncService.awaitSuccessfulSync()) {
                    _uiState.update { it.copy(isLoading = true) }
                    friendRepository.refresh()
                    artistRepository.refresh()
                    _uiState.update { it.copy(isLoading = false) }
                } else {
                    Logger.e("findme", "users sync failed, skipping friends refresh")
                }
            } catch (e: Exception) {

            }
        }
        observeFriends()
        observeFriendRequests()
        observeLikedArtists()
    }

    private fun observeLikedArtists() {
        viewModelScope.launch {
            getLikedArtistCountUseCase.invoke().collect { likedArtistCount ->
                _uiState.update { it.copy(likedArtistCount = likedArtistCount) }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            friendRepository.refresh()
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun onIntent(intent: ProfileIntent) {
        when (intent) {
            ProfileIntent.AddFriend -> viewModelScope.launch {
                _effects.emit(ProfileEffect.NavigateToAddFriend)
            }
            is ProfileIntent.AcceptFriendRequest -> viewModelScope.launch {
                friendRepository.acceptFriendRequest(currentUser.id, intent.friendId)
            }
            is ProfileIntent.DeclineFriendRequest -> viewModelScope.launch {
                friendRepository.declineFriendRequest(currentUser.id, intent.friendId)
            }
            is ProfileIntent.ViewFriend -> {

            }
            is ProfileIntent.Logout -> viewModelScope.launch {
                _effects.emit(ProfileEffect.Logout)
            }
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

package com.ilyne.helloszigetkmp.presentation.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ilyne.helloszigetkmp.core.api.SzigetApiService
import com.ilyne.helloszigetkmp.core.auth.CurrentUserProvider
import com.ilyne.helloszigetkmp.core.media.DeviceImage
import com.ilyne.helloszigetkmp.core.repository.ArtistRepository
import com.ilyne.helloszigetkmp.core.repository.FriendRepository
import com.ilyne.helloszigetkmp.core.repository.UserRepository
import com.ilyne.helloszigetkmp.core.sync.UsersSyncService
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
    val imageUrl: String? = null,
    // A "file://" URL for the on-disk cached copy of a just-picked photo (see DeviceImage.localUri),
    // shown immediately in place of `picture` while the upload is in flight. Cleared on upload
    // failure so the avatar reverts to `picture`; left in place (and `picture` updated) on
    // success, so there's no flicker.
    val pendingImageUrl: String? = null,
    val friends: List<User> = emptyList(),
    val friendRequests: List<User> = emptyList(),
    val likedArtistCount: Int = 0,
    val isLoading: Boolean = false,
    val isUploadingImage: Boolean = false,
    val error: String? = null,
    val removeFriendAlert: User? = null,
    val showLogoutAlert: Boolean = false,
)

sealed class ProfileEffect {
    data object NavigateToAddFriend : ProfileEffect()

    object Logout : ProfileEffect()
}

sealed class ProfileIntent {
    object AddFriend : ProfileIntent()

    data class PhotoPicked(
        val image: DeviceImage?,
    ) : ProfileIntent()

    object LogoutClicked : ProfileIntent()

    object ConfirmLogout : ProfileIntent()

    object DismissLogoutAlert : ProfileIntent()

    data class AcceptFriendRequest(
        val friendId: String,
    ) : ProfileIntent()

    data class DeclineFriendRequest(
        val friendId: String,
    ) : ProfileIntent()

    data class ViewFriend(
        val friendId: String,
    ) : ProfileIntent()

    object DismissRemoveFriendAlert : ProfileIntent()

    object RemoveFriendClicked : ProfileIntent()
}

class ProfileViewModel(
    private val friendRepository: FriendRepository,
    private val artistRepository: ArtistRepository,
    private val userRepository: UserRepository,
    private val usersSyncService: UsersSyncService,
    private val getLikedArtistCountUseCase: GetLikedArtistCountUseCase,
    private val api: SzigetApiService,
    currentUserProvider: CurrentUserProvider,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<ProfileEffect>()
    val effects = _effects.asSharedFlow()

    // Nullable rather than CurrentUserProvider.currentUser (which throws): this ViewModel can be
    // constructed mid-navigation concurrently with a session invalidation clearing the provider
    // (e.g. a background token refresh failing right as the user taps into Profile). In that
    // case the screen is about to be replaced by Login anyway, so degrade gracefully instead of
    // crashing on construction.
    private val currentUser: User? = currentUserProvider.currentUserOrNull

    init {
        val user = currentUser
        if (user == null) {
            Logger.e("ProfileViewModel", "init: no current user, session was likely invalidated during navigation")
            _uiState.update { it.copy(isLoading = false, error = "Session expired. Please log in again.") }
        } else {
            viewModelScope.launch {
                try {
                    _uiState.update {
                        it.copy(name = user.name, imageUrl = user.imageUrl)
                    }

                    if (usersSyncService.awaitSuccessfulSync()) {
                        _uiState.update { it.copy(isLoading = true) }
                        artistRepository.refresh(force = false)
                        friendRepository.refresh(force = false)
                        _uiState.update { it.copy(isLoading = false) }
                    } else {
                        Logger.e("ProfileViewModel", "init: users sync failed, skipping friends refresh")
                    }
                } catch (e: Exception) {
                    Logger.e("ProfileViewModel", "init: failed to load profile", e)
                    _uiState.update { it.copy(isLoading = false, error = "Failed to load profile") }
                }
            }
            observeFriends()
            observeFriendRequests()
            observeLikedArtists()
        }
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
            friendRepository.refresh(force = true)
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private fun uploadPhoto(image: DeviceImage) {
        viewModelScope.launch {
            _uiState.update { it.copy(pendingImageUrl = image.localUri, isUploadingImage = true, error = null) }
            try {
                val user = userRepository.uploadProfilePicture(
                    api = api,
                    bytes = image.bytes,
                    contentType = image.contentType,
                )
                _uiState.update { it.copy(isUploadingImage = false, imageUrl = user.imageUrl) }
            } catch (e: Exception) {
                Logger.e("ProfileViewModel", "Failed to upload photo", e)
                _uiState.update {
                    it.copy(isUploadingImage = false, pendingImageUrl = null, error = "Failed to upload photo")
                }
            }
        }
    }

    fun onIntent(intent: ProfileIntent) {
        when (intent) {
            ProfileIntent.AddFriend -> {
                viewModelScope.launch {
                    _effects.emit(ProfileEffect.NavigateToAddFriend)
                }
            }

            is ProfileIntent.PhotoPicked -> {
                intent.image?.let { uploadPhoto(it) }
            }

            is ProfileIntent.AcceptFriendRequest -> {
                viewModelScope.launch {
                    val userId = currentUser?.id ?: return@launch
                    try {
                        friendRepository.acceptFriendRequest(userId, intent.friendId)
                    } catch (e: Exception) {
                        Logger.e("ProfileViewModel", "Failed to accept friend request", e)
                        _uiState.update { it.copy(error = "Failed to accept friend request") }
                    }
                }
            }

            is ProfileIntent.DeclineFriendRequest -> {
                viewModelScope.launch {
                    val userId = currentUser?.id ?: return@launch
                    try {
                        friendRepository.declineFriendRequest(userId, intent.friendId)
                    } catch (e: Exception) {
                        Logger.e("ProfileViewModel", "Failed to decline friend request", e)
                        _uiState.update { it.copy(error = "Failed to decline friend request") }
                    }
                }
            }

            is ProfileIntent.ViewFriend -> {
                val friend = _uiState.value.friends.find { it.id == intent.friendId }
                _uiState.update { it.copy(removeFriendAlert = friend) }
            }

            ProfileIntent.LogoutClicked -> {
                _uiState.update { it.copy(showLogoutAlert = true) }
            }

            ProfileIntent.DismissLogoutAlert -> {
                _uiState.update { it.copy(showLogoutAlert = false) }
            }

            ProfileIntent.ConfirmLogout -> {
                _uiState.update { it.copy(showLogoutAlert = false) }
                viewModelScope.launch {
                    _effects.emit(ProfileEffect.Logout)
                }
            }

            ProfileIntent.DismissRemoveFriendAlert -> {
                _uiState.update { it.copy(removeFriendAlert = null) }
            }

            ProfileIntent.RemoveFriendClicked -> {
                val friend = _uiState.value.removeFriendAlert ?: return
                _uiState.update { it.copy(removeFriendAlert = null) }
                viewModelScope.launch {
                    val userId = currentUser?.id ?: return@launch
                    try {
                        friendRepository.removeFriend(userId, friend.id)
                    } catch (e: Exception) {
                        Logger.e("ProfileViewModel", "Failed to remove friend", e)
                        _uiState.update { it.copy(error = "Failed to remove friend") }
                    }
                }
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

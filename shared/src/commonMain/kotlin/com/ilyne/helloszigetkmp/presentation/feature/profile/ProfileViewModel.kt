package com.ilyne.helloszigetkmp.presentation.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ilyne.helloszigetkmp.core.api.SzigetApiService
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
    data class PhotoPicked(val image: DeviceImage?) : ProfileIntent()
    object LogoutClicked : ProfileIntent()
    object ConfirmLogout : ProfileIntent()
    object DismissLogoutAlert : ProfileIntent()

    data class AcceptFriendRequest(
        val friendId: String
    ) : ProfileIntent()

    data class DeclineFriendRequest(
        val friendId: String
    ) : ProfileIntent()

    data class ViewFriend(
        val friendId: String
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
                    it.copy(name = currentUser.name, imageUrl = currentUser.imageUrl)
                }

                if (usersSyncService.awaitSuccessfulSync()) {
                    _uiState.update { it.copy(isLoading = true) }
                    friendRepository.refresh(force = false)
                    artistRepository.refresh(force = false)
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
                _uiState.update {
                    it.copy(isUploadingImage = false, pendingImageUrl = null, error = "Failed to upload photo")
                }
            }
        }
    }

    fun onIntent(intent: ProfileIntent) {
        when (intent) {
            ProfileIntent.AddFriend -> viewModelScope.launch {
                _effects.emit(ProfileEffect.NavigateToAddFriend)
            }
            is ProfileIntent.PhotoPicked -> intent.image?.let { uploadPhoto(it) }
            is ProfileIntent.AcceptFriendRequest -> viewModelScope.launch {
                friendRepository.acceptFriendRequest(currentUser.id, intent.friendId)
            }
            is ProfileIntent.DeclineFriendRequest -> viewModelScope.launch {
                friendRepository.declineFriendRequest(currentUser.id, intent.friendId)
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
                    try {
                        friendRepository.removeFriend(currentUser.id, friend.id)
                    } catch (e: Exception) {
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

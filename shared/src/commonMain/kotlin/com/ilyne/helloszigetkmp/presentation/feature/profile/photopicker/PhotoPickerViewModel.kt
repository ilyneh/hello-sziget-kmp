package com.ilyne.helloszigetkmp.presentation.feature.profile.photopicker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ilyne.helloszigetkmp.core.api.SzigetApiService
import com.ilyne.helloszigetkmp.core.media.DevicePhoto
import com.ilyne.helloszigetkmp.core.media.DevicePhotoLibrary
import com.ilyne.helloszigetkmp.core.media.PhotoAccessStatus
import com.ilyne.helloszigetkmp.core.repository.UserRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PhotoPickerUiState(
    val accessStatus: PhotoAccessStatus = PhotoAccessStatus.NotDetermined,
    val photos: List<DevicePhoto> = emptyList(),
    val isLoading: Boolean = false,
    val uploading: Boolean = false,
    val error: String? = null,
)

sealed class PhotoPickerIntent {
    object Enter : PhotoPickerIntent()
    data class AndroidPermissionResult(val granted: Boolean) : PhotoPickerIntent()
    data class PhotoTapped(val id: String) : PhotoPickerIntent()
    object ManageClicked : PhotoPickerIntent()
    object Retry : PhotoPickerIntent()
    object OpenSettingsClicked : PhotoPickerIntent()
}

sealed class PhotoPickerEffect {
    data class Dismiss(val newPictureUrl: String?) : PhotoPickerEffect()

    // Android can't prompt for a runtime permission from the ViewModel — the screen must
    // launch the request itself via rememberLauncherForActivityResult.
    object RequestAndroidPermission : PhotoPickerEffect()
}

class PhotoPickerViewModel(
    private val devicePhotoLibrary: DevicePhotoLibrary,
    private val userRepository: UserRepository,
    private val api: SzigetApiService,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PhotoPickerUiState())
    val uiState = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<PhotoPickerEffect>()
    val effects = _effects.asSharedFlow()

    fun onIntent(intent: PhotoPickerIntent) {
        when (intent) {
            PhotoPickerIntent.Enter -> checkAccessAndLoad()
            is PhotoPickerIntent.AndroidPermissionResult -> onAndroidPermissionResult(intent.granted)
            is PhotoPickerIntent.PhotoTapped -> uploadPhoto(intent.id)
            PhotoPickerIntent.ManageClicked -> manageAccess()
            PhotoPickerIntent.Retry -> checkAccessAndLoad()
            PhotoPickerIntent.OpenSettingsClicked -> devicePhotoLibrary.openAppSettings()
        }
    }

    private fun checkAccessAndLoad() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            var status = devicePhotoLibrary.getAccessStatus()
            if (status == PhotoAccessStatus.NotDetermined) {
                status = devicePhotoLibrary.requestAccess()
            }
            _uiState.update { it.copy(accessStatus = status) }

            when (status) {
                PhotoAccessStatus.NotDetermined -> {
                    _uiState.update { it.copy(isLoading = false) }
                    _effects.emit(PhotoPickerEffect.RequestAndroidPermission)
                }
                PhotoAccessStatus.Denied -> {
                    _uiState.update { it.copy(isLoading = false) }
                }
                PhotoAccessStatus.Full, PhotoAccessStatus.Limited -> loadPhotos()
            }
        }
    }

    private fun onAndroidPermissionResult(granted: Boolean) {
        viewModelScope.launch {
            val status = devicePhotoLibrary.getAccessStatus()
            _uiState.update { it.copy(accessStatus = status) }
            if (granted && status != PhotoAccessStatus.Denied) {
                loadPhotos()
            }
        }
    }

    private fun manageAccess() {
        viewModelScope.launch {
            devicePhotoLibrary.presentManageAccess()
            checkAccessAndLoad()
        }
    }

    private suspend fun loadPhotos() {
        _uiState.update { it.copy(isLoading = true) }
        val photos = devicePhotoLibrary.loadPhotos()
        _uiState.update { it.copy(photos = photos, isLoading = false) }
    }

    private fun uploadPhoto(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(uploading = true, error = null) }
            try {
                val image = devicePhotoLibrary.loadFullImage(id)
                val user = userRepository.uploadProfilePicture(
                    api = api,
                    bytes = image.bytes,
                    contentType = image.contentType,
                )
                _uiState.update { it.copy(uploading = false) }
                _effects.emit(PhotoPickerEffect.Dismiss(newPictureUrl = user.picture))
            } catch (e: Exception) {
                _uiState.update { it.copy(uploading = false, error = "Failed to upload photo") }
            }
        }
    }
}

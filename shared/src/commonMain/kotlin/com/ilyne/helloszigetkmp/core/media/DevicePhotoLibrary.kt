package com.ilyne.helloszigetkmp.core.media

enum class PhotoAccessStatus {
    NotDetermined,
    Full,
    Limited,
    Denied,
}

data class DevicePhoto(
    val id: String,
    val thumbnail: ByteArray,
)

data class DeviceImage(
    val bytes: ByteArray,
    val contentType: String,
)

expect class DevicePhotoLibrary() {
    suspend fun getAccessStatus(): PhotoAccessStatus
    suspend fun requestAccess(): PhotoAccessStatus
    suspend fun loadPhotos(): List<DevicePhoto>
    suspend fun loadFullImage(id: String): DeviceImage

    // Opens the OS's native "manage selected photos" flow, if one is available on this
    // platform/OS version. No-op otherwise; callers should offer openAppSettings() as a fallback.
    suspend fun presentManageAccess()

    fun openAppSettings()
}

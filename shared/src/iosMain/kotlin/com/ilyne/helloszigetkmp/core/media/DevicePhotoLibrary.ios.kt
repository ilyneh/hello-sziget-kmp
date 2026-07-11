package com.ilyne.helloszigetkmp.core.media

import kotlinx.coroutines.suspendCancellableCoroutine

// Implemented by calling into Swift via provided callback hooks, following the same
// bridge pattern as GoogleAuthProvider.ios.kt: ContentView.swift sets these before the
// KMP root is presented, wiring the native PHPhotoLibrary/PHImageManager APIs through.
var photoLibraryAccessStatusHandler: (() -> PhotoAccessStatus)? = null
var photoLibraryRequestAccessHandler: ((onResult: (PhotoAccessStatus) -> Unit) -> Unit)? = null
var photoLibraryLoadPhotosHandler: ((onResult: (List<DevicePhoto>) -> Unit) -> Unit)? = null
var photoLibraryLoadFullImageHandler: ((id: String, onResult: (ByteArray?, String?) -> Unit) -> Unit)? = null
var photoLibraryManageAccessHandler: ((onComplete: () -> Unit) -> Unit)? = null
var photoLibraryOpenSettingsHandler: (() -> Unit)? = null

actual class DevicePhotoLibrary actual constructor() {
    actual suspend fun getAccessStatus(): PhotoAccessStatus =
        photoLibraryAccessStatusHandler?.invoke() ?: PhotoAccessStatus.NotDetermined

    actual suspend fun requestAccess(): PhotoAccessStatus {
        val handler = photoLibraryRequestAccessHandler ?: return PhotoAccessStatus.Denied
        return suspendCancellableCoroutine { continuation ->
            handler { status -> continuation.resume(status, onCancellation = null) }
        }
    }

    actual suspend fun loadPhotos(): List<DevicePhoto> {
        val handler = photoLibraryLoadPhotosHandler ?: return emptyList()
        return suspendCancellableCoroutine { continuation ->
            handler { photos -> continuation.resume(photos, onCancellation = null) }
        }
    }

    actual suspend fun loadFullImage(id: String): DeviceImage {
        val handler = photoLibraryLoadFullImageHandler
            ?: error("Photo library handler not configured on iOS")
        return suspendCancellableCoroutine { continuation ->
            handler(id) { bytes, contentType ->
                if (bytes != null) {
                    continuation.resume(
                        DeviceImage(bytes = bytes, contentType = contentType ?: "image/jpeg"),
                        onCancellation = null,
                    )
                } else {
                    continuation.resumeWith(Result.failure(IllegalStateException("Unable to load photo $id")))
                }
            }
        }
    }

    actual suspend fun presentManageAccess() {
        val handler = photoLibraryManageAccessHandler ?: return
        suspendCancellableCoroutine<Unit> { continuation ->
            handler { continuation.resume(Unit, onCancellation = null) }
        }
    }

    actual fun openAppSettings() {
        photoLibraryOpenSettingsHandler?.invoke()
    }
}

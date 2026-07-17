package com.ilyne.helloszigetkmp.core.media

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

// Set from ContentView.swift (ProfileImagePickerBridge), which presents a PHPickerViewController,
// writes the (possibly downscaled) picked image to a cache file, and calls back with the raw
// bytes + MIME type + "file://" path to that cached copy (or null if cancelled).
var profileImagePickerHandler: ((onResult: (ByteArray?, String?, String?) -> Unit) -> Unit)? = null

private suspend fun pickProfileImage(): DeviceImage? {
    val handler = profileImagePickerHandler ?: return null
    return suspendCancellableCoroutine { continuation ->
        handler { bytes, contentType, localUri ->
            val image = if (bytes != null && localUri != null) {
                DeviceImage(bytes = bytes, contentType = contentType ?: "image/jpeg", localUri = localUri)
            } else {
                null
            }
            continuation.resume(image) { _, _, _ -> }
        }
    }
}

@Composable
actual fun rememberProfileImagePicker(onPickImage: (DeviceImage?) -> Unit): () -> Unit {
    val scope = rememberCoroutineScope()
    val currentOnPicked by rememberUpdatedState(onPickImage)

    return {
        scope.launch {
            currentOnPicked(pickProfileImage())
        }
    }
}

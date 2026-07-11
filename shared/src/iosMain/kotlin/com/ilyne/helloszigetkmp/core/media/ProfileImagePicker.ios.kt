package com.ilyne.helloszigetkmp.core.media

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

// Set from ContentView.swift (PhotoPickerBridge), which presents a PHPickerViewController
// and calls back with the picked image's raw bytes + MIME type (or null if cancelled).
var profileImagePickerHandler: ((onResult: (ByteArray?, String?) -> Unit) -> Unit)? = null

private suspend fun pickProfileImage(): DeviceImage? {
    val handler = profileImagePickerHandler ?: return null
    return suspendCancellableCoroutine { continuation ->
        handler { bytes, contentType ->
            val image = bytes?.let { DeviceImage(bytes = it, contentType = contentType ?: "image/jpeg") }
            continuation.resume(image, onCancellation = null)
        }
    }
}

@Composable
actual fun rememberProfileImagePicker(onPicked: (DeviceImage?) -> Unit): () -> Unit {
    val scope = rememberCoroutineScope()
    val currentOnPicked by rememberUpdatedState(onPicked)

    return {
        scope.launch {
            currentOnPicked(pickProfileImage())
        }
    }
}

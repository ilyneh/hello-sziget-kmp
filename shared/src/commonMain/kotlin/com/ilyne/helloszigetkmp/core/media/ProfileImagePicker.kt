package com.ilyne.helloszigetkmp.core.media

import androidx.compose.runtime.Composable

data class DeviceImage(
    val bytes: ByteArray,
    val contentType: String,
    // A "file://" URL pointing at a cached on-disk copy of `bytes`, so the picked photo can be
    // displayed (e.g. via Coil's AsyncImage) without holding the raw bytes in UI state.
    val localUri: String,
)

// Keep in sync with the downscaling threshold used in ContentView.swift's picker bridge.
const val MAX_PROFILE_IMAGE_BYTES: Long = 10L * 1024 * 1024

// Launches the OS's native single-image picker (Android Photo Picker / iOS PHPickerViewController).
// Both run out-of-process and grant access only to the picked item, so no runtime permission
// is required. Returns a launcher; onPicked is called with null if the user cancels.
@Composable
expect fun rememberProfileImagePicker(onPicked: (DeviceImage?) -> Unit): () -> Unit

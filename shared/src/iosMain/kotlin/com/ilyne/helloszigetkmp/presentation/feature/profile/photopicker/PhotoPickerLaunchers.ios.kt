package com.ilyne.helloszigetkmp.presentation.feature.profile.photopicker

import androidx.compose.runtime.Composable

// iOS drives both permission requests and the "manage selection" flow directly through
// DevicePhotoLibrary's suspend APIs (backed by PHPhotoLibrary), so no Activity-style
// launcher is needed here.
@Composable
actual fun rememberPhotoPermissionLauncher(onResult: (granted: Boolean) -> Unit): () -> Unit = {}

@Composable
actual fun rememberManagePhotosLauncher(onResult: () -> Unit): (() -> Unit)? = null

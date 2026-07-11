package com.ilyne.helloszigetkmp.presentation.feature.profile.photopicker

import androidx.compose.runtime.Composable

// Android needs an Activity-driven ActivityResultLauncher to show the runtime permission
// dialog; iOS drives the whole request through DevicePhotoLibrary's suspend APIs instead,
// so its actual is a no-op.
@Composable
expect fun rememberPhotoPermissionLauncher(onResult: (granted: Boolean) -> Unit): () -> Unit

// Android 14+ can jump straight to the "choose more photos" system UI via an
// ActivityResultLauncher; below that (or on iOS, where DevicePhotoLibrary.presentManageAccess()
// already drives the native flow directly) this returns null and callers fall back to that.
@Composable
expect fun rememberManagePhotosLauncher(onResult: () -> Unit): (() -> Unit)?

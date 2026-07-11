package com.ilyne.helloszigetkmp.presentation.feature.profile.photopicker

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState

@Composable
actual fun rememberPhotoPermissionLauncher(onResult: (granted: Boolean) -> Unit): () -> Unit {
    val currentOnResult by rememberUpdatedState(onResult)
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        currentOnResult(grants.values.any { it })
    }
    return {
        val permissions = if (Build.VERSION.SDK_INT >= 34) {
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
        } else if (Build.VERSION.SDK_INT >= 33) {
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        launcher.launch(permissions)
    }
}

@Composable
actual fun rememberManagePhotosLauncher(onResult: () -> Unit): (() -> Unit)? {
    val currentOnResult by rememberUpdatedState(onResult)
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { currentOnResult() }

    if (Build.VERSION.SDK_INT < 34) return null

    return {
        launcher.launch(Intent(MediaStore.ACTION_PICK_IMAGES_SETTINGS))
    }
}

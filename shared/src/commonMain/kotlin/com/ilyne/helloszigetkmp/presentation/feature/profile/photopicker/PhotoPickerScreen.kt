package com.ilyne.helloszigetkmp.presentation.feature.profile.photopicker

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.ilyne.helloszigetkmp.core.media.DevicePhoto
import com.ilyne.helloszigetkmp.core.media.PhotoAccessStatus
import com.ilyne.helloszigetkmp.presentation.component.header.ModalHeader
import com.ilyne.helloszigetkmp.presentation.theme.AppTheme
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoPickerScreen(
    onDismiss: () -> Unit,
    onPhotoUploaded: (newPictureUrl: String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = koinViewModel<PhotoPickerViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val requestPermission = rememberPhotoPermissionLauncher { granted ->
        viewModel.onIntent(PhotoPickerIntent.AndroidPermissionResult(granted))
    }
    val launchManagePhotos = rememberManagePhotosLauncher {
        viewModel.onIntent(PhotoPickerIntent.Retry)
    }

    LaunchedEffect(Unit) {
        viewModel.onIntent(PhotoPickerIntent.Enter)
    }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is PhotoPickerEffect.Dismiss -> onPhotoUploaded(effect.newPictureUrl)
                PhotoPickerEffect.RequestAndroidPermission -> requestPermission()
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        PhotoPickerContent(
            uiState = uiState,
            onPhotoTapped = { viewModel.onIntent(PhotoPickerIntent.PhotoTapped(it)) },
            onManageClicked = {
                if (launchManagePhotos != null) launchManagePhotos() else viewModel.onIntent(PhotoPickerIntent.ManageClicked)
            },
            onOpenSettingsClicked = { viewModel.onIntent(PhotoPickerIntent.OpenSettingsClicked) },
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    }
}

@Composable
private fun PhotoPickerContent(
    uiState: PhotoPickerUiState,
    onPhotoTapped: (String) -> Unit,
    onManageClicked: () -> Unit,
    onOpenSettingsClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        ModalHeader(
            text = "Choose Photo",
            modifier = Modifier.fillMaxWidth(),
            trailingContent = {
                if (uiState.accessStatus == PhotoAccessStatus.Limited || uiState.accessStatus == PhotoAccessStatus.Full) {
                    TextButton(onClick = onManageClicked) {
                        Text("Manage")
                    }
                }
            },
        )

        when {
            uiState.accessStatus == PhotoAccessStatus.Denied -> {
                PhotoPickerMessage(
                    message = "Sziget needs access to your photos to set a profile picture.",
                    actionLabel = "Open Settings",
                    onAction = onOpenSettingsClicked,
                )
            }
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxWidth().height(240.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            uiState.photos.isEmpty() -> {
                PhotoPickerMessage(message = "No photos available.")
            }
            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth().height(360.dp),
                ) {
                    items(items = uiState.photos, key = { it.id }) { photo ->
                        PhotoGridItem(
                            photo = photo,
                            enabled = !uiState.uploading,
                            onClick = { onPhotoTapped(photo.id) },
                        )
                    }
                }
            }
        }

        uiState.error?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                textAlign = TextAlign.Center,
            )
        }

        if (uiState.uploading) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }
    }
}

@Composable
private fun PhotoGridItem(
    photo: DevicePhoto,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AsyncImage(
        model = photo.thumbnail,
        contentDescription = null,
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(enabled = enabled, onClick = onClick),
    )
}

@Composable
private fun PhotoPickerMessage(
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth().height(240.dp).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = message, textAlign = TextAlign.Center)
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction) {
                Text(actionLabel)
            }
        }
    }
}

@Preview
@Composable
private fun PhotoPickerContentPreview() {
    AppTheme {
        PhotoPickerContent(
            uiState = PhotoPickerUiState(),
            onPhotoTapped = {},
            onManageClicked = {},
            onOpenSettingsClicked = {},
        )
    }
}

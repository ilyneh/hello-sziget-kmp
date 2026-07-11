package com.ilyne.helloszigetkmp.core.media

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.Settings
import android.util.Size
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import java.io.ByteArrayOutputStream
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

actual class DevicePhotoLibrary actual constructor() : KoinComponent {
    private val context: Context by inject()

    actual suspend fun getAccessStatus(): PhotoAccessStatus {
        if (ContextCompat.checkSelfPermission(context, readImagesPermission()) == PackageManager.PERMISSION_GRANTED) {
            return PhotoAccessStatus.Full
        }
        if (Build.VERSION.SDK_INT >= 34 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) ==
                PackageManager.PERMISSION_GRANTED
        ) {
            return PhotoAccessStatus.Limited
        }
        return PhotoAccessStatus.Denied
    }

    // The permission dialog itself must be launched from an Activity via
    // rememberLauncherForActivityResult (see PhotoPickerScreen) — this only re-checks status
    // after that launcher has run.
    actual suspend fun requestAccess(): PhotoAccessStatus = getAccessStatus()

    actual suspend fun loadPhotos(): List<DevicePhoto> {
        val photos = mutableListOf<DevicePhoto>()
        val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(MediaStore.Images.Media._ID)
        context.contentResolver.query(
            collection,
            projection,
            null,
            null,
            "${MediaStore.Images.Media.DATE_ADDED} DESC",
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val uri = ContentUris.withAppendedId(collection, id)
                val thumbnail = runCatching { loadThumbnailBytes(uri) }.getOrNull() ?: continue
                photos += DevicePhoto(id = id.toString(), thumbnail = thumbnail)
            }
        }
        return photos
    }

    actual suspend fun loadFullImage(id: String): DeviceImage {
        val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id.toLong())
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: error("Unable to open photo $id")
        val contentType = context.contentResolver.getType(uri) ?: "image/jpeg"
        return DeviceImage(bytes = bytes, contentType = contentType)
    }

    actual suspend fun presentManageAccess() {
        if (Build.VERSION.SDK_INT >= 34) {
            val intent = Intent(MediaStore.ACTION_PICK_IMAGES_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            runCatching { context.startActivity(intent) }
        }
    }

    actual fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = "package:${context.packageName}".toUri()
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    private fun loadThumbnailBytes(uri: Uri): ByteArray {
        val bitmap: Bitmap = if (Build.VERSION.SDK_INT >= 29) {
            context.contentResolver.loadThumbnail(uri, Size(300, 300), null)
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        }
        return ByteArrayOutputStream().use { stream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream)
            stream.toByteArray()
        }
    }

    private fun readImagesPermission(): String =
        if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_IMAGES else Manifest.permission.READ_EXTERNAL_STORAGE
}

package com.ilyne.helloszigetkmp.core.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.sqrt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class RawImage(val bytes: ByteArray, val contentType: String)

@Composable
actual fun rememberProfileImagePicker(onPicked: (DeviceImage?) -> Unit): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentOnPicked by rememberUpdatedState(onPicked)

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri == null) {
            currentOnPicked(null)
            return@rememberLauncherForActivityResult
        }
        scope.launch {
            val image = withContext(Dispatchers.IO) {
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                bytes?.let {
                    RawImage(bytes = it, contentType = context.contentResolver.getType(uri) ?: "image/jpeg")
                        .downscaledIfNeeded()
                        .cacheToDisk(context)
                }
            }
            currentOnPicked(image)
        }
    }

    return {
        launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }
}

private fun RawImage.downscaledIfNeeded(): RawImage {
    if (bytes.size <= MAX_PROFILE_IMAGE_BYTES) return this

    val original = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return this
    val scale = sqrt(MAX_PROFILE_IMAGE_BYTES.toDouble() / bytes.size.toDouble()).coerceAtMost(1.0)
    val scaled = Bitmap.createScaledBitmap(
        original,
        (original.width * scale).toInt().coerceAtLeast(1),
        (original.height * scale).toInt().coerceAtLeast(1),
        true,
    )

    var quality = 90
    var output = scaled.toJpegBytes(quality)
    while (output.size > MAX_PROFILE_IMAGE_BYTES && quality > 10) {
        quality -= 10
        output = scaled.toJpegBytes(quality)
    }

    return RawImage(bytes = output, contentType = "image/jpeg")
}

private fun Bitmap.toJpegBytes(quality: Int): ByteArray =
    ByteArrayOutputStream().use { stream ->
        compress(Bitmap.CompressFormat.JPEG, quality, stream)
        stream.toByteArray()
    }

private fun RawImage.cacheToDisk(context: Context): DeviceImage {
    val dir = File(context.cacheDir, "profile_photos").apply { mkdirs() }
    dir.listFiles()?.forEach { it.delete() }
    val extension = if (contentType.contains("png")) "png" else "jpg"
    val file = File(dir, "pending_${System.currentTimeMillis()}.$extension")
    file.writeBytes(bytes)
    return DeviceImage(bytes = bytes, contentType = contentType, localUri = Uri.fromFile(file).toString())
}

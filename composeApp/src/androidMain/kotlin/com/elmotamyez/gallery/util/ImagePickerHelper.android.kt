package com.elmotamyez.gallery.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

private fun processImage(context: Context, uri: Uri, maxDimension: Int = 1200, quality: Int = 80): ByteArray? {
    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
    var bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return bytes

    // Apply EXIF orientation — camera images carry rotation metadata that BitmapFactory ignores
    val orientation = context.contentResolver.openInputStream(uri)?.use { stream ->
        ExifInterface(stream).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
    } ?: ExifInterface.ORIENTATION_NORMAL

    val degrees = when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90  -> 90f
        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
        else -> 0f
    }
    if (degrees != 0f) {
        val matrix = Matrix().apply { postRotate(degrees) }
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        bitmap.recycle()
        bitmap = rotated
    }

    // Scale down to maxDimension on the longest side
    val scale = minOf(1f, maxDimension.toFloat() / maxOf(bitmap.width, bitmap.height))
    val scaled = if (scale < 1f)
        Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt(), (bitmap.height * scale).toInt(), true)
    else bitmap

    val out = ByteArrayOutputStream()
    scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
    if (scaled !== bitmap) scaled.recycle()
    bitmap.recycle()
    return out.toByteArray()
}

@Composable
actual fun rememberImagePickerLauncher(onImagePicked: (ByteArray) -> Unit): () -> Unit {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        uris.forEach { u ->
            scope.launch(Dispatchers.IO) {
                processImage(context, u)?.let { onImagePicked(it) }
            }
        }
    }

    return { launcher.launch("image/*") }
}

/** Rotate a landscape image 90° CW to portrait. Returns original bytes unchanged if already portrait. */
actual fun rotateLandscapeToPortrait(bytes: ByteArray): ByteArray {
    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return bytes
    if (bitmap.width <= bitmap.height) {
        bitmap.recycle()
        return bytes
    }
    val matrix = Matrix().apply { postRotate(90f) }
    val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    bitmap.recycle()
    val out = ByteArrayOutputStream()
    rotated.compress(Bitmap.CompressFormat.JPEG, 85, out)
    rotated.recycle()
    return out.toByteArray()
}

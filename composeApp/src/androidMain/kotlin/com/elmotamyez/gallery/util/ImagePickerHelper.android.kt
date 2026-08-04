package com.elmotamyez.gallery.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

private fun compressImage(bytes: ByteArray, maxDimension: Int = 1200, quality: Int = 80): ByteArray {
    val original = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return bytes
    val scale = minOf(1f, maxDimension.toFloat() / maxOf(original.width, original.height))
    val bitmap = if (scale < 1f)
        Bitmap.createScaledBitmap(original, (original.width * scale).toInt(), (original.height * scale).toInt(), true)
    else original
    val out = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
    if (bitmap !== original) bitmap.recycle()
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
                val raw = context.contentResolver.openInputStream(u)?.use { it.readBytes() }
                raw?.let { onImagePicked(compressImage(it)) }
            }
        }
    }

    return { launcher.launch("image/*") }
}

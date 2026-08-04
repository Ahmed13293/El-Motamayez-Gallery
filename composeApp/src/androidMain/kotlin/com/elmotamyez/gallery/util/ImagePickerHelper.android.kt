package com.elmotamyez.gallery.util

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
actual fun rememberImagePickerLauncher(onImagePicked: (ByteArray) -> Unit): () -> Unit {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { u ->
            scope.launch(Dispatchers.IO) {
                val bytes = context.contentResolver.openInputStream(u)?.use { it.readBytes() }
                bytes?.let { onImagePicked(it) }
            }
        }
    }

    return { launcher.launch("image/*") }
}

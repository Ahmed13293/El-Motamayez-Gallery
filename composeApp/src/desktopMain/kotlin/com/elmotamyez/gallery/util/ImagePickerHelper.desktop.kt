package com.elmotamyez.gallery.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

@Composable
actual fun rememberImagePickerLauncher(onImagePicked: (ByteArray) -> Unit): () -> Unit {
    val scope = rememberCoroutineScope()
    return {
        scope.launch(Dispatchers.IO) {
            val chooser = JFileChooser()
            chooser.fileFilter = FileNameExtensionFilter("Images", "jpg", "jpeg", "png", "webp")
            chooser.isMultiSelectionEnabled = true
            if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                chooser.selectedFiles.forEach { file -> onImagePicked(file.readBytes()) }
            }
        }
    }
}

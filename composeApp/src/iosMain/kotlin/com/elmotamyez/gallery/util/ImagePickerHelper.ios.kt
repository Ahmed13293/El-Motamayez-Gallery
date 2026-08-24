package com.elmotamyez.gallery.util

import androidx.compose.runtime.Composable

@Composable
actual fun rememberImagePickerLauncher(onImagePicked: (ByteArray) -> Unit): () -> Unit = { }

actual fun rotateLandscapeToPortrait(bytes: ByteArray): ByteArray = bytes
actual fun rotateImage90CW(bytes: ByteArray): ByteArray = bytes

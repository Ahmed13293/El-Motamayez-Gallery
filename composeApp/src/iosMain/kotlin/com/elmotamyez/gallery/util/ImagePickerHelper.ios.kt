package com.elmotamyez.gallery.util

import androidx.compose.runtime.Composable

@Composable
actual fun rememberImagePickerLauncher(onImagePicked: (ByteArray) -> Unit): () -> Unit = { }

@Composable
actual fun rememberCameraLauncher(onImageCaptured: (ByteArray) -> Unit): (() -> Unit)? = null

actual fun rotateLandscapeToPortrait(bytes: ByteArray): ByteArray = bytes
actual fun rotateImage90CW(bytes: ByteArray): ByteArray = bytes

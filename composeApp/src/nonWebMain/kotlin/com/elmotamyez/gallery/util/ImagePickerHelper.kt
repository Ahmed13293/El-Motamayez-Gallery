package com.elmotamyez.gallery.util

import androidx.compose.runtime.Composable

@Composable
expect fun rememberImagePickerLauncher(onImagePicked: (ByteArray) -> Unit): () -> Unit

/** Returns [bytes] rotated 90° CW if the image is landscape (width > height), otherwise unchanged. */
expect fun rotateLandscapeToPortrait(bytes: ByteArray): ByteArray

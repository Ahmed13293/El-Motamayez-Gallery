package com.elmotamyez.gallery.util

import androidx.compose.runtime.Composable

@Composable
expect fun rememberImagePickerLauncher(onImagePicked: (ByteArray) -> Unit): () -> Unit

/** Returns a lambda that opens the device camera and delivers the captured image as [ByteArray].
 *  EXIF orientation is corrected automatically. Desktop returns null (no camera). */
@Composable
expect fun rememberCameraLauncher(onImageCaptured: (ByteArray) -> Unit): (() -> Unit)?

/** Returns [bytes] rotated 90° CW if the image is landscape (width > height), otherwise unchanged. */
expect fun rotateLandscapeToPortrait(bytes: ByteArray): ByteArray

/** Always rotates [bytes] 90° CW regardless of orientation. */
expect fun rotateImage90CW(bytes: ByteArray): ByteArray

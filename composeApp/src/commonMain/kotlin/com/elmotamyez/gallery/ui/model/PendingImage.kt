package com.elmotamyez.gallery.ui.model

sealed interface PendingImage {
    data class Remote(val url: String) : PendingImage
    class Local(val bytes: ByteArray) : PendingImage
}

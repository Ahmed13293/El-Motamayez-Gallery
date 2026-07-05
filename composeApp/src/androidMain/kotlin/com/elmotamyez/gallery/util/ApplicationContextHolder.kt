package com.elmotamyez.gallery.util

import android.content.Context

/** Application-scoped Context holder — set once in MainApplication.onCreate(). */
object ApplicationContextHolder {
    var context: Context? = null
}

package com.elmotamyez.gallery

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.CanvasBasedWindow
import com.elmotamyez.gallery.di.appModule
import org.koin.core.context.startKoin

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    startKoin { modules(appModule) }
    CanvasBasedWindow(canvasElementId = "ComposeTarget") {
        App()
    }
}

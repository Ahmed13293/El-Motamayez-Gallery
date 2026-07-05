package com.elmotamyez.gallery

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.elmotamyez.gallery.di.appModule
import org.koin.core.context.startKoin

fun main() = application {
    startKoin { modules(appModule) }
    Window(onCloseRequest = ::exitApplication, title = "El-Motamyez Gallery") {
        App()
    }
}

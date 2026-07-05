package com.elmotamyez.gallery

import androidx.compose.ui.window.ComposeUIViewController
import com.elmotamyez.gallery.di.appModule
import org.koin.core.context.startKoin

fun MainViewController() = ComposeUIViewController(
    configure = { startKoin { modules(appModule) } }
) { App() }

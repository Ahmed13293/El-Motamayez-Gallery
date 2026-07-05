package com.elmotamyez.gallery

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.elmotamyez.gallery.ui.screens.auth.AuthViewModel
import com.elmotamyez.gallery.ui.screens.auth.LoginScreen
import com.elmotamyez.gallery.ui.screens.main.MainScreen
import com.elmotamyez.gallery.ui.theme.AppTheme
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.SlideTransition
import org.koin.compose.KoinContext
import org.koin.compose.koinInject

@Composable
actual fun App() {
    AppTheme {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            KoinContext {
                val authVm: AuthViewModel = koinInject()
                val authState by authVm.uiState.collectAsState()

                if (authState.user == null) {
                    Navigator(LoginScreen()) { SlideTransition(it) }
                } else {
                    Navigator(MainScreen()) { SlideTransition(it) }
                }
            }
        }
    }
}

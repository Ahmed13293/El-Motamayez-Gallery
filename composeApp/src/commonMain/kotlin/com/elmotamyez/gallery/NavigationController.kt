package com.elmotamyez.gallery

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class NavigationController {
    private val _pendingTab = MutableStateFlow<String?>(null)
    val pendingTab = _pendingTab.asStateFlow()

    fun navigateTo(tab: String) { _pendingTab.value = tab }
    fun consume() { _pendingTab.value = null }
}

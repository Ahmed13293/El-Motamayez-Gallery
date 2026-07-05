package com.elmotamyez.gallery.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elmotamyez.gallery.data.model.User
import com.elmotamyez.gallery.data.model.UserRole
import com.elmotamyez.gallery.data.repository.AuthRepository
import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val KEY_USER_ID       = "session_user_id"
private const val KEY_USER_USERNAME = "session_username"
private const val KEY_USER_NAME     = "session_name"
private const val KEY_USER_ROLE     = "session_role"

data class AuthUiState(
    val isLoading: Boolean = false,
    val error: String?     = null,
    val user: User?        = null
)

class AuthViewModel(
    private val repo: AuthRepository
) : ViewModel() {

    private val settings = Settings()

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    val currentUser: User? get() = _uiState.value.user

    init {
        // Restore session from local storage on every app start
        val savedId = settings.getStringOrNull(KEY_USER_ID)
        if (savedId != null) {
            val user = User(
                id       = savedId,
                username = settings.getString(KEY_USER_USERNAME, ""),
                name     = settings.getString(KEY_USER_NAME, ""),
                role     = if (settings.getString(KEY_USER_ROLE, "user") == "admin")
                               UserRole.ADMIN else UserRole.USER
            )
            _uiState.value = AuthUiState(user = user)
        }
    }

    fun login(username: String, password: String) {
        if (username.isBlank() || password.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "يرجى إدخال اسم المستخدم وكلمة المرور")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val user = repo.login(username.trim(), password.trim())
                if (user != null) {
                    saveSession(user)
                    _uiState.value = _uiState.value.copy(isLoading = false, user = user)
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "اسم المستخدم أو كلمة المرور غير صحيحة"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "خطأ في الاتصال: ${e.message}"
                )
            }
        }
    }

    fun logout() {
        clearSession()
        _uiState.value = AuthUiState()
    }

    private fun saveSession(user: User) {
        settings.putString(KEY_USER_ID,       user.id)
        settings.putString(KEY_USER_USERNAME, user.username)
        settings.putString(KEY_USER_NAME,     user.name)
        settings.putString(KEY_USER_ROLE,     user.role.name.lowercase())
    }

    private fun clearSession() {
        settings.remove(KEY_USER_ID)
        settings.remove(KEY_USER_USERNAME)
        settings.remove(KEY_USER_NAME)
        settings.remove(KEY_USER_ROLE)
    }
}

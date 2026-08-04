package com.elmotamyez.gallery.ui.screens.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elmotamyez.gallery.data.model.Attendance
import com.elmotamyez.gallery.data.repository.AttendanceRepository
import com.elmotamyez.gallery.data.repository.UserSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AttendanceViewModel(private val repo: AttendanceRepository) : ViewModel() {

    private val _records   = MutableStateFlow<List<Attendance>>(emptyList())
    val records = _records.asStateFlow()

    private val _users     = MutableStateFlow<List<UserSummary>>(emptyList())
    val users = _users.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            runCatching {
                _records.value = repo.getAll()
                _users.value   = repo.getUsers()
            }.onFailure { _error.value = "خطأ في القراءة: ${it.message}" }
            _isLoading.value = false
        }
    }

    fun addRecord(userId: String, userName: String, checkIn: String, checkOut: String?) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            runCatching {
                repo.insertManual(userId, userName, checkIn, checkOut)
                _records.value = repo.getAll()
            }.onFailure { _error.value = "خطأ في الإضافة: ${it.message}" }
            _isLoading.value = false
        }
    }

    fun updateRecord(id: String, checkIn: String, checkOut: String?) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            runCatching {
                repo.updateRecord(id, checkIn, checkOut)
                _records.value = repo.getAll()
            }.onFailure { _error.value = "خطأ في التعديل: ${it.message}" }
            _isLoading.value = false
        }
    }

    fun testInsert() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            runCatching {
                repo.recordSignIn(userId = "test-user-id", userName = "اختبار")
                _records.value = repo.getAll()
            }.onFailure { _error.value = "خطأ في الإدخال: ${it.message}" }
            _isLoading.value = false
        }
    }
}

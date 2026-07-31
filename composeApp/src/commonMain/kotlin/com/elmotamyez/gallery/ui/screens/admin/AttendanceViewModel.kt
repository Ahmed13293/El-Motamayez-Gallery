package com.elmotamyez.gallery.ui.screens.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elmotamyez.gallery.data.model.Attendance
import com.elmotamyez.gallery.data.repository.AttendanceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AttendanceViewModel(private val repo: AttendanceRepository) : ViewModel() {

    private val _records   = MutableStateFlow<List<Attendance>>(emptyList())
    val records = _records.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _isLoading.value = true
            runCatching { _records.value = repo.getAll() }
                .onFailure { _error.value = it.message }
            _isLoading.value = false
        }
    }
}

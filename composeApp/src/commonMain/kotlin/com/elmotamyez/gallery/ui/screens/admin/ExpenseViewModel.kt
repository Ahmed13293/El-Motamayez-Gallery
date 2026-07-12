package com.elmotamyez.gallery.ui.screens.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elmotamyez.gallery.data.model.Expense
import com.elmotamyez.gallery.data.repository.ExpenseRepository
import com.elmotamyez.gallery.util.dateTimeString
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class ExpenseViewModel(private val repo: ExpenseRepository) : ViewModel() {

    private val _expenses = MutableStateFlow<List<Expense>>(emptyList())
    val expenses = _expenses.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving = _isSaving.asStateFlow()

    init { load() }

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    fun load() {
        viewModelScope.launch {
            runCatching { _expenses.value = repo.fetchAll() }
                .onFailure { _error.value = "fetchAll: ${it.message}" }
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    fun addExpense(type: String, amount: Double, note: String?, createdAt: String? = null, onDone: () -> Unit) {
        viewModelScope.launch {
            _isSaving.value = true
            val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            val nowIso = createdAt ?: dateTimeString(now.year, now.monthNumber, now.dayOfMonth, now.hour, now.minute, now.second)
            val expense = Expense(id = Uuid.random().toString(), type = type, amount = amount, note = note?.ifBlank { null }, createdAt = nowIso)
            _expenses.value = listOf(expense) + _expenses.value
            runCatching { repo.insert(expense) }
                .onFailure { _error.value = "insert: ${it.message}" }
            _isSaving.value = false
            onDone()
        }
    }

    fun updateExpense(expense: Expense, onDone: () -> Unit) {
        viewModelScope.launch {
            _isSaving.value = true
            _expenses.value = _expenses.value.map { if (it.id == expense.id) expense else it }
            runCatching { repo.update(expense) }
                .onFailure { _error.value = "update: ${it.message}" }
            _isSaving.value = false
            onDone()
        }
    }

    fun deleteExpense(id: String) {
        viewModelScope.launch {
            _expenses.value = _expenses.value.filter { it.id != id }
            runCatching { repo.delete(id) }
                .onFailure { _error.value = "delete: ${it.message}" }
        }
    }
}

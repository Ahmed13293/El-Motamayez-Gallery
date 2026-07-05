package com.elmotamyez.gallery.ui.screens.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elmotamyez.gallery.data.model.Expense
import com.elmotamyez.gallery.data.repository.ExpenseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class ExpenseViewModel(private val repo: ExpenseRepository) : ViewModel() {

    private val _expenses = MutableStateFlow<List<Expense>>(emptyList())
    val expenses = _expenses.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving = _isSaving.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            runCatching { _expenses.value = repo.fetchAll() }
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    fun addExpense(type: String, amount: Double, note: String?, onDone: () -> Unit) {
        viewModelScope.launch {
            _isSaving.value = true
            val expense = Expense(id = Uuid.random().toString(), type = type, amount = amount, note = note?.ifBlank { null })
            // Optimistic update — list refreshes immediately regardless of network
            _expenses.value = listOf(expense) + _expenses.value
            runCatching { repo.insert(expense) }
            _isSaving.value = false
            onDone()
        }
    }

    fun updateExpense(expense: Expense, onDone: () -> Unit) {
        viewModelScope.launch {
            _isSaving.value = true
            // Optimistic update
            _expenses.value = _expenses.value.map { if (it.id == expense.id) expense else it }
            runCatching { repo.update(expense) }
            _isSaving.value = false
            onDone()
        }
    }

    fun deleteExpense(id: String) {
        viewModelScope.launch {
            // Optimistic update
            _expenses.value = _expenses.value.filter { it.id != id }
            runCatching { repo.delete(id) }
        }
    }
}

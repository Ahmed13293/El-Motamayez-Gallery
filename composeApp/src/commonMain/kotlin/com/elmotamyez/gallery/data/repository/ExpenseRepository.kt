package com.elmotamyez.gallery.data.repository

import com.elmotamyez.gallery.data.model.Expense
import com.elmotamyez.gallery.data.remote.supabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.Serializable

@Serializable
private data class ExpenseRow(
    val id: String,
    val type: String,
    val amount: Double,
    val note: String? = null,
    val created_at: String? = null
)

@Serializable
private data class ExpenseInsert(
    val id: String,
    val type: String,
    val amount: Double,
    val note: String? = null
)

@Serializable
private data class ExpenseUpdate(
    val type: String,
    val amount: Double,
    val note: String? = null
)

class ExpenseRepository {

    suspend fun fetchAll(): List<Expense> =
        supabaseClient
            .from("expenses")
            .select { order("created_at", Order.DESCENDING) }
            .decodeList<ExpenseRow>()
            .map { Expense(id = it.id, type = it.type, amount = it.amount, note = it.note, createdAt = it.created_at) }

    suspend fun insert(expense: Expense) {
        supabaseClient.from("expenses").insert(
            ExpenseInsert(id = expense.id, type = expense.type, amount = expense.amount, note = expense.note)
        )
    }

    suspend fun update(expense: Expense) {
        supabaseClient.from("expenses").update(
            ExpenseUpdate(type = expense.type, amount = expense.amount, note = expense.note)
        ) { filter { eq("id", expense.id) } }
    }

    suspend fun delete(id: String) {
        supabaseClient.from("expenses").delete {
            filter { eq("id", id) }
        }
    }
}

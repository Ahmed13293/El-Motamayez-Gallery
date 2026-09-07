package com.elmotamyez.gallery.data.repository

import com.elmotamyez.gallery.data.model.DailyReconciliation
import com.elmotamyez.gallery.data.remote.supabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.Serializable

class DailyReconciliationRepository {

    @Serializable
    private data class ReconciliationRow(
        val date: String,
        val actual_cash: Double,
        val entered_by: String? = null
    )

    suspend fun upsert(date: String, actualCash: Double, enteredBy: String?) {
        supabaseClient.from("daily_reconciliation")
            .upsert(ReconciliationRow(date = date, actual_cash = actualCash, entered_by = enteredBy))
    }

    suspend fun fetchAll(): List<DailyReconciliation> {
        return supabaseClient.from("daily_reconciliation")
            .select()
            .decodeList<ReconciliationRow>()
            .map { DailyReconciliation(date = it.date, actualCash = it.actual_cash, enteredBy = it.entered_by) }
    }
}

package com.elmotamyez.gallery.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DailyReconciliation(
    val date: String,
    @SerialName("actual_cash") val actualCash: Double,
    @SerialName("entered_by") val enteredBy: String? = null
)

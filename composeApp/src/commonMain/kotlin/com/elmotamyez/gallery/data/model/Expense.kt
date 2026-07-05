package com.elmotamyez.gallery.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Expense(
    val id: String,
    val type: String,
    val amount: Double,
    val note: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

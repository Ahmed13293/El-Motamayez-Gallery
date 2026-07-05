package com.elmotamyez.gallery.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Category(
    val id: String,
    val name: String,
    @SerialName("created_at") val createdAt: String? = null
)

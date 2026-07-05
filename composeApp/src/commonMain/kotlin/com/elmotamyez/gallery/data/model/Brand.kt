package com.elmotamyez.gallery.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Brand(
    val id: String,
    val name: String,
    @SerialName("category_id") val categoryId: String,
    @SerialName("parent_id")   val parentId: String? = null,
    @SerialName("created_at")  val createdAt: String? = null
)

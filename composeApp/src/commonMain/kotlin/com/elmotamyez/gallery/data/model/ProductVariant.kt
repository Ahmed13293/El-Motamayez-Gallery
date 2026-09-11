package com.elmotamyez.gallery.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProductVariant(
    val id: String,
    @SerialName("product_id") val productId: String,
    val name: String,
    val stock: Int = 0
)

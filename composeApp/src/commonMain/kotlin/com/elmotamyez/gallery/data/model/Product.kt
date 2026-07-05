package com.elmotamyez.gallery.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Product(
    val id: String,
    val name: String,
    val price: Double,
    @SerialName("wholesale_price") val wholesalePrice: Double? = null,
    val stock: Int = 0,
    @SerialName("brand_id") val brandId: String,
    @SerialName("category_id") val categoryId: String,
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

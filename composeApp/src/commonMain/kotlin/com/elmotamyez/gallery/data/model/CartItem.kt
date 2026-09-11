package com.elmotamyez.gallery.data.model

import kotlinx.serialization.Serializable

@Serializable
data class CartItem(
    val product: Product,
    val quantity: Int = 1,
    val variantId: String? = null,
    val variantName: String? = null
) {
    val totalPrice: Double get() = product.price * quantity
    val cartKey: String get() = "${product.id}:${variantId ?: ""}"
}

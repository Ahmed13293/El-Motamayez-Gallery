package com.elmotamyez.gallery.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Receipt(
    val id: String,
    val orderNumber: Int,
    val items: List<CartItem>,
    val total: Double,
    val discount: Double = 0.0,
    @SerialName("payment_method")  val paymentMethod:  String  = "كاش",
    @SerialName("created_at")      val createdAt:      String? = null,
    @SerialName("is_paid")         val isPaid:         Boolean = true,
    @SerialName("customer_phone")  val customerPhone:  String? = null,
    @SerialName("customer_info")   val customerInfo:   String? = null,
    val username:                               String? = null
)

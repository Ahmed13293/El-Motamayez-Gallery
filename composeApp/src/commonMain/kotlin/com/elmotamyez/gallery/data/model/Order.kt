package com.elmotamyez.gallery.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Order(
    val id: String = "",
    val items: List<CartItem>,
    val total: Double = 0.0,
    val discount: Double = 0.0,
    @SerialName("delivery_fee") val deliveryFee: Double = 0.0,
    @SerialName("payment_method") val paymentMethod: String = "كاش",
    val status: String = "received",
    @SerialName("prepared_by") val preparedBy: String? = null,
    @SerialName("customer_name") val customerName: String? = null,
    @SerialName("customer_phone") val customerPhone: String? = null,
    val notes: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("created_by") val createdBy: String? = null
)

enum class OrderStatus(val key: String, val arabicLabel: String) {
    RECEIVED("received", "استلام الطلب"),
    PREPARING("preparing", "جاري التحضير"),
    DELIVERING("delivering", "جاري التوصيل"),
    DELIVERED("delivered", "تم التسليم");

    fun next(): OrderStatus? = when (this) {
        RECEIVED   -> PREPARING
        PREPARING  -> DELIVERING
        DELIVERING -> DELIVERED
        DELIVERED  -> null
    }

    companion object {
        fun fromKey(key: String) = entries.find { it.key == key } ?: RECEIVED
    }
}

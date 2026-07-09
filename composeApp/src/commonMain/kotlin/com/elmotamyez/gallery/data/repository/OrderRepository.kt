package com.elmotamyez.gallery.data.repository

import com.elmotamyez.gallery.data.model.CartItem
import com.elmotamyez.gallery.data.model.Order
import com.elmotamyez.gallery.data.remote.supabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order as SbOrder
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

// ── Supabase DTOs ─────────────────────────────────────────────────────────────

@Serializable
private data class OrderRow(
    val id: String,
    val items: String,
    val total: Double,
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

@Serializable
private data class OrderInsert(
    val items: String,
    val total: Double,
    val discount: Double = 0.0,
    @SerialName("delivery_fee") val deliveryFee: Double = 0.0,
    @SerialName("payment_method") val paymentMethod: String,
    val status: String = "received",
    @SerialName("customer_name") val customerName: String? = null,
    @SerialName("customer_phone") val customerPhone: String? = null,
    val notes: String? = null,
    @SerialName("created_by") val createdBy: String? = null
)

@Serializable
private data class OrderStatusUpdate(
    val status: String,
    @SerialName("prepared_by") val preparedBy: String? = null
)

@Serializable
private data class OrderUpdate(
    val items: String,
    val total: Double,
    val discount: Double,
    @SerialName("delivery_fee") val deliveryFee: Double,
    @SerialName("payment_method") val paymentMethod: String
)

// ── Repository ────────────────────────────────────────────────────────────────

class OrderRepository {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    suspend fun fetchAll(): List<Order> =
        supabaseClient.from("orders")
            .select { order("created_at", SbOrder.DESCENDING) }
            .decodeList<OrderRow>()
            .map { it.toDomain() }

    suspend fun insert(order: Order) {
        supabaseClient.from("orders").insert(
            OrderInsert(
                items         = json.encodeToString(order.items),
                total         = order.total,
                discount      = order.discount,
                deliveryFee   = order.deliveryFee,
                paymentMethod = order.paymentMethod,
                customerName  = order.customerName,
                customerPhone = order.customerPhone,
                notes         = order.notes,
                createdBy     = order.createdBy
            )
        )
    }

    suspend fun updateStatus(orderId: String, status: String, preparedBy: String? = null) {
        supabaseClient.from("orders")
            .update(OrderStatusUpdate(status = status, preparedBy = preparedBy)) {
                filter { eq("id", orderId) }
            }
    }

    suspend fun update(order: Order) {
        supabaseClient.from("orders")
            .update(
                OrderUpdate(
                    items         = json.encodeToString(order.items),
                    total         = order.total,
                    discount      = order.discount,
                    deliveryFee   = order.deliveryFee,
                    paymentMethod = order.paymentMethod
                )
            ) { filter { eq("id", order.id) } }
    }

    suspend fun delete(orderId: String) {
        supabaseClient.from("orders").delete { filter { eq("id", orderId) } }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private fun OrderRow.toDomain() = Order(
        id            = id,
        items         = runCatching { json.decodeFromString<List<CartItem>>(items) }.getOrElse { emptyList() },
        total         = total,
        discount      = discount,
        deliveryFee   = deliveryFee,
        paymentMethod = paymentMethod,
        status        = status,
        preparedBy    = preparedBy,
        customerName  = customerName,
        customerPhone = customerPhone,
        notes         = notes,
        createdAt     = createdAt,
        createdBy     = createdBy
    )
}

package com.elmotamyez.gallery.data.repository

import com.elmotamyez.gallery.data.model.CartItem
import com.elmotamyez.gallery.data.model.Receipt
import com.elmotamyez.gallery.data.remote.supabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

// ── Supabase DTO ─────────────────────────────────────────────────────────────

@Serializable
private data class ReceiptRow(
    val id: String,
    val order_number: Int,
    val items: String,
    val total: Double,
    val discount: Double        = 0.0,
    val payment_method: String  = "كاش",
    val created_at: String?     = null,
    val is_paid: Boolean        = true,
    val customer_phone: String? = null,
    val customer_info: String?  = null,
    val username: String?       = null
)

@Serializable
private data class ReceiptInsert(
    val id: String,
    val order_number: Int,
    val items: String,
    val total: Double,
    val discount: Double,
    val payment_method: String,
    val is_paid: Boolean,
    val customer_phone: String? = null,
    val customer_info: String?  = null,
    val username: String?       = null
)

@Serializable
private data class ReceiptItemsUpdate(
    val items: String,
    val total: Double,
    val discount: Double
)

// ── Repository ────────────────────────────────────────────────────────────────

class ReceiptRepository {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    /** Fetch all receipts ordered by creation time (oldest first). */
    suspend fun fetchAll(): List<Receipt> {
        val rows = supabaseClient
            .from("receipts")
            .select {
                order("created_at", Order.ASCENDING)
            }
            .decodeList<ReceiptRow>()

        return rows.map { it.toDomain() }
    }

    /** Update items, total and discount of an existing receipt. */
    suspend fun update(receipt: Receipt) {
        supabaseClient.from("receipts")
            .update(ReceiptItemsUpdate(
                items    = json.encodeToString(receipt.items),
                total    = receipt.total,
                discount = receipt.discount
            )) { filter { eq("id", receipt.id) } }
    }

    /** Persist a new receipt. */
    suspend fun insert(receipt: Receipt) {
        val row = ReceiptInsert(
            id             = receipt.id,
            order_number   = receipt.orderNumber,
            items          = json.encodeToString(receipt.items),
            total          = receipt.total,
            discount       = receipt.discount,
            payment_method = receipt.paymentMethod,
            is_paid        = receipt.isPaid,
            customer_phone = receipt.customerPhone,
            customer_info  = receipt.customerInfo,
            username       = receipt.username
        )
        supabaseClient.from("receipts").insert(row)
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private fun ReceiptRow.toDomain() = Receipt(
        id            = id,
        orderNumber   = order_number,
        items         = json.decodeFromString<List<CartItem>>(items),
        total         = total,
        discount      = discount,
        paymentMethod = payment_method,
        createdAt     = created_at,
        isPaid        = is_paid,
        customerPhone = customer_phone,
        customerInfo  = customer_info,
        username      = username
    )
}

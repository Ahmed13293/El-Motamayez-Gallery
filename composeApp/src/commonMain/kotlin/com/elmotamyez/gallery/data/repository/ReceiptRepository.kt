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
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

// ── Supabase DTO ─────────────────────────────────────────────────────────────

@Serializable
private data class ReceiptRow(
    val id: String,
    val order_number: Int,
    val items: JsonElement,     // jsonb column — comes back as JsonArray, not a String
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
    val created_at: String?     = null,
    val customer_phone: String? = null,
    val customer_info: String?  = null,
    val username: String?       = null
)

@Serializable
private data class ReceiptItemsUpdate(
    val items: String,
    val total: Double,
    val discount: Double,
    val payment_method: String,
    val is_paid: Boolean
)

// ── Repository ────────────────────────────────────────────────────────────────

class ReceiptRepository {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    /** Fetch all receipts ordered by creation time (oldest first).
     *  Decodes each row individually with our own json instance (ignoreUnknownKeys = true)
     *  so one malformed/schema-drifted row cannot hide every other receipt. */
    suspend fun fetchAll(): List<Receipt> {
        val raw = supabaseClient
            .from("receipts")
            .select { order("created_at", Order.ASCENDING) }
            .data

        return runCatching { json.parseToJsonElement(raw).jsonArray }
            .getOrElse { JsonArray(emptyList()) }
            .mapNotNull { element ->
                runCatching {
                    json.decodeFromJsonElement<ReceiptRow>(element).toDomain()
                }.getOrNull()
            }
    }

    /** Update items, total, discount and payment method of an existing receipt. */
    suspend fun update(receipt: Receipt) {
        supabaseClient.from("receipts")
            .update(ReceiptItemsUpdate(
                items          = json.encodeToString(receipt.items),
                total          = receipt.total,
                discount       = receipt.discount,
                payment_method = receipt.paymentMethod,
                is_paid        = receipt.paymentMethod != "آجل"
            )) { filter { eq("id", receipt.id) } }
    }

    /** Delete a receipt by id. */
    suspend fun delete(receiptId: String) {
        supabaseClient.from("receipts")
            .delete { filter { eq("id", receiptId) } }
    }

    /** Persist a new receipt (upsert so retries don't fail on duplicate key). */
    suspend fun insert(receipt: Receipt) {
        val row = ReceiptInsert(
            id             = receipt.id,
            order_number   = receipt.orderNumber,
            items          = json.encodeToString(receipt.items),
            total          = receipt.total,
            discount       = receipt.discount,
            payment_method = receipt.paymentMethod,
            is_paid        = receipt.isPaid,
            created_at     = receipt.createdAt,
            customer_phone = receipt.customerPhone,
            customer_info  = receipt.customerInfo,
            username       = receipt.username
        )
        supabaseClient.from("receipts").upsert(row)
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private fun ReceiptRow.toDomain() = Receipt(
        id            = id,
        orderNumber   = order_number,
        items         = when (items) {
            // jsonb column → Supabase returns a JsonArray directly
            is JsonArray -> json.decodeFromJsonElement(items)
            // text/varchar fallback — wrapped in a JsonPrimitive string
            is JsonPrimitive -> json.decodeFromString(items.jsonPrimitive.content)
            else -> emptyList()
        },
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

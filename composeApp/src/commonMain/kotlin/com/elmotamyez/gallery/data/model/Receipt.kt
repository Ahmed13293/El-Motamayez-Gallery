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
    val username:                               String? = null,
    @SerialName("is_quotation")  val isQuotation:  Boolean = false,
    // Local-only flag — true when the Supabase insert hasn't succeeded yet.
    // Never sent to Supabase (not in ReceiptInsert DTO). Cleared on successful sync.
    val pendingSave: Boolean = false,
    // Set by Supabase when the receipt is soft-deleted. Null = active.
    @SerialName("deleted_at") val deletedAt: String? = null
)

private fun String.startsWithYear() = length >= 4 && this[0].isDigit() && this[1].isDigit() && this[2].isDigit() && this[3].isDigit()

/** Extracts "YYYY-MM" from the receipt ID regardless of format:
 *  new "YYYY-MM-DD-NNNN" or old "YYYYMMDDNN".
 *  Returns "unknown" for non-date IDs (e.g. "ORD-…"). */
fun Receipt.idMonthKey(): String = when {
    !id.startsWithYear() -> "unknown"
    id.length >= 7 && id.getOrNull(4) == '-' -> id.take(7)
    id.length >= 6 -> "${id.take(4)}-${id.substring(4, 6)}"
    else -> "unknown"
}

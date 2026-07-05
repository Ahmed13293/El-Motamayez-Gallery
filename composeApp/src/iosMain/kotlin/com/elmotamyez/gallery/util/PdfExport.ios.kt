package com.elmotamyez.gallery.util

import com.elmotamyez.gallery.data.model.Receipt

actual fun exportReceiptToPdf(receipt: Receipt, fileName: String) {
    println("=== مكتبة المتميز - فرع الشيخ زايد ===")
    println("رقم الفاتورة: ${receipt.orderNumber}")
    receipt.createdAt?.let { println("تاريخ الفاتورة: $it") }
    println("طريقة الدفع: ${receipt.paymentMethod}")
    receipt.customerPhone?.let { println("رقم العميل: $it") }
    receipt.customerInfo?.let  { println("معلومات العميل: $it") }
    println("-".repeat(60))
    receipt.items.forEach { item ->
        println("${item.product.name}  x${item.quantity}  ${item.totalPrice.formatPrice()}")
    }
    println("-".repeat(60))
    if (receipt.discount > 0.0) println("الخصم: -${receipt.discount.formatPrice()}")
    println("الإجمالي: ${receipt.total.formatPrice()}")
    println("========================================")
}

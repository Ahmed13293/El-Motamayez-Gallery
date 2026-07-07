package com.elmotamyez.gallery.util

import com.elmotamyez.gallery.data.model.Receipt

actual fun exportReceiptToPdf(receipt: Receipt, fileName: String) {
    val dateText = receipt.createdAt?.let { raw ->
        runCatching {
            "${raw.substring(8, 10)}/${raw.substring(5, 7)}/${raw.substring(0, 4)}  ${raw.substring(11, 16)}"
        }.getOrElse { raw }
    } ?: ""

    val itemRows = receipt.items.joinToString("") { item ->
        val total = item.totalPrice.formatPrice()
        val price = item.product.price.formatPrice()
        "<tr><td>$total ج</td><td>$price ج</td><td>${item.quantity}</td><td class=\"name-col\">${item.product.name}</td></tr>"
    }

    val discountRows = if (receipt.discount > 0.0) {
        val subtotal = (receipt.total + receipt.discount).formatPrice()
        val discount = receipt.discount.formatPrice()
        "<tr class=\"sub-row\"><td colspan=\"3\" class=\"lbl\">المجموع</td><td>$subtotal ج</td></tr>" +
        "<tr class=\"sub-row\"><td colspan=\"3\" class=\"lbl\">الخصم</td><td>- $discount ج</td></tr>"
    } else ""

    val customerSection = buildString {
        if (!receipt.customerPhone.isNullOrBlank()) append("<p>رقم العميل: ${receipt.customerPhone}</p>")
        if (!receipt.customerInfo.isNullOrBlank())  append("<p>معلومات العميل: ${receipt.customerInfo}</p>")
    }

    val dateRow = if (dateText.isNotBlank()) "<p>تاريخ الفاتورة: $dateText</p>" else ""

    val html = """<!DOCTYPE html>
<html dir="rtl" lang="ar"><head><meta charset="UTF-8">
<title>فاتورة رقم ${receipt.orderNumber}</title>
<style>
@import url('https://fonts.googleapis.com/css2?family=Cairo:wght@400;600;700;900&display=swap');
*{box-sizing:border-box;margin:0;padding:0}
body{font-family:'Cairo',sans-serif;direction:rtl;background:#fff;color:#111;padding:32px;max-width:620px;margin:auto}
.hdr{text-align:center;border-bottom:2px solid #111;padding-bottom:12px;margin-bottom:16px}
.hdr h1{font-size:26px;font-weight:900}.hdr p{font-size:13px;color:#444;margin-top:4px}
.info{display:flex;justify-content:space-between;margin-bottom:14px;font-size:12px;color:#444}
.info p{margin-bottom:4px}
table{width:100%;border-collapse:collapse;margin-bottom:14px;font-size:13px}
thead tr{border-top:1.5px solid #111;border-bottom:1.5px solid #111}
th{padding:8px 6px;font-weight:700;text-align:center}
td{padding:7px 6px;text-align:center;border-bottom:.5px solid #ddd}
.name-col{text-align:right}.lbl{text-align:right}
.sub-row td{font-size:12px;color:#555}
.tot td{font-size:15px;font-weight:900;border-top:2px solid #111;border-bottom:2px solid #111;padding:10px 6px}
.ftr{text-align:center;border-top:1px solid #111;margin-top:16px;padding-top:10px;font-size:12px;color:#555}
@media print{body{padding:0}}
</style></head><body>
<div class="hdr">
  <h1>مكتبة المتميز</h1>
  <p>فرع الشيخ زايد  |  فاتورة طلب</p>
  <p>رقم الفاتورة: ${receipt.orderNumber}</p>
</div>
<div class="info">
  <div>$customerSection</div>
  <div style="text-align:left">
    <p>رقم الفاتورة: ${receipt.orderNumber}</p>
    $dateRow
    <p>طريقة الدفع: ${receipt.paymentMethod}</p>
  </div>
</div>
<table>
  <thead><tr><th>الإجمالي</th><th>السعر</th><th>الكمية</th><th class="name-col">المنتج</th></tr></thead>
  <tbody>
    $itemRows
    $discountRows
    <tr class="tot"><td colspan="3" class="lbl">الإجمالي الكلي</td><td>${receipt.total.formatPrice()} ج</td></tr>
  </tbody>
</table>
<div class="ftr">شكراً لتسوقكم معنا!</div>
</body></html>"""

    openAndPrint(html)
}

@JsFun("(html) => { var w = window.open('','_blank'); if(!w) return; w.document.write(html); w.document.close(); w.focus(); setTimeout(function(){ w.print(); }, 800); }")
private external fun openAndPrint(html: String)

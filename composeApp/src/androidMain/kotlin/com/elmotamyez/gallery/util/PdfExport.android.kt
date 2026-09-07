package com.elmotamyez.gallery.util

import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.elmotamyez.gallery.data.model.Receipt
import java.io.File
import java.io.FileOutputStream

actual fun exportReceiptToPdf(receipt: Receipt, fileName: String, isQuotation: Boolean) {
    val context = ApplicationContextHolder.context ?: return

    val pw = 595; val ph = 842
    val left = 40f; val right = pw.toFloat() - 40f; val cx = pw / 2f
    val rowH = 22f
    val footerH = 46f                      // space reserved at bottom of every page
    val usableBottom = ph.toFloat() - footerH

    fun txt(size: Float, bold: Boolean = false, col: Int = Color.BLACK, align: Paint.Align = Paint.Align.LEFT) =
        Paint().apply { color = col; textSize = size; isFakeBoldText = bold; isAntiAlias = true; textAlign = align }
    fun line(col: Int = Color.BLACK, w: Float = 0.8f) =
        Paint().apply { color = col; strokeWidth = w; isAntiAlias = true }

    val receiptRef = receipt.createdAt?.take(10)?.replace("-", "")?.let { "$it${receipt.orderNumber}" }
        ?: "${receipt.orderNumber}"

    // Column x-centres (RTL: total | price | qty | name)
    val sep1 = left + 145f
    val sep2 = sep1 + 100f
    val sep3 = sep2 + 75f
    val totalCx = (left + sep1) / 2f
    val priceCx = (sep1 + sep2) / 2f
    val qtyCx   = (sep2 + sep3) / 2f
    val sepLine = line(Color.BLACK, 0.5f)

    val pdf = PdfDocument()
    var pageIndex = 1
    var page = pdf.startPage(PdfDocument.PageInfo.Builder(pw, ph, pageIndex).create())
    var c: Canvas = page.canvas

    fun drawFooter() {
        c.drawLine(left, usableBottom, right, usableBottom, line(Color.BLACK, 0.8f))
        c.drawText("شكراً لتسوقكم معنا!", cx, usableBottom + 20f,
            txt(11f, col = Color.DKGRAY, align = Paint.Align.CENTER))
    }

    fun drawTableHeader(y: Float): Float {
        val thY = y + 15f
        val thP = txt(11f, bold = true, col = Color.BLACK, align = Paint.Align.CENTER)
        c.drawText("الإجمالي", totalCx, thY, thP)
        c.drawText("السعر",    priceCx, thY, thP)
        c.drawText("الكمية",   qtyCx,   thY, thP)
        c.drawText("المنتج",   (sep3 + right) / 2f, thY, thP)
        val endY = thY + 6f
        c.drawLine(sep1, endY - 20f, sep1, endY, sepLine)
        c.drawLine(sep2, endY - 20f, sep2, endY, sepLine)
        c.drawLine(sep3, endY - 20f, sep3, endY, sepLine)
        c.drawLine(left, endY, right, endY, line(Color.BLACK, 1f))
        return endY + 12f
    }

    fun startNewPage(): Float {
        drawFooter()
        pdf.finishPage(page)
        pageIndex++
        page = pdf.startPage(PdfDocument.PageInfo.Builder(pw, ph, pageIndex).create())
        c = page.canvas
        c.drawText("مكتبة المتميز — تابع", cx, 28f,
            txt(14f, bold = true, col = Color.BLACK, align = Paint.Align.CENTER))
        c.drawLine(left, 36f, right, 36f, line(Color.BLACK, 1f))

        if (isQuotation) {
            val stampPaint = Paint().apply {
                color = Color.argb(40, 200, 0, 0); textSize = 64f
                isFakeBoldText = true; isAntiAlias = true; textAlign = Paint.Align.CENTER
            }
            c.save(); c.rotate(-35f, cx, ph / 2f)
            c.drawText("غير مؤكد", cx, ph / 2f, stampPaint)
            c.restore()
        }

        return drawTableHeader(44f)
    }

    // ── Page 1: header ────────────────────────────────────────────────────────
    c.drawLine(left, 90f, right, 90f, line(Color.BLACK, 1.5f))
    c.drawText("مكتبة المتميز", cx, 36f,
        txt(22f, bold = true, col = Color.BLACK, align = Paint.Align.CENTER))
    val subTitle = if (isQuotation) "فرع الشيخ زايد  |  عرض سعر" else "فرع الشيخ زايد  |  فاتورة طلب"
    c.drawText(subTitle, cx, 58f, txt(12f, col = Color.BLACK, align = Paint.Align.CENTER))
    c.drawText("رقم المرجع: $receiptRef", cx, 78f,
        txt(10f, col = Color.DKGRAY, align = Paint.Align.CENTER))

    if (isQuotation) {
        val stampPaint = Paint().apply {
            color = Color.argb(40, 200, 0, 0); textSize = 64f
            isFakeBoldText = true; isAntiAlias = true; textAlign = Paint.Align.CENTER
        }
        c.save(); c.rotate(-35f, cx, ph / 2f)
        c.drawText("غير مؤكد", cx, ph / 2f, stampPaint)
        c.restore()
    }

    var y = 108f

    // ── Info row ──────────────────────────────────────────────────────────────
    val dateText = receipt.createdAt?.let { raw ->
        runCatching {
            "${raw.substring(8, 10)}/${raw.substring(5, 7)}/${raw.substring(0, 4)}  ${raw.substring(11, 16)}"
        }.getOrElse { raw }
    }
    val infoR = txt(10.5f, col = Color.DKGRAY, align = Paint.Align.RIGHT)
    val infoL = txt(10.5f, col = Color.DKGRAY, align = Paint.Align.LEFT)
    var ry = y; var ly = y
    if (dateText != null) { c.drawText("تاريخ الفاتورة: $dateText", right, ry, infoR); ry += 16f }
    c.drawText("طريقة الدفع: ${receipt.paymentMethod}", right, ry, infoR); ry += 16f
    if (!receipt.customerPhone.isNullOrBlank()) { c.drawText("رقم العميل: ${receipt.customerPhone}", left, ly, infoL); ly += 16f }
    if (!receipt.customerInfo.isNullOrBlank())  { c.drawText("معلومات العميل: ${receipt.customerInfo}", left, ly, infoL); ly += 16f }
    y = maxOf(ry, ly) + 10f
    c.drawLine(left, y, right, y, line(Color.BLACK, 0.8f)); y += 12f

    // ── Table header (page 1) ─────────────────────────────────────────────────
    y = drawTableHeader(y)

    // ── Items (paginated) ─────────────────────────────────────────────────────
    receipt.items.forEachIndexed { _, item ->
        // Need room for this row + at least the totals block (~80px) on the last item
        val needsNewPage = y + rowH > usableBottom - 10f
        if (needsNewPage) y = startNewPage()

        val rp = txt(10.5f, align = Paint.Align.CENTER)
        c.drawText(item.totalPrice.formatPrice(),    totalCx,        y + 13f, rp)
        c.drawText(item.product.price.formatPrice(), priceCx,        y + 13f, rp)
        c.drawText("${item.quantity}",               qtyCx,          y + 13f, rp)
        rp.textAlign = Paint.Align.RIGHT
        c.drawText(item.product.name.take(32),       right - 4f,     y + 13f, rp)
        c.drawLine(sep1, y, sep1, y + rowH, sepLine)
        c.drawLine(sep2, y, sep2, y + rowH, sepLine)
        c.drawLine(sep3, y, sep3, y + rowH, sepLine)
        c.drawLine(left, y + rowH, right, y + rowH, line(Color.LTGRAY, 0.5f))
        y += rowH
    }

    // ── Totals — start new page if not enough room (~90px needed) ─────────────
    if (y + 90f > usableBottom) y = startNewPage()

    y += 10f
    c.drawLine(left, y, right, y, line(Color.BLACK, 0.8f)); y += 14f

    val discount = receipt.discount
    if (discount > 0.0) {
        c.drawText("المجموع", right, y, txt(11f, col = Color.DKGRAY, align = Paint.Align.RIGHT))
        c.drawText((receipt.total + discount).formatPrice(), totalCx, y, txt(11f, col = Color.DKGRAY, align = Paint.Align.CENTER))
        y += 18f
        c.drawText("الخصم", right, y, txt(11f, col = Color.DKGRAY, align = Paint.Align.RIGHT))
        c.drawText("-${discount.formatPrice()}", totalCx, y, txt(11f, col = Color.DKGRAY, align = Paint.Align.CENTER))
        y += 8f
        c.drawLine(left, y, right, y, line(Color.BLACK, 0.8f)); y += 10f
    }

    c.drawRoundRect(RectF(left, y - 2f, right, y + 22f), 6f, 6f,
        Paint().apply { style = Paint.Style.STROKE; color = Color.BLACK; strokeWidth = 1.2f })
    c.drawText("الإجمالي", right - 8f, y + 15f,
        txt(14f, bold = true, col = Color.BLACK, align = Paint.Align.RIGHT))
    c.drawText(receipt.total.formatPrice(), totalCx, y + 15f,
        txt(14f, bold = true, col = Color.BLACK, align = Paint.Align.CENTER))

    drawFooter()
    pdf.finishPage(page)

    val cacheFile = File(context.cacheDir, fileName)
    FileOutputStream(cacheFile).use { pdf.writeTo(it) }
    pdf.close()

    val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", cacheFile)
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "application/pdf")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}

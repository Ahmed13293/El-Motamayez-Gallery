package com.elmotamyez.gallery.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import androidx.core.content.FileProvider
import com.elmotamyez.gallery.data.model.Receipt
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

actual fun exportReceiptToPdf(receipt: Receipt, fileName: String) {
    val context = ApplicationContextHolder.context ?: return

    val pw = 595; val ph = 842
    val left = 40f; val right = pw - 40f; val cx = pw / 2f

    val pdf  = PdfDocument()
    val page = pdf.startPage(PdfDocument.PageInfo.Builder(pw, ph, 1).create())
    val c: Canvas = page.canvas

    fun txt(size: Float, bold: Boolean = false, col: Int = Color.BLACK, align: Paint.Align = Paint.Align.LEFT) =
        Paint().apply { color = col; textSize = size; isFakeBoldText = bold; isAntiAlias = true; textAlign = align }
    fun fill(col: Int) = Paint().apply { color = col; style = Paint.Style.FILL }
    fun line(col: Int = Color.BLACK, w: Float = 0.8f) = Paint().apply { color = col; strokeWidth = w }

    // ── Header ────────────────────────────────────────────────────────────────
    // Bottom border line under header instead of filled band
    c.drawLine(left, 90f, right, 90f, line(Color.BLACK, 1.5f))

    c.drawText("مكتبة المتميز", cx, 36f,
        txt(22f, bold = true, col = Color.BLACK, align = Paint.Align.CENTER))
    c.drawText("فرع الشيخ زايد  |  فاتورة طلب", cx, 58f,
        txt(12f, col = Color.BLACK, align = Paint.Align.CENTER))
    c.drawText("رقم الفاتورة: ${receipt.orderNumber}", cx, 78f,
        txt(10f, col = Color.DKGRAY, align = Paint.Align.CENTER))

    var y = 108f

    // ── Info row ──────────────────────────────────────────────────────────────
    val dateText = receipt.createdAt?.let { raw ->
        runCatching {
            "${raw.substring(8, 10)}/${raw.substring(5, 7)}/${raw.substring(0, 4)}  ${raw.substring(11, 16)}"
        }.getOrElse { raw }
    }

    val infoR = txt(10.5f, col = Color.DKGRAY, align = Paint.Align.RIGHT)
    val infoL = txt(10.5f, col = Color.DKGRAY, align = Paint.Align.LEFT)

    var ry = y
    c.drawText("رقم الفاتورة: ${receipt.orderNumber}", right, ry, infoR);  ry += 16f
    if (dateText != null) { c.drawText("تاريخ الفاتورة: $dateText", right, ry, infoR); ry += 16f }
    c.drawText("طريقة الدفع: ${receipt.paymentMethod}", right, ry, infoR); ry += 16f

    var ly = y
    if (!receipt.customerPhone.isNullOrBlank()) {
        c.drawText("رقم العميل: ${receipt.customerPhone}", left, ly, infoL); ly += 16f
    }
    if (!receipt.customerInfo.isNullOrBlank()) {
        c.drawText("معلومات العميل: ${receipt.customerInfo}", left, ly, infoL); ly += 16f
    }

    y = maxOf(ry, ly) + 10f
    c.drawLine(left, y, right, y, line(Color.BLACK, 0.8f)); y += 12f

    // ── Table columns (RTL: الإجمالي | السعر | الكمية | المنتج) ───────────────
    val sep1 = left + 145f
    val sep2 = sep1 + 100f
    val sep3 = sep2 + 75f

    val totalCx = (left + sep1) / 2f
    val priceCx = (sep1 + sep2) / 2f
    val qtyCx   = (sep2 + sep3) / 2f
    val nameCx  = (sep3 + right) / 2f

    // Table header — bold text with underline, no fill
    val thY = y + 15f
    val thP = txt(11f, bold = true, col = Color.BLACK, align = Paint.Align.CENTER)
    c.drawText("الإجمالي", totalCx, thY, thP)
    c.drawText("السعر",    priceCx, thY, thP)
    c.drawText("الكمية",   qtyCx,   thY, thP)
    c.drawText("المنتج",   nameCx,  thY, thP)
    y = thY + 6f

    // Vertical separators in header
    val sepLine = line(Color.BLACK, 0.5f)
    c.drawLine(sep1, y - 20f, sep1, y, sepLine)
    c.drawLine(sep2, y - 20f, sep2, y, sepLine)
    c.drawLine(sep3, y - 20f, sep3, y, sepLine)

    c.drawLine(left, y, right, y, line(Color.BLACK, 1f)); y += 12f

    // Items
    val rowH = 22f
    receipt.items.forEachIndexed { i, item ->
        val rp = txt(10.5f, align = Paint.Align.CENTER)
        c.drawText(item.totalPrice.formatPrice(),         totalCx,     y + 13f, rp)
        c.drawText(item.product.price.formatPrice(),      priceCx,     y + 13f, rp)
        c.drawText("${item.quantity}",                    qtyCx,       y + 13f, rp)
        rp.textAlign = Paint.Align.RIGHT
        c.drawText(item.product.name.take(30),            right - 4f,  y + 13f, rp)

        // Vertical separators
        c.drawLine(sep1, y, sep1, y + rowH, sepLine)
        c.drawLine(sep2, y, sep2, y + rowH, sepLine)
        c.drawLine(sep3, y, sep3, y + rowH, sepLine)

        c.drawLine(left, y + rowH, right, y + rowH, line(Color.LTGRAY, 0.5f))
        y += rowH
    }

    y += 10f
    c.drawLine(left, y, right, y, line(Color.BLACK, 0.8f)); y += 14f

    // ── Totals ────────────────────────────────────────────────────────────────
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

    // Grand total — bold, boxed with border only
    c.drawRoundRect(RectF(left, y - 2f, right, y + 22f), 6f, 6f,
        Paint().apply { style = Paint.Style.STROKE; color = Color.BLACK; strokeWidth = 1.2f })
    c.drawText("الإجمالي", right - 8f, y + 15f,
        txt(14f, bold = true, col = Color.BLACK, align = Paint.Align.RIGHT))
    c.drawText(receipt.total.formatPrice(), totalCx, y + 15f,
        txt(14f, bold = true, col = Color.BLACK, align = Paint.Align.CENTER))

    // ── Footer ────────────────────────────────────────────────────────────────
    val footerY = ph - 36f
    c.drawLine(left, footerY - 10f, right, footerY - 10f, line(Color.BLACK, 0.8f))
    c.drawText("شكراً لتسوقكم معنا!", cx, footerY,
        txt(11f, col = Color.DKGRAY, align = Paint.Align.CENTER))

    pdf.finishPage(page)

    val cacheFile = File(context.cacheDir, fileName)
    cacheFile.outputStream().use { pdf.writeTo(it) }
    pdf.close()

    // Use PrintManager so Mopria works as a print service (not a viewer).
    val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
    val jobName = "فاتورة #${receipt.orderNumber}"

    val adapter = object : PrintDocumentAdapter() {
        override fun onLayout(
            oldAttributes: PrintAttributes?,
            newAttributes: PrintAttributes,
            cancellationSignal: CancellationSignal,
            callback: LayoutResultCallback,
            extras: Bundle?
        ) {
            if (cancellationSignal.isCanceled) { callback.onLayoutCancelled(); return }
            val info = PrintDocumentInfo.Builder(fileName)
                .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                .setPageCount(1)
                .build()
            callback.onLayoutFinished(info, newAttributes != oldAttributes)
        }

        override fun onWrite(
            pages: Array<out PageRange>,
            destination: ParcelFileDescriptor,
            cancellationSignal: CancellationSignal,
            callback: WriteResultCallback
        ) {
            try {
                FileInputStream(cacheFile).use { input ->
                    FileOutputStream(destination.fileDescriptor).use { output ->
                        input.copyTo(output)
                    }
                }
                callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
            } catch (e: Exception) {
                callback.onWriteFailed(e.message)
            }
        }
    }

    val printAttributes = PrintAttributes.Builder()
        .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
        .setResolution(PrintAttributes.Resolution("default", "default", 300, 300))
        .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
        .build()

    printManager.print(jobName, adapter, printAttributes)
}

package com.elmotamyez.gallery.util

import com.elmotamyez.gallery.data.model.Receipt
import com.itextpdf.kernel.colors.DeviceRgb
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.borders.SolidBorder
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import com.itextpdf.layout.properties.VerticalAlignment
import java.awt.Desktop
import java.io.File

private val BLACK  = DeviceRgb(0,   0,   0)
private val WHITE  = DeviceRgb(255, 255, 255)
private val GREY   = DeviceRgb(180, 180, 180)   // divider lines
private val DKGREY = DeviceRgb(80,  80,  80)    // secondary text

actual fun exportReceiptToPdf(receipt: Receipt, fileName: String) {
    try {
        val outputFile = File(System.getProperty("user.home"), fileName)
        val pdfDoc     = PdfDocument(PdfWriter(outputFile))
        val document   = Document(pdfDoc, PageSize.A4).apply {
            setMargins(36f, 40f, 40f, 40f)
        }

        // ── Header — black border, white background ────────────────────────────
        val headerTable = Table(UnitValue.createPercentArray(floatArrayOf(1f)))
            .useAllAvailableWidth()
            .setMarginTop(0f)
        val headerCell = Cell()
            .setBorder(SolidBorder(BLACK, 1.5f))
            .setPadding(16f)
            .add(Paragraph("مكتبة المتميز")
                .setBold().setFontSize(22f).setFontColor(BLACK)
                .setTextAlignment(TextAlignment.CENTER).setMarginBottom(4f))
            .add(Paragraph("فرع الشيخ زايد  |  فاتورة طلب")
                .setFontSize(12f).setFontColor(BLACK)
                .setTextAlignment(TextAlignment.CENTER).setMarginBottom(2f))
            .add(Paragraph("رقم الفاتورة: ${receipt.orderNumber}")
                .setFontSize(10f).setFontColor(DKGREY)
                .setTextAlignment(TextAlignment.CENTER))
        headerTable.addCell(headerCell)
        document.add(headerTable)

        // ── Info section ──────────────────────────────────────────────────────
        val dateText = receipt.createdAt?.let { raw ->
            runCatching {
                "${raw.substring(8, 10)}/${raw.substring(5, 7)}/${raw.substring(0, 4)}  ${raw.substring(11, 16)}"
            }.getOrElse { raw }
        }

        val infoTable = Table(UnitValue.createPercentArray(floatArrayOf(1f, 1f)))
            .useAllAvailableWidth()
            .setMarginTop(14f).setMarginBottom(4f)

        // Left cell — customer info
        val custLines = mutableListOf<String>()
        if (!receipt.customerPhone.isNullOrBlank()) custLines += "رقم العميل: ${receipt.customerPhone}"
        if (!receipt.customerInfo.isNullOrBlank())  custLines += "معلومات العميل: ${receipt.customerInfo}"

        val leftCell = Cell().setBorder(com.itextpdf.layout.borders.Border.NO_BORDER)
            .setVerticalAlignment(VerticalAlignment.TOP)
        custLines.forEach { line ->
            leftCell.add(Paragraph(line).setFontSize(10.5f).setFontColor(DKGREY)
                .setTextAlignment(TextAlignment.LEFT).setMarginBottom(2f))
        }
        infoTable.addCell(leftCell)

        // Right cell — invoice details
        val rightCell = Cell().setBorder(com.itextpdf.layout.borders.Border.NO_BORDER)
            .setVerticalAlignment(VerticalAlignment.TOP)
        buildList {
            add("رقم الفاتورة: ${receipt.orderNumber}")
            if (dateText != null) add("تاريخ الفاتورة: $dateText")
            add("طريقة الدفع: ${receipt.paymentMethod}")
        }.forEach { line ->
            rightCell.add(Paragraph(line).setFontSize(10.5f).setFontColor(DKGREY)
                .setTextAlignment(TextAlignment.RIGHT).setMarginBottom(2f))
        }
        infoTable.addCell(rightCell)
        document.add(infoTable)

        // Divider line
        document.add(Table(UnitValue.createPercentArray(floatArrayOf(1f)))
            .useAllAvailableWidth()
            .addCell(Cell().setHeight(1f).setBackgroundColor(GREY)
                .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER)))
        document.add(Paragraph(" ").setFontSize(4f))

        // ── Items table (RTL: الإجمالي | السعر | الكمية | المنتج) ──────────────
        val itemsTable = Table(UnitValue.createPercentArray(floatArrayOf(27f, 19f, 14f, 40f)))
            .useAllAvailableWidth()
            .setMarginBottom(6f)

        // Header row — black background, white text
        listOf("الإجمالي", "السعر", "الكمية", "المنتج").forEach { h ->
            itemsTable.addHeaderCell(
                Cell().setBackgroundColor(BLACK)
                    .setBorder(SolidBorder(WHITE, 0.3f))
                    .setPaddingTop(7f).setPaddingBottom(7f)
                    .add(Paragraph(h).setBold().setFontSize(11f).setFontColor(WHITE)
                        .setTextAlignment(TextAlignment.CENTER))
            )
        }

        // Item rows — no alternating colour, just grey borders
        val rowBorder = SolidBorder(GREY, 0.5f)
        receipt.items.forEach { item ->
            fun cell(text: String, align: TextAlignment) = Cell()
                .setBorder(rowBorder)
                .setPaddingTop(6f).setPaddingBottom(6f)
                .add(Paragraph(text).setFontSize(10.5f).setFontColor(BLACK).setTextAlignment(align))

            itemsTable.addCell(cell(item.totalPrice.formatPrice(),    TextAlignment.CENTER))
            itemsTable.addCell(cell(item.product.price.formatPrice(), TextAlignment.CENTER))
            itemsTable.addCell(cell("${item.quantity}",               TextAlignment.CENTER))
            itemsTable.addCell(cell(item.product.name,                TextAlignment.RIGHT))
        }
        document.add(itemsTable)

        // ── Totals ────────────────────────────────────────────────────────────
        val discount = receipt.discount
        if (discount > 0.0) {
            val totTable = Table(UnitValue.createPercentArray(floatArrayOf(1f, 1f)))
                .useAllAvailableWidth().setMarginTop(2f)
            fun totRow(label: String, value: String) {
                totTable.addCell(Cell().setBorder(com.itextpdf.layout.borders.Border.NO_BORDER)
                    .add(Paragraph(value).setFontSize(11f).setFontColor(DKGREY).setTextAlignment(TextAlignment.LEFT)))
                totTable.addCell(Cell().setBorder(com.itextpdf.layout.borders.Border.NO_BORDER)
                    .add(Paragraph(label).setFontSize(11f).setFontColor(DKGREY).setTextAlignment(TextAlignment.RIGHT)))
            }
            totRow("المجموع", (receipt.total + discount).formatPrice())
            totRow("الخصم",   "-${discount.formatPrice()}")
            document.add(totTable)

            document.add(Table(UnitValue.createPercentArray(floatArrayOf(1f)))
                .useAllAvailableWidth()
                .addCell(Cell().setHeight(1f).setBackgroundColor(GREY)
                    .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER)))
        }

        // Grand total — bold black, outlined box only
        val totalTable = Table(UnitValue.createPercentArray(floatArrayOf(1f, 1f)))
            .useAllAvailableWidth().setMarginTop(4f)
        totalTable.addCell(Cell()
            .setBorder(SolidBorder(BLACK, 1f))
            .setPadding(10f)
            .add(Paragraph(receipt.total.formatPrice())
                .setBold().setFontSize(15f).setFontColor(BLACK).setTextAlignment(TextAlignment.LEFT)))
        totalTable.addCell(Cell()
            .setBorder(SolidBorder(BLACK, 1f))
            .setPadding(10f)
            .add(Paragraph("الإجمالي")
                .setBold().setFontSize(15f).setFontColor(BLACK).setTextAlignment(TextAlignment.RIGHT)))
        document.add(totalTable)

        // ── Footer ────────────────────────────────────────────────────────────
        document.add(Paragraph(" ").setFontSize(6f))
        document.add(Table(UnitValue.createPercentArray(floatArrayOf(1f)))
            .useAllAvailableWidth()
            .addCell(Cell().setHeight(1f).setBackgroundColor(GREY)
                .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER)))
        document.add(Paragraph("شكراً لتسوقكم معنا!")
            .setFontSize(11f).setFontColor(DKGREY).setItalic()
            .setTextAlignment(TextAlignment.CENTER).setMarginTop(8f))

        document.close()

        if (Desktop.isDesktopSupported()) Desktop.getDesktop().open(outputFile)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

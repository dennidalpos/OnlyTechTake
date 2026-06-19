package com.onlytechtake.repairapp.core.pdf

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.graphics.withRotation
import com.onlytechtake.repairapp.core.media.decodeOrientedBitmap
import com.onlytechtake.repairapp.data.local.entities.CustomerEntity
import com.onlytechtake.repairapp.data.local.entities.RepairOrderEntity
import com.onlytechtake.repairapp.data.local.entities.SignatureEntity
import com.onlytechtake.repairapp.data.local.entities.TimelineEventEntity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class OrderStatusPdfPayload(
    val order: RepairOrderEntity,
    val customer: CustomerEntity,
    val categoryPath: String,
    val statusLabel: String,
    val estimateAmountLabel: String?,
    val assignedOperatorName: String?,
    val timeline: List<TimelineEventEntity>,
    val signature: SignatureEntity?,
    val generatedAt: Long,
    val generatedBy: String,
    val watermarkText: String? = null,
)

interface OrderStatusPdfWriter {
    fun generate(
        outputFile: File,
        payload: OrderStatusPdfPayload,
    ): File
}

class OrderStatusPdfGenerator : OrderStatusPdfWriter {
    private val titlePaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            color = Color.BLACK
        }

    private val sectionPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            color = Color.BLACK
        }

    private val textPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 10f
            color = Color.BLACK
        }

    private val labelPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 10f
            typeface = Typeface.DEFAULT_BOLD
            color = Color.BLACK
        }

    private val watermarkPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 56f
            typeface = Typeface.DEFAULT_BOLD
            color = Color.RED
            alpha = 38
            textAlign = Paint.Align.CENTER
        }

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    override fun generate(
        outputFile: File,
        payload: OrderStatusPdfPayload,
    ): File {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4
        var page = document.startPage(pageInfo)
        var canvas = page.canvas
        var y = 36f
        val margin = 36f
        val contentWidth = 595f - 2 * margin

        fun finishCurrentPage() {
            drawWatermark(canvas, payload.watermarkText)
            document.finishPage(page)
        }

        fun checkNewPage(neededHeight: Float) {
            if (y + neededHeight > 842f - margin) {
                finishCurrentPage()
                page =
                    document.startPage(
                        PdfDocument.PageInfo
                            .Builder(
                                595,
                                842,
                                pageInfo.pageNumber + 1,
                            ).create(),
                    )
                canvas = page.canvas
                y = margin
            }
        }

        // Header
        canvas.drawText("OnlyTechTake - Stato avanzamento commessa", margin, y, titlePaint)
        y += 30f

        // Info General
        canvas.drawText("Commessa N:", margin, y, labelPaint)
        canvas.drawText(payload.order.orderNumber, margin + 80f, y, textPaint)
        y += 15f
        canvas.drawText("Data:", margin, y, labelPaint)
        canvas.drawText(dateFormat.format(Date(payload.generatedAt)), margin + 80f, y, textPaint)
        y += 15f
        canvas.drawText("Generato da:", margin, y, labelPaint)
        canvas.drawText(payload.generatedBy, margin + 80f, y, textPaint)
        y += 25f

        // Customer Section
        canvas.drawText("CLIENTE", margin, y, sectionPaint)
        y += 15f
        val customerName =
            if (payload.customer.type.name ==
                "BUSINESS"
            ) {
                payload.customer.companyName
            } else {
                "${payload.customer.firstName} ${payload.customer.lastName}"
            }
        canvas.drawText("Nominativo:", margin, y, labelPaint)
        canvas.drawText(customerName ?: "", margin + 80f, y, textPaint)
        y += 15f
        canvas.drawText("Contatto:", margin, y, labelPaint)
        canvas.drawText(
            "${payload.customer.phone ?: ""} / ${payload.customer.email ?: ""}",
            margin + 80f,
            y,
            textPaint,
        )
        y += 25f

        // Order Details
        canvas.drawText("DETTAGLI DISPOSITIVO", margin, y, sectionPaint)
        y += 15f
        canvas.drawText("Categoria:", margin, y, labelPaint)
        canvas.drawText(payload.categoryPath, margin + 80f, y, textPaint)
        y += 15f
        canvas.drawText("Oggetto:", margin, y, labelPaint)
        canvas.drawText(payload.order.title, margin + 80f, y, textPaint)
        y += 15f
        canvas.drawText("Problema:", margin, y, labelPaint)
        y =
            drawWrappedText(
                canvas,
                payload.order.description ?: "Nessuna descrizione",
                margin + 80f,
                y,
                contentWidth - 80f,
                textPaint,
            )
        y += 10f

        // Status
        canvas.drawText("STATO ATTUALE", margin, y, sectionPaint)
        y += 15f
        canvas.drawText("Stato:", margin, y, labelPaint)
        canvas.drawText(payload.statusLabel, margin + 80f, y, textPaint)
        y += 15f
        if (payload.estimateAmountLabel != null) {
            canvas.drawText("Preventivo:", margin, y, labelPaint)
            canvas.drawText(payload.estimateAmountLabel, margin + 80f, y, textPaint)
            y += 15f
        }
        y += 10f

        // Timeline
        canvas.drawText("STORICO EVENTI (visibili al cliente)", margin, y, sectionPaint)
        y += 15f
        payload.timeline.filter { it.visibleToCustomer }.forEach { event ->
            checkNewPage(20f)
            val dateStr = dateFormat.format(Date(event.createdAt))
            canvas.drawText(dateStr, margin, y, labelPaint)
            canvas.drawText(event.title, margin + 90f, y, textPaint)
            y += 12f
            if (!event.description.isNullOrBlank()) {
                y =
                    drawWrappedText(
                        canvas,
                        event.description,
                        margin + 90f,
                        y,
                        contentWidth - 90f,
                        textPaint,
                    )
            }
            y += 5f
        }
        y += 15f

        // Signature
        if (payload.signature != null) {
            checkNewPage(150f)
            canvas.drawText("FIRMA CLIENTE", margin, y, sectionPaint)
            y += 15f
            canvas.drawText("Firmatario: ${payload.signature.signerName}", margin, y, textPaint)
            y += 12f
            canvas.drawText(
                "Data firma: ${dateFormat.format(Date(payload.signature.signedAt))}",
                margin,
                y,
                textPaint,
            )
            y += 12f
            canvas.drawText(
                "Privacy: Accettata (v${payload.signature.privacyTemplateVersion})",
                margin,
                y,
                textPaint,
            )
            y += 12f
            canvas.drawText(
                "Condizioni: Accettate (v${payload.signature.termsTemplateVersion})",
                margin,
                y,
                textPaint,
            )
            y += 10f

            val bitmap =
                decodeOrientedBitmap(payload.signature.localPath, SIGNATURE_MAX_DECODE_DIMENSION)
            if (bitmap != null) {
                val maxWidth = 220f
                val maxHeight = 90f
                val scale = Math.min(maxWidth / bitmap.width, maxHeight / bitmap.height)
                val drawWidth = bitmap.width * scale
                val drawHeight = bitmap.height * scale

                canvas.drawBitmap(
                    bitmap,
                    null,
                    android.graphics.RectF(margin, y, margin + drawWidth, y + drawHeight),
                    null,
                )
                y += drawHeight + 10f
                bitmap.recycle()
            }
        }

        finishCurrentPage()
        FileOutputStream(outputFile).use { document.writeTo(it) }
        document.close()
        return outputFile
    }

    private fun drawWatermark(
        canvas: Canvas,
        watermarkText: String?,
    ) {
        if (watermarkText.isNullOrBlank()) return
        canvas.withRotation(-35f, PAGE_WIDTH / 2f, PAGE_HEIGHT / 2f) {
            drawText(watermarkText, PAGE_WIDTH / 2f, PAGE_HEIGHT / 2f, watermarkPaint)
        }
    }

    private fun drawWrappedText(
        canvas: Canvas,
        text: String,
        x: Float,
        y: Float,
        maxWidth: Float,
        paint: Paint,
    ): Float {
        var currentY = y
        val words = text.split(" ")
        var line = ""
        for (word in words) {
            val testLine = if (line.isEmpty()) word else "$line $word"
            if (paint.measureText(testLine) > maxWidth) {
                canvas.drawText(line, x, currentY, paint)
                line = word
                currentY += paint.textSize + 2f
            } else {
                line = testLine
            }
        }
        canvas.drawText(line, x, currentY, paint)
        return currentY + paint.textSize + 2f
    }
}

private const val SIGNATURE_MAX_DECODE_DIMENSION = 480
private const val PAGE_WIDTH = 595f
private const val PAGE_HEIGHT = 842f

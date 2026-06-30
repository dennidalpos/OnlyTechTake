package com.onlytechtake.repairapp.core.pdf

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.graphics.withRotation
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.core.media.decodeOrientedBitmap
import com.onlytechtake.repairapp.data.local.entities.CustomerEntity
import com.onlytechtake.repairapp.data.local.entities.RepairOrderEntity
import com.onlytechtake.repairapp.data.local.entities.SignatureEntity
import com.onlytechtake.repairapp.data.local.entities.TimelineEventEntity
import com.onlytechtake.repairapp.domain.workspace.OwnerInfo
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
    val ownerInfo: OwnerInfo? = null,
    val watermarkText: String? = null,
)

interface OrderStatusPdfWriter {
    fun generate(
        context: Context,
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
        context: Context,
        outputFile: File,
        payload: OrderStatusPdfPayload,
    ): File {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4
        var page = document.startPage(pageInfo)
        var canvas = page.canvas
        var y = 36f
        val margin = 36f
        val contentWidth = 595f - (2 * margin)

        fun finishCurrentPage() {
            drawWatermark(canvas, payload.watermarkText)
            document.finishPage(page)
        }

        fun createNewPage() {
            finishCurrentPage()
            page =
                document.startPage(
                    PdfDocument.PageInfo
                        .Builder(
                            595,
                            842,
                            document.pages.size + 1,
                        ).create(),
                )
            canvas = page.canvas
            y = margin
        }

        fun checkNewPage(neededHeight: Float) {
            if ((y + neededHeight) > (842f - margin)) {
                createNewPage()
            }
        }

        fun drawWrappedText(
            text: String,
            x: Float,
            maxWidth: Float,
            paint: Paint,
            lineSpacing: Float = 2f,
        ) {
            val words = text.split(Regex("\\s+"))
            var line = ""
            for (word in words) {
                val testLine = if (line.isEmpty()) word else "$line $word"
                if (paint.measureText(testLine) > maxWidth) {
                    canvas.drawText(line, x, y, paint)
                    line = word
                    y += paint.textSize + lineSpacing
                    if (y > (842f - margin)) {
                        createNewPage()
                    }
                } else {
                    line = testLine
                }
            }
            if (line.isNotEmpty()) {
                canvas.drawText(line, x, y, paint)
                y += paint.textSize + lineSpacing
            }
        }

        // Header
        val logo = BitmapFactory.decodeResource(context.resources, R.drawable.pdf_logo)
        if (logo != null) {
            val logoSize = 48f
            canvas.drawBitmap(
                logo,
                null,
                android.graphics.RectF(595f - margin - logoSize, y - 10f, 595f - margin, y - 10f + logoSize),
                null,
            )
            logo.recycle()
        }
        canvas.drawText(context.getString(R.string.pdf_title), margin, y, titlePaint)
        y += 20f

        payload.ownerInfo?.let { owner ->
            if (owner.name.isNotBlank()) {
                canvas.drawText(owner.name, margin, y, labelPaint)
                y += 12f
                if (owner.vatNumber.isNotBlank()) {
                    canvas.drawText(owner.vatNumber, margin, y, textPaint)
                    y += 12f
                }
                if (owner.address.isNotBlank()) {
                    canvas.drawText(owner.address, margin, y, textPaint)
                    y += 12f
                }
                if (owner.phone.isNotBlank() || owner.email.isNotBlank()) {
                    canvas.drawText("${owner.phone} ${owner.email}".trim(), margin, y, textPaint)
                    y += 12f
                }
            }
        }
        y += 10f

        // Info General
        canvas.drawText(context.getString(R.string.pdf_label_order_number), margin, y, labelPaint)
        canvas.drawText(payload.order.orderNumber, margin + 80f, y, textPaint)
        y += 15f
        canvas.drawText(context.getString(R.string.pdf_label_date), margin, y, labelPaint)
        canvas.drawText(dateFormat.format(Date(payload.generatedAt)), margin + 80f, y, textPaint)
        y += 15f
        canvas.drawText(context.getString(R.string.pdf_label_generated_by), margin, y, labelPaint)
        canvas.drawText(payload.generatedBy, margin + 80f, y, textPaint)
        y += 25f

        // Customer Section
        canvas.drawText(context.getString(R.string.pdf_section_customer), margin, y, sectionPaint)
        y += 15f
        val customerName =
            if (payload.customer.type.name == "BUSINESS") {
                payload.customer.companyName
            } else {
                "${payload.customer.firstName} ${payload.customer.lastName}"
            }
        canvas.drawText(context.getString(R.string.pdf_label_customer_name), margin, y, labelPaint)
        canvas.drawText(customerName ?: "", margin + 80f, y, textPaint)
        y += 15f
        canvas.drawText(
            context.getString(R.string.pdf_label_customer_contact),
            margin,
            y,
            labelPaint,
        )
        canvas.drawText(
            "${payload.customer.phone ?: ""} / ${payload.customer.email ?: ""}",
            margin + 80f,
            y,
            textPaint,
        )
        y += 25f

        // Order Details
        canvas.drawText(
            context.getString(R.string.pdf_section_device_details),
            margin,
            y,
            sectionPaint,
        )
        y += 15f
        canvas.drawText(context.getString(R.string.pdf_label_category), margin, y, labelPaint)
        canvas.drawText(payload.categoryPath, margin + 80f, y, textPaint)
        y += 15f
        canvas.drawText(context.getString(R.string.pdf_label_object), margin, y, labelPaint)
        canvas.drawText(payload.order.title, margin + 80f, y, textPaint)
        y += 15f
        canvas.drawText(context.getString(R.string.pdf_label_problem), margin, y, labelPaint)
        drawWrappedText(
            payload.order.description ?: context.getString(R.string.pdf_no_description),
            margin + 80f,
            contentWidth - 80f,
            textPaint,
        )
        y += 10f

        // Status
        checkNewPage(40f)
        canvas.drawText(
            context.getString(R.string.pdf_section_current_status),
            margin,
            y,
            sectionPaint,
        )
        y += 15f
        canvas.drawText(context.getString(R.string.pdf_label_status), margin, y, labelPaint)
        canvas.drawText(payload.statusLabel, margin + 80f, y, textPaint)
        y += 15f
        if (payload.estimateAmountLabel != null) {
            canvas.drawText(context.getString(R.string.pdf_label_estimate), margin, y, labelPaint)
            canvas.drawText(payload.estimateAmountLabel, margin + 80f, y, textPaint)
            y += 15f
        }
        y += 10f

        // Timeline
        checkNewPage(30f)
        canvas.drawText(context.getString(R.string.pdf_section_timeline), margin, y, sectionPaint)
        y += 15f
        payload.timeline.filter { it.visibleToCustomer }.forEach { event ->
            checkNewPage(25f)
            val dateStr = dateFormat.format(Date(event.createdAt))
            canvas.drawText(dateStr, margin, y, labelPaint)
            canvas.drawText(event.title, margin + 90f, y, textPaint)
            y += 12f
            if (!event.description.isNullOrBlank()) {
                drawWrappedText(
                    event.description,
                    margin + 90f,
                    contentWidth - 90f,
                    textPaint,
                )
            }
            y += 5f
        }
        y += 15f

        // Signature
        if (payload.signature != null) {
            checkNewPage(180f)
            canvas.drawText(
                context.getString(R.string.pdf_section_signature),
                margin,
                y,
                sectionPaint,
            )
            y += 15f
            canvas.drawText(
                context.getString(
                    R.string.pdf_label_signer,
                    payload.signature.signerName,
                ),
                margin,
                y,
                textPaint,
            )
            y += 12f
            canvas.drawText(
                context.getString(
                    R.string.pdf_label_signed_at,
                    dateFormat.format(Date(payload.signature.signedAt)),
                ),
                margin,
                y,
                textPaint,
            )
            y += 12f
            canvas.drawText(
                context.getString(
                    R.string.pdf_label_privacy_accepted,
                    payload.signature.privacyTemplateVersion,
                ),
                margin,
                y,
                textPaint,
            )
            y += 12f
            canvas.drawText(
                context.getString(
                    R.string.pdf_label_terms_accepted,
                    payload.signature.termsTemplateVersion,
                ),
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
                val scale = kotlin.math.min(maxWidth / bitmap.width, maxHeight / bitmap.height)
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
}

private const val SIGNATURE_MAX_DECODE_DIMENSION = 480
private const val PAGE_WIDTH = 595f
private const val PAGE_HEIGHT = 842f

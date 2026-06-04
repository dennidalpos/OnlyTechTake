package com.onlytechtake.repairapp.data.repositories

import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument

internal class PdfTextWriter(private val document: PdfDocument) {
    private val regularPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 11f
    }
    private val titlePaint = Paint(regularPaint).apply {
        textSize = 18f
        typeface = Typeface.DEFAULT_BOLD
    }
    private val sectionPaint = Paint(regularPaint).apply {
        textSize = 13f
        typeface = Typeface.DEFAULT_BOLD
    }
    private var pageNumber = 0
    private var page = newPage()
    private var y = TOP_MARGIN

    fun title(text: String) {
        drawWrapped(text, titlePaint, maxChars = 54)
        y += 10
    }

    fun section(text: String) {
        y += 10
        drawWrapped(text, sectionPaint, maxChars = 76)
    }

    fun line(text: String) {
        drawWrapped(text, regularPaint, maxChars = 88)
    }

    fun finish() {
        document.finishPage(page)
    }

    private fun drawWrapped(text: String, paint: Paint, maxChars: Int) {
        text.replace('\n', ' ')
            .chunked(maxChars)
            .ifEmpty { listOf("") }
            .forEach { part ->
                if (y > PAGE_HEIGHT - BOTTOM_MARGIN) {
                    document.finishPage(page)
                    page = newPage()
                    y = TOP_MARGIN
                }
                page.canvas.drawText(part, LEFT_MARGIN.toFloat(), y.toFloat(), paint)
                y += LINE_HEIGHT
            }
    }

    private fun newPage(): PdfDocument.Page {
        pageNumber += 1
        return document.startPage(
            PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create(),
        )
    }

    private companion object {
        const val PAGE_WIDTH = 595
        const val PAGE_HEIGHT = 842
        const val LEFT_MARGIN = 40
        const val TOP_MARGIN = 48
        const val BOTTOM_MARGIN = 48
        const val LINE_HEIGHT = 17
    }
}

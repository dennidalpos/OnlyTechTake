package com.onlytechtake.repairapp.core.media

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint as AndroidPaint

fun Bitmap.rotateRight(): Bitmap {
    val matrix = Matrix().apply { postRotate(90f) }
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}

fun buildEditedBitmap(
    source: Bitmap,
    arrows: List<ImageEditArrow>,
    cropRect: Rect?,
): Bitmap {
    val output = source.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = AndroidCanvas(output)
    val paint =
        AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.RED
            strokeWidth = max(6f, source.width / 160f)
            style = AndroidPaint.Style.STROKE
            strokeCap = AndroidPaint.Cap.ROUND
        }
    arrows.forEach { arrow ->
        canvas.drawLine(arrow.start.x, arrow.start.y, arrow.end.x, arrow.end.y, paint)
        drawAndroidArrowHead(canvas, paint, arrow.start, arrow.end)
    }
    val rect = cropRect ?: return output
    val left = floor(rect.left).toInt().coerceIn(0, output.width - 1)
    val top = floor(rect.top).toInt().coerceIn(0, output.height - 1)
    val right = rect.right.roundToInt().coerceIn(left + 1, output.width)
    val bottom = rect.bottom.roundToInt().coerceIn(top + 1, output.height)
    return Bitmap.createBitmap(output, left, top, right - left, bottom - top)
}

private fun drawAndroidArrowHead(
    canvas: AndroidCanvas,
    paint: AndroidPaint,
    start: Offset,
    end: Offset,
) {
    val angle = atan2(end.y - start.y, end.x - start.x)
    val headLength = max(24f, paint.strokeWidth * 4f)
    val leftX = end.x - headLength * cos(angle - ARROW_HEAD_ANGLE)
    val leftY = end.y - headLength * sin(angle - ARROW_HEAD_ANGLE)
    val rightX = end.x - headLength * cos(angle + ARROW_HEAD_ANGLE)
    val rightY = end.y - headLength * sin(angle + ARROW_HEAD_ANGLE)
    canvas.drawLine(end.x, end.y, leftX, leftY, paint)
    canvas.drawLine(end.x, end.y, rightX, rightY, paint)
}

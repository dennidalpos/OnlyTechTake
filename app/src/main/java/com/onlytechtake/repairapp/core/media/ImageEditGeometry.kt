package com.onlytechtake.repairapp.core.media

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.IntSize
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

enum class ImageEditMode {
    CROP,
    ARROW,
}

data class ImageEditArrow(
    val start: Offset,
    val end: Offset,
)

data class ImagePlacement(
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float,
)

fun imagePlacement(imageSize: Size, canvasSize: Size): ImagePlacement {
    val scale = min(canvasSize.width / imageSize.width, canvasSize.height / imageSize.height)
    val width = imageSize.width * scale
    val height = imageSize.height * scale
    return ImagePlacement(
        left = (canvasSize.width - width) / 2f,
        top = (canvasSize.height - height) / 2f,
        width = width,
        height = height,
    )
}

fun Offset.toCanvasOffset(placement: ImagePlacement, bitmap: Bitmap): Offset =
    Offset(
        x = placement.left + (x / bitmap.width.toFloat()) * placement.width,
        y = placement.top + (y / bitmap.height.toFloat()) * placement.height,
    )

fun Size.toCanvasSize(placement: ImagePlacement, bitmap: Bitmap): Size =
    Size(
        width = (width / bitmap.width.toFloat()) * placement.width,
        height = (height / bitmap.height.toFloat()) * placement.height,
    )

fun Offset.toBitmapOffset(canvasSize: IntSize, bitmap: Bitmap): Offset? {
    if (canvasSize.width <= 0 || canvasSize.height <= 0) return null
    val placement = imagePlacement(
        imageSize = Size(bitmap.width.toFloat(), bitmap.height.toFloat()),
        canvasSize = Size(canvasSize.width.toFloat(), canvasSize.height.toFloat()),
    )
    if (x < placement.left || x > placement.left + placement.width) return null
    if (y < placement.top || y > placement.top + placement.height) return null
    return Offset(
        x = ((x - placement.left) / placement.width * bitmap.width).coerceIn(0f, bitmap.width.toFloat()),
        y = ((y - placement.top) / placement.height * bitmap.height).coerceIn(0f, bitmap.height.toFloat()),
    )
}

fun normalizeBitmapRect(start: Offset, end: Offset, bitmap: Bitmap?): Rect? {
    bitmap ?: return null
    val left = min(start.x, end.x).coerceIn(0f, bitmap.width.toFloat())
    val top = min(start.y, end.y).coerceIn(0f, bitmap.height.toFloat())
    val right = max(start.x, end.x).coerceIn(0f, bitmap.width.toFloat())
    val bottom = max(start.y, end.y).coerceIn(0f, bitmap.height.toFloat())
    if (right - left < MIN_CROP_SIZE || bottom - top < MIN_CROP_SIZE) return null
    return Rect(left, top, right, bottom)
}

const val MIN_DRAG_DISTANCE = 12f
const val MIN_CROP_SIZE = 24f
const val ARROW_HEAD_ANGLE = 0.7f

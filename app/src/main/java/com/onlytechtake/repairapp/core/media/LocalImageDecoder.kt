package com.onlytechtake.repairapp.core.media

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import kotlin.math.max

fun decodeOrientedBitmap(
    path: String,
    maxDimension: Int = Int.MAX_VALUE,
): Bitmap? {
    val options = BitmapFactory.Options()
    if (maxDimension != Int.MAX_VALUE) {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        options.inSampleSize = calculateBitmapSampleSize(bounds.outWidth, bounds.outHeight, maxDimension)
    }
    val source = BitmapFactory.decodeFile(path, options) ?: return null
    val orientation =
        runCatching {
            ExifInterface(path).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL,
            )
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
    val transform = exifOrientationTransform(orientation)
    if (transform == ExifOrientationTransform()) return source

    val matrix =
        Matrix().apply {
            if (transform.flipHorizontal) postScale(-1f, 1f)
            if (transform.flipVertical) postScale(1f, -1f)
            if (transform.rotationDegrees != 0f) postRotate(transform.rotationDegrees)
        }
    return Bitmap
        .createBitmap(source, 0, 0, source.width, source.height, matrix, true)
        .also { oriented ->
            if (oriented !== source) source.recycle()
        }
}

data class ExifOrientationTransform(
    val rotationDegrees: Float = 0f,
    val flipHorizontal: Boolean = false,
    val flipVertical: Boolean = false,
)

fun exifOrientationTransform(orientation: Int): ExifOrientationTransform =
    when (orientation) {
        2 -> ExifOrientationTransform(flipHorizontal = true)
        3 -> ExifOrientationTransform(rotationDegrees = 180f)
        4 -> ExifOrientationTransform(flipVertical = true)
        5 -> ExifOrientationTransform(rotationDegrees = 90f, flipHorizontal = true)
        6 -> ExifOrientationTransform(rotationDegrees = 90f)
        7 -> ExifOrientationTransform(rotationDegrees = -90f, flipHorizontal = true)
        8 -> ExifOrientationTransform(rotationDegrees = -90f)
        else -> ExifOrientationTransform()
    }

fun calculateBitmapSampleSize(
    width: Int,
    height: Int,
    maxDimension: Int,
): Int {
    if (width <= 0 || height <= 0 || maxDimension <= 0) return 1
    var sampleSize = 1
    val largestDimension = max(width, height)
    while ((largestDimension / (sampleSize * 2)) >= maxDimension) {
        sampleSize *= 2
    }
    return sampleSize
}

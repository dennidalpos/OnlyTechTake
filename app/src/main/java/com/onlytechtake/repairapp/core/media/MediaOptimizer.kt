package com.onlytechtake.repairapp.core.media

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max

/**
 * MediaOptimizer gestisce il ridimensionamento e la compressione delle immagini
 * per ridurre lo spazio occupato e migliorare le performance.
 */
class MediaOptimizer(
    private val maxDimension: Int = 1920,
    private val quality: Int = 85,
) {
    fun optimize(file: File): Result<File> =
        runCatching {
            if (!isImage(file)) return@runCatching file

            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, bounds)

            val width = bounds.outWidth
            val height = bounds.outHeight

            if ((width <= 0) || (height <= 0)) return@runCatching file

            // Ridimensionamento se necessario
            val scale =
                if (max(width, height) > maxDimension) {
                    maxDimension.toFloat() / max(width, height)
                } else {
                    1.0f
                }

            // Se l'immagine è già piccola e non vogliamo cambiare qualità, potremmo saltare.
            // Ma qui applichiamo sempre per rimuovere EXIF e normalizzare la compressione.

            val orientation =
                try {
                    ExifInterface(file.absolutePath).getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL,
                    )
                } catch (_: Exception) {
                    ExifInterface.ORIENTATION_NORMAL
                }

            val decodeOptions =
                BitmapFactory.Options().apply {
                    inSampleSize = calculateBitmapSampleSize(width, height, maxDimension)
                }
            val original =
                BitmapFactory.decodeFile(file.absolutePath, decodeOptions)
                    ?: return@runCatching file

            val targetWidth = width * scale
            val targetHeight = height * scale
            val matrix =
                Matrix().apply {
                    postScale(
                        targetWidth / original.width.toFloat(),
                        targetHeight / original.height.toFloat(),
                    )
                }

            applyExifRotation(matrix, orientation)

            val optimized =
                Bitmap.createBitmap(
                    original,
                    0,
                    0,
                    original.width,
                    original.height,
                    matrix,
                    true,
                )

            // Se createBitmap restituisce lo stesso oggetto (nessuna scala o rotazione),
            // e vogliamo forzare la compressione, procediamo comunque.

            val tempFile = File(file.parent, "opt_${file.name}")
            FileOutputStream(tempFile).use { out ->
                optimized.compress(Bitmap.CompressFormat.JPEG, quality, out)
            }

            if (optimized != original) {
                optimized.recycle()
            }
            original.recycle()

            // Sostituisci l'originale con l'ottimizzato
            if (tempFile.exists()) {
                if (file.delete()) {
                    tempFile.renameTo(file)
                } else {
                    tempFile.delete()
                    return@runCatching file
                }
            }

            file
        }

    private fun isImage(file: File): Boolean {
        val name = file.name.lowercase()
        return name.endsWith(".jpg") ||
            name.endsWith(".jpeg") ||
            name.endsWith(".png") ||
            name.endsWith(
                ".webp",
            )
    }

    private fun applyExifRotation(
        matrix: Matrix,
        orientation: Int,
    ) {
        val transform = exifOrientationTransform(orientation)
        if (transform.flipHorizontal) matrix.postScale(-1f, 1f)
        if (transform.flipVertical) matrix.postScale(1f, -1f)
        if (transform.rotationDegrees != 0f) matrix.postRotate(transform.rotationDegrees)
    }
}

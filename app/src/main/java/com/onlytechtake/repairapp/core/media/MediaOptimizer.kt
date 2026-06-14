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
    fun optimize(file: File): Result<File> = runCatching {
        if (!isImage(file)) return@runCatching file

        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, options)

        val width = options.outWidth
        val height = options.outHeight

        if (width <= 0 || height <= 0) return@runCatching file

        // Ridimensionamento se necessario
        val scale = if (max(width, height) > maxDimension) {
            maxDimension.toFloat() / max(width, height)
        } else {
            1.0f
        }

        // Se l'immagine è già piccola e non vogliamo cambiare qualità, potremmo saltare.
        // Ma qui applichiamo sempre per rimuovere EXIF e normalizzare la compressione.

        val orientation = try {
            ExifInterface(file.absolutePath).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
        } catch (e: Exception) {
            ExifInterface.ORIENTATION_NORMAL
        }

        val matrix = Matrix()
        if (scale < 1.0f) {
            matrix.postScale(scale, scale)
        }
        
        applyExifRotation(matrix, orientation)

        val original = BitmapFactory.decodeFile(file.absolutePath) ?: return@runCatching file
        val optimized = try {
            Bitmap.createBitmap(original, 0, 0, width, height, matrix, true)
        } finally {
            // original will be recycled later if it's different from optimized
        }

        // Se createBitmap restituisce lo stesso oggetto (nessuna scala o rotazione), 
        // e vogliamo forzare la compressione, procediamo comunque.

        val tempFile = File(file.parent, "opt_${file.name}")
        FileOutputStream(tempFile).use { out ->
            optimized.compress(Bitmap.CompressFormat.JPEG, quality, out)
        }

        if (optimized != original) optimized.recycle()
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
        return name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") || name.endsWith(".webp")
    }

    private fun applyExifRotation(matrix: Matrix, orientation: Int) {
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f)
                matrix.postScale(-1f, 1f)
            }
        }
    }
}

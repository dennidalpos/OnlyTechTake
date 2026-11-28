package com.onlytechtake.repairapp.core.media

import org.junit.Assert.assertEquals
import org.junit.Test

class LocalImageDecoderTest {
    @Test
    fun mapsExifRotationsAndMirroring() {
        assertEquals(ExifOrientationTransform(), exifOrientationTransform(1))
        assertEquals(ExifOrientationTransform(flipHorizontal = true), exifOrientationTransform(2))
        assertEquals(ExifOrientationTransform(rotationDegrees = 180f), exifOrientationTransform(3))
        assertEquals(
            ExifOrientationTransform(rotationDegrees = 90f, flipHorizontal = true),
            exifOrientationTransform(5),
        )
        assertEquals(ExifOrientationTransform(rotationDegrees = 90f), exifOrientationTransform(6))
        assertEquals(ExifOrientationTransform(rotationDegrees = -90f), exifOrientationTransform(8))
    }

    @Test
    fun calculatesPowerOfTwoSampleSizeForPreviewBounds() {
        assertEquals(1, calculateBitmapSampleSize(width = 1200, height = 800, maxDimension = 1920))
        assertEquals(2, calculateBitmapSampleSize(width = 4000, height = 3000, maxDimension = 1920))
        assertEquals(4, calculateBitmapSampleSize(width = 4000, height = 3000, maxDimension = 640))
        assertEquals(8, calculateBitmapSampleSize(width = 8000, height = 6000, maxDimension = 640))
    }

    @Test
    fun invalidSampleInputsFallBackToFullSize() {
        assertEquals(1, calculateBitmapSampleSize(width = 0, height = 3000, maxDimension = 640))
        assertEquals(1, calculateBitmapSampleSize(width = 4000, height = -1, maxDimension = 640))
        assertEquals(1, calculateBitmapSampleSize(width = 4000, height = 3000, maxDimension = 0))
    }
}

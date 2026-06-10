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
}

package com.onlytechtake.repairapp.core.media

import org.junit.Assert.assertEquals
import org.junit.Test

class CameraRotationTest {
    @Test
    fun keepsAllSupportedDisplayRotations() {
        (0..3).forEach { rotation ->
            assertEquals(rotation, normalizeCameraTargetRotation(rotation))
        }
    }

    @Test
    fun fallsBackToPortraitWhenDisplayIsUnavailableOrInvalid() {
        assertEquals(0, normalizeCameraTargetRotation(null))
        assertEquals(0, normalizeCameraTargetRotation(-1))
        assertEquals(0, normalizeCameraTargetRotation(4))
    }
}

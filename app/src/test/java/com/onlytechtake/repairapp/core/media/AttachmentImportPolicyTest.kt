package com.onlytechtake.repairapp.core.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class AttachmentImportPolicyTest {
    private val policy = AttachmentImportPolicy(maxSizeBytes = 1024L)

    @Test
    fun normalizesMimeTypeBeforeValidation() {
        policy.requireAllowed(" Image/JPEG ; charset=utf-8 ", 1024L)

        assertEquals("image/jpeg", policy.normalize(" Image/JPEG ; charset=utf-8 "))
        assertTrue(policy.isImage("IMAGE/PNG"))
    }

    @Test
    fun rejectsUnknownMimeType() {
        val error =
            assertThrows(IllegalArgumentException::class.java) {
                policy.requireAllowed(null, 1L)
            }

        assertEquals("Tipo allegato non riconosciuto", error.message)
    }

    @Test
    fun rejectsMimeTypeOutsideAllowlist() {
        val error =
            assertThrows(IllegalArgumentException::class.java) {
                policy.requireAllowed("application/x-msdownload", 1L)
            }

        assertEquals("Tipo allegato non consentito: application/x-msdownload", error.message)
    }

    @Test
    fun rejectsDeclaredSizeOverLimit() {
        val error =
            assertThrows(IllegalArgumentException::class.java) {
                policy.requireAllowed("application/pdf", 1025L)
            }

        assertEquals("Allegato troppo grande: massimo 1024 bytes", error.message)
    }
}

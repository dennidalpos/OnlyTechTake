package com.onlytechtake.repairapp.core.media

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class AttachmentImportPolicyTest {
    private val policy = AttachmentImportPolicy(maxSizeBytes = 1024L)
    private val context =
        mockk<Context>(relaxed = true) {
            every { getString(any()) } returns "error"
            every { getString(any(), any()) } returns "error"
        }

    @Test
    fun normalizesMimeTypeBeforeValidation() {
        policy.requireAllowed(context, " Image/JPEG ; charset=utf-8 ", 1024L)

        assertEquals("image/jpeg", policy.normalize(" Image/JPEG ; charset=utf-8 "))
        assertTrue(policy.isImage("IMAGE/PNG"))
    }

    @Test
    fun rejectsUnknownMimeType() {
        val error =
            assertThrows(IllegalArgumentException::class.java) {
                policy.requireAllowed(context, null, 1L)
            }

        assertEquals("error", error.message)
    }

    @Test
    fun rejectsMimeTypeOutsideAllowlist() {
        val error =
            assertThrows(IllegalArgumentException::class.java) {
                policy.requireAllowed(context, "application/x-msdownload", 1L)
            }

        assertEquals("error", error.message)
    }

    @Test
    fun rejectsDeclaredSizeOverLimit() {
        val error =
            assertThrows(IllegalArgumentException::class.java) {
                policy.requireAllowed(context, "application/pdf", 1025L)
            }

        assertEquals("error", error.message)
    }
}

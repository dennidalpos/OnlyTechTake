package com.onlytechtake.repairapp.domain.workspace

import android.content.Context
import com.onlytechtake.repairapp.data.local.entities.CustomerFeedbackType
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FeedbackInputTest {
    private val context =
        mockk<Context> {
            every { getString(any()) } returns "error"
        }

    @Test
    fun acceptedQuoteDoesNotRequireNote() {
        val errors = FeedbackInput(CustomerFeedbackType.ACCEPT).validationErrors(context)

        assertTrue(errors.isEmpty())
    }

    @Test
    fun rejectedQuoteRequiresManualReason() {
        val errors = FeedbackInput(CustomerFeedbackType.REJECT).validationErrors(context)

        assertEquals(listOf("error"), errors)
        assertTrue(
            FeedbackInput(
                CustomerFeedbackType.REJECT,
                note = "Troppo caro",
            ).validationErrors(context).isEmpty(),
        )
    }
}

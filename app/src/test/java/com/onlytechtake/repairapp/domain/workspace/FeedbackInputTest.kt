package com.onlytechtake.repairapp.domain.models

import com.onlytechtake.repairapp.domain.models.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FeedbackInputTest {
    @Test
    fun acceptedQuoteDoesNotRequireNote() {
        val errors = FeedbackInput(CustomerFeedbackType.ACCEPT).validationErrors()

        assertTrue(errors.isEmpty())
    }

    @Test
    fun rejectedQuoteRequiresManualReason() {
        val errors = FeedbackInput(CustomerFeedbackType.REJECT).validationErrors()

        assertEquals(listOf(FeedbackValidationError.REJECT_REASON_REQUIRED), errors)
        assertTrue(
            FeedbackInput(
                CustomerFeedbackType.REJECT,
                note = "Troppo caro",
            ).validationErrors().isEmpty(),
        )
    }
}

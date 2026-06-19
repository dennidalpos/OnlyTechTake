package com.onlytechtake.repairapp.domain.workspace

import com.onlytechtake.repairapp.data.local.entities.CustomerFeedbackType
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

        assertEquals(listOf("Motivo rifiuto obbligatorio"), errors)
        assertTrue(
            FeedbackInput(
                CustomerFeedbackType.REJECT,
                note = "Troppo caro",
            ).validationErrors().isEmpty(),
        )
    }
}

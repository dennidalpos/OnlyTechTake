package com.onlytechtake.repairapp.features.dashboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QuoteDecisionTest {
    @Test
    fun togglingDecisionKeepsOnlyOneSelection() {
        assertEquals(QuoteDecision.ACCEPTED, toggleQuoteDecision(null, QuoteDecision.ACCEPTED))
        assertEquals(
            QuoteDecision.REJECTED,
            toggleQuoteDecision(QuoteDecision.ACCEPTED, QuoteDecision.REJECTED),
        )
        assertNull(toggleQuoteDecision(QuoteDecision.REJECTED, QuoteDecision.REJECTED))
    }

    @Test
    fun confirmationRequiresDecisionAndRejectNote() {
        assertFalse(canConfirmQuoteDecision(null, ""))
        assertTrue(canConfirmQuoteDecision(QuoteDecision.ACCEPTED, ""))
        assertFalse(canConfirmQuoteDecision(QuoteDecision.REJECTED, ""))
        assertTrue(canConfirmQuoteDecision(QuoteDecision.REJECTED, "Cliente non accetta"))
    }
}

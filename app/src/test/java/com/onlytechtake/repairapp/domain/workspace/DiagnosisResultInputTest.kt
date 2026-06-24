package com.onlytechtake.repairapp.domain.workspace

import android.content.Context
import com.onlytechtake.repairapp.data.local.entities.DiagnosisOutcome
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DiagnosisResultInputTest {
    private val context =
        mockk<Context> {
            every { getString(any()) } returns "error"
        }

    @Test
    fun repairableRequiresPositiveEstimate() {
        val missing = DiagnosisResultInput("Display sostituibile", DiagnosisOutcome.REPAIRABLE)
        val zero = missing.copy(estimateAmountCents = 0L)
        val valid = missing.copy(estimateAmountCents = 12_300L)

        assertEquals(listOf("error"), missing.validationErrors(context))
        assertEquals(listOf("error"), zero.validationErrors(context))
        assertTrue(valid.validationErrors(context).isEmpty())
        assertEquals(12_300L, valid.normalizedEstimateAmountCents())
    }

    @Test
    fun notRepairableClearsAnyIncoherentEstimate() {
        val input =
            DiagnosisResultInput(
                summary = "Ricambio non disponibile",
                diagnosisOutcome = DiagnosisOutcome.NOT_REPAIRABLE,
                estimateAmountCents = 12_300L,
            )

        assertTrue(input.validationErrors(context).isEmpty())
        assertEquals(null, input.normalizedEstimateAmountCents())
    }
}

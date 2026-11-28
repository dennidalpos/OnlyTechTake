package com.onlytechtake.repairapp.domain.models

import com.onlytechtake.repairapp.domain.models.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DiagnosisResultInputTest {
    @Test
    fun repairableRequiresPositiveEstimate() {
        val missing = DiagnosisResultInput("Display sostituibile", DiagnosisOutcome.REPAIRABLE)
        val zero = missing.copy(estimateAmountCents = 0L)
        val valid = missing.copy(estimateAmountCents = 12_300L)

        assertEquals(listOf(DiagnosisValidationError.POSITIVE_ESTIMATE_REQUIRED), missing.validationErrors())
        assertEquals(listOf(DiagnosisValidationError.POSITIVE_ESTIMATE_REQUIRED), zero.validationErrors())
        assertTrue(valid.validationErrors().isEmpty())
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

        assertTrue(input.validationErrors().isEmpty())
        assertEquals(null, input.normalizedEstimateAmountCents())
    }
}

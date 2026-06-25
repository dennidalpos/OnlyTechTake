package com.onlytechtake.repairapp.core.templates

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class TemplateRendererTest {
    private val renderer = TemplateRenderer()

    @Test
    fun renderReplacesKnownVariables() {
        val result =
            renderer.render(
                template = "Commessa {{ numero_commessa }} per {{cliente}}",
                variables = mapOf("numero_commessa" to "RIP-2026-000001", "cliente" to "Mario"),
            )

        assertEquals("Commessa RIP-2026-000001 per Mario", result)
    }

    @Test
    fun renderRejectsUnknownVariables() {
        assertThrows(IllegalArgumentException::class.java) {
            renderer.render(
                template = "Commessa {{missing}}",
                variables = mapOf("numero_commessa" to "RIP-2026-000001"),
            )
        }
    }

    @Test
    fun extractVariablesReturnsUniqueVariableNames() {
        val variables = renderer.extractVariables("{{cliente}} {{ numero_commessa }} {{cliente}}")

        assertEquals(setOf("cliente", "numero_commessa"), variables)
    }

    @Test
    fun requireSupportedVariablesRejectsUnknownVariables() {
        assertThrows(IllegalArgumentException::class.java) {
            renderer.requireSupportedVariables("{{cliente}} {{missing}}", setOf("cliente"))
        }
    }

    @Test
    fun templateAnchorsExposeStepMetadataForSettingsWorkflow() {
        assertEquals(1, TemplateAnchor.WHATSAPP_INTAKE.stepNumber)
        assertEquals(
            com.onlytechtake.repairapp.R.string.anchor_step_intake,
            TemplateAnchor.WHATSAPP_INTAKE.stepNameRes
        )
        assertEquals(0, TemplateAnchor.PRIVACY_TEXT.stepNumber)
        assertEquals(
            com.onlytechtake.repairapp.R.string.anchor_step_legal,
            TemplateAnchor.PRIVACY_TEXT.stepNameRes
        )
    }
}

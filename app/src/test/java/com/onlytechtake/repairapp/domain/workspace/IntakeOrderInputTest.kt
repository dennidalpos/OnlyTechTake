package com.onlytechtake.repairapp.domain.workspace

import com.onlytechtake.repairapp.data.local.entities.CustomerType
import com.onlytechtake.repairapp.data.local.entities.OrderPriority
import com.onlytechtake.repairapp.data.local.entities.OrderStatus
import com.onlytechtake.repairapp.data.local.entities.RepairOrderEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IntakeOrderInputTest {
    @Test
    fun validationRequiresCustomerProblemMediaAndSignature() {
        val errors = IntakeOrderInput().validationErrors()

        assertEquals(
            listOf(
                "Cliente obbligatorio",
                "Problema obbligatorio",
                "Categoria obbligatoria",
                "Almeno una foto o allegato obbligatorio",
                "Privacy e firma obbligatorie",
            ),
            errors,
        )
    }

    @Test
    fun validationAcceptsExistingCustomerWithProblemMediaAndSignature() {
        val input = IntakeOrderInput(
            existingCustomerId = "customer-1",
            problemSummary = "Schermo rotto",
            categoryId = "phone",
            stagedMediaCount = 1,
            hasPrivacySignature = true,
        )

        assertTrue(input.isReadyForSubmit())
    }

    @Test
    fun validationAcceptsValidNewBusinessCustomer() {
        val input = IntakeOrderInput(
            newCustomer = CustomerInput(
                type = CustomerType.BUSINESS,
                companyName = "Only Tech",
                phone = "3331234567",
            ),
            problemSummary = "Notebook non si avvia",
            categoryId = "notebook",
            stagedMediaCount = 2,
            hasPrivacySignature = true,
        )

        assertTrue(input.validationErrors().isEmpty())
    }

    @Test
    fun validationAcceptsCategoryWithoutDynamicFields() {
        val input = IntakeOrderInput(
            existingCustomerId = "customer-1",
            problemSummary = "Batteria gonfia",
            categoryId = "phone",
            stagedMediaCount = 1,
            hasPrivacySignature = true,
        )

        assertTrue(input.isReadyForSubmit())
    }

    @Test
    fun completeResultCarriesCreatedOrder() {
        val result = IntakeOrderCreationResult.Complete(order = testOrder())

        assertEquals("2026-0001", result.order.orderNumber)
    }

    private fun testOrder(): RepairOrderEntity =
        RepairOrderEntity(
            id = "order-1",
            orderNumber = "2026-0001",
            customerId = "customer-1",
            title = "Batteria gonfia",
            categoryId = "phone",
            status = OrderStatus.OPEN,
            priority = OrderPriority.NORMAL,
            assignedToUserId = "operator-1",
            createdByUserId = "operator-1",
            description = null,
            internalNotes = null,
            estimateAmountCents = null,
            finalAmountCents = null,
            createdAt = 0L,
            updatedAt = 0L,
            closedAt = null,
            closureReason = null,
        )
}

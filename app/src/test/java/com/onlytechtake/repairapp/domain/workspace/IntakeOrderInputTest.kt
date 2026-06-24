package com.onlytechtake.repairapp.domain.workspace

import android.content.Context
import com.onlytechtake.repairapp.data.local.entities.CustomerType
import com.onlytechtake.repairapp.data.local.entities.OrderPriority
import com.onlytechtake.repairapp.data.local.entities.OrderStatus
import com.onlytechtake.repairapp.data.local.entities.RepairOrderEntity
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IntakeOrderInputTest {
    private val context =
        mockk<Context> {
            every { getString(any()) } returns "error"
        }

    @Test
    fun validationRequiresCustomerProblemMediaAndSignature() {
        val errors = IntakeOrderInput().validationErrors(context)

        assertEquals(
            listOf(
                "error",
                "error",
                "error",
                "error",
                "error",
            ),
            errors,
        )
    }

    @Test
    fun validationAcceptsExistingCustomerWithProblemMediaAndSignature() {
        val input =
            IntakeOrderInput(
                existingCustomerId = "customer-1",
                problemSummary = "Schermo rotto",
                categoryId = "phone",
                stagedMediaCount = 1,
                hasPrivacySignature = true,
            )

        assertTrue(input.isReadyForSubmit(context))
    }

    @Test
    fun validationAcceptsValidNewBusinessCustomer() {
        val input =
            IntakeOrderInput(
                newCustomer =
                    CustomerInput(
                        type = CustomerType.BUSINESS,
                        companyName = "Only Tech",
                        phone = "3331234567",
                    ),
                problemSummary = "Notebook non si avvia",
                categoryId = "notebook",
                stagedMediaCount = 2,
                hasPrivacySignature = true,
            )

        assertTrue(input.validationErrors(context).isEmpty())
    }

    @Test
    fun validationRejectsBlankExistingCustomerAndIncompleteNewCustomer() {
        val input =
            IntakeOrderInput(
                existingCustomerId = "   ",
                newCustomer =
                    CustomerInput(
                        type = CustomerType.PRIVATE,
                        firstName = "Mario",
                        lastName = "Rossi",
                        phone = "   ",
                        email = "",
                    ),
                problemSummary = "Display rotto",
                categoryId = "phone",
                stagedMediaCount = 1,
                hasPrivacySignature = true,
            )

        assertEquals(listOf("error"), input.validationErrors(context))
    }

    @Test
    fun validationRejectsBusinessCustomerWithoutCompanyName() {
        val input =
            IntakeOrderInput(
                newCustomer =
                    CustomerInput(
                        type = CustomerType.BUSINESS,
                        companyName = " ",
                        email = "azienda@example.test",
                    ),
                problemSummary = "Notebook non si avvia",
                categoryId = "notebook",
                stagedMediaCount = 1,
                hasPrivacySignature = true,
            )

        assertEquals(listOf("error"), input.validationErrors(context))
    }

    @Test
    fun validationAcceptsCategoryWithoutDynamicFields() {
        val input =
            IntakeOrderInput(
                existingCustomerId = "customer-1",
                problemSummary = "Batteria gonfia",
                categoryId = "phone",
                stagedMediaCount = 1,
                hasPrivacySignature = true,
            )

        assertTrue(input.isReadyForSubmit(context))
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

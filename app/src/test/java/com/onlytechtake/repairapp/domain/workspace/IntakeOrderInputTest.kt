package com.onlytechtake.repairapp.domain.models

import android.content.Context
import com.onlytechtake.repairapp.data.local.entities.RepairOrderEntity
import com.onlytechtake.repairapp.data.mappers.toDomain
import com.onlytechtake.repairapp.domain.models.*
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
        val errors = IntakeOrderInput().validationErrors()

        assertEquals(
            listOf(
                IntakeValidationError.CUSTOMER_REQUIRED,
                IntakeValidationError.PROBLEM_REQUIRED,
                IntakeValidationError.CATEGORY_REQUIRED,
                IntakeValidationError.MEDIA_REQUIRED,
                IntakeValidationError.PRIVACY_REQUIRED,
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

        assertTrue(input.validationErrors().isEmpty())
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

        assertEquals(listOf(IntakeValidationError.CUSTOMER_REQUIRED), input.validationErrors())
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

        assertEquals(listOf(IntakeValidationError.CUSTOMER_REQUIRED), input.validationErrors())
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
        val result = IntakeOrderCreationResult.Complete(order = testOrder().toDomain())

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

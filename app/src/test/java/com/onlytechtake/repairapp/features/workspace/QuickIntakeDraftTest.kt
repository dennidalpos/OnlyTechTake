package com.onlytechtake.repairapp.features.workspace

import com.onlytechtake.repairapp.data.local.entities.CustomerType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class QuickIntakeDraftTest {
    @Test
    fun draftKeepsEnteredCustomerProblemAndCategoryFields() {
        val draft =
            QuickIntakeDraft().copy(
                selectedCustomerId = "customer-1",
                selectedCustomerLabel = "Mario Rossi",
                selectedCategoryId = "category-1",
                customerType = CustomerType.BUSINESS,
                companyName = "Only Tech",
                problemSummary = "Display rotto",
                problemDetails = "Vetro crepato",
                currentStep = 3,
                completedStep = 2,
            )

        assertEquals("customer-1", draft.selectedCustomerId)
        assertEquals("Mario Rossi", draft.selectedCustomerLabel)
        assertEquals("category-1", draft.selectedCategoryId)
        assertEquals(CustomerType.BUSINESS, draft.customerType)
        assertEquals("Only Tech", draft.companyName)
        assertEquals("Display rotto", draft.problemSummary)
        assertEquals("Vetro crepato", draft.problemDetails)
        assertEquals(3, draft.currentStep)
        assertEquals(2, draft.completedStep)
    }

    @Test
    fun newDraftAfterSubmitClearsUserInput() {
        val reset = QuickIntakeDraft()

        assertNull(reset.selectedCustomerId)
        assertNull(reset.selectedCategoryId)
        assertEquals("", reset.problemSummary)
        assertEquals("", reset.problemDetails)
        assertEquals(1, reset.currentStep)
        assertEquals(0, reset.completedStep)
        assertFalse(reset.returnToIntakeAfterCategorySave)
    }

    @Test
    fun categoryCreatedFromIntakeSelectsCategoryAndInvalidatesFollowingSteps() {
        val draft =
            QuickIntakeDraft(
                selectedCustomerId = "customer-1",
                problemSummary = "Display rotto",
                currentStep = 5,
                completedStep = 5,
                returnToIntakeAfterCategorySave = true,
            ).withCategoryCreatedFromIntake("category-new")

        assertEquals("category-new", draft.selectedCategoryId)
        assertEquals("customer-1", draft.selectedCustomerId)
        assertEquals("Display rotto", draft.problemSummary)
        assertEquals(2, draft.currentStep)
        assertEquals(1, draft.completedStep)
        assertFalse(draft.returnToIntakeAfterCategorySave)
    }
}

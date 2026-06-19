package com.onlytechtake.repairapp.features.dashboard.neworder

import com.onlytechtake.repairapp.data.local.entities.CustomerType
import com.onlytechtake.repairapp.data.local.entities.OrderCategoryEntity
import com.onlytechtake.repairapp.data.repositories.StagedOrderMedia
import com.onlytechtake.repairapp.features.workspace.QuickIntakeDraft
import com.onlytechtake.repairapp.features.workspace.categoryPath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IntakeStepperTest {
    @Test
    fun advanceLocksStepAndMovesForward() {
        assertEquals(3, completedStepAfterAdvance(completedStep = 2, step = 3))
        assertEquals(4, currentStepAfterAdvance(step = 3))
    }

    @Test
    fun stepNavigationIsClampedToKnownBounds() {
        assertEquals(0, completedStepAfterEdit(completedStep = 5, step = -2))
        assertEquals(5, completedStepAfterAdvance(completedStep = 0, step = 99))
        assertEquals(6, currentStepAfterAdvance(step = 99))
    }

    @Test
    fun editingPreviousStepRequiresFollowingStepsToBeConfirmedAgain() {
        assertEquals(1, completedStepAfterEdit(completedStep = 5, step = 2))
        assertFalse(isIntakeSubmitUnlocked(completedStep = 1))
    }

    @Test
    fun submitUnlocksOnlyAfterSignatureStep() {
        assertFalse(isIntakeSubmitUnlocked(completedStep = 4))
        assertTrue(isIntakeSubmitUnlocked(completedStep = 5))
    }

    @Test
    fun categoryClickTogglesCurrentSelection() {
        assertEquals(
            "phone",
            selectedCategoryAfterClick(currentCategoryId = null, clickedCategoryId = "phone"),
        )
        assertEquals(
            "tablet",
            selectedCategoryAfterClick(currentCategoryId = "phone", clickedCategoryId = "tablet"),
        )
        assertEquals(
            null,
            selectedCategoryAfterClick(currentCategoryId = "phone", clickedCategoryId = "phone"),
        )
    }

    @Test
    fun categoryChangeInvalidatesFollowingSteps() {
        assertEquals(1, completedStepAfterCategorySelection(completedStep = 5))
    }

    @Test
    fun categoryPathSupportsArbitraryDepth() {
        val categories =
            (0..5).map { level ->
                OrderCategoryEntity(
                    id = "level-$level",
                    parentId = "level-${level - 1}".takeIf { level > 0 },
                    level = level,
                    name = "L$level",
                    description = null,
                    active = true,
                )
            }

        assertEquals("L0 > L1 > L2 > L3 > L4 > L5", categoryPath(categories, "level-5"))
    }

    @Test
    fun dirtyStateIdentifiesUserChanges() {
        val draft = QuickIntakeDraft()
        assertFalse(calculateIsDirty(draft))

        assertTrue(calculateIsDirty(draft.copy(problemSummary = "Broken screen")))
        assertTrue(calculateIsDirty(draft.copy(selectedCategoryId = "display")))
        assertTrue(
            calculateIsDirty(
                draft.copy(
                    stagedMedia =
                        listOf(
                            StagedOrderMedia(
                                localPath = "p",
                                fileName = "p.jpg",
                                mimeType = "image/jpeg",
                                mediaType = "PHOTO",
                                sizeBytes = 1024,
                                width = 800,
                                height = 600,
                            ),
                        ),
                ),
            ),
        )
    }

    @Test
    fun canSaveNewCustomerValidatesMinimumFields() {
        assertFalse(calculateCanSaveNewCustomer(CustomerType.PRIVATE, "", "", "", "", ""))

        // Private requires First Name, Last Name and Phone or Email
        assertFalse(calculateCanSaveNewCustomer(CustomerType.PRIVATE, "John", "", "", "123", ""))
        assertTrue(calculateCanSaveNewCustomer(CustomerType.PRIVATE, "John", "Doe", "", "123", ""))
        assertTrue(
            calculateCanSaveNewCustomer(
                CustomerType.PRIVATE,
                "John",
                "Doe",
                "",
                "",
                "john@example.com",
            ),
        )

        // Business requires Company Name and Phone or Email
        assertFalse(calculateCanSaveNewCustomer(CustomerType.BUSINESS, "", "", "Acme Corp", "", ""))
        assertTrue(
            calculateCanSaveNewCustomer(
                CustomerType.BUSINESS,
                "",
                "",
                "Acme Corp",
                "123",
                "",
            ),
        )
    }
}

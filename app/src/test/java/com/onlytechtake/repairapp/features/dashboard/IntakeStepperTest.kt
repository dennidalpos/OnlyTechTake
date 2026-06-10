package com.onlytechtake.repairapp.features.dashboard

import com.onlytechtake.repairapp.data.local.entities.OrderCategoryEntity
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
        assertEquals("phone", selectedCategoryAfterClick(currentCategoryId = null, clickedCategoryId = "phone"))
        assertEquals("tablet", selectedCategoryAfterClick(currentCategoryId = "phone", clickedCategoryId = "tablet"))
        assertEquals(null, selectedCategoryAfterClick(currentCategoryId = "phone", clickedCategoryId = "phone"))
    }

    @Test
    fun categoryChangeInvalidatesFollowingSteps() {
        assertEquals(1, completedStepAfterCategorySelection(completedStep = 5))
    }

    @Test
    fun categoryPathSupportsArbitraryDepth() {
        val categories = (0..5).map { level ->
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
}

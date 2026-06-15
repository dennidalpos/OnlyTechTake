package com.onlytechtake.repairapp.core.design

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.onlytechtake.repairapp.core.design.components.AppButton
import com.onlytechtake.repairapp.core.design.components.AppButtonSize
import com.onlytechtake.repairapp.core.design.components.StandardCard
import com.onlytechtake.repairapp.features.dashboard.common.CustomerTypeToggle
import com.onlytechtake.repairapp.features.dashboard.neworder.LAST_INTAKE_STEP
import com.onlytechtake.repairapp.features.dashboard.neworder.StepperIndicator
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DesignComponentsTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun compactButtonKeepsMinimumTouchTarget() {
        composeRule.setContent {
            RepairAppTheme {
                AppButton(
                    text = "Compatto",
                    size = AppButtonSize.Compact,
                    testTag = "compact_button",
                    onClick = {},
                )
            }
        }

        composeRule.onNodeWithTag("compact_button")
            .assertHeightIsAtLeast(RepairAppSizes.touchTarget)
    }

    @Test
    fun clickableCardExposesButtonRole() {
        composeRule.setContent {
            RepairAppTheme {
                StandardCard(
                    onClick = {},
                ) {}
            }
        }

        composeRule.onNode(
            SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button),
            useUnmergedTree = true,
        ).assertExists()
    }

    @Test
    fun customerTypeToggleExposesSelectedState() {
        composeRule.setContent {
            RepairAppTheme {
                CustomerTypeToggle(isBusiness = true, onBusinessChange = {})
            }
        }

        composeRule.onNodeWithTag(RepairAppTestTags.CustomerBusinessToggle)
            .assertIsSelected()
    }

    @Test
    fun intakeStepperExposesStableTagsAndTouchTargets() {
        composeRule.setContent {
            RepairAppTheme {
                StepperIndicator(currentStep = 1, completedStep = 0, onStepClick = {})
            }
        }

        composeRule.onNodeWithTag(RepairAppTestTags.intakeStepperStep(1))
            .assertHeightIsAtLeast(48.dp)
        composeRule.onNodeWithTag(RepairAppTestTags.intakeStepperStep(LAST_INTAKE_STEP))
            .assertHeightIsAtLeast(48.dp)
    }
}

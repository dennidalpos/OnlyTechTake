package com.onlytechtake.repairapp.demo

import android.content.Context
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToIndex
import com.onlytechtake.repairapp.core.design.RepairAppTestTags
import com.onlytechtake.repairapp.domain.workspace.OrderStatusAction

class DemoAssetRobot(
    composeRule: ComposeTestRule,
    context: Context,
) : BaseAssetRobot(composeRule, context)

class ProAssetRobot(
    composeRule: ComposeTestRule,
    context: Context,
) : BaseAssetRobot(composeRule, context) {
    fun applySimpleStatus(
        action: OrderStatusAction,
        nextAction: OrderStatusAction,
    ) {
        waitForTag(RepairAppTestTags.OrderDetailLazyColumn)
        scrollOrderDetailToTop()
        waitForTag(RepairAppTestTags.OrderActionSection, scroll = true)
        clickTag(RepairAppTestTags.statusAction(action.name), scroll = true)
        waitForTag(RepairAppTestTags.StatusDialogConfirmButton)
        clickTag(RepairAppTestTags.StatusDialogConfirmButton)
        waitForTagGone(RepairAppTestTags.StatusDialogConfirmButton)
        composeRule.waitForIdle()
        dismissWhatsAppPreviewIfPresent()
        waitForTag(RepairAppTestTags.statusAction(nextAction.name), scroll = true)
    }

    fun applyResultStatus(
        summary: String = "Display AMOLED originale Samsung.",
        estimate: String = "249,00",
    ) {
        waitForTag(RepairAppTestTags.OrderActionSection, scroll = true)
        clickTag(
            RepairAppTestTags.statusAction(OrderStatusAction.RECORD_RESULT.name),
            scroll = true,
        )
        waitForTag(RepairAppTestTags.StatusResultSummaryField)
        typeTag(RepairAppTestTags.StatusResultSummaryField, summary, scroll = false)
        clickTag(RepairAppTestTags.DiagnosisRepairableToggle)
        typeTag(RepairAppTestTags.DiagnosisEstimateField, estimate, scroll = false)
        clickTag(RepairAppTestTags.StatusDialogConfirmButton)
        waitForTagGone(RepairAppTestTags.StatusDialogConfirmButton)
        composeRule.waitForIdle()
        dismissWhatsAppPreviewIfPresent()
        waitForTag(RepairAppTestTags.QuoteAcceptedToggle, scroll = true)
    }

    fun acceptQuote() {
        composeRule.onNodeWithTag(RepairAppTestTags.OrderDetailLazyColumn).performScrollToIndex(2)
        composeRule.waitForIdle()
        waitForTag(RepairAppTestTags.QuoteAcceptedToggle)
        clickTag(RepairAppTestTags.QuoteAcceptedToggle)
        clickTag(RepairAppTestTags.QuoteConfirmButton, scroll = true)
        composeRule.waitForIdle()
        waitForTag(
            RepairAppTestTags.statusAction(OrderStatusAction.READY_FOR_PICKUP.name),
            scroll = true,
        )
    }

    fun closeOrder(note: String = "Riparazione completata con successo.") {
        waitForTag(RepairAppTestTags.OrderActionSection, scroll = true)
        clickTag(RepairAppTestTags.statusAction(OrderStatusAction.CLOSE_ORDER.name), scroll = true)
        waitForTag(RepairAppTestTags.StatusNoteField)
        typeTag(RepairAppTestTags.StatusNoteField, note)
        clickTag(RepairAppTestTags.closureReason("COMPLETED"))
        clickTag(RepairAppTestTags.StatusDialogConfirmButton)
        waitForTagGone(RepairAppTestTags.StatusDialogConfirmButton)
        composeRule.waitForIdle()
        waitForText("Commesse")
    }

    fun scrollOrderDetailToTop() {
        composeRule.onNodeWithTag(RepairAppTestTags.OrderDetailLazyColumn).performScrollToIndex(0)
        composeRule.waitForIdle()
    }
}

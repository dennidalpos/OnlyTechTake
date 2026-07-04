package com.onlytechtake.repairapp.demo

import android.content.Context
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToIndex
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.core.design.RepairAppTestTags
import com.onlytechtake.repairapp.domain.workspace.OrderStatusAction
import com.onlytechtake.repairapp.test.R as TestR

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
        summary: String? = null,
        estimate: String? = null,
    ) {
        val finalSummary = summary ?: getTestString(TestR.string.demo_diagnosis_result_s23)
        val finalEstimate = estimate ?: getTestString(TestR.string.demo_estimate_s23)

        waitForTag(RepairAppTestTags.OrderActionSection, scroll = true)
        clickTag(
            RepairAppTestTags.statusAction(OrderStatusAction.RECORD_RESULT.name),
            scroll = true,
        )
        waitForTag(RepairAppTestTags.StatusResultSummaryField)
        typeTag(
            RepairAppTestTags.StatusResultSummaryField,
            finalSummary,
            scroll = false,
            fast = true
        )
        clickTag(RepairAppTestTags.DiagnosisRepairableToggle)
        typeTag(
            RepairAppTestTags.DiagnosisEstimateField,
            finalEstimate,
            scroll = false,
            fast = true
        )
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
        clickTag(RepairAppTestTags.statusAction(OrderStatusAction.START_REPAIR.name), scroll = true)
        composeRule.waitForIdle()
        waitForTag(
            RepairAppTestTags.statusAction(OrderStatusAction.READY_FOR_PICKUP.name),
            scroll = true,
        )
    }

    fun recordTechnicalVideo() {
        waitForTag(RepairAppTestTags.IntakeMediaTakeVideoButton, scroll = true)
        clickTag(RepairAppTestTags.IntakeMediaTakeVideoButton)
        waitForTag(RepairAppTestTags.IntakeMediaCaptureVideoButton)
        clickTag(RepairAppTestTags.IntakeMediaCaptureVideoButton)
        narrativePause(2)
        clickTag(RepairAppTestTags.IntakeMediaStopVideoButton)
        waitForTagGone(RepairAppTestTags.IntakeMediaStopVideoButton)
        composeRule.waitForIdle()
    }

    fun closeOrder(note: String? = null) {
        val finalNote = note ?: getTestString(TestR.string.demo_closure_note_completed)

        waitForTag(RepairAppTestTags.OrderActionSection, scroll = true)
        clickTag(RepairAppTestTags.statusAction(OrderStatusAction.CLOSE_ORDER.name), scroll = true)
        waitForTag(RepairAppTestTags.StatusNoteField)
        typeTag(RepairAppTestTags.StatusNoteField, finalNote, fast = true)
        clickTag(RepairAppTestTags.closureReason("COMPLETED"))
        clickTag(RepairAppTestTags.StatusDialogConfirmButton)
        waitForTagGone(RepairAppTestTags.StatusDialogConfirmButton)
        composeRule.waitForIdle()
        waitForText(R.string.dashboard_orders)
    }

    fun expandNotes() {
        waitForTag(RepairAppTestTags.OrderNotesSectionCard, scroll = true)
        clickTag(RepairAppTestTags.OrderNotesSectionCard)
        composeRule.waitForIdle()
    }

    fun expandPhotos() {
        waitForTag(RepairAppTestTags.OrderPhotosSectionCard, scroll = true)
        clickTag(RepairAppTestTags.OrderPhotosSectionCard)
        composeRule.waitForIdle()
    }

    fun expandSignatures() {
        waitForTag(RepairAppTestTags.OrderSignaturesCard, scroll = true)
        clickTag(RepairAppTestTags.OrderSignaturesCard)
        composeRule.waitForIdle()
    }

    fun expandTimeline() {
        waitForTag(RepairAppTestTags.OrderTimelineSection, scroll = true)
        clickTag(RepairAppTestTags.OrderTimelineSection)
        composeRule.waitForIdle()
    }

    private fun scrollOrderDetailToTop() {
        composeRule.onNodeWithTag(RepairAppTestTags.OrderDetailLazyColumn).performScrollToIndex(0)
        composeRule.waitForIdle()
    }
}

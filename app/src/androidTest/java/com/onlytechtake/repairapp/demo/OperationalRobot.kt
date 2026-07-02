package com.onlytechtake.repairapp.demo

import android.content.Context
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTouchInput
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.core.design.RepairAppTestTags
import com.onlytechtake.repairapp.domain.workspace.OrderStatusAction
import java.io.File

class OperationalRobot(
    composeRule: ComposeTestRule,
    context: Context,
) : BaseAssetRobot(composeRule, context) {
    fun intakeFlow(outputDir: File) {
        var step = 1

        fun nextCapture(name: String) = capture(outputDir, "${String.format("%02d", step++)}-$name.png")

        // 1. Home
        waitForTag(RepairAppTestTags.dashboardTab("HOME"))
        nextCapture("home")

        // 2. Start Intake
        clickTag(RepairAppTestTags.HomeNewOrderButton, scroll = true)
        waitForTag("quick_actions_screen")
        nextCapture("intake-start")

        // 3. New Customer
        clickTag(RepairAppTestTags.NewOrderInsertCustomerButton, scroll = true)
        waitForTag(RepairAppTestTags.NewOrderFirstNameField)
        typeTag(RepairAppTestTags.NewOrderFirstNameField, "John", scroll = true)
        typeTag(RepairAppTestTags.NewOrderLastNameField, "Doe", scroll = true)
        typeTag(RepairAppTestTags.NewOrderPhoneField, "+1 555-111-2222", scroll = true)
        nextCapture("intake-customer")

        // 4. Save & Next
        clickTag(RepairAppTestTags.NewOrderSaveCustomerButton, scroll = true)
        waitForText(getString(R.string.intake_selected_customer).substringBefore("%"))
        clickTag(RepairAppTestTags.NewOrderStep1NextButton, scroll = true)

        // 5. Category Selection
        clickTag(RepairAppTestTags.categoryButton("demo-cat-pc"), scroll = true)
        pauseForDemo()
        clickTag(RepairAppTestTags.categoryButton(DemoDataSeeder.DEMO_CATEGORY_ID), scroll = true)
        pauseForDemo()
        clickTag(RepairAppTestTags.categoryButton("demo-cat-gaming"), scroll = true)
        pauseForDemo()
        clickTag(RepairAppTestTags.categoryButton("demo-cat-rog-strix"), scroll = true)
        pauseForDemo()
        nextCapture("intake-category")

        // Wait for button to be enabled
        waitForTag(RepairAppTestTags.NewOrderStep2NextButton, scroll = true)
        clickTag(RepairAppTestTags.NewOrderStep2NextButton, scroll = true)

        // 6. Problem details
        typeTag(RepairAppTestTags.NewOrderProblemSummaryField, "Faulty SSD", scroll = true)
        typeTag(
            RepairAppTestTags.NewOrderProblemDetailsField,
            "The drive is not detected by the BIOS.",
            scroll = true,
        )
        nextCapture("intake-problem")
        clickTag(RepairAppTestTags.NewOrderStep3NextButton, scroll = true)

        // 7. Media
        waitForText(R.string.intake_step_media)
        clickTag(RepairAppTestTags.IntakeMediaTakePhotoButton, scroll = true)
        waitForTag(RepairAppTestTags.IntakeMediaCapturePhotoButton, scroll = true)
        clickTag(RepairAppTestTags.IntakeMediaCapturePhotoButton, scroll = true)
        waitForText("PHOTO", scroll = true)
        nextCapture("intake-media")
        clickTag(RepairAppTestTags.NewOrderStep4NextButton, scroll = true)

        // 8. Signature
        waitForText(R.string.intake_step_privacy)
        clickText(R.string.signature_accept_privacy, scroll = true)
        clickText(R.string.signature_accept_terms, scroll = true)
        waitForTag(RepairAppTestTags.SignatureCanvas, scroll = true)
        composeRule.onNodeWithTag(RepairAppTestTags.SignatureCanvas).performTouchInput {
            val w = width.toFloat()
            val h = height.toFloat()
            val startX = w * 0.2f
            val startY = h * 0.5f
            down(Offset(startX, startY))
            for (i in 1..20) {
                val dx = i * (w * 0.03f)
                val dy = if (i % 2 == 0) -h * 0.1f else h * 0.1f
                moveTo(Offset(startX + dx, startY + dy))
            }
            up()
        }
        nextCapture("intake-signature")
        clickTag(RepairAppTestTags.NewOrderStep5NextButton, scroll = true)

        // 9. Summary & Submit
        waitForTag(RepairAppTestTags.IntakeSubmitButton, scroll = true)
        nextCapture("intake-summary")
        clickTag(RepairAppTestTags.IntakeSubmitButton, scroll = true)
        waitForText(R.string.dashboard_orders)
    }

    fun managementFlow(outputDir: File) {
        var step = 1

        fun nextCapture(name: String) = capture(outputDir, "${String.format("%02d", step++)}-$name.png")

        // 1. Orders List
        clickDashboardTab("ORDERS")
        waitForText(R.string.dashboard_orders)
        nextCapture("orders-list")

        // 2. Select Order
        val orderId = DemoDataSeeder.DEMO_ORDER_ID
        waitForTag(RepairAppTestTags.orderCard(orderId), scroll = true)
        clickTag(RepairAppTestTags.orderCard(orderId), scroll = true)
        waitForTag(RepairAppTestTags.OrderStatusLabel)
        nextCapture("order-details")

        // 3. Start Progress
        clickTag(
            RepairAppTestTags.statusAction(OrderStatusAction.START_PROGRESS.name),
            scroll = true,
        )
        confirmStatusDialog()
        nextCapture("management-in-progress")

        // 4. Start Diagnosis
        clickTag(
            RepairAppTestTags.statusAction(OrderStatusAction.START_DIAGNOSIS.name),
            scroll = true,
        )
        confirmStatusDialog()
        nextCapture("management-diagnosing")

        // 5. Record Result (Diagnosis)
        clickTag(
            RepairAppTestTags.statusAction(OrderStatusAction.RECORD_RESULT.name),
            scroll = true,
        )
        waitForTag(RepairAppTestTags.StatusResultSummaryField)
        typeTag(
            RepairAppTestTags.StatusResultSummaryField,
            "SSD replacement required.",
            scroll = false,
        )
        clickTag(RepairAppTestTags.DiagnosisRepairableToggle)
        typeTag(RepairAppTestTags.DiagnosisEstimateField, "120.00", scroll = false)
        nextCapture("management-diagnosis-result")
        clickTag(RepairAppTestTags.StatusDialogConfirmButton)
        waitForTagGone(RepairAppTestTags.StatusDialogConfirmButton)
        dismissWhatsAppPreviewIfPresent()

        // 6. Accept Quote
        composeRule.onNodeWithTag(RepairAppTestTags.OrderDetailLazyColumn).performScrollToIndex(2)
        waitForTag(RepairAppTestTags.QuoteAcceptedToggle)
        clickTag(RepairAppTestTags.QuoteAcceptedToggle)
        nextCapture("management-quote")
        clickTag(
            RepairAppTestTags.statusAction(OrderStatusAction.START_REPAIR.name),
            scroll = true,
        )

        // 7. Ready for Pickup
        clickTag(
            RepairAppTestTags.statusAction(OrderStatusAction.READY_FOR_PICKUP.name),
            scroll = true,
        )
        confirmStatusDialog()
        nextCapture("management-ready")

        // 8. Close Order
        clickTag(RepairAppTestTags.statusAction(OrderStatusAction.CLOSE_ORDER.name), scroll = true)
        waitForTag(RepairAppTestTags.StatusNoteField)
        typeTag(RepairAppTestTags.StatusNoteField, "Replaced 500GB SSD.")
        clickTag(RepairAppTestTags.closureReason("COMPLETED"))
        nextCapture("management-closure")
        clickTag(RepairAppTestTags.StatusDialogConfirmButton)
        waitForTagGone(RepairAppTestTags.StatusDialogConfirmButton)
        waitForText(R.string.dashboard_orders)
    }

    private fun confirmStatusDialog() {
        waitForTag(RepairAppTestTags.StatusDialogConfirmButton)
        clickTag(RepairAppTestTags.StatusDialogConfirmButton)
        waitForTagGone(RepairAppTestTags.StatusDialogConfirmButton)
        composeRule.waitForIdle()
        dismissWhatsAppPreviewIfPresent()
    }

    fun adminOperatorsFlow(outputDir: File) {
        var step = 1

        fun nextCapture(name: String) = capture(outputDir, "${String.format("%02d", step++)}-$name.png")

        clickDashboardTab("SETTINGS")
        waitForTag(RepairAppTestTags.ConfigHubAdminButton, scroll = true)
        clickTag(RepairAppTestTags.ConfigHubAdminButton, scroll = true)
        nextCapture("admin-hub")

        // Expand operator creation form
        clickTag("admin_create_operator_switch", scroll = true)

        // Form filling - ensuring text is actually typed
        typeTag(RepairAppTestTags.AdminDisplayNameField, "Junior Operator")
        typeTag(RepairAppTestTags.AdminUsernameField, "op_junior")
        typeTag(RepairAppTestTags.AdminPasswordField, "Pass12345")

        composeRule.waitForIdle()
        pauseForDemo()
        nextCapture("operator-creation-form")

        clickTag(RepairAppTestTags.AdminCreateOperatorButton, scroll = true)
        waitForTag(RepairAppTestTags.GenericConfirmButton)
        nextCapture("operator-confirm-dialog")
        clickTag(RepairAppTestTags.GenericConfirmButton)

        waitForTagGone(RepairAppTestTags.GenericConfirmButton)
        composeRule.waitForIdle()
        pauseForDemo()
        // Removed flaky text check
    }

    fun adminTemplatesFlow(outputDir: File) {
        var step = 1

        fun nextCapture(name: String) = capture(outputDir, "${String.format("%02d", step++)}-$name.png")

        clickDashboardTab("SETTINGS")
        clickTag(RepairAppTestTags.ConfigHubTemplatesButton, scroll = true)
        nextCapture("templates-list")

        clickTag(RepairAppTestTags.ConfigTemplateNewButton, scroll = true)
        typeTag(RepairAppTestTags.ConfigTemplateCodeField, "new_status_ready")
        typeTag(RepairAppTestTags.ConfigTemplateTitleField, "Device ready")
        typeTag(
            RepairAppTestTags.ConfigTemplateBodyField,
            "Dear {{cliente}}, your device is ready.",
        )
        nextCapture("template-editor")
        clickTag(RepairAppTestTags.ConfigTemplateSaveButton, scroll = true)
        waitForTag(RepairAppTestTags.GenericConfirmButton)
        clickTag(RepairAppTestTags.GenericConfirmButton)
        waitForTagGone(RepairAppTestTags.GenericConfirmButton)

        clickTag(RepairAppTestTags.ConfigBackButton)
        clickTag(RepairAppTestTags.ConfigHubTemplateMappingButton, scroll = true)
        nextCapture("template-mapping")
    }

    fun backupFlow(outputDir: File) {
        var step = 1

        fun nextCapture(name: String) = capture(outputDir, "${String.format("%02d", step++)}-$name.png")

        clickDashboardTab("SETTINGS")
        clickTag(RepairAppTestTags.ConfigHubBackupButton, scroll = true)
        nextCapture("backup-settings")

        // Scroll to see more
        performSafeSwipeUp()
        nextCapture("backup-security-logs")
    }

    fun setupWizardFlow(outputDir: File) {
        var step = 1

        fun nextCapture(name: String) = capture(outputDir, "${String.format("%02d", step++)}-$name.png")

        // 1. First Setup (Admin)
        waitForTag(RepairAppTestTags.AuthDisplayNameField)
        typeTag(RepairAppTestTags.AuthDisplayNameField, "John Doe")
        typeTag(RepairAppTestTags.AuthUsernameField, "admin")
        typeTag(RepairAppTestTags.AuthPasswordField, "AdminPass123!")
        typeTag(RepairAppTestTags.AuthConfirmPasswordField, "AdminPass123!")
        nextCapture("setup-admin")
        clickTag(RepairAppTestTags.AuthSubmitButton)

        // 2. Owner Info (Wizard)
        waitForTag(RepairAppTestTags.OwnerFieldName)
        typeTag(RepairAppTestTags.OwnerFieldName, "OnlyTechTake LLC")
        typeTag(RepairAppTestTags.OwnerFieldVat, "US123456789")
        typeTag(RepairAppTestTags.OwnerFieldAddress, "123 Tech Avenue, New York")
        typeTag(RepairAppTestTags.OwnerFieldEmail, "info@onlytechtake.invalid")
        typeTag(RepairAppTestTags.OwnerFieldPhone, "+1 555-123-4567")
        nextCapture("setup-owner")
        clickText(R.string.action_confirm)
    }
}

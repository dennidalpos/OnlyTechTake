package com.onlytechtake.repairapp.demo

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.click
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.swipeUp
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.onlytechtake.repairapp.BuildConfig
import com.onlytechtake.repairapp.MainActivity
import com.onlytechtake.repairapp.core.design.RepairAppTestTags
import com.onlytechtake.repairapp.data.local.db.RepairAppDatabase
import com.onlytechtake.repairapp.domain.workspace.OrderStatusAction
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

@RunWith(AndroidJUnit4::class)
class OperatorIntakeDemoScreenshotTest {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    @get:Rule
    val cameraPermissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(android.Manifest.permission.CAMERA)

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun capturesOperatorIntakeDemo() {
        runBlocking { DemoDataSeeder.seedDemoProfile(context) }
        val outputDir = prepareScreenshotDirectory()

        ActivityScenario.launch(MainActivity::class.java).use {
            waitForTag(RepairAppTestTags.AuthUsernameField)
            capture(outputDir, "01-login.png")

            loginOperatorDemo()
            capture(outputDir, "02-home.png")

            clickTag(RepairAppTestTags.HomeNewOrderButton, scroll = true)
            pauseForDemo()
            waitForTag("quick_actions_screen")

            var formOpened = false
            repeat(3) {
                if (!formOpened) {
                    if (isTagPresent(RepairAppTestTags.NewOrderInsertCustomerButton)) {
                        clickTag(RepairAppTestTags.NewOrderInsertCustomerButton, scroll = true)
                    }
                    pauseForDemo()
                    formOpened = isTagPresent(RepairAppTestTags.NewOrderFirstNameField)
                }
            }

            typeTag(RepairAppTestTags.NewOrderFirstNameField, "Giulia", scroll = true)
            typeTag(RepairAppTestTags.NewOrderLastNameField, "Bianchi", scroll = true)
            typeTag(RepairAppTestTags.NewOrderPhoneField, "+39 333 010 2020", scroll = true)
            typeTag(
                RepairAppTestTags.NewOrderEmailField,
                "giulia.bianchi@example.invalid",
                scroll = true,
            )
            capture(outputDir, "03-new-customer.png")

            clickTag(RepairAppTestTags.NewOrderSaveCustomerButton, scroll = true)
            waitForText("Cliente selezionato")
            clickTag(RepairAppTestTags.NewOrderStep1NextButton, scroll = true)

            clickTag(RepairAppTestTags.categoryButton("demo-cat-pc"), scroll = true)
            clickTag(
                RepairAppTestTags.categoryButton(DemoDataSeeder.DEMO_CATEGORY_ID),
                scroll = true,
            )
            clickTag(RepairAppTestTags.categoryButton("demo-cat-gaming"), scroll = true)
            clickTag(RepairAppTestTags.categoryButton("demo-cat-rog-strix"), scroll = true)

            clickTag(RepairAppTestTags.NewOrderStep2NextButton, scroll = true)
            typeTag(
                RepairAppTestTags.NewOrderProblemSummaryField,
                "Notebook non si accende",
                scroll = true,
            )
            typeTag(
                RepairAppTestTags.NewOrderProblemDetailsField,
                "Il cliente segnala spegnimento improvviso dopo caduta accidentale.",
                scroll = true,
            )
            capture(outputDir, "04-category-problem.png")

            clickTag(RepairAppTestTags.NewOrderStep3NextButton, scroll = true)
            waitForText("Foto e allegati")
            clickTag(RepairAppTestTags.IntakeMediaTakePhotoButton, scroll = true)
            waitForTag(RepairAppTestTags.IntakeMediaCapturePhotoButton, scroll = true)
            clickTag(RepairAppTestTags.IntakeMediaCapturePhotoButton, scroll = true)
            waitForText("PHOTO", scroll = true)
            clickTag(RepairAppTestTags.NewOrderStep4NextButton, scroll = true)

            waitForText("Privacy e firma")
            clickText("Accetta informativa privacy", scroll = true)
            clickText("Accetta condizioni di presa in carico", scroll = true)
            waitForTag(RepairAppTestTags.SignatureCanvas, scroll = true)
            composeRule.onNodeWithTag(RepairAppTestTags.SignatureCanvas).performTouchInput {
                swipeRight()
            }
            waitForTag(RepairAppTestTags.NewOrderStep5NextButton, scroll = true)
            capture(outputDir, "05-signature-capture.png")

            clickTag(RepairAppTestTags.NewOrderStep5NextButton, scroll = true)
            waitForTag(RepairAppTestTags.IntakeSubmitButton, scroll = true)
            capture(outputDir, "06-summary-submit.png")
            clickTag(RepairAppTestTags.IntakeSubmitButton, scroll = true)

            val createdOrderId = waitForCreatedOrderId()
            clickDashboardTab("ORDERS")
            waitForText("Commesse")

            // Show Back to Top in Orders
            composeRule.onNodeWithTag("orders_list").performTouchInput { swipeUp() }
            pauseForDemo()
            if (isTagPresent(RepairAppTestTags.ScrollToTopButton)) {
                capture(outputDir, "06b-back-to-top-orders.png")
                clickTag(RepairAppTestTags.ScrollToTopButton)
                pauseForDemo()
            }

            waitForTag(RepairAppTestTags.orderCard(createdOrderId), scroll = true)
            clickTag(RepairAppTestTags.orderCard(createdOrderId), scroll = true)

            waitForTag(RepairAppTestTags.OrderStatusLabel)
            waitForTag(RepairAppTestTags.OrderDetailOrderReference, useUnmergedTree = true)
            waitForTag(RepairAppTestTags.OrderDetailCustomerReference, useUnmergedTree = true)
            waitForTag(RepairAppTestTags.orderDetailStatusStep(1), useUnmergedTree = true)
            waitForText("Asus ROG Strix")
            capture(outputDir, "07-order-created.png")

            waitForTag(RepairAppTestTags.OrderWhatsAppButton, scroll = true)
            clickTag(RepairAppTestTags.OrderWhatsAppButton)
            waitForTag(RepairAppTestTags.OrderWhatsAppPreviewPanel)
            capture(outputDir, "08-whatsapp-preview.png")
            dismissWhatsAppPreviewIfPresent()
            pauseForDemo()

            clickTag(RepairAppTestTags.OrderSignaturesCard, scroll = true)
            composeRule.waitForIdle()

            pauseForDemo()
            waitForTag(
                RepairAppTestTags.OrderSignatureSection,
                scroll = true,
                useUnmergedTree = true,
            )
            capture(outputDir, "09-signature-detail.png")

            // Collapse signatures card to keep UI clean and test the toggle
            clickTag(RepairAppTestTags.OrderSignaturesCard)
            pauseForDemo()

            applySimpleStatus(
                OrderStatusAction.START_PROGRESS,
                nextAction = OrderStatusAction.START_DIAGNOSIS,
            )
            capture(outputDir, "10-in-progress.png")

            applySimpleStatus(
                OrderStatusAction.START_DIAGNOSIS,
                nextAction = OrderStatusAction.RECORD_RESULT,
            )
            applyResultStatus()
            capture(outputDir, "11-diagnosis-result.png")

            waitForText("Documenti cliente", scroll = true)
            capture(outputDir, "12-pdf-documents.png")

            acceptQuote()
            applySimpleStatus(
                OrderStatusAction.READY_FOR_PICKUP,
                nextAction = OrderStatusAction.CLOSE_ORDER,
            )
            capture(outputDir, "13-ready-for-pickup.png")

            closeOrder()
            clickTag(RepairAppTestTags.orderCard(createdOrderId), scroll = true)
            waitForText("Chiusa")
            capture(outputDir, "14-closed-timeline.png")
        }
    }

    private fun waitForCreatedOrderId(): String {
        val database = RepairAppDatabase.getInstance(context)
        val deadline = System.currentTimeMillis() + WAIT_TIMEOUT_MILLIS
        while (System.currentTimeMillis() < deadline) {
            val order =
                runBlocking {
                    database
                        .repairOrderDao()
                        .observeRecentActiveOrders(limit = 20)
                        .first()
                        .firstOrNull {
                            it.id != DemoDataSeeder.DEMO_ORDER_ID &&
                                it.title == "Notebook non si accende"
                        }
                }
            if (order != null) return order.id
            Thread.sleep(TAG_POLL_MILLIS)
        }
        error("Ordine creato dal wizard non trovato")
    }

    private fun applySimpleStatus(
        action: OrderStatusAction,
        nextAction: OrderStatusAction,
    ) {
        scrollOrderDetailToTop()
        waitForTag(RepairAppTestTags.OrderActionSection, scroll = true)
        pauseForDemo()
        clickTag(RepairAppTestTags.statusAction(action.name), scroll = true)
        pauseForDemo()
        waitForTag(RepairAppTestTags.StatusDialogConfirmButton)
        clickTag(RepairAppTestTags.StatusDialogConfirmButton)
        waitForTagGone(RepairAppTestTags.StatusDialogConfirmButton)
        composeRule.waitForIdle()
        pauseForDemo()

        // Handle auto-opened WhatsApp preview after status change
        dismissWhatsAppPreviewIfPresent()

        waitForTag(RepairAppTestTags.statusAction(nextAction.name), scroll = true)
    }

    private fun applyResultStatus() {
        waitForTag(RepairAppTestTags.OrderActionSection, scroll = true)
        pauseForDemo()
        clickTag(
            RepairAppTestTags.statusAction(OrderStatusAction.RECORD_RESULT.name),
            scroll = true,
        )
        pauseForDemo()
        waitForTag(RepairAppTestTags.StatusResultSummaryField)
        typeTag(
            RepairAppTestTags.StatusResultSummaryField,
            "Connettore di alimentazione danneggiato.",
            scroll = false,
        )
        clickTag(RepairAppTestTags.DiagnosisRepairableToggle)
        typeTag(RepairAppTestTags.DiagnosisEstimateField, "149,00", scroll = false)
        clickTag(RepairAppTestTags.StatusDialogConfirmButton)
        waitForTagGone(RepairAppTestTags.StatusDialogConfirmButton)
        composeRule.waitForIdle()
        pauseForDemo()

        // Handle auto-opened WhatsApp preview after recording result
        dismissWhatsAppPreviewIfPresent()

        waitForTag(RepairAppTestTags.QuoteAcceptedToggle, scroll = true)
    }

    private fun acceptQuote() {
        composeRule
            .onNodeWithTag(RepairAppTestTags.OrderDetailLazyColumn)
            .performScrollToIndex(2)
        composeRule.waitForIdle()
        pauseForDemo()
        waitForTag(RepairAppTestTags.QuoteAcceptedToggle)
        clickTag(RepairAppTestTags.QuoteAcceptedToggle)
        pauseForDemo()
        clickTag(RepairAppTestTags.QuoteConfirmButton, scroll = true)
        composeRule.waitForIdle()
        pauseForDemo()
        waitForTag(
            RepairAppTestTags.statusAction(OrderStatusAction.READY_FOR_PICKUP.name),
            scroll = true,
        )
    }

    private fun closeOrder() {
        waitForTag(RepairAppTestTags.OrderActionSection, scroll = true)
        pauseForDemo()
        clickTag(RepairAppTestTags.statusAction(OrderStatusAction.CLOSE_ORDER.name), scroll = true)
        pauseForDemo()
        waitForTag(RepairAppTestTags.StatusNoteField)
        typeTag(
            RepairAppTestTags.StatusNoteField,
            "Riparazione completata e consegnata al cliente.",
        )
        clickTag(RepairAppTestTags.closureReason("COMPLETED"))
        pauseForDemo()
        clickTag(RepairAppTestTags.StatusDialogConfirmButton)
        waitForTagGone(RepairAppTestTags.StatusDialogConfirmButton)
        composeRule.waitForIdle()
        pauseForDemo()
        waitForText("Commesse")
    }

    private fun clickDashboardTab(destination: String) {
        val tabTag = RepairAppTestTags.dashboardTab(destination)
        waitForTagExists(tabTag)

        composeRule.onNodeWithTag(tabTag).performClick()
        composeRule.waitForIdle()

        if (isTextDisplayed("Dati non salvati")) {
            clickTag(RepairAppTestTags.DashboardDiscardChangesButton)
        }

        composeRule.waitForIdle()
        Thread.sleep(2000)
    }

    private fun isTextDisplayed(text: String): Boolean =
        composeRule.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty()

    private fun loginOperatorDemo() {
        val username =
            if (BuildConfig.IS_DEMO_VERSION) DemoDataSeeder.DEMO_ADMIN_USERNAME else DemoDataSeeder.DEMO_OPERATOR_USERNAME
        repeat(LOGIN_ATTEMPTS) { attempt ->
            waitForTag(RepairAppTestTags.AuthUsernameField)
            typeTag(RepairAppTestTags.AuthUsernameField, username)
            pauseForDemo()
            typeTag(RepairAppTestTags.AuthPasswordField, DemoDataSeeder.DEMO_PASSWORD)
            pauseForDemo()
            clickTag(RepairAppTestTags.AuthSubmitButton)

            if (waitForTagIfPresent(
                    RepairAppTestTags.dashboardTab("HOME"),
                    LOGIN_ATTEMPT_TIMEOUT_MILLIS,
                )
            ) {
                return
            }

            if (attempt == (LOGIN_ATTEMPTS - 1) || !isTagPresent(RepairAppTestTags.AuthUsernameField)) {
                waitForTag(RepairAppTestTags.dashboardTab("HOME"))
            }
            pauseForDemo()
        }
    }

    private fun typeTag(
        tag: String,
        text: String,
        scroll: Boolean = false,
    ) {
        waitForTagExists(tag, scroll = scroll)
        val node = composeRule.onNodeWithTag(tag)
        node.performClick()
        composeRule.waitForIdle()
        Thread.sleep(200)
        node.performTextReplacement(text)
        composeRule.waitForIdle()
        Espresso.closeSoftKeyboard()
        Thread.sleep(500)
        pauseForDemo()
    }

    private fun clickTag(
        tag: String,
        scroll: Boolean = false,
        useUnmergedTree: Boolean = false,
    ) {
        waitForTag(tag, scroll = scroll, useUnmergedTree = useUnmergedTree)
        val node = composeRule.onNodeWithTag(tag, useUnmergedTree = useUnmergedTree)

        // Ensure the node is enabled before clicking
        try {
            composeRule.waitUntil(timeoutMillis = 5_000) {
                runCatching { node.assertIsEnabled() }.isSuccess
            }
        } catch (e: Throwable) {
            node.assertIsEnabled()
        }

        try {
            node.performClick()
        } catch (e: Throwable) {
            try {
                node.performTouchInput { click() }
            } catch (e2: Throwable) {
                node.performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick)
            }
        }

        composeRule.waitForIdle()
        pauseForDemo()
    }

    private fun clickText(
        text: String,
        scroll: Boolean = false,
    ) {
        waitForText(text, scroll = scroll)
        val node = composeRule.onNodeWithText(text, substring = true)

        try {
            node.performClick()
        } catch (e: Throwable) {
            try {
                node.performTouchInput { click() }
            } catch (e2: Throwable) {
                node.performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick)
            }
        }

        composeRule.waitForIdle()
        pauseForDemo()
    }

    private fun performSafeSwipeUp() {
        val listTags =
            listOf(
                RepairAppTestTags.OrderDetailLazyColumn,
                "categories_list",
                "templates_list",
                "customer_list",
                "customer_form",
            )
        for (tag in listTags) {
            try {
                composeRule.onNodeWithTag(tag).performTouchInput {
                    swipeUp(startY = bottom * 0.8f, endY = top + 30f)
                }
                composeRule.waitForIdle()
                return
            } catch (e: Throwable) {
            }
        }
        try {
            composeRule.onAllNodes(isRoot()).onFirst().performTouchInput {
                swipeUp(startY = bottom * 0.8f, endY = top + 30f)
            }
        } catch (e: Throwable) {
        }
        composeRule.waitForIdle()
    }

    private fun performSafeSwipeDown() {
        val listTags =
            listOf(
                RepairAppTestTags.OrderDetailLazyColumn,
                "categories_list",
                "templates_list",
                "customer_list",
                "customer_form",
            )
        for (tag in listTags) {
            try {
                composeRule.onNodeWithTag(tag).performTouchInput {
                    swipeDown(startY = top + 30f, endY = bottom * 0.8f)
                }
                composeRule.waitForIdle()
                return
            } catch (e: Throwable) {
            }
        }
        try {
            composeRule.onAllNodes(isRoot()).onFirst().performTouchInput {
                swipeDown(startY = top + 30f, endY = bottom * 0.8f)
            }
        } catch (e: Throwable) {
        }
        composeRule.waitForIdle()
    }

    private fun scrollOrderDetailToTop() {
        composeRule
            .onNodeWithTag(RepairAppTestTags.OrderDetailLazyColumn)
            .performScrollToIndex(0)
        composeRule.waitForIdle()
        pauseForDemo()
    }

    private fun waitForTag(
        tag: String,
        scroll: Boolean = false,
        useUnmergedTree: Boolean = false,
    ) {
        waitForTagExists(tag, scroll = scroll, useUnmergedTree = useUnmergedTree)
        val deadline = System.currentTimeMillis() + WAIT_TIMEOUT_MILLIS
        var attempts = 0
        while (System.currentTimeMillis() < deadline) {
            val displayed =
                runCatching {
                    val node = composeRule.onNodeWithTag(tag, useUnmergedTree = useUnmergedTree)
                    if (scroll) {
                        runCatching { node.performScrollTo() }
                        composeRule.waitForIdle()
                    }
                    node.assertIsDisplayed()
                    true
                }.getOrDefault(false)
            if (displayed) return

            if (scroll) {
                if (attempts % 15 < 10) {
                    performSafeSwipeUp()
                } else {
                    performSafeSwipeDown()
                }
            }
            Thread.sleep(TAG_POLL_MILLIS)
            attempts++
        }
        composeRule.onNodeWithTag(tag, useUnmergedTree = useUnmergedTree).assertIsDisplayed()
    }

    private fun waitForTagExists(
        tag: String,
        scroll: Boolean = false,
        useUnmergedTree: Boolean = false,
    ) {
        var found = false
        val deadline = System.currentTimeMillis() + WAIT_TIMEOUT_MILLIS
        var attempts = 0

        while (!found && System.currentTimeMillis() < deadline) {
            found = isTagPresent(tag, useUnmergedTree)
            if (found) break

            if (scroll) {
                if (attempts % 2 == 0) {
                    try {
                        composeRule
                            .onNodeWithTag(tag, useUnmergedTree = useUnmergedTree)
                            .performScrollTo()
                        composeRule.waitForIdle()
                        if (isTagPresent(tag, useUnmergedTree)) {
                            found = true
                            break
                        }
                    } catch (e: Throwable) {
                        // Scan logic: try DOWN a lot (reveal below), then UP a bit (reveal above)
                        if (attempts % 15 < 10) {
                            performSafeSwipeUp() // Scroll DOWN
                        } else {
                            performSafeSwipeDown() // Scroll UP
                        }
                    }
                }
            }
            if (!found) {
                Thread.sleep(TAG_POLL_MILLIS)
                attempts++
            }
        }
        if (!found) {
            composeRule.onNodeWithTag(tag, useUnmergedTree = useUnmergedTree).assertExists()
        }
    }

    private fun dismissWhatsAppPreviewIfPresent() {
        if (waitForTagIfPresent(
                RepairAppTestTags.OrderWhatsAppPreviewPanel,
                5000,
                useUnmergedTree = true,
            )
        ) {
            // Try to click the close button. If it's still there, retry.
            repeat(5) {
                if (isTagPresent(
                        RepairAppTestTags.OrderWhatsAppPreviewPanel,
                        useUnmergedTree = true,
                    )
                ) {
                    try {
                        composeRule
                            .onNodeWithTag(
                                RepairAppTestTags.OrderWhatsAppClosePreviewButton,
                                useUnmergedTree = true,
                            ).performClick()
                        composeRule.waitForIdle()
                        Thread.sleep(1000)
                    } catch (e: Throwable) {
                    }
                }
            }
            if (isTagPresent(RepairAppTestTags.OrderWhatsAppPreviewPanel, useUnmergedTree = true)) {
                // If still there, try back button as fallback for the dialog
                try {
                    androidx.test.espresso.Espresso
                        .pressBackUnconditionally()
                    composeRule.waitForIdle()
                    Thread.sleep(500)
                } catch (e: Throwable) {
                }
            }
            waitForTagGone(
                RepairAppTestTags.OrderWhatsAppPreviewPanel,
                useUnmergedTree = true,
                timeoutMillis = 3000,
            )
        }
    }

    private fun waitForTagIfPresent(
        tag: String,
        timeoutMillis: Long,
        useUnmergedTree: Boolean = false,
    ): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMillis
        do {
            if (isTagPresent(tag, useUnmergedTree)) {
                return true
            }
            Thread.sleep(TAG_POLL_MILLIS)
        } while (System.currentTimeMillis() < deadline)
        return false
    }

    private fun waitForTagGone(
        tag: String,
        useUnmergedTree: Boolean = false,
        timeoutMillis: Long = WAIT_TIMEOUT_MILLIS,
    ) {
        composeRule.waitUntil(timeoutMillis = timeoutMillis) {
            composeRule
                .onAllNodesWithTag(tag, useUnmergedTree = useUnmergedTree)
                .fetchSemanticsNodes()
                .isEmpty()
        }
        composeRule.waitForIdle()
    }

    private fun isTagPresent(
        tag: String,
        useUnmergedTree: Boolean = false,
    ): Boolean =
        composeRule
            .onAllNodesWithTag(tag, useUnmergedTree = useUnmergedTree)
            .fetchSemanticsNodes()
            .isNotEmpty()

    private fun waitForText(
        text: String,
        scroll: Boolean = false,
    ) {
        var found = false
        val deadline = System.currentTimeMillis() + WAIT_TIMEOUT_MILLIS
        var attempts = 0

        while (!found && System.currentTimeMillis() < deadline) {
            found = isTextDisplayed(text)
            if (found) break

            if (scroll) {
                if (attempts % 2 == 0) {
                    try {
                        composeRule.onNodeWithText(text, substring = true).performScrollTo()
                        composeRule.waitForIdle()
                        if (isTextDisplayed(text)) {
                            found = true
                            break
                        }
                    } catch (e: Throwable) {
                        if (attempts % 15 < 10) {
                            performSafeSwipeDown() // Scroll UP
                        } else {
                            performSafeSwipeUp() // Scroll DOWN
                        }
                    }
                }
            }
            if (!found) {
                Thread.sleep(TAG_POLL_MILLIS)
                attempts++
            }
        }
        if (!found) {
            composeRule.onNodeWithText(text, substring = true).assertExists()
        }
    }

    private fun capture(
        outputDir: File,
        fileName: String,
    ) {
        composeRule.waitForIdle()
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        pauseForScreenshot()
        val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        FileOutputStream(File(outputDir, fileName)).use { output ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        }
        bitmap.recycle()
    }

    private fun pauseForDemo() {
        Thread.sleep(if (isRecordingDemo()) RECORDING_ACTION_PAUSE_MILLIS else ACTION_PAUSE_MILLIS)
    }

    private fun pauseForScreenshot() {
        Thread.sleep(if (isRecordingDemo()) RECORDING_SCREENSHOT_PAUSE_MILLIS else SCREENSHOT_PAUSE_MILLIS)
    }

    private fun isRecordingDemo(): Boolean =
        InstrumentationRegistry.getArguments().getString("demoRecording").toBoolean()

    private fun prepareScreenshotDirectory(): File {
        val directory = File(requireNotNull(context.getExternalFilesDir(null)), SCREENSHOT_DIR_NAME)
        directory.deleteRecursively()
        directory.mkdirs()
        return directory
    }

    private companion object {
        const val SCREENSHOT_DIR_NAME = "demo-screenshots"
        const val WAIT_TIMEOUT_MILLIS = 30_000L
        const val LOGIN_ATTEMPTS = 2
        const val LOGIN_ATTEMPT_TIMEOUT_MILLIS = 20_000L
        const val TAG_POLL_MILLIS = 250L
        const val ACTION_PAUSE_MILLIS = 2_000L
        const val SCREENSHOT_PAUSE_MILLIS = 2_000L
        const val RECORDING_ACTION_PAUSE_MILLIS = 500L
        const val RECORDING_SCREENSHOT_PAUSE_MILLIS = 250L
    }
}

package com.onlytechtake.repairapp.demo

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.click
import androidx.test.espresso.Espresso
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.onlytechtake.repairapp.MainActivity
import com.onlytechtake.repairapp.core.design.RepairAppTestTags
import com.onlytechtake.repairapp.domain.workspace.OrderStatusAction
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OperatorIntakeDemoScreenshotTest {
    @get:Rule
    val composeRule = createEmptyComposeRule()

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
            typeTag(RepairAppTestTags.NewOrderEmailField, "giulia.bianchi@example.invalid", scroll = true)
            capture(outputDir, "03-new-customer.png")

            clickTag(RepairAppTestTags.NewOrderSaveCustomerButton, scroll = true)
            waitForText("Cliente selezionato")
            clickTag(RepairAppTestTags.NewOrderStep1NextButton, scroll = true)
            
            clickTag(RepairAppTestTags.categoryButton(DemoDataSeeder.DEMO_CATEGORY_ID), scroll = true)
            clickTag(RepairAppTestTags.categoryButton("demo-cat-hp"), scroll = true)
            clickTag(RepairAppTestTags.categoryButton("demo-cat-pavilion"), scroll = true)
            clickTag(RepairAppTestTags.categoryButton("demo-cat-pavilion-15"), scroll = true)

            clickTag(RepairAppTestTags.NewOrderStep2NextButton, scroll = true)
            typeTag(RepairAppTestTags.NewOrderProblemSummaryField, "Notebook non si accende", scroll = true)
            typeTag(
                RepairAppTestTags.NewOrderProblemDetailsField,
                "Il cliente segnala spegnimento improvviso dopo caduta accidentale.",
                scroll = true,
            )
            capture(outputDir, "04-category-problem.png")

            clickDashboardTab("ORDERS")
            waitForText("Commesse")
            waitForTag(RepairAppTestTags.orderCard(DemoDataSeeder.DEMO_ORDER_ID), scroll = true)
            clickTag(RepairAppTestTags.orderCard(DemoDataSeeder.DEMO_ORDER_ID))
            
            waitForTag(RepairAppTestTags.OrderStatusLabel)
            waitForText("Notebook HP")
            capture(outputDir, "06-order-created.png")

            waitForTag(RepairAppTestTags.OrderWhatsAppButton, scroll = true)
            clickTag(RepairAppTestTags.OrderWhatsAppButton)
            waitForTag(RepairAppTestTags.OrderWhatsAppPreviewPanel, scroll = true)
            capture(outputDir, "07-whatsapp-preview.png")
            clickTag(RepairAppTestTags.OrderWhatsAppClosePreviewButton, scroll = true)

            waitForTag(RepairAppTestTags.OrderSignatureSection, scroll = true)
            capture(outputDir, "05-signature-capture.png")

            applySimpleStatus(OrderStatusAction.START_PROGRESS, "In lavorazione")
            capture(outputDir, "08-in-progress.png")

            applySimpleStatus(OrderStatusAction.START_DIAGNOSIS, "Diagnosi")
            applyResultStatus()
            capture(outputDir, "09-diagnosis-result.png")

            waitForText("Documenti cliente", scroll = true)
            capture(outputDir, "10-pdf-documents.png")

            acceptQuote()
            applySimpleStatus(OrderStatusAction.READY_FOR_PICKUP, "Pronta al ritiro")
            capture(outputDir, "11-ready-for-pickup.png")

            closeOrder()
            clickTag(RepairAppTestTags.orderCard(DemoDataSeeder.DEMO_ORDER_ID), scroll = true)
            waitForText("Chiusa")
            capture(outputDir, "12-closed-timeline.png")
        }
    }

    private fun applySimpleStatus(action: OrderStatusAction, expectedText: String) {
        scrollOrderDetailToTop()
        waitForTag(RepairAppTestTags.OrderActionSection, scroll = true)
        pauseForDemo()
        clickTag(RepairAppTestTags.statusAction(action.name), scroll = true)
        pauseForDemo()
        waitForTag(RepairAppTestTags.StatusDialogConfirmButton)
        clickTag(RepairAppTestTags.StatusDialogConfirmButton)
        composeRule.waitForIdle()
        pauseForDemo()
        waitForStatus(expectedText)
    }

    private fun applyResultStatus() {
        waitForTag(RepairAppTestTags.OrderActionSection, scroll = true)
        pauseForDemo()
        clickTag(RepairAppTestTags.statusAction(OrderStatusAction.RECORD_RESULT.name), scroll = true)
        pauseForDemo()
        waitForTag(RepairAppTestTags.StatusResultSummaryField)
        typeTag(RepairAppTestTags.StatusResultSummaryField, "Connettore di alimentazione danneggiato.", scroll = false)
        clickTag(RepairAppTestTags.DiagnosisRepairableToggle)
        typeTag(RepairAppTestTags.DiagnosisEstimateField, "149,00", scroll = false)
        clickTag(RepairAppTestTags.StatusDialogConfirmButton)
        composeRule.waitForIdle()
        pauseForDemo()
        waitForStatus("Risultato")
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
        waitForStatus("Riparazione")
    }

    private fun closeOrder() {
        waitForTag(RepairAppTestTags.OrderActionSection, scroll = true)
        pauseForDemo()
        clickTag(RepairAppTestTags.statusAction(OrderStatusAction.CLOSE_ORDER.name), scroll = true)
        pauseForDemo()
        waitForTag(RepairAppTestTags.StatusNoteField)
        typeTag(RepairAppTestTags.StatusNoteField, "Riparazione completata e consegnata al cliente.")
        clickTag(RepairAppTestTags.closureReason("COMPLETED"), scroll = true)
        pauseForDemo()
        clickTag(RepairAppTestTags.StatusDialogConfirmButton)
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
        repeat(LOGIN_ATTEMPTS) { attempt ->
            waitForTag(RepairAppTestTags.AuthUsernameField)
            typeTag(RepairAppTestTags.AuthUsernameField, DemoDataSeeder.DEMO_OPERATOR_USERNAME)
            pauseForDemo()
            typeTag(RepairAppTestTags.AuthPasswordField, DemoDataSeeder.DEMO_PASSWORD)
            pauseForDemo()
            clickTag(RepairAppTestTags.AuthSubmitButton)

            if (waitForTagIfPresent(RepairAppTestTags.dashboardTab("HOME"), LOGIN_ATTEMPT_TIMEOUT_MILLIS)) {
                return
            }

            if (attempt == (LOGIN_ATTEMPTS - 1) || !isTagPresent(RepairAppTestTags.AuthUsernameField)) {
                waitForTag(RepairAppTestTags.dashboardTab("HOME"))
            }
            pauseForDemo()
        }
    }

    private fun typeTag(tag: String, text: String, scroll: Boolean = false) {
        waitForTagExists(tag, scroll = scroll)
        val node = composeRule.onNodeWithTag(tag)
        node.assertIsDisplayed()
        node.performClick()
        node.performTextReplacement(text)
        composeRule.waitForIdle()
        Espresso.closeSoftKeyboard()
        pauseForDemo()
    }

    private fun clickTag(tag: String, scroll: Boolean = false) {
        waitForTagExists(tag, scroll = scroll)
        val node = composeRule.onNodeWithTag(tag)
        
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

    private fun clickText(text: String, scroll: Boolean = false) {
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
        try {
            composeRule.onNodeWithTag(RepairAppTestTags.OrderDetailLazyColumn).performTouchInput {
                swipeUp(startY = bottom * 0.8f, endY = top + 30f)
            }
        } catch (e: Throwable) {
            composeRule.onAllNodes(isRoot()).onFirst().performTouchInput { 
                swipeUp(startY = bottom * 0.8f, endY = top + 30f)
            }
        }
        composeRule.waitForIdle()
    }

    private fun performSafeSwipeDown() {
        try {
            composeRule.onNodeWithTag(RepairAppTestTags.OrderDetailLazyColumn).performTouchInput {
                swipeDown(startY = top + 30f, endY = bottom * 0.8f)
            }
        } catch (e: Throwable) {
            composeRule.onAllNodes(isRoot()).onFirst().performTouchInput { 
                swipeDown(startY = top + 30f, endY = bottom * 0.8f)
            }
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

    private fun waitForStatus(text: String) {
        composeRule
            .onNodeWithTag(RepairAppTestTags.OrderDetailLazyColumn)
            .performScrollToIndex(0)
        composeRule.waitForIdle()
        composeRule.waitUntil(timeoutMillis = WAIT_TIMEOUT_MILLIS) {
            runCatching {
                composeRule
                    .onNodeWithTag(RepairAppTestTags.OrderStatusLabel)
                    .performScrollTo()
                    .assertTextContains(text, substring = true)
                true
            }.getOrDefault(false)
        }
    }

    private fun waitForTag(tag: String, scroll: Boolean = false) {
        waitForTagExists(tag, scroll = scroll)
        composeRule.onNodeWithTag(tag).assertIsDisplayed()
    }

    private fun waitForTagExists(tag: String, scroll: Boolean = false) {
        var found = false
        val deadline = System.currentTimeMillis() + WAIT_TIMEOUT_MILLIS
        var attempts = 0
        
        while (!found && System.currentTimeMillis() < deadline) {
            found = isTagPresent(tag)
            if (found) break

            if (scroll) {
                if (attempts % 2 == 0) {
                    try {
                        composeRule.onNodeWithTag(tag).performScrollTo()
                        composeRule.waitForIdle()
                        if (isTagPresent(tag)) {
                            found = true
                            break
                        }
                    } catch (e: Throwable) {
                        // Scan logic: try UP a lot, then DOWN a bit
                        if (attempts % 15 < 10) {
                            performSafeSwipeDown() // Scroll UP
                        } else {
                            performSafeSwipeUp()   // Scroll DOWN
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
            composeRule.onNodeWithTag(tag).assertExists()
        }
    }

    private fun waitForTagIfPresent(tag: String, timeoutMillis: Long): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMillis
        do {
            if (isTagPresent(tag)) {
                composeRule.onNodeWithTag(tag).assertIsDisplayed()
                return true
            }
            Thread.sleep(TAG_POLL_MILLIS)
        } while (System.currentTimeMillis() < deadline)
        return false
    }

    private fun isTagPresent(tag: String): Boolean =
        composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()

    private fun waitForText(text: String, scroll: Boolean = false) {
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
                            performSafeSwipeUp()   // Scroll DOWN
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

    private fun capture(outputDir: File, fileName: String) {
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

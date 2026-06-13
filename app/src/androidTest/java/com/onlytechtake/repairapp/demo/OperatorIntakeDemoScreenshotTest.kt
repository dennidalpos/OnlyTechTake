package com.onlytechtake.repairapp.demo

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeUp
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

        val scenario = ActivityScenario.launch(MainActivity::class.java)
        try {
            waitForTag(RepairAppTestTags.AuthUsernameField)
            capture(outputDir, "01-login.png")

            loginOperatorDemo()
            capture(outputDir, "02-home.png")

            clickDashboardTab("NEW_ORDER")
            waitForTag("quick_actions_screen")
            clickTag(RepairAppTestTags.NewOrderInsertCustomerButton, scroll = true)
            typeTag(RepairAppTestTags.NewOrderFirstNameField, "Giulia", scroll = true)
            typeTag(RepairAppTestTags.NewOrderLastNameField, "Bianchi", scroll = true)
            typeTag(RepairAppTestTags.NewOrderPhoneField, "+39 333 010 2020", scroll = true)
            typeTag(RepairAppTestTags.NewOrderEmailField, "giulia.bianchi@example.invalid", scroll = true)
            capture(outputDir, "03-new-customer.png")

            clickTag(RepairAppTestTags.NewOrderSaveCustomerButton, scroll = true)
            waitForText("Cliente selezionato")
            clickTag(RepairAppTestTags.NewOrderStep1NextButton, scroll = true)
            clickTag(RepairAppTestTags.categoryButton(DemoDataSeeder.DEMO_CATEGORY_ID), scroll = true)
            clickTag(RepairAppTestTags.NewOrderStep2NextButton, scroll = true)
            typeTag(RepairAppTestTags.NewOrderProblemSummaryField, "Notebook non si accende", scroll = true)
            typeTag(
                RepairAppTestTags.NewOrderProblemDetailsField,
                "Il cliente segnala spegnimento improvviso dopo caduta accidentale.",
                scroll = true,
            )
            capture(outputDir, "04-category-problem.png")

            clickDashboardTab("ORDERS")
            clickTag(RepairAppTestTags.orderCard(DemoDataSeeder.DEMO_ORDER_ID), scroll = true)
            waitForText("Stato corrente: Aperta")
            capture(outputDir, "05-order-created.png")

            applySimpleStatus(OrderStatusAction.START_PROGRESS, "Stato corrente: In lavorazione")
            capture(outputDir, "06-in-progress.png")

            applySimpleStatus(OrderStatusAction.START_DIAGNOSIS, "Stato corrente: In diagnosi")
            applyResultStatus()
            capture(outputDir, "07-diagnosis-result.png")

            acceptQuote()
            capture(outputDir, "08-repairing.png")

            applySimpleStatus(OrderStatusAction.READY_FOR_PICKUP, "Stato corrente: Pronta al ritiro")
            capture(outputDir, "09-ready-for-pickup.png")

            closeOrder()
            clickTag(RepairAppTestTags.orderCard(DemoDataSeeder.DEMO_ORDER_ID), scroll = true)
            waitForText("Stato corrente: Chiusa")
            capture(outputDir, "10-closed-timeline.png")
        } finally {
            scenario.close()
        }
    }

    private fun applySimpleStatus(action: OrderStatusAction, expectedText: String) {
        clickTag(RepairAppTestTags.statusAction(action.name), scroll = true)
        clickTag(RepairAppTestTags.StatusDialogConfirmButton)
        waitForText(expectedText)
    }

    private fun applyResultStatus() {
        clickTag(RepairAppTestTags.statusAction(OrderStatusAction.RECORD_RESULT.name), scroll = true)
        typeTag(RepairAppTestTags.StatusResultSummaryField, "Connettore di alimentazione danneggiato.", scroll = false)
        clickTag(RepairAppTestTags.DiagnosisRepairableToggle)
        typeTag(RepairAppTestTags.DiagnosisEstimateField, "149,00", scroll = false)
        clickTag(RepairAppTestTags.StatusDialogConfirmButton)
        waitForText("Stato corrente: Risultato")
    }

    private fun acceptQuote() {
        clickTag(RepairAppTestTags.QuoteAcceptedToggle, scroll = true)
        clickTag(RepairAppTestTags.QuoteConfirmButton, scroll = true)
        waitForText("Stato corrente: In riparazione")
    }

    private fun closeOrder() {
        clickTag(RepairAppTestTags.statusAction(OrderStatusAction.CLOSE_ORDER.name), scroll = true)
        typeTag(RepairAppTestTags.StatusNoteField, "Riparazione completata e consegnata al cliente.")
        clickTag(RepairAppTestTags.closureReason("COMPLETED"), scroll = true)
        clickTag(RepairAppTestTags.StatusDialogConfirmButton)
        waitForText("Commesse")
    }

    private fun clickDashboardTab(destination: String) {
        val tabTag = RepairAppTestTags.dashboardTab(destination)
        repeat(DASHBOARD_TAB_SWIPE_ATTEMPTS) {
            if (isTagDisplayed(tabTag)) {
                composeRule.onNodeWithTag(tabTag).performClick()
                composeRule.waitForIdle()
                Thread.sleep(2000)
                return
            }
            composeRule.onNodeWithTag(RepairAppTestTags.DashboardTabs)
                .performTouchInput { swipeLeft() }
            composeRule.waitForIdle()
            pauseForDemo()
        }
        composeRule.onNodeWithTag(tabTag).performClick()
        composeRule.waitForIdle()
        Thread.sleep(2000)
    }

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

            if (attempt == LOGIN_ATTEMPTS - 1 || !isTagPresent(RepairAppTestTags.AuthUsernameField)) {
                waitForTag(RepairAppTestTags.dashboardTab("HOME"))
            }
            pauseForDemo()
        }
    }

    private fun typeTag(tag: String, text: String, scroll: Boolean = false) {
        waitForTagExists(tag)
        val node = composeRule.onNodeWithTag(tag)
        if (scroll) runCatching { node.performScrollTo() }
        node.assertIsDisplayed()
        node.performClick()
        node.performTextReplacement(text)
        composeRule.waitForIdle()
        pauseForDemo()
    }

    private fun clickTag(tag: String, scroll: Boolean = false) {
        waitForTagExists(tag)
        val node = composeRule.onNodeWithTag(tag)
        if (scroll) runCatching { node.performScrollTo() }
        if (scroll && runCatching { node.assertIsDisplayed() }.isFailure) {
            var displayed = false
            repeat(VISIBILITY_SWIPE_ATTEMPTS) {
                if (displayed) return@repeat
                runCatching { node.performScrollTo() }
                displayed = runCatching { node.assertIsDisplayed() }.isSuccess
                if (displayed) {
                    return@repeat
                }
                composeRule.onRoot().performTouchInput { swipeUp() }
                composeRule.waitForIdle()
                pauseForDemo()
            }
        }
        node.assertIsDisplayed()
        node.performClick()
        composeRule.waitForIdle()
        pauseForDemo()
    }

    private fun waitForTag(tag: String) {
        waitForTagExists(tag)
        composeRule.onNodeWithTag(tag).assertIsDisplayed()
    }

    private fun waitForTagExists(tag: String) {
        composeRule.waitUntil(timeoutMillis = WAIT_TIMEOUT_MILLIS) {
            composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
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

    private fun isTagDisplayed(tag: String): Boolean =
        runCatching {
            composeRule.onNodeWithTag(tag).assertIsDisplayed()
        }.isSuccess

    private fun waitForText(text: String) {
        composeRule.waitUntil(timeoutMillis = WAIT_TIMEOUT_MILLIS) {
            composeRule.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty()
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
        Thread.sleep(ACTION_PAUSE_MILLIS)
    }

    private fun pauseForScreenshot() {
        Thread.sleep(SCREENSHOT_PAUSE_MILLIS)
    }

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
        const val DASHBOARD_TAB_SWIPE_ATTEMPTS = 4
        const val VISIBILITY_SWIPE_ATTEMPTS = 5
        const val TAG_POLL_MILLIS = 250L
        const val ACTION_PAUSE_MILLIS = 1_400L
        const val SCREENSHOT_PAUSE_MILLIS = 2_000L
    }
}

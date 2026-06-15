package com.onlytechtake.repairapp.demo

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onFirst
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

            clickDashboardTab("NEW_ORDER")
            waitForTag("quick_actions_screen")
            clickText("Inserisci nuovo cliente", scroll = true)
            typeTag(RepairAppTestTags.NewOrderFirstNameField, "Giulia", scroll = true)
            typeTag(RepairAppTestTags.NewOrderLastNameField, "Bianchi", scroll = true)
            typeTag(RepairAppTestTags.NewOrderPhoneField, "+39 333 010 2020", scroll = true)
            typeTag(RepairAppTestTags.NewOrderEmailField, "giulia.bianchi@example.invalid", scroll = true)
            capture(outputDir, "03-new-customer.png")

            clickText("Salva cliente", scroll = true)
            waitForText("Cliente selezionato")
            clickText("Avanti", scroll = true)
            clickTag(RepairAppTestTags.categoryButton(DemoDataSeeder.DEMO_CATEGORY_ID), scroll = true)
            clickText("Avanti", scroll = true)
            typeTag(RepairAppTestTags.NewOrderProblemSummaryField, "Notebook non si accende", scroll = true)
            typeTag(
                RepairAppTestTags.NewOrderProblemDetailsField,
                "Il cliente segnala spegnimento improvviso dopo caduta accidentale.",
                scroll = true,
            )
            capture(outputDir, "04-category-problem.png")

            clickDashboardTab("ORDERS")
            waitForText("Commesse")
            clickTag(RepairAppTestTags.orderCard(DemoDataSeeder.DEMO_ORDER_ID), scroll = true)
            waitForText("Notebook HP")
            capture(outputDir, "05-order-created.png")

            applySimpleStatus(OrderStatusAction.START_PROGRESS, "In lavorazione")
            capture(outputDir, "06-in-progress.png")

            applySimpleStatus(OrderStatusAction.START_DIAGNOSIS, "Diagnosi")
            applyResultStatus()
            capture(outputDir, "07-diagnosis-result.png")

            acceptQuote()
            capture(outputDir, "08-repairing.png")

            applySimpleStatus(OrderStatusAction.READY_FOR_PICKUP, "Pronta al ritiro")
            capture(outputDir, "09-ready-for-pickup.png")

            closeOrder()
            clickTag(RepairAppTestTags.orderCard(DemoDataSeeder.DEMO_ORDER_ID), scroll = true)
            waitForText("Chiusa")
            capture(outputDir, "10-closed-timeline.png")
        }
    }

    private fun applySimpleStatus(action: OrderStatusAction, expectedText: String) {
        clickText("Azioni Commessa", scroll = true)
        pauseForDemo()
        // Clicca l'azione specifica usando il tag
        clickTag(RepairAppTestTags.statusAction(action.name), scroll = true)
        pauseForDemo()
        // Attende che il dialogo di conferma sia visibile e clicca conferma
        waitForTag(RepairAppTestTags.StatusDialogConfirmButton)
        clickTag(RepairAppTestTags.StatusDialogConfirmButton)
        composeRule.waitForIdle()
        pauseForDemo()
        // Verifica il cambio di stato nell'header
        waitForStatus(expectedText)
    }

    private fun applyResultStatus() {
        clickText("Azioni Commessa", scroll = true)
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
        // Pannello esito preventivo
        clickTag(RepairAppTestTags.QuoteAcceptedToggle, scroll = true)
        pauseForDemo()
        clickTag(RepairAppTestTags.QuoteConfirmButton, scroll = true)
        composeRule.waitForIdle()
        pauseForDemo()
        waitForStatus("Riparazione")
    }

    private fun closeOrder() {
        clickText("Azioni Commessa", scroll = true)
        pauseForDemo()
        clickTag(RepairAppTestTags.statusAction(OrderStatusAction.CLOSE_ORDER.name), scroll = true)
        pauseForDemo()
        waitForTag(RepairAppTestTags.StatusNoteField)
        typeTag(RepairAppTestTags.StatusNoteField, "Riparazione completata e consegnata al cliente.")
        // Seleziona motivo COMPLETATA
        clickTag(RepairAppTestTags.closureReason("COMPLETED"), scroll = true)
        pauseForDemo()
        clickTag(RepairAppTestTags.StatusDialogConfirmButton)
        composeRule.waitForIdle()
        pauseForDemo()
        // Torna alla lista commesse dopo la chiusura
        waitForText("Commesse")
    }

    private fun clickDashboardTab(destination: String) {
        val tabTag = RepairAppTestTags.dashboardTab(destination)
        waitForTagExists(RepairAppTestTags.DashboardTabs)
        
        repeat(DASHBOARD_TAB_SWIPE_ATTEMPTS) {
            if (isTagPresent(tabTag)) {
                composeRule.onNodeWithTag(tabTag).performClick()
                composeRule.waitForIdle()

                // Handle "Unsaved changes" dialog if it appears
                if (isTextDisplayed("Dati non salvati")) {
                    clickText("Esci e perdi dati")
                }

                composeRule.waitForIdle()
                Thread.sleep(3000) // Increased wait for UI to settle
                return
            }
            composeRule.onNodeWithTag(RepairAppTestTags.DashboardTabs)
                .performTouchInput { swipeLeft() }
            composeRule.waitForIdle()
            pauseForDemo()
        }
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
        waitForTagExists(tag)
        val node = composeRule.onNodeWithTag(tag)
        if (scroll) runCatching { node.performScrollTo() }
        node.assertIsDisplayed()
        node.performClick()
        node.performTextReplacement(text)
        composeRule.waitForIdle()
        Espresso.closeSoftKeyboard()
        pauseForDemo()
    }

    private fun clickTag(tag: String, scroll: Boolean = false) {
        waitForTagExists(tag)
        val node = composeRule.onNodeWithTag(tag)
        if (scroll) {
            runCatching { node.performScrollTo() }
            composeRule.waitForIdle()
        }
        if (scroll && runCatching { node.assertIsDisplayed() }.isFailure) {
            var displayed = false
            repeat(VISIBILITY_SWIPE_ATTEMPTS) {
                if (displayed) return@repeat
                performSafeSwipeUp()
                composeRule.waitForIdle()
                runCatching { node.performScrollTo() }
                displayed = runCatching { node.assertIsDisplayed() }.isSuccess
                pauseForDemo()
            }
        }
        node.assertIsDisplayed()
        node.performClick()
        composeRule.waitForIdle()
        pauseForDemo()
    }

    private fun clickText(text: String, scroll: Boolean = false) {
        waitForText(text)
        val node = composeRule.onNodeWithText(text, substring = false)
        if (scroll) {
            runCatching { node.performScrollTo() }
            composeRule.waitForIdle()
        }
        if (scroll && runCatching { node.assertIsDisplayed() }.isFailure) {
            var displayed = false
            repeat(VISIBILITY_SWIPE_ATTEMPTS) {
                if (displayed) return@repeat
                performSafeSwipeUp()
                composeRule.waitForIdle()
                runCatching { node.performScrollTo() }
                displayed = runCatching { node.assertIsDisplayed() }.isSuccess
                pauseForDemo()
            }
        }
        node.assertIsDisplayed()
        node.performClick()
        composeRule.waitForIdle()
        pauseForDemo()
    }

    private fun performSafeSwipeUp() {
        composeRule.onAllNodes(isRoot()).onFirst().performTouchInput { swipeUp() }
    }

    private fun waitForStatus(text: String) {
        composeRule.waitUntil(timeoutMillis = WAIT_TIMEOUT_MILLIS) {
            // Force scroll up to see the header status label
            runCatching {
                composeRule.onNodeWithTag(RepairAppTestTags.OrderStatusLabel).performScrollTo()
            }
            isTextDisplayed(text)
        }
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
        const val ACTION_PAUSE_MILLIS = 2_000L
        const val SCREENSHOT_PAUSE_MILLIS = 2_000L
    }
}

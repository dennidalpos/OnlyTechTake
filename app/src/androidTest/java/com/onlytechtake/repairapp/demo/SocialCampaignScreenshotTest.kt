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
import androidx.compose.ui.test.swipeUp
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.onlytechtake.repairapp.BuildConfig
import com.onlytechtake.repairapp.MainActivity
import com.onlytechtake.repairapp.core.design.RepairAppTestTags
import com.onlytechtake.repairapp.domain.workspace.OrderStatusAction
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

@RunWith(AndroidJUnit4::class)
class SocialCampaignScreenshotTest {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun capturesSocialCampaignFlow() {
        runBlocking { DemoDataSeeder.seedDemoProfile(context) }
        val outputDir = prepareScreenshotDirectory()

        ActivityScenario.launch(MainActivity::class.java).use {
            waitForTag(RepairAppTestTags.AuthUsernameField)

            loginOperatorDemo()
            capture(outputDir, "01-home.png")

            // 1. Wizard: Cliente
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

            typeTag(RepairAppTestTags.NewOrderFirstNameField, "Marco", scroll = true)
            typeTag(RepairAppTestTags.NewOrderLastNameField, "Rossi", scroll = true)
            typeTag(RepairAppTestTags.NewOrderPhoneField, "+39 347 123 4567", scroll = true)
            typeTag(RepairAppTestTags.NewOrderEmailField, "m.rossi@example.invalid", scroll = true)
            capture(outputDir, "02-accoglienza.png")

            clickTag(RepairAppTestTags.NewOrderSaveCustomerButton, scroll = true)
            waitForText("Cliente selezionato")
            clickTag(RepairAppTestTags.NewOrderStep1NextButton, scroll = true)

            // 2. Wizard: Categorie
            clickTag(RepairAppTestTags.categoryButton("demo-cat-pc"), scroll = true)
            clickTag(RepairAppTestTags.categoryButton(DemoDataSeeder.DEMO_CATEGORY_ID), scroll = true)
            clickTag(RepairAppTestTags.categoryButton("demo-cat-gaming"), scroll = true)
            clickTag(RepairAppTestTags.categoryButton("demo-cat-rog-strix"), scroll = true)

            clickTag(RepairAppTestTags.NewOrderStep2NextButton, scroll = true)

            // 3. Wizard: Problema
            typeTag(RepairAppTestTags.NewOrderProblemSummaryField, "Asus ROG - Surriscaldamento", scroll = true)
            typeTag(RepairAppTestTags.NewOrderProblemDetailsField, "Richiesta pulizia interna e cambio pasta termica.", scroll = true)
            capture(outputDir, "03-diagnosi.png")

            // Proseguiamo per mostrare lo stepper che avanza (Step 4: Media)
            clickText("Avanti", scroll = true)
            waitForText("Foto e allegati")
            pauseForDemo()

            // Usciamo dal wizard per andare all'ordine reale (già pronto con media e firma nel seeder)
            clickDashboardTab("ORDERS")
            waitForTag(RepairAppTestTags.orderCard(DemoDataSeeder.DEMO_ORDER_ID), scroll = true)
            clickTag(RepairAppTestTags.orderCard(DemoDataSeeder.DEMO_ORDER_ID), scroll = true)

            // 4. FIRMA (Mostriamo la firma nel dettaglio ordine)
            waitForTag(RepairAppTestTags.OrderStatusLabel)
            clickTag(RepairAppTestTags.OrderSignaturesCard, scroll = true)
            composeRule.waitForIdle()
            pauseForDemo()
            capture(outputDir, "04-firma.png")
            clickTag(RepairAppTestTags.OrderSignaturesCard)
            pauseForDemo()

            // Workflow di lavorazione
            applySimpleStatus(OrderStatusAction.START_PROGRESS, nextAction = OrderStatusAction.START_DIAGNOSIS)
            applySimpleStatus(OrderStatusAction.START_DIAGNOSIS, nextAction = OrderStatusAction.RECORD_RESULT)

            // 5. WHATSAPP & PREVENTIVO
            applyResultStatus()
            waitForTag(RepairAppTestTags.OrderWhatsAppButton, scroll = true)
            clickTag(RepairAppTestTags.OrderWhatsAppButton)
            waitForTag(RepairAppTestTags.OrderWhatsAppPreviewPanel)
            capture(outputDir, "05-whatsapp.png")
            dismissWhatsAppPreviewIfPresent()
            pauseForDemo()

            acceptQuote()
            applySimpleStatus(OrderStatusAction.READY_FOR_PICKUP, nextAction = OrderStatusAction.CLOSE_ORDER)

            // 6. SUCCESSO (Timeline chiusura)
            closeOrder()
            clickDashboardTab("ORDERS")
            waitForTag(RepairAppTestTags.orderCard(DemoDataSeeder.DEMO_ORDER_ID), scroll = true)
            clickTag(RepairAppTestTags.orderCard(DemoDataSeeder.DEMO_ORDER_ID), scroll = true)
            waitForText("Chiusa")
            capture(outputDir, "06-successo.png")
        }
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
        dismissWhatsAppPreviewIfPresent()
        waitForTag(RepairAppTestTags.statusAction(nextAction.name), scroll = true)
    }

    private fun applyResultStatus() {
        waitForTag(RepairAppTestTags.OrderActionSection, scroll = true)
        pauseForDemo()
        clickTag(RepairAppTestTags.statusAction(OrderStatusAction.RECORD_RESULT.name), scroll = true)
        pauseForDemo()
        waitForTag(RepairAppTestTags.StatusResultSummaryField)
        typeTag(RepairAppTestTags.StatusResultSummaryField, "Pasta termica secca.", scroll = false)
        clickTag(RepairAppTestTags.DiagnosisRepairableToggle)
        typeTag(RepairAppTestTags.DiagnosisEstimateField, "89,00", scroll = false)
        clickTag(RepairAppTestTags.StatusDialogConfirmButton)
        waitForTagGone(RepairAppTestTags.StatusDialogConfirmButton)
        composeRule.waitForIdle()
        pauseForDemo()
        dismissWhatsAppPreviewIfPresent()
        waitForTag(RepairAppTestTags.QuoteAcceptedToggle, scroll = true)
    }

    private fun acceptQuote() {
        composeRule.onNodeWithTag(RepairAppTestTags.OrderDetailLazyColumn).performScrollToIndex(2)
        composeRule.waitForIdle()
        pauseForDemo()
        waitForTag(RepairAppTestTags.QuoteAcceptedToggle)
        clickTag(RepairAppTestTags.QuoteAcceptedToggle)
        pauseForDemo()
        clickTag(RepairAppTestTags.QuoteConfirmButton, scroll = true)
        composeRule.waitForIdle()
        pauseForDemo()
        waitForTag(RepairAppTestTags.statusAction(OrderStatusAction.READY_FOR_PICKUP.name), scroll = true)
    }

    private fun closeOrder() {
        waitForTag(RepairAppTestTags.OrderActionSection, scroll = true)
        pauseForDemo()
        clickTag(RepairAppTestTags.statusAction(OrderStatusAction.CLOSE_ORDER.name), scroll = true)
        pauseForDemo()
        waitForTag(RepairAppTestTags.StatusNoteField)
        typeTag(RepairAppTestTags.StatusNoteField, "Riparazione completata.")
        clickTag(RepairAppTestTags.closureReason("COMPLETED"))
        pauseForDemo()
        clickTag(RepairAppTestTags.StatusDialogConfirmButton)
        waitForTagGone(RepairAppTestTags.StatusDialogConfirmButton)
        composeRule.waitForIdle()
        pauseForDemo()
    }

    private fun loginOperatorDemo() {
        val username = if (BuildConfig.IS_DEMO_VERSION) DemoDataSeeder.DEMO_ADMIN_USERNAME else DemoDataSeeder.DEMO_OPERATOR_USERNAME
        repeat(LOGIN_ATTEMPTS) { attempt ->
            waitForTag(RepairAppTestTags.AuthUsernameField)
            typeTag(RepairAppTestTags.AuthUsernameField, username)
            pauseForDemo()
            typeTag(RepairAppTestTags.AuthPasswordField, DemoDataSeeder.DEMO_PASSWORD)
            pauseForDemo()
            clickTag(RepairAppTestTags.AuthSubmitButton)

            if (waitForTagIfPresent(RepairAppTestTags.dashboardTab("HOME"), LOGIN_ATTEMPT_TIMEOUT_MILLIS)) return
            if (attempt == (LOGIN_ATTEMPTS - 1) || !isTagPresent(RepairAppTestTags.AuthUsernameField)) {
                waitForTag(RepairAppTestTags.dashboardTab("HOME"))
            }
            pauseForDemo()
        }
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
        val listTags = listOf(RepairAppTestTags.OrderDetailLazyColumn, "categories_list", "customer_list", "customer_form")
        for (tag in listTags) {
            try {
                composeRule.onNodeWithTag(tag).performTouchInput { swipeUp(startY = bottom * 0.8f, endY = top + 30f) }
                composeRule.waitForIdle()
                return
            } catch (e: Throwable) {
            }
        }
        try {
            composeRule.onAllNodes(isRoot()).onFirst().performTouchInput { swipeUp(startY = bottom * 0.8f, endY = top + 30f) }
        } catch (e: Throwable) {
        }
        composeRule.waitForIdle()
    }

    private fun performSafeSwipeDown() {
        val listTags = listOf(RepairAppTestTags.OrderDetailLazyColumn, "categories_list", "customer_list", "customer_form")
        for (tag in listTags) {
            try {
                composeRule.onNodeWithTag(tag).performTouchInput { swipeDown(startY = top + 30f, endY = bottom * 0.8f) }
                composeRule.waitForIdle()
                return
            } catch (e: Throwable) {
            }
        }
        try {
            composeRule.onAllNodes(isRoot()).onFirst().performTouchInput { swipeDown(startY = top + 30f, endY = bottom * 0.8f) }
        } catch (e: Throwable) {
        }
        composeRule.waitForIdle()
    }

    private fun scrollOrderDetailToTop() {
        composeRule.onNodeWithTag(RepairAppTestTags.OrderDetailLazyColumn).performScrollToIndex(0)
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
                if (attempts % 15 < 10) performSafeSwipeUp() else performSafeSwipeDown()
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
                        composeRule.onNodeWithTag(tag, useUnmergedTree = useUnmergedTree).performScrollTo()
                        composeRule.waitForIdle()
                        if (isTagPresent(tag, useUnmergedTree)) {
                            found = true
                            break
                        }
                    } catch (e: Throwable) {
                        if (attempts % 15 < 10) performSafeSwipeUp() else performSafeSwipeDown()
                    }
                }
            }
            if (!found) {
                Thread.sleep(TAG_POLL_MILLIS)
                attempts++
            }
        }
        if (!found) composeRule.onNodeWithTag(tag, useUnmergedTree = useUnmergedTree).assertExists()
    }

    private fun dismissWhatsAppPreviewIfPresent() {
        if (waitForTagIfPresent(RepairAppTestTags.OrderWhatsAppPreviewPanel, 5000, useUnmergedTree = true)) {
            repeat(5) {
                if (isTagPresent(RepairAppTestTags.OrderWhatsAppPreviewPanel, useUnmergedTree = true)) {
                    try {
                        composeRule.onNodeWithTag(RepairAppTestTags.OrderWhatsAppClosePreviewButton, useUnmergedTree = true).performClick()
                        composeRule.waitForIdle()
                        Thread.sleep(1000)
                    } catch (e: Throwable) {
                    }
                }
            }
            if (isTagPresent(RepairAppTestTags.OrderWhatsAppPreviewPanel, useUnmergedTree = true)) {
                try {
                    Espresso.pressBackUnconditionally()
                    composeRule.waitForIdle()
                } catch (e: Throwable) {
                }
            }
            waitForTagGone(RepairAppTestTags.OrderWhatsAppPreviewPanel, useUnmergedTree = true, timeoutMillis = 3000)
        }
    }

    private fun waitForTagIfPresent(
        tag: String,
        timeoutMillis: Long,
        useUnmergedTree: Boolean = false,
    ): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMillis
        do {
            if (isTagPresent(tag, useUnmergedTree)) return true
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
            composeRule.onAllNodesWithTag(tag, useUnmergedTree = useUnmergedTree).fetchSemanticsNodes().isEmpty()
        }
        composeRule.waitForIdle()
    }

    private fun isTagPresent(
        tag: String,
        useUnmergedTree: Boolean = false,
    ): Boolean = composeRule.onAllNodesWithTag(tag, useUnmergedTree = useUnmergedTree).fetchSemanticsNodes().isNotEmpty()

    private fun isTextDisplayed(text: String): Boolean = composeRule.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty()

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
                        if (attempts % 15 < 10) performSafeSwipeUp() else performSafeSwipeDown()
                    }
                }
            }
            if (!found) {
                Thread.sleep(TAG_POLL_MILLIS)
                attempts++
            }
        }
        if (!found) composeRule.onNodeWithText(text, substring = true).assertExists()
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

    private fun isRecordingDemo(): Boolean = InstrumentationRegistry.getArguments().getString("demoRecording").toBoolean()

    private fun prepareScreenshotDirectory(): File {
        val directory = File(requireNotNull(context.getExternalFilesDir(null)), "social-campaign")
        directory.deleteRecursively()
        directory.mkdirs()
        return directory
    }

    private companion object {
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

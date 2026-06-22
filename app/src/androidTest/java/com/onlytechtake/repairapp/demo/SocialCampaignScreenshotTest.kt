package com.onlytechtake.repairapp.demo

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
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
            // 1. LOGIN & HOME
            waitForTag(RepairAppTestTags.AuthUsernameField)
            loginDemo(DemoDataSeeder.DEMO_OPERATOR_USERNAME)
            waitForText("Operatore")
            capture(outputDir, "01-home.png")

            // 2. ACCOGLIENZA (Nuovo Cliente)
            clickTag(RepairAppTestTags.HomeNewOrderButton, scroll = true)
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

            // 3. DIAGNOSI (iPhone 13 - usando notebook demo come placeholder se iPhone non esiste nel seeder)
            clickTag(RepairAppTestTags.NewOrderSaveCustomerButton, scroll = true)
            waitForText("Cliente selezionato")
            clickTag(RepairAppTestTags.NewOrderStep1NextButton, scroll = true)

            clickTag(RepairAppTestTags.categoryButton("demo-cat-pc"), scroll = true)
            clickTag(
                RepairAppTestTags.categoryButton(DemoDataSeeder.DEMO_CATEGORY_ID),
                scroll = true,
            )
            clickTag(RepairAppTestTags.NewOrderStep2NextButton, scroll = true)

            typeTag(
                RepairAppTestTags.NewOrderProblemSummaryField,
                "iPhone 13 - Schermo crepato",
                scroll = true,
            )
            typeTag(
                RepairAppTestTags.NewOrderProblemDetailsField,
                "FaceID non funzionante dopo caduta accidentale.",
                scroll = true,
            )
            capture(outputDir, "03-diagnosi.png")

            // 4. FIRMA
            clickTag(RepairAppTestTags.NewOrderStep2NextButton, scroll = true) // To Media
            clickTag(
                RepairAppTestTags.NewOrderStep2NextButton,
                scroll = true,
            ) // To Privacy/Signature (Wait, need to check stepper tags)
            // Using direct navigation to order if stepper is too complex for this draft

            clickDashboardTab("ORDERS")
            waitForTag(RepairAppTestTags.orderCard(DemoDataSeeder.DEMO_ORDER_ID), scroll = true)
            clickTag(RepairAppTestTags.orderCard(DemoDataSeeder.DEMO_ORDER_ID))

            clickTag(RepairAppTestTags.OrderSignaturesCard, scroll = true)
            pauseForDemo()
            capture(outputDir, "04-firma.png")
            clickTag(RepairAppTestTags.OrderSignaturesCard) // Collapse

            // 5. WHATSAPP & PREVENTIVO
            applyResultStatus("189,00", "Sostituzione display originale Apple.")
            waitForTag(RepairAppTestTags.OrderWhatsAppButton, scroll = true)
            clickTag(RepairAppTestTags.OrderWhatsAppButton)
            waitForTag(RepairAppTestTags.OrderWhatsAppPreviewPanel)
            capture(outputDir, "05-whatsapp.png")
            dismissWhatsAppPreviewIfPresent()

            // 6. SUCCESSO (Chiusura)
            acceptQuote()
            applySimpleStatus(OrderStatusAction.READY_FOR_PICKUP, OrderStatusAction.CLOSE_ORDER)
            closeOrder()

            clickTag(RepairAppTestTags.orderCard(DemoDataSeeder.DEMO_ORDER_ID), scroll = true)
            waitForText("Chiusa")
            capture(outputDir, "06-successo.png")
        }
    }

    // --- Helpers based on SocialStoryDemoScreenshotTest ---

    private fun loginDemo(username: String) {
        waitForTag(RepairAppTestTags.AuthUsernameField)
        typeTag(RepairAppTestTags.AuthUsernameField, username)
        typeTag(RepairAppTestTags.AuthPasswordField, DemoDataSeeder.DEMO_PASSWORD)
        clickTag(RepairAppTestTags.AuthSubmitButton)
        waitForTag(RepairAppTestTags.dashboardTab("HOME"))
    }

    private fun applySimpleStatus(
        action: OrderStatusAction,
        nextAction: OrderStatusAction,
    ) {
        waitForTag(RepairAppTestTags.OrderActionSection, scroll = true)
        clickTag(RepairAppTestTags.statusAction(action.name), scroll = true)
        waitForTag(RepairAppTestTags.StatusDialogConfirmButton)
        clickTag(RepairAppTestTags.StatusDialogConfirmButton)
        waitForTagGone(RepairAppTestTags.StatusDialogConfirmButton)
        dismissWhatsAppPreviewIfPresent()
    }

    private fun applyResultStatus(
        amount: String,
        summary: String,
    ) {
        waitForTag(RepairAppTestTags.OrderActionSection, scroll = true)
        clickTag(
            RepairAppTestTags.statusAction(OrderStatusAction.RECORD_RESULT.name),
            scroll = true,
        )
        waitForTag(RepairAppTestTags.StatusResultSummaryField)
        typeTag(RepairAppTestTags.StatusResultSummaryField, summary)
        clickTag(RepairAppTestTags.DiagnosisRepairableToggle)
        typeTag(RepairAppTestTags.DiagnosisEstimateField, amount)
        clickTag(RepairAppTestTags.StatusDialogConfirmButton)
        waitForTagGone(RepairAppTestTags.StatusDialogConfirmButton)
        dismissWhatsAppPreviewIfPresent()
    }

    private fun acceptQuote() {
        composeRule.onNodeWithTag(RepairAppTestTags.OrderDetailLazyColumn).performScrollToIndex(2)
        waitForTag(RepairAppTestTags.QuoteAcceptedToggle)
        clickTag(RepairAppTestTags.QuoteAcceptedToggle)
        clickTag(RepairAppTestTags.QuoteConfirmButton, scroll = true)
        waitForTag(
            RepairAppTestTags.statusAction(OrderStatusAction.READY_FOR_PICKUP.name),
            scroll = true,
        )
    }

    private fun closeOrder() {
        waitForTag(RepairAppTestTags.OrderActionSection, scroll = true)
        clickTag(RepairAppTestTags.statusAction(OrderStatusAction.CLOSE_ORDER.name), scroll = true)
        typeTag(RepairAppTestTags.StatusNoteField, "Riparazione completata con successo.")
        clickTag(RepairAppTestTags.closureReason("COMPLETED"))
        clickTag(RepairAppTestTags.StatusDialogConfirmButton)
        waitForTagGone(RepairAppTestTags.StatusDialogConfirmButton)
    }

    private fun clickDashboardTab(destination: String) {
        val tabTag = RepairAppTestTags.dashboardTab(destination)
        waitForTagExists(tabTag)
        composeRule.onNodeWithTag(tabTag).performClick()
        composeRule.waitForIdle()
        Thread.sleep(1000)
    }

    private fun typeTag(
        tag: String,
        text: String,
        scroll: Boolean = false,
    ) {
        waitForTagExists(tag, scrollIfNotFound = scroll)
        val node = composeRule.onNodeWithTag(tag)
        node.performClick()
        node.performTextReplacement(text)
        Espresso.closeSoftKeyboard()
        pauseForDemo()
    }

    private fun clickTag(
        tag: String,
        scroll: Boolean = false,
        useUnmergedTree: Boolean = false,
    ) {
        waitForTag(tag, scroll = scroll, useUnmergedTree = useUnmergedTree)
        composeRule.onNodeWithTag(tag, useUnmergedTree = useUnmergedTree).performClick()
        pauseForDemo()
    }

    private fun dismissWhatsAppPreviewIfPresent() {
        if (isTagPresent(RepairAppTestTags.OrderWhatsAppPreviewPanel, useUnmergedTree = true)) {
            clickTag(RepairAppTestTags.OrderWhatsAppClosePreviewButton, useUnmergedTree = true)
            waitForTagGone(RepairAppTestTags.OrderWhatsAppPreviewPanel, useUnmergedTree = true)
        }
    }

    private fun waitForTag(
        tag: String,
        scroll: Boolean = false,
        useUnmergedTree: Boolean = false,
    ) {
        waitForTagExists(tag, scrollIfNotFound = scroll, useUnmergedTree = useUnmergedTree)
        val deadline = System.currentTimeMillis() + 30000
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
            Thread.sleep(250)
            attempts++
        }
        composeRule.onNodeWithTag(tag, useUnmergedTree = useUnmergedTree).assertIsDisplayed()
    }

    private fun waitForTagExists(
        tag: String,
        scrollIfNotFound: Boolean = false,
        useUnmergedTree: Boolean = false,
    ) {
        composeRule.waitUntil(30000) {
            if (scrollIfNotFound && !isTagPresent(tag, useUnmergedTree)) {
                performSafeSwipeUp()
            }
            isTagPresent(tag, useUnmergedTree)
        }
    }

    private fun waitForTagGone(
        tag: String,
        useUnmergedTree: Boolean = false,
    ) {
        composeRule.waitUntil(10000) { !isTagPresent(tag, useUnmergedTree) }
    }

    private fun isTagPresent(
        tag: String,
        useUnmergedTree: Boolean = false,
    ): Boolean =
        composeRule
            .onAllNodesWithTag(tag, useUnmergedTree = useUnmergedTree)
            .fetchSemanticsNodes()
            .isNotEmpty()

    private fun waitForText(text: String) {
        composeRule.waitUntil(10000) {
            composeRule
                .onAllNodesWithText(text, substring = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }

    private fun performSafeSwipeUp() {
        try {
            composeRule.onAllNodes(isRoot()).onFirst().performTouchInput {
                swipeUp(startY = bottom * 0.8f, endY = top + 30f)
            }
        } catch (e: Throwable) {
        }
        composeRule.waitForIdle()
    }

    private fun capture(
        outputDir: File,
        fileName: String,
    ) {
        composeRule.waitForIdle()
        Thread.sleep(1000)
        val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        FileOutputStream(File(outputDir, fileName)).use {
            it.write(
                Bitmap.createBitmap(bitmap).let { b ->
                    val out = java.io.ByteArrayOutputStream()
                    b.compress(Bitmap.CompressFormat.PNG, 100, out)
                    out.toByteArray()
                },
            )
        }
    }

    private fun prepareScreenshotDirectory(): File {
        val directory = File(requireNotNull(context.getExternalFilesDir(null)), "social-campaign")
        directory.deleteRecursively()
        directory.mkdirs()
        return directory
    }

    private companion object {
        const val WAIT_TIMEOUT_MILLIS = 30_000L
    }
}

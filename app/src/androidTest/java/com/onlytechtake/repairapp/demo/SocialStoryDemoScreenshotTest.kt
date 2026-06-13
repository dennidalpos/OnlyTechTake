package com.onlytechtake.repairapp.demo

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
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
class SocialStoryDemoScreenshotTest {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun capturesOrderIntakeToClosureStories() {
        runBlocking { DemoDataSeeder.seedDemoProfile(context) }
        val outputDir = prepareCaseDirectory(CASE_ORDER)

        withDemoApp {
            waitForTag(RepairAppTestTags.AuthUsernameField)
            capture(outputDir, "01-login.png")

            loginDemo(DemoDataSeeder.DEMO_OPERATOR_USERNAME)
            capture(outputDir, "02-dashboard-home.png")

            clickDashboardTab("NEW_ORDER")
            clickTag(RepairAppTestTags.NewOrderInsertCustomerButton, scroll = true)
            typeTag(RepairAppTestTags.NewOrderFirstNameField, "Giulia", scroll = true)
            typeTag(RepairAppTestTags.NewOrderLastNameField, "Bianchi", scroll = true)
            typeTag(RepairAppTestTags.NewOrderPhoneField, "+39 333 010 2020", scroll = true)
            typeTag(RepairAppTestTags.NewOrderEmailField, "giulia.bianchi@example.invalid", scroll = true)
            capture(outputDir, "03-nuovo-cliente.png")

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
            capture(outputDir, "04-categoria-problema.png")

            clickDashboardTab("ORDERS")
            clickTag(RepairAppTestTags.orderCard(DemoDataSeeder.DEMO_ORDER_ID), scroll = true)
            waitForText("Stato corrente: Aperta")
            capture(outputDir, "05-commessa-aperta.png")

            applySimpleStatus(OrderStatusAction.START_PROGRESS, "Stato corrente: In lavorazione")
            capture(outputDir, "06-presa-in-carico.png")

            applySimpleStatus(OrderStatusAction.START_DIAGNOSIS, "Stato corrente: In diagnosi")
            applyResultStatus()
            capture(outputDir, "07-diagnosi-risultato.png")

            acceptQuote()
            capture(outputDir, "08-riparazione.png")

            applySimpleStatus(OrderStatusAction.READY_FOR_PICKUP, "Stato corrente: Pronta al ritiro")
            capture(outputDir, "09-pronta-ritiro.png")

            closeOrder()
            clickTag(RepairAppTestTags.orderCard(DemoDataSeeder.DEMO_ORDER_ID), scroll = true)
            waitForText("Stato corrente: Chiusa")
            capture(outputDir, "10-chiusura-timeline.png")
        }
    }

    @Test
    fun capturesCategoryAndTemplateStories() {
        runBlocking { DemoDataSeeder.seedDemoProfile(context) }
        val outputDir = prepareCaseDirectory(CASE_CONFIG)

        withDemoApp {
            waitForTag(RepairAppTestTags.AuthUsernameField)
            loginDemo(DemoDataSeeder.DEMO_ADMIN_USERNAME)
            capture(outputDir, "01-dashboard-admin.png")

            clickDashboardTab("CONFIG")
            capture(outputDir, "02-config-hub.png")

            clickTag(RepairAppTestTags.ConfigHubCategoriesButton, scroll = true)
            typeTag(RepairAppTestTags.ConfigCategoryNameField, "Smartphone", scroll = true)
            typeTag(RepairAppTestTags.ConfigCategoryDescriptionField, "Riparazioni smartphone, display e batterie.", scroll = true)
            capture(outputDir, "03-creazione-categoria.png")
            clickTag(RepairAppTestTags.ConfigCategorySaveButton, scroll = true)
            clickText("Conferma")
            waitForText("Operazione completata")
            capture(outputDir, "04-categoria-salvata.png")

            clickTag(RepairAppTestTags.ConfigBackButton, scroll = true)
            clickTag(RepairAppTestTags.ConfigHubTemplatesButton, scroll = true)
            typeTag(RepairAppTestTags.ConfigTemplateCodeField, "whatsapp_demo_social", scroll = true)
            typeTag(RepairAppTestTags.ConfigTemplateTitleField, "Aggiornamento social demo", scroll = true)
            typeTag(
                RepairAppTestTags.ConfigTemplateBodyField,
                "Ciao {{cliente}}, la commessa {{numero_commessa}} e' ora in stato {{stato_commessa}}.",
                scroll = true,
            )
            capture(outputDir, "05-creazione-template.png")
            clickTag(RepairAppTestTags.ConfigTemplateSaveButton, scroll = true)
            clickText("Conferma")
            waitForText("Operazione completata")
            capture(outputDir, "06-template-salvato.png")
        }
    }

    @Test
    fun capturesPrivateAndBusinessCustomerStories() {
        runBlocking { DemoDataSeeder.seedDemoProfile(context) }
        val outputDir = prepareCaseDirectory(CASE_CUSTOMERS)

        withDemoApp {
            waitForTag(RepairAppTestTags.AuthUsernameField)
            loginDemo(DemoDataSeeder.DEMO_ADMIN_USERNAME)
            clickDashboardTab("CUSTOMERS")
            capture(outputDir, "01-archivio-clienti.png")

            clickTag(RepairAppTestTags.CustomerNewButton, scroll = true)
            clickTag(RepairAppTestTags.CustomerPrivateToggle, scroll = true)
            typeTag(RepairAppTestTags.CustomerFirstNameField, "Luca", scroll = true)
            typeTag(RepairAppTestTags.CustomerLastNameField, "Rossi", scroll = true)
            typeTag(RepairAppTestTags.CustomerTaxCodeField, "RSSLCU90A01F205X", scroll = true)
            typeTag(RepairAppTestTags.CustomerPhoneField, "+39 333 010 3030", scroll = true)
            typeTag(RepairAppTestTags.CustomerEmailField, "luca.rossi@example.invalid", scroll = true)
            capture(outputDir, "02-cliente-privato.png")
            clickTag(RepairAppTestTags.CustomerSaveButton, scroll = true, requireDisplayed = false)
            clickTag(RepairAppTestTags.CustomerConfirmSaveButton)
            waitForText("Luca Rossi")
            capture(outputDir, "03-privato-salvato.png")

            clickTag(RepairAppTestTags.CustomerNewButton, scroll = true)
            clickTag(RepairAppTestTags.CustomerBusinessToggle, scroll = true)
            typeTag(RepairAppTestTags.CustomerCompanyNameField, "Demo Lab Srl", scroll = true)
            typeTag(RepairAppTestTags.CustomerVatNumberField, "IT12345678901", scroll = true)
            typeTag(RepairAppTestTags.CustomerPhoneField, "+39 02 010 4040", scroll = true)
            typeTag(RepairAppTestTags.CustomerEmailField, "amministrazione@example.invalid", scroll = true)
            typeTag(RepairAppTestTags.CustomerAddressField, "Via Demo 24, Milano", scroll = true)
            capture(outputDir, "04-cliente-azienda.png")
            clickTag(RepairAppTestTags.CustomerSaveButton, scroll = true, requireDisplayed = false)
            clickTag(RepairAppTestTags.CustomerConfirmSaveButton)
            waitForText("Demo Lab Srl")
            capture(outputDir, "05-azienda-salvata.png")
        }
    }

    private fun withDemoApp(block: () -> Unit) {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        try {
            block()
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
                clickTag(tabTag)
                return
            }
            composeRule.onNodeWithTag(RepairAppTestTags.DashboardTabs)
                .performTouchInput { swipeLeft() }
            composeRule.waitForIdle()
            pauseForDemo()
        }
        composeRule.onNodeWithTag(tabTag).performClick()
        composeRule.waitForIdle()
        pauseForDemo()
    }

    private fun loginDemo(username: String) {
        repeat(LOGIN_ATTEMPTS) { attempt ->
            waitForTag(RepairAppTestTags.AuthUsernameField)
            typeTag(RepairAppTestTags.AuthUsernameField, username)
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

    private fun clickTag(tag: String, scroll: Boolean = false, requireDisplayed: Boolean = true) {
        waitForTagExists(tag)
        val node = composeRule.onNodeWithTag(tag)
        if (scroll) runCatching { node.performScrollTo() }
        if (requireDisplayed && scroll && runCatching { node.assertIsDisplayed() }.isFailure) {
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
        if (requireDisplayed) {
            node.assertIsDisplayed()
        }
        if (requireDisplayed) {
            node.performClick()
        } else {
            node.performSemanticsAction(SemanticsActions.OnClick)
        }
        composeRule.waitForIdle()
        pauseForDemo()
    }

    private fun clickText(text: String) {
        waitForText(text)
        composeRule.onNodeWithText(text).performClick()
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

    private fun prepareCaseDirectory(caseName: String): File {
        val root = File(requireNotNull(context.getExternalFilesDir(null)), SCREENSHOT_ROOT_DIR_NAME)
        val directory = File(root, caseName)
        directory.deleteRecursively()
        directory.mkdirs()
        return directory
    }

    private fun pauseForDemo() {
        Thread.sleep(ACTION_PAUSE_MILLIS)
    }

    private fun pauseForScreenshot() {
        Thread.sleep(SCREENSHOT_PAUSE_MILLIS)
    }

    private companion object {
        const val CASE_ORDER = "01-commessa-nuovo-cliente-chiusura"
        const val CASE_CONFIG = "02-categorie-template"
        const val CASE_CUSTOMERS = "03-clienti-privato-azienda"
        const val SCREENSHOT_ROOT_DIR_NAME = "demo-social-screenshots"
        const val WAIT_TIMEOUT_MILLIS = 30_000L
        const val LOGIN_ATTEMPTS = 2
        const val LOGIN_ATTEMPT_TIMEOUT_MILLIS = 20_000L
        const val DASHBOARD_TAB_SWIPE_ATTEMPTS = 4
        const val VISIBILITY_SWIPE_ATTEMPTS = 5
        const val TAG_POLL_MILLIS = 250L
        const val ACTION_PAUSE_MILLIS = 900L
        const val SCREENSHOT_PAUSE_MILLIS = 1_400L
    }
}

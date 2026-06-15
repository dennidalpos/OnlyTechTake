package com.onlytechtake.repairapp.demo

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
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
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeLeft
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
            waitForText("Operatore")
            capture(outputDir, "02-dashboard-home.png")

            clickDashboardTab("NEW_ORDER")
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
            
            // Garantisce che il tag sia presente prima di procedere
            waitForTagExists(RepairAppTestTags.NewOrderFirstNameField)
            
            typeTag(RepairAppTestTags.NewOrderFirstNameField, "Giulia", scroll = true)
            typeTag(RepairAppTestTags.NewOrderLastNameField, "Bianchi", scroll = true)
            typeTag(RepairAppTestTags.NewOrderPhoneField, "+39 333 010 2020", scroll = true)
            typeTag(RepairAppTestTags.NewOrderEmailField, "giulia.bianchi@example.invalid", scroll = true)
            capture(outputDir, "03-nuovo-cliente.png")

            clickTag(RepairAppTestTags.NewOrderSaveCustomerButton, scroll = true)
            waitForText("Cliente selezionato")
            clickTag(RepairAppTestTags.NewOrderStep1NextButton, scroll = true)

            // Selezione categoria (navigazione gerarchia fino a foglia)
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
            capture(outputDir, "04-categoria-problema.png")

            clickDashboardTab("ORDERS")
            waitForText("Commesse")
            waitForTag(RepairAppTestTags.orderCard(DemoDataSeeder.DEMO_ORDER_ID), scroll = true)
            clickTag(RepairAppTestTags.orderCard(DemoDataSeeder.DEMO_ORDER_ID))
            
            // Verifica di essere nel dettaglio
            waitForTag(RepairAppTestTags.OrderStatusLabel)
            waitForText("Notebook HP")
            capture(outputDir, "05-commessa-aperta.png")

            applySimpleStatus(OrderStatusAction.START_PROGRESS, "In lavorazione")
            capture(outputDir, "06-presa-in-carico.png")

            applySimpleStatus(OrderStatusAction.START_DIAGNOSIS, "Diagnosi")
            applyResultStatus()
            capture(outputDir, "07-diagnosi-risultato.png")

            acceptQuote()
            capture(outputDir, "08-riparazione.png")

            applySimpleStatus(OrderStatusAction.READY_FOR_PICKUP, "Pronta al ritiro")
            capture(outputDir, "09-pronta-ritiro.png")

            closeOrder()
            clickTag(RepairAppTestTags.orderCard(DemoDataSeeder.DEMO_ORDER_ID), scroll = true)
            waitForText("Chiusa")
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
            waitForText("Admin")
            capture(outputDir, "01-dashboard-admin.png")

            clickDashboardTab("SETTINGS")
            waitForText("Setup")
            capture(outputDir, "02-config-hub.png")

            clickTag(RepairAppTestTags.ConfigHubCategoriesButton, scroll = true)
            clickTag(RepairAppTestTags.ConfigCategoryNewButton, scroll = true)
            typeTag(RepairAppTestTags.ConfigCategoryNameField, "Smartphone", scroll = true)
            typeTag(RepairAppTestTags.ConfigCategoryDescriptionField, "Riparazioni smartphone, display e batterie.", scroll = true)
            capture(outputDir, "03-creazione-categoria.png")
            clickTag(RepairAppTestTags.ConfigCategorySaveButton, scroll = true)
            clickText("Conferma")
            waitForText("Operazione completata")
            capture(outputDir, "04-categoria-salvata.png")

            clickTag(RepairAppTestTags.ConfigBackButton, scroll = true)
            clickTag(RepairAppTestTags.ConfigHubTemplatesButton, scroll = true)
            clickTag(RepairAppTestTags.ConfigTemplateNewButton, scroll = true)
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

    @Test
    fun capturesMaintenanceStories() {
        runBlocking { DemoDataSeeder.seedDemoProfile(context) }
        val outputDir = prepareCaseDirectory(CASE_MAINTENANCE)

        withDemoApp {
            waitForTag(RepairAppTestTags.AuthUsernameField)
            loginDemo(DemoDataSeeder.DEMO_ADMIN_USERNAME)
            
            clickDashboardTab("SETTINGS")
            waitForText("Setup")
            
            clickTag(RepairAppTestTags.ConfigHubStorageButton, scroll = true)
            waitForText("Memoria")
            capture(outputDir, "01-gestione-memoria.png")
            clickTag(RepairAppTestTags.ConfigBackButton, scroll = true)

            clickTag(RepairAppTestTags.ConfigHubBackupButton, scroll = true)
            waitForText("Backup")
            capture(outputDir, "02-backup-dati.png")
            
            clickText("Esporta Excel", scroll = true)
            waitForText("Esportazione")
            capture(outputDir, "03-export-excel.png")
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
        clickTag(RepairAppTestTags.QuoteAcceptedToggle, scroll = true)
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

        // Handle "Unsaved changes" dialog if it appears
        if (isTextDisplayed("Dati non salvati")) {
            clickText("Esci e perdi dati")
        }

        Thread.sleep(2000)
    }

    private fun isTextDisplayed(text: String): Boolean =
        composeRule.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty()

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
        Espresso.closeSoftKeyboard()
        pauseForDemo()
    }

    private fun clickTag(tag: String, scroll: Boolean = false, requireDisplayed: Boolean = true) {
        waitForTagExists(tag, scrollIfNotFound = scroll)
        val node = composeRule.onNodeWithTag(tag)
        
        // Approccio di click multi-livello invertito per priorità semantica (più robusto in emulatore)
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

    private fun clickText(text: String, scroll: Boolean = false, requireDisplayed: Boolean = true) {
        waitForText(text, scrollIfNotFound = scroll)
        val node = composeRule.onNodeWithText(text, substring = true)
        
        // Approccio di click multi-livello invertito per priorità semantica (più robusto in emulatore)
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
        // Swipe UP = Scroll DOWN
        try {
            composeRule.onNodeWithTag(RepairAppTestTags.OrderDetailLazyColumn).performTouchInput {
                swipeUp(startY = bottom * 0.7f, endY = top * 0.3f)
            }
        } catch (e: Throwable) {
            composeRule.onAllNodes(isRoot()).onFirst().performTouchInput { 
                swipeUp(startY = bottom * 0.7f, endY = top * 0.3f)
            }
        }
    }

    private fun performSafeSwipeDown() {
        // Swipe DOWN = Scroll UP
        try {
            composeRule.onNodeWithTag(RepairAppTestTags.OrderDetailLazyColumn).performTouchInput {
                swipeDown(startY = top * 0.3f, endY = bottom * 0.7f)
            }
        } catch (e: Throwable) {
            composeRule.onAllNodes(isRoot()).onFirst().performTouchInput { 
                swipeDown(startY = top * 0.3f, endY = bottom * 0.7f)
            }
        }
    }


    private fun waitForTag(tag: String, scroll: Boolean = false) {
        waitForTagExists(tag, scrollIfNotFound = scroll)
        composeRule.onNodeWithTag(tag).assertIsDisplayed()
    }

    private fun waitForTagExists(tag: String, scrollIfNotFound: Boolean = false) {
        var found = false
        val deadline = System.currentTimeMillis() + WAIT_TIMEOUT_MILLIS
        var attempts = 0

        while (!found && System.currentTimeMillis() < deadline) {
            found = isTagPresent(tag)
            if (!found && scrollIfNotFound && (attempts % 4 == 0)) {
                // Prova native prima di swipe manuale
                try {
                    composeRule.onNodeWithTag(tag).performScrollTo()
                    composeRule.waitForIdle()
                } catch (e: Throwable) {
                    // Alterna direzioni con priorità "Su" (per tornare da fondo pagina)
                    if (attempts % 12 < 8) {
                        performSafeSwipeDown() // Scroll UP
                    } else {
                        performSafeSwipeUp()   // Scroll DOWN
                    }
                    composeRule.waitForIdle()
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

    private fun waitForStatus(text: String) {
        composeRule.waitUntil(timeoutMillis = WAIT_TIMEOUT_MILLIS) {
            runCatching {
                composeRule.onNodeWithTag(RepairAppTestTags.OrderStatusLabel).performScrollTo()
            }
            isTextDisplayed(text)
        }
    }

    private fun isTagDisplayed(tag: String): Boolean =
        runCatching {
            composeRule.onNodeWithTag(tag).assertIsDisplayed()
        }.isSuccess

    private fun waitForText(text: String, scrollIfNotFound: Boolean = false) {
        var found = false
        val deadline = System.currentTimeMillis() + WAIT_TIMEOUT_MILLIS
        var attempts = 0

        while (!found && System.currentTimeMillis() < deadline) {
            found = isTextDisplayed(text)
            if (!found && scrollIfNotFound && (attempts % 4 == 0)) {
                try {
                    composeRule.onNodeWithText(text, substring = true).performScrollTo()
                    composeRule.waitForIdle()
                } catch (e: Throwable) {
                    if (attempts % 12 < 8) {
                        performSafeSwipeDown() // Scroll UP
                    } else {
                        performSafeSwipeUp()   // Scroll DOWN
                    }
                    composeRule.waitForIdle()
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
        const val CASE_MAINTENANCE = "05-manutenzione-backup"
        const val SCREENSHOT_ROOT_DIR_NAME = "demo-social-screenshots"
        const val WAIT_TIMEOUT_MILLIS = 30_000L
        const val LOGIN_ATTEMPTS = 2
        const val LOGIN_ATTEMPT_TIMEOUT_MILLIS = 20_000L
        const val VISIBILITY_SWIPE_ATTEMPTS = 5
        const val TAG_POLL_MILLIS = 250L
        const val ACTION_PAUSE_MILLIS = 1_400L
        const val SCREENSHOT_PAUSE_MILLIS = 1_400L
    }
}

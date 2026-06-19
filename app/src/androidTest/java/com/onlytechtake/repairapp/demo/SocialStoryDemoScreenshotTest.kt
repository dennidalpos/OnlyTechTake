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

            val username =
                if (BuildConfig.IS_DEMO_VERSION) DemoDataSeeder.DEMO_ADMIN_USERNAME else DemoDataSeeder.DEMO_OPERATOR_USERNAME
            loginDemo(username)
            waitForText(if (BuildConfig.IS_DEMO_VERSION) "Admin" else "Operatore")
            capture(outputDir, "02-dashboard-home.png")

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

            waitForTagExists(RepairAppTestTags.NewOrderFirstNameField)

            typeTag(RepairAppTestTags.NewOrderFirstNameField, "Giulia", scroll = true)
            typeTag(RepairAppTestTags.NewOrderLastNameField, "Bianchi", scroll = true)
            typeTag(RepairAppTestTags.NewOrderPhoneField, "+39 333 010 2020", scroll = true)
            typeTag(
                RepairAppTestTags.NewOrderEmailField,
                "giulia.bianchi@example.invalid",
                scroll = true
            )
            capture(outputDir, "03-nuovo-cliente.png")

            clickTag(RepairAppTestTags.NewOrderSaveCustomerButton, scroll = true)
            waitForText("Cliente selezionato")
            clickTag(RepairAppTestTags.NewOrderStep1NextButton, scroll = true)

            clickTag(RepairAppTestTags.categoryButton("demo-cat-pc"), scroll = true)
            clickTag(
                RepairAppTestTags.categoryButton(DemoDataSeeder.DEMO_CATEGORY_ID),
                scroll = true
            )
            clickTag(RepairAppTestTags.categoryButton("demo-cat-gaming"), scroll = true)
            clickTag(RepairAppTestTags.categoryButton("demo-cat-rog-strix"), scroll = true)

            clickTag(RepairAppTestTags.NewOrderStep2NextButton, scroll = true)
            typeTag(
                RepairAppTestTags.NewOrderProblemSummaryField,
                "Notebook non si accende",
                scroll = true
            )
            typeTag(
                RepairAppTestTags.NewOrderProblemDetailsField,
                "Il cliente segnala spegnimento improvviso dopo caduta accidentale.",
                scroll = true,
            )
            capture(outputDir, "04-categoria-problema.png")

            clickDashboardTab("ORDERS")
            waitForText("Commesse")
            waitForTag(RepairAppTestTags.orderCard(DemoDataSeeder.DEMO_ORDER_ID), scroll = true)
            waitForTag(
                RepairAppTestTags.orderCardOrderReference(DemoDataSeeder.DEMO_ORDER_ID),
                scroll = true,
                useUnmergedTree = true
            )
            waitForTag(
                RepairAppTestTags.orderCardCustomerReference(DemoDataSeeder.DEMO_ORDER_ID),
                scroll = true,
                useUnmergedTree = true
            )
            waitForTag(
                RepairAppTestTags.orderCardStatusStep(DemoDataSeeder.DEMO_ORDER_ID, 1),
                scroll = true,
                useUnmergedTree = true
            )
            clickTag(RepairAppTestTags.orderCard(DemoDataSeeder.DEMO_ORDER_ID))

            waitForTag(RepairAppTestTags.OrderStatusLabel)
            waitForTag(RepairAppTestTags.OrderDetailOrderReference, useUnmergedTree = true)
            waitForTag(RepairAppTestTags.OrderDetailCustomerReference, useUnmergedTree = true)
            waitForTag(RepairAppTestTags.orderDetailStatusStep(1), useUnmergedTree = true)
            waitForText("Asus ROG Strix")
            capture(outputDir, "05-commessa-aperta.png")

            applySimpleStatus(
                OrderStatusAction.START_PROGRESS,
                nextAction = OrderStatusAction.START_DIAGNOSIS
            )
            capture(outputDir, "06-presa-in-carico.png")

            applySimpleStatus(
                OrderStatusAction.START_DIAGNOSIS,
                nextAction = OrderStatusAction.RECORD_RESULT
            )
            applyResultStatus()
            capture(outputDir, "07-diagnosi-risultato.png")

            acceptQuote()
            capture(outputDir, "08-riparazione.png")

            applySimpleStatus(
                OrderStatusAction.READY_FOR_PICKUP,
                nextAction = OrderStatusAction.CLOSE_ORDER
            )
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
            typeTag(RepairAppTestTags.ConfigCategoryNameField, "Console portatile", scroll = true)
            typeTag(
                RepairAppTestTags.ConfigCategoryDescriptionField,
                "Riparazioni console portatili, display e batterie.",
                scroll = true
            )
            capture(outputDir, "03-creazione-categoria.png")
            clickTag(RepairAppTestTags.ConfigCategorySaveButton, scroll = true)
            waitForTag(RepairAppTestTags.GenericConfirmButton)
            clickTag(RepairAppTestTags.GenericConfirmButton)
            waitForText("Operazione completata")
            capture(outputDir, "04-categoria-salvata.png")

            clickTag(RepairAppTestTags.ConfigBackButton, scroll = true)
            clickTag(RepairAppTestTags.ConfigHubTemplateMappingButton, scroll = true)
            waitForText("Associazione Template")
            capture(outputDir, "05-template-associazioni.png")
            clickTag(RepairAppTestTags.ConfigBackButton, scroll = true)
            clickTag(RepairAppTestTags.ConfigHubTemplatesButton, scroll = true)
            waitForText("Gestione Template")
            clickTag(RepairAppTestTags.ConfigTemplateNewButton, scroll = true)
            typeTag(
                RepairAppTestTags.ConfigTemplateCodeField,
                "whatsapp_demo_social",
                scroll = true
            )
            typeTag(
                RepairAppTestTags.ConfigTemplateTitleField,
                "Aggiornamento social demo",
                scroll = true
            )
            typeTag(
                RepairAppTestTags.ConfigTemplateBodyField,
                "Ciao {{cliente}}, la commessa {{numero_commessa}} e' ora in stato {{stato_commessa}}.",
                scroll = true,
            )
            capture(outputDir, "05-creazione-template.png")
            clickTag(RepairAppTestTags.ConfigTemplateSaveButton, scroll = true)
            waitForTag(RepairAppTestTags.GenericConfirmButton)
            clickTag(RepairAppTestTags.GenericConfirmButton)
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
            typeTag(
                RepairAppTestTags.CustomerEmailField,
                "luca.rossi@example.invalid",
                scroll = true
            )
            capture(outputDir, "02-cliente-privato.png")
            clickTag(RepairAppTestTags.CustomerSaveButton, scroll = true)
            waitForTag(RepairAppTestTags.CustomerConfirmSaveButton, useUnmergedTree = true)
            clickText("Conferma")
            typeTag(RepairAppTestTags.CustomerSearchField, "Luca")
            waitForText("Luca Rossi")
            capture(outputDir, "03-privato-salvato.png")

            clickTag(RepairAppTestTags.CustomerNewButton, scroll = true)
            clickTag(RepairAppTestTags.CustomerBusinessToggle, scroll = true)
            typeTag(RepairAppTestTags.CustomerCompanyNameField, "Demo Lab Srl", scroll = true)
            typeTag(RepairAppTestTags.CustomerVatNumberField, "IT12345678901", scroll = true)
            typeTag(RepairAppTestTags.CustomerPhoneField, "+39 02 010 4040", scroll = true)
            typeTag(
                RepairAppTestTags.CustomerEmailField,
                "amministrazione@example.invalid",
                scroll = true
            )
            typeTag(RepairAppTestTags.CustomerAddressField, "Via Demo 24, Milano", scroll = true)
            capture(outputDir, "04-cliente-azienda.png")
            clickTag(RepairAppTestTags.CustomerSaveButton, scroll = true)
            waitForTag(RepairAppTestTags.CustomerConfirmSaveButton, useUnmergedTree = true)
            clickText("Conferma")
            typeTag(RepairAppTestTags.CustomerSearchField, "Demo")
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
            waitForText("Archiviazione")
            capture(outputDir, "01-gestione-memoria.png")
            clickTag(RepairAppTestTags.ConfigBackButton, scroll = true)

            clickTag(RepairAppTestTags.ConfigHubBackupButton, scroll = true)
            waitForText("Backup")
            capture(outputDir, "02-backup-dati.png")

            clickText("Esportazione Commesse", scroll = true)
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

    private fun applySimpleStatus(
        action: OrderStatusAction,
        nextAction: OrderStatusAction,
    ) {
        waitForTag(RepairAppTestTags.OrderActionSection, scroll = true)
        pauseForDemo()
        clickTag(RepairAppTestTags.statusAction(action.name), scroll = true)
        pauseForDemo()
        waitForTag(RepairAppTestTags.StatusDialogConfirmButton)
        clickTag(RepairAppTestTags.StatusDialogConfirmButton)
        composeRule.waitForIdle()
        pauseForDemo()
        waitForTag(RepairAppTestTags.statusAction(nextAction.name), scroll = true)
    }

    private fun applyResultStatus() {
        waitForTag(RepairAppTestTags.OrderActionSection, scroll = true)
        pauseForDemo()
        clickTag(
            RepairAppTestTags.statusAction(OrderStatusAction.RECORD_RESULT.name),
            scroll = true
        )
        pauseForDemo()
        waitForTag(RepairAppTestTags.StatusResultSummaryField)
        typeTag(
            RepairAppTestTags.StatusResultSummaryField,
            "Connettore di alimentazione danneggiato.",
            scroll = false
        )
        clickTag(RepairAppTestTags.DiagnosisRepairableToggle)
        typeTag(RepairAppTestTags.DiagnosisEstimateField, "149,00", scroll = false)
        clickTag(RepairAppTestTags.StatusDialogConfirmButton)
        composeRule.waitForIdle()
        pauseForDemo()
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
            scroll = true
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
            "Riparazione completata e consegnata al cliente."
        )
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

            if (waitForTagIfPresent(
                    RepairAppTestTags.dashboardTab("HOME"),
                    LOGIN_ATTEMPT_TIMEOUT_MILLIS
                )
            ) {
                return
            }

            if (attempt == LOGIN_ATTEMPTS - 1 || !isTagPresent(RepairAppTestTags.AuthUsernameField)) {
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
        waitForTagExists(tag, scrollIfNotFound = scroll)
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
        waitForText(text, scrollIfNotFound = scroll)
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
                "customer_form"
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
                "customer_form"
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

    private fun waitForTag(
        tag: String,
        scroll: Boolean = false,
        useUnmergedTree: Boolean = false,
    ) {
        waitForTagExists(tag, scrollIfNotFound = scroll, useUnmergedTree = useUnmergedTree)
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
        scrollIfNotFound: Boolean = false,
        useUnmergedTree: Boolean = false,
    ) {
        var found = false
        val deadline = System.currentTimeMillis() + WAIT_TIMEOUT_MILLIS
        var attempts = 0

        while (!found && System.currentTimeMillis() < deadline) {
            found = isTagPresent(tag, useUnmergedTree)
            if (found) break

            if (scrollIfNotFound) {
                if (attempts % 2 == 0) {
                    try {
                        composeRule.onNodeWithTag(tag, useUnmergedTree = useUnmergedTree)
                            .performScrollTo()
                        composeRule.waitForIdle()
                        if (isTagPresent(tag, useUnmergedTree)) {
                            found = true
                            break
                        }
                    } catch (e: Throwable) {
                        if (attempts % 15 < 10) {
                            performSafeSwipeUp() // Scroll DOWN (reveal items below)
                        } else {
                            performSafeSwipeDown() // Scroll UP (reveal items above)
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

    private fun waitForTagIfPresent(
        tag: String,
        timeoutMillis: Long,
    ): Boolean {
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

    private fun isTagPresent(
        tag: String,
        useUnmergedTree: Boolean = false,
    ): Boolean =
        composeRule.onAllNodesWithTag(tag, useUnmergedTree = useUnmergedTree).fetchSemanticsNodes()
            .isNotEmpty()

    private fun isTagDisplayed(tag: String): Boolean =
        runCatching {
            composeRule.onNodeWithTag(tag).assertIsDisplayed()
        }.isSuccess

    private fun waitForText(
        text: String,
        scrollIfNotFound: Boolean = false,
    ) {
        var found = false
        val deadline = System.currentTimeMillis() + WAIT_TIMEOUT_MILLIS
        var attempts = 0

        while (!found && System.currentTimeMillis() < deadline) {
            found = isTextDisplayed(text)
            if (found) break

            if (scrollIfNotFound) {
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
                            performSafeSwipeUp() // Scroll DOWN (reveal items below)
                        } else {
                            performSafeSwipeDown() // Scroll UP (reveal items above)
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

    private fun prepareCaseDirectory(caseName: String): File {
        val root = File(requireNotNull(context.getExternalFilesDir(null)), SCREENSHOT_ROOT_DIR_NAME)
        val directory = File(root, caseName)
        directory.deleteRecursively()
        directory.mkdirs()
        return directory
    }

    private fun pauseForDemo() {
        Thread.sleep(if (isRecordingDemo()) RECORDING_ACTION_PAUSE_MILLIS else ACTION_PAUSE_MILLIS)
    }

    private fun pauseForScreenshot() {
        Thread.sleep(if (isRecordingDemo()) RECORDING_SCREENSHOT_PAUSE_MILLIS else SCREENSHOT_PAUSE_MILLIS)
    }

    private fun isRecordingDemo(): Boolean =
        InstrumentationRegistry.getArguments().getString("demoRecording").toBoolean()

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
        const val RECORDING_ACTION_PAUSE_MILLIS = 500L
        const val RECORDING_SCREENSHOT_PAUSE_MILLIS = 250L
    }
}

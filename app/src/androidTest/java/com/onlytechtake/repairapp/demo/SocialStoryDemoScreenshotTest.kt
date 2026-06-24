package com.onlytechtake.repairapp.demo

import android.content.Context
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.onlytechtake.repairapp.BuildConfig
import com.onlytechtake.repairapp.MainActivity
import com.onlytechtake.repairapp.core.design.RepairAppTestTags
import com.onlytechtake.repairapp.domain.workspace.OrderStatusAction
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class SocialStoryDemoScreenshotTest {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    @get:Rule
    val cameraPermissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(android.Manifest.permission.CAMERA)

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun capturesOrderIntakeToClosureStories() {
        runBlocking { DemoDataSeeder.seedDemoProfile(context) }
        val outputDir = prepareCaseDirectory(CASE_ORDER)
        val robot = ProAssetRobot(composeRule, context)

        withDemoApp {
            robot.waitForTag(RepairAppTestTags.AuthUsernameField)
            robot.capture(outputDir, "01-login.png")

            val username =
                if (BuildConfig.IS_DEMO_VERSION) DemoDataSeeder.DEMO_ADMIN_USERNAME else DemoDataSeeder.DEMO_OPERATOR_USERNAME
            robot.loginDemo(username)
            robot.waitForText(if (BuildConfig.IS_DEMO_VERSION) "Admin" else "Operatore")
            robot.capture(outputDir, "02-dashboard-home.png")

            robot.clickTag(RepairAppTestTags.HomeNewOrderButton, scroll = true)
            robot.waitForTag("quick_actions_screen")

            var formOpened = false
            repeat(3) {
                if (!formOpened) {
                    if (robot.isTagPresent(RepairAppTestTags.NewOrderInsertCustomerButton)) {
                        robot.clickTag(RepairAppTestTags.NewOrderInsertCustomerButton, scroll = true)
                    }
                    robot.pauseForDemo()
                    formOpened = robot.isTagPresent(RepairAppTestTags.NewOrderFirstNameField)
                }
            }

            robot.waitForTagExists(RepairAppTestTags.NewOrderFirstNameField)

            robot.typeTag(RepairAppTestTags.NewOrderFirstNameField, "Giulia", scroll = true)
            robot.typeTag(RepairAppTestTags.NewOrderLastNameField, "Bianchi", scroll = true)
            robot.typeTag(RepairAppTestTags.NewOrderPhoneField, "+39 333 010 2020", scroll = true)
            robot.typeTag(
                RepairAppTestTags.NewOrderEmailField,
                "giulia.bianchi@example.invalid",
                scroll = true,
            )
            robot.capture(outputDir, "03-nuovo-cliente.png")

            robot.clickTag(RepairAppTestTags.NewOrderSaveCustomerButton, scroll = true)
            robot.waitForText("Cliente selezionato")
            robot.clickTag(RepairAppTestTags.NewOrderStep1NextButton, scroll = true)

            robot.clickTag(RepairAppTestTags.categoryButton("demo-cat-pc"), scroll = true)
            robot.clickTag(
                RepairAppTestTags.categoryButton(DemoDataSeeder.DEMO_CATEGORY_ID),
                scroll = true,
            )
            robot.clickTag(RepairAppTestTags.categoryButton("demo-cat-gaming"), scroll = true)
            robot.clickTag(RepairAppTestTags.categoryButton("demo-cat-rog-strix"), scroll = true)
            robot.capture(outputDir, "04-selezione-categoria.png")

            robot.clickTag(RepairAppTestTags.NewOrderStep2NextButton, scroll = true)
            robot.typeTag(
                RepairAppTestTags.NewOrderProblemSummaryField,
                "Notebook non si accende",
                scroll = true,
            )
            robot.typeTag(
                RepairAppTestTags.NewOrderProblemDetailsField,
                "Il cliente segnala spegnimento improvviso dopo caduta accidentale.",
                scroll = true,
            )
            robot.capture(outputDir, "05-dettaglio-problema.png")

            // 4. Wizard: media
            robot.clickTag(RepairAppTestTags.NewOrderStep3NextButton, scroll = true)
            robot.waitForText("Foto e allegati")
            robot.clickTag(RepairAppTestTags.IntakeMediaTakePhotoButton, scroll = true)
            robot.waitForTag(RepairAppTestTags.IntakeMediaCapturePhotoButton, scroll = true)
            robot.clickTag(RepairAppTestTags.IntakeMediaCapturePhotoButton, scroll = true)
            robot.waitForText("PHOTO", scroll = true)
            robot.capture(outputDir, "06-acquisizione-media.png")

            // 5. Wizard: privacy e firma
            robot.clickTag(RepairAppTestTags.NewOrderStep4NextButton, scroll = true)
            robot.waitForText("Privacy e firma")
            robot.clickText("Accetta informativa privacy", scroll = true)
            robot.clickText("Accetta condizioni di presa in carico", scroll = true)
            robot.drawSignature(RepairAppTestTags.SignatureCanvas)
            robot.waitForTag(RepairAppTestTags.NewOrderStep5NextButton, scroll = true)
            robot.capture(outputDir, "07-firma-cliente.png")

            robot.clickTag(RepairAppTestTags.NewOrderStep5NextButton, scroll = true)
            robot.waitForTag(RepairAppTestTags.IntakeSubmitButton, scroll = true)
            robot.capture(outputDir, "08-riepilogo-invio.png")
            robot.clickTag(RepairAppTestTags.IntakeSubmitButton, scroll = true)

            robot.clickDashboardTab("ORDERS")
            robot.waitForText("Commesse")
            robot.capture(outputDir, "09-elenco-commesse.png")
            robot.waitForTag(RepairAppTestTags.orderCard(DemoDataSeeder.DEMO_ORDER_ID), scroll = true)
            robot.clickTag(RepairAppTestTags.orderCard(DemoDataSeeder.DEMO_ORDER_ID))

            robot.waitForTag(RepairAppTestTags.OrderStatusLabel)
            robot.waitForTag(RepairAppTestTags.OrderDetailOrderReference, useUnmergedTree = true)
            robot.waitForTag(RepairAppTestTags.OrderDetailCustomerReference, useUnmergedTree = true)
            robot.waitForTag(RepairAppTestTags.orderDetailStatusStep(1), useUnmergedTree = true)
            robot.waitForText("Asus ROG Strix")
            robot.capture(outputDir, "10-dettaglio-commessa.png")

            robot.applySimpleStatus(
                OrderStatusAction.START_PROGRESS,
                nextAction = OrderStatusAction.START_DIAGNOSIS,
            )
            robot.applySimpleStatus(
                OrderStatusAction.START_DIAGNOSIS,
                nextAction = OrderStatusAction.RECORD_RESULT,
            )
            robot.applyResultStatus(
                summary = "Connettore di alimentazione danneggiato.",
                estimate = "149,00",
            )
            robot.capture(outputDir, "11-risultato-diagnosi.png")

            robot.acceptQuote()
            robot.applySimpleStatus(
                OrderStatusAction.READY_FOR_PICKUP,
                nextAction = OrderStatusAction.CLOSE_ORDER,
            )
            robot.closeOrder()
            robot.clickTag(RepairAppTestTags.orderCard(DemoDataSeeder.DEMO_ORDER_ID), scroll = true)
            robot.waitForText("Chiusa")
            robot.capture(outputDir, "12-chiusura-timeline.png")
        }
    }

    @Test
    fun capturesCategoryAndTemplateStories() {
        runBlocking { DemoDataSeeder.seedDemoProfile(context) }
        val outputDir = prepareCaseDirectory(CASE_CONFIG)
        val robot = ProAssetRobot(composeRule, context)

        withDemoApp {
            robot.waitForTag(RepairAppTestTags.AuthUsernameField)
            robot.loginDemo(DemoDataSeeder.DEMO_ADMIN_USERNAME)
            robot.waitForText("Admin")
            robot.capture(outputDir, "01-dashboard-admin.png")

            robot.clickDashboardTab("SETTINGS")
            robot.waitForText("Setup")
            robot.capture(outputDir, "02-config-hub.png")

            robot.clickTag(RepairAppTestTags.ConfigHubCategoriesButton, scroll = true)
            robot.clickTag(RepairAppTestTags.ConfigCategoryNewButton, scroll = true)
            robot.typeTag(RepairAppTestTags.ConfigCategoryNameField, "Console portatile", scroll = true)
            robot.typeTag(
                RepairAppTestTags.ConfigCategoryDescriptionField,
                "Riparazioni console portatili, display e batterie.",
                scroll = true,
            )
            robot.capture(outputDir, "03-creazione-categoria.png")
            robot.clickTag(RepairAppTestTags.ConfigCategorySaveButton, scroll = true)
            robot.waitForTag(RepairAppTestTags.GenericConfirmButton)
            robot.clickTag(RepairAppTestTags.GenericConfirmButton)
            robot.waitForText("Operazione completata")
            robot.capture(outputDir, "04-categoria-salvata.png")

            robot.clickTag(RepairAppTestTags.ConfigBackButton, scroll = true)
            robot.clickTag(RepairAppTestTags.ConfigHubTemplateMappingButton, scroll = true)
            robot.waitForText("Associazione Template")
            robot.capture(outputDir, "05-template-associazioni.png")
            robot.clickTag(RepairAppTestTags.ConfigBackButton, scroll = true)
            robot.clickTag(RepairAppTestTags.ConfigHubTemplatesButton, scroll = true)
            robot.waitForText("Gestione Template")
            robot.clickTag(RepairAppTestTags.ConfigTemplateNewButton, scroll = true)

            // Capture Tooltip for variables
            robot.clickTag(RepairAppTestTags.ConfigTemplateVariablesTooltip, scroll = true)
            robot.pauseForDemo()
            robot.capture(outputDir, "06-template-tooltip.png")

            robot.typeTag(
                RepairAppTestTags.ConfigTemplateCodeField,
                "whatsapp_demo_social",
                scroll = true,
            )
            robot.typeTag(
                RepairAppTestTags.ConfigTemplateTitleField,
                "Aggiornamento social demo",
                scroll = true,
            )
            robot.typeTag(
                RepairAppTestTags.ConfigTemplateBodyField,
                "Ciao {{cliente}}, la commessa {{numero_commessa}} e' ora in stato {{stato_commessa}}.",
                scroll = true,
            )
            robot.capture(outputDir, "07-creazione-template.png")
            robot.clickTag(RepairAppTestTags.ConfigTemplateSaveButton, scroll = true)
            robot.waitForTag(RepairAppTestTags.GenericConfirmButton)
            robot.clickTag(RepairAppTestTags.GenericConfirmButton)
            robot.waitForText("Operazione completata")
            robot.capture(outputDir, "08-template-salvato.png")
        }
    }

    @Test
    fun capturesPrivateAndBusinessCustomerStories() {
        runBlocking { DemoDataSeeder.seedDemoProfile(context) }
        val outputDir = prepareCaseDirectory(CASE_CUSTOMERS)
        val robot = ProAssetRobot(composeRule, context)

        withDemoApp {
            robot.waitForTag(RepairAppTestTags.AuthUsernameField)
            robot.loginDemo(DemoDataSeeder.DEMO_ADMIN_USERNAME)
            robot.clickDashboardTab("CUSTOMERS")
            robot.capture(outputDir, "01-archivio-clienti.png")

            robot.clickTag(RepairAppTestTags.CustomerNewButton, scroll = true)
            robot.clickTag(RepairAppTestTags.CustomerPrivateToggle, scroll = true)
            robot.typeTag(RepairAppTestTags.CustomerFirstNameField, "Luca", scroll = true)
            robot.typeTag(RepairAppTestTags.CustomerLastNameField, "Rossi", scroll = true)
            robot.typeTag(RepairAppTestTags.CustomerTaxCodeField, "RSSLCU90A01F205X", scroll = true)
            robot.typeTag(RepairAppTestTags.CustomerPhoneField, "+39 333 010 3030", scroll = true)
            robot.typeTag(
                RepairAppTestTags.CustomerEmailField,
                "luca.rossi@example.invalid",
                scroll = true,
            )
            robot.capture(outputDir, "02-cliente-privato.png")
            robot.clickTag(RepairAppTestTags.CustomerSaveButton, scroll = true)
            robot.waitForTag(RepairAppTestTags.CustomerConfirmSaveButton, useUnmergedTree = true)
            robot.clickText("Conferma")
            robot.typeTag(RepairAppTestTags.CustomerSearchField, "Luca")
            robot.waitForText("Luca Rossi")
            robot.capture(outputDir, "03-privato-salvato.png")

            robot.clickTag(RepairAppTestTags.CustomerNewButton, scroll = true)
            robot.clickTag(RepairAppTestTags.CustomerBusinessToggle, scroll = true)
            robot.typeTag(RepairAppTestTags.CustomerCompanyNameField, "Demo Lab Srl", scroll = true)
            robot.typeTag(RepairAppTestTags.CustomerVatNumberField, "IT12345678901", scroll = true)
            robot.typeTag(RepairAppTestTags.CustomerPhoneField, "+39 02 010 4040", scroll = true)
            robot.typeTag(
                RepairAppTestTags.CustomerEmailField,
                "amministrazione@example.invalid",
                scroll = true,
            )
            robot.typeTag(RepairAppTestTags.CustomerAddressField, "Via Demo 24, Milano", scroll = true)
            robot.capture(outputDir, "04-cliente-azienda.png")
            robot.clickTag(RepairAppTestTags.CustomerSaveButton, scroll = true)
            robot.waitForTag(RepairAppTestTags.CustomerConfirmSaveButton, useUnmergedTree = true)
            robot.clickText("Conferma")
            robot.typeTag(RepairAppTestTags.CustomerSearchField, "Demo")
            robot.waitForText("Demo Lab Srl")
            robot.capture(outputDir, "05-azienda-salvata.png")
        }
    }

    @Test
    fun capturesMaintenanceStories() {
        runBlocking { DemoDataSeeder.seedDemoProfile(context) }
        val outputDir = prepareCaseDirectory(CASE_MAINTENANCE)
        val robot = ProAssetRobot(composeRule, context)

        withDemoApp {
            robot.waitForTag(RepairAppTestTags.AuthUsernameField)
            robot.loginDemo(DemoDataSeeder.DEMO_ADMIN_USERNAME)

            robot.clickDashboardTab("SETTINGS")
            robot.waitForText("Setup")

            robot.clickTag(RepairAppTestTags.ConfigHubStorageButton, scroll = true)
            robot.waitForText("Archiviazione")
            robot.capture(outputDir, "01-gestione-memoria.png")
            robot.clickTag(RepairAppTestTags.ConfigBackButton, scroll = true)

            robot.clickTag(RepairAppTestTags.ConfigHubBackupButton, scroll = true)
            robot.waitForText("Backup")
            robot.capture(outputDir, "02-backup-dati.png")

            robot.clickText("Esportazione Commesse", scroll = true)
            robot.waitForText("Esportazione")
            robot.capture(outputDir, "03-export-excel.png")
        }
    }

    @Test
    fun capturesDemoLimitStory() {
        if (!BuildConfig.IS_DEMO_VERSION) return

        runBlocking { DemoDataSeeder.seedDemoProfile(context) }
        val outputDir = prepareCaseDirectory(CASE_DEMO_LIMIT)
        val robot = ProAssetRobot(composeRule, context)

        withDemoApp {
            robot.waitForTag(RepairAppTestTags.AuthUsernameField)
            robot.loginDemo(DemoDataSeeder.DEMO_ADMIN_USERNAME)

            // We already have 1 order from seeder
            robot.clickTag(RepairAppTestTags.HomeNewOrderButton, scroll = true)
            robot.waitForTag("quick_actions_screen")

            // Try to create another one
            robot.clickTag(RepairAppTestTags.NewOrderInsertCustomerButton, scroll = true)
            robot.pauseForDemo()

            // Error dialog should appear
            robot.waitForText(
                context.resources
                    .getQuantityString(
                        com.onlytechtake.repairapp.R.plurals.demo_error_order_limit,
                        BuildConfig.MAX_ORDERS,
                        BuildConfig.MAX_ORDERS,
                    ).substring(0, 10),
            )
            robot.capture(outputDir, "01-demo-limit-error.png")
            robot.clickTag(RepairAppTestTags.MessageBannerCloseButton)
        }
    }

    private fun withDemoApp(block: () -> Unit) {
        ActivityScenario.launch(MainActivity::class.java).use {
            block()
        }
    }

    private fun prepareCaseDirectory(caseName: String): File {
        val root = File(requireNotNull(context.getExternalFilesDir(null)), SCREENSHOT_ROOT_DIR_NAME)
        val directory = File(root, caseName)
        directory.deleteRecursively()
        directory.mkdirs()
        return directory
    }

    private companion object {
        const val CASE_ORDER = "01-commessa-nuovo-cliente-chiusura"
        const val CASE_CONFIG = "02-categorie-template"
        const val CASE_CUSTOMERS = "03-clienti-privato-azienda"
        const val CASE_MAINTENANCE = "05-manutenzione-backup"
        const val CASE_DEMO_LIMIT = "06-demo-limit"
        const val SCREENSHOT_ROOT_DIR_NAME = "social-stories"
    }
}

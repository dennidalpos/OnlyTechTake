package com.onlytechtake.repairapp.demo

import android.content.Context
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
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

@RunWith(AndroidJUnit4::class)
class StoreProScreenshotTest {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    @get:Rule
    val cameraPermissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(android.Manifest.permission.CAMERA)

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun capturesStoreProScreenshots() {
        runBlocking { DemoDataSeeder.seedDemoProfile(context) }
        val outputDir = prepareScreenshotDirectory()
        val robot = ProAssetRobot(composeRule, context)

        ActivityScenario.launch(MainActivity::class.java).use {
            robot.waitForTag(RepairAppTestTags.AuthUsernameField)
            robot.loginDemo(DemoDataSeeder.DEMO_OPERATOR_USERNAME)

            // 01-dashboard-pro
            robot.waitForTag(RepairAppTestTags.dashboardTab("HOME"))
            robot.capture(outputDir, "01-dashboard-pro.png")

            // 02-nuova-commessa-pro (Step 1 Customer)
            robot.clickTag(RepairAppTestTags.HomeNewOrderButton, scroll = true)
            robot.waitForTag("quick_actions_screen")
            robot.clickTag(RepairAppTestTags.NewOrderInsertCustomerButton, scroll = true)
            robot.typeTag(RepairAppTestTags.NewOrderFirstNameField, "Marco", scroll = true)
            robot.typeTag(RepairAppTestTags.NewOrderLastNameField, "Bianchi", scroll = true)
            robot.typeTag(RepairAppTestTags.NewOrderPhoneField, "+39 347 123 4567", scroll = true)
            robot.typeTag(
                RepairAppTestTags.NewOrderEmailField,
                "m.bianchi@example.invalid",
                scroll = true,
            )
            robot.capture(outputDir, "02-nuova-commessa-pro.png")

            robot.clickTag(RepairAppTestTags.NewOrderSaveCustomerButton, scroll = true)
            robot.waitForText("Cliente selezionato")
            robot.clickTag(RepairAppTestTags.NewOrderStep1NextButton, scroll = true)

            // Fill Step 2 (Category)
            robot.clickTag(RepairAppTestTags.categoryButton("demo-cat-pc"), scroll = true)
            robot.clickTag(
                RepairAppTestTags.categoryButton(DemoDataSeeder.DEMO_CATEGORY_ID),
                scroll = true,
            )
            robot.clickTag(RepairAppTestTags.categoryButton("demo-cat-gaming"), scroll = true)
            robot.clickTag(RepairAppTestTags.categoryButton("demo-cat-rog-strix"), scroll = true)
            robot.capture(outputDir, "02b-categoria-pro.png")
            robot.clickTag(RepairAppTestTags.NewOrderStep2NextButton, scroll = true)

            // Step 3 (Problem)
            robot.typeTag(
                RepairAppTestTags.NewOrderProblemSummaryField,
                "Samsung Galaxy S23 - Schermo rotto",
                scroll = true,
            )
            robot.typeTag(
                RepairAppTestTags.NewOrderProblemDetailsField,
                "Sostituzione display originale dopo caduta.",
                scroll = true,
            )
            robot.capture(outputDir, "02c-problema-pro.png")
            robot.clickTag(RepairAppTestTags.NewOrderStep3NextButton, scroll = true)

            // Step 4: Photo
            robot.waitForText("Foto e allegati")
            robot.clickTag(RepairAppTestTags.IntakeMediaTakePhotoButton, scroll = true)
            robot.waitForTag(RepairAppTestTags.IntakeMediaCapturePhotoButton, scroll = true)
            robot.clickTag(RepairAppTestTags.IntakeMediaCapturePhotoButton, scroll = true)
            robot.waitForText("PHOTO", scroll = true)
            robot.capture(outputDir, "02d-foto-pro.png")
            robot.clickTag(RepairAppTestTags.NewOrderStep4NextButton, scroll = true)

            // 03-foto-firma-pro (Step 5 Signature)
            robot.waitForText("Privacy e firma")
            robot.clickText("Accetta informativa privacy", scroll = true)
            robot.clickText("Accetta condizioni di presa in carico", scroll = true)
            robot.drawSignature(RepairAppTestTags.SignatureCanvas)
            robot.capture(outputDir, "03-foto-firma-pro.png")
            robot.clickTag(RepairAppTestTags.NewOrderStep5NextButton, scroll = true)

            // Step 6: Summary and Submit
            robot.waitForTag(RepairAppTestTags.IntakeSubmitButton, scroll = true)
            robot.clickTag(RepairAppTestTags.IntakeSubmitButton, scroll = true)

            val createdOrderId = waitForCreatedOrderId("Samsung Galaxy S23 - Schermo rotto")
            robot.clickDashboardTab("ORDERS")
            robot.waitForTag(RepairAppTestTags.orderCard(createdOrderId), scroll = true)
            robot.clickTag(RepairAppTestTags.orderCard(createdOrderId), scroll = true)

            // 04-diagnosi-preventivo-pro
            robot.applySimpleStatus(
                OrderStatusAction.START_PROGRESS,
                OrderStatusAction.START_DIAGNOSIS,
            )
            robot.applySimpleStatus(
                OrderStatusAction.START_DIAGNOSIS,
                OrderStatusAction.RECORD_RESULT,
            )
            robot.applyResultStatus("Display AMOLED originale Samsung.", "249,00")
            robot.capture(outputDir, "04-diagnosi-preventivo-pro.png")

            // 05-whatsapp-pdf-pro
            robot.waitForTag(RepairAppTestTags.OrderWhatsAppButton, scroll = true)
            robot.clickTag(RepairAppTestTags.OrderWhatsAppButton)
            robot.waitForTag(RepairAppTestTags.OrderWhatsAppPreviewPanel)
            robot.capture(outputDir, "05-whatsapp-pdf-pro.png")
            robot.dismissWhatsAppPreviewIfPresent()

            // 06-timeline-chiusura-pro
            robot.acceptQuote()
            robot.applySimpleStatus(
                OrderStatusAction.READY_FOR_PICKUP,
                OrderStatusAction.CLOSE_ORDER,
            )
            robot.closeOrder("Riparazione completata con successo.")
            robot.clickTag(RepairAppTestTags.orderCard(createdOrderId), scroll = true)
            robot.waitForText("Chiusa")
            robot.capture(outputDir, "06-timeline-chiusura-pro.png")

            // 07-clienti-pro
            robot.clickDashboardTab("CUSTOMERS")
            robot.waitForTag(RepairAppTestTags.CustomerSearchField)
            robot.capture(outputDir, "07-clienti-pro.png")

            // 08-configurazione-pro
            robot.clickDashboardTab("SETTINGS")
            robot.waitForText("Setup")
            robot.capture(outputDir, "08-configurazione-pro.png")
        }
    }

    private fun waitForCreatedOrderId(summary: String): String {
        val database = RepairAppDatabase.getInstance(context)
        val deadline = System.currentTimeMillis() + 30_000L
        while (System.currentTimeMillis() < deadline) {
            val order =
                runBlocking {
                    database
                        .repairOrderDao()
                        .observeRecentActiveOrders(limit = 20)
                        .first()
                        .firstOrNull { (it.id != DemoDataSeeder.DEMO_ORDER_ID) && (it.title == summary) }
                }
            order?.let { return it.id }
            Thread.sleep(250)
        }
        error("Ordine creato dal wizard non trovato")
    }

    private fun prepareScreenshotDirectory(): File {
        val directory = File(requireNotNull(context.getExternalFilesDir(null)), "store-pro")
        directory.deleteRecursively()
        directory.mkdirs()
        return directory
    }
}

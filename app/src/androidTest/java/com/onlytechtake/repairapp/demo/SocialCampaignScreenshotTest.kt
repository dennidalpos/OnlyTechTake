package com.onlytechtake.repairapp.demo

import android.content.Context
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeRight
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.onlytechtake.repairapp.BuildConfig
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
class SocialCampaignScreenshotTest {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    @get:Rule
    val cameraPermissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(android.Manifest.permission.CAMERA)

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun capturesSocialCampaignFlow() {
        runBlocking { DemoDataSeeder.seedDemoProfile(context) }
        val outputDir = prepareScreenshotDirectory()
        val robot = ProAssetRobot(composeRule, context)

        ActivityScenario.launch(MainActivity::class.java).use {
            robot.waitForTag(RepairAppTestTags.AuthUsernameField)

            val username =
                if (BuildConfig.IS_DEMO_VERSION) DemoDataSeeder.DEMO_ADMIN_USERNAME else DemoDataSeeder.DEMO_OPERATOR_USERNAME
            robot.loginDemo(username)
            robot.capture(outputDir, "01-home.png")

            // 1. Wizard: Cliente
            robot.clickTag(RepairAppTestTags.HomeNewOrderButton, scroll = true)
            robot.pauseForDemo()
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
            robot.capture(outputDir, "02-accoglienza.png")

            robot.clickTag(RepairAppTestTags.NewOrderSaveCustomerButton, scroll = true)
            robot.waitForText("Cliente selezionato")
            robot.clickTag(RepairAppTestTags.NewOrderStep1NextButton, scroll = true)

            // 2. Wizard: Categorie
            robot.clickTag(RepairAppTestTags.categoryButton("demo-cat-pc"), scroll = true)
            robot.clickTag(
                RepairAppTestTags.categoryButton(DemoDataSeeder.DEMO_CATEGORY_ID),
                scroll = true,
            )
            robot.clickTag(RepairAppTestTags.categoryButton("demo-cat-gaming"), scroll = true)
            robot.clickTag(RepairAppTestTags.categoryButton("demo-cat-rog-strix"), scroll = true)

            robot.clickTag(RepairAppTestTags.NewOrderStep2NextButton, scroll = true)

            // 3. Wizard: Problema
            robot.typeTag(
                RepairAppTestTags.NewOrderProblemSummaryField,
                "Samsung Galaxy S23 - Schermo rotto",
                scroll = true,
            )
            robot.typeTag(
                RepairAppTestTags.NewOrderProblemDetailsField,
                "Richiesta sostituzione display originale.",
                scroll = true,
            )
            robot.capture(outputDir, "03-step2-diagnosi.png")

            // 4. Wizard: media
            robot.clickTag(RepairAppTestTags.NewOrderStep3NextButton, scroll = true)
            robot.waitForText("Foto e allegati")
            robot.clickTag(RepairAppTestTags.IntakeMediaTakePhotoButton, scroll = true)
            robot.waitForTag(RepairAppTestTags.IntakeMediaCapturePhotoButton, scroll = true)
            robot.clickTag(RepairAppTestTags.IntakeMediaCapturePhotoButton, scroll = true)
            robot.waitForText("PHOTO", scroll = true)
            robot.capture(outputDir, "03b-step3-media.png")

            // 5. Wizard: privacy e firma
            robot.clickTag(RepairAppTestTags.NewOrderStep4NextButton, scroll = true)
            robot.waitForText("Privacy e firma")
            robot.clickText("Accetta informativa privacy", scroll = true)
            robot.clickText("Accetta condizioni di presa in carico", scroll = true)
            robot.drawSignature(RepairAppTestTags.SignatureCanvas)
            robot.waitForTag(RepairAppTestTags.NewOrderStep5NextButton, scroll = true)
            robot.capture(outputDir, "04-step4-firma.png")

            robot.clickTag(RepairAppTestTags.NewOrderStep5NextButton, scroll = true)
            robot.waitForTag(RepairAppTestTags.IntakeSubmitButton, scroll = true)
            robot.clickTag(RepairAppTestTags.IntakeSubmitButton, scroll = true)
            val createdOrderId = waitForCreatedOrderId("Samsung Galaxy S23 - Schermo rotto")

            robot.clickDashboardTab("ORDERS")
            robot.waitForTag(RepairAppTestTags.orderCard(createdOrderId), scroll = true)
            robot.clickTag(RepairAppTestTags.orderCard(createdOrderId), scroll = true)
            robot.waitForTag(RepairAppTestTags.OrderStatusLabel)

            // Workflow di lavorazione
            robot.applySimpleStatus(
                OrderStatusAction.START_PROGRESS,
                nextAction = OrderStatusAction.START_DIAGNOSIS,
            )
            robot.applySimpleStatus(
                OrderStatusAction.START_DIAGNOSIS,
                nextAction = OrderStatusAction.RECORD_RESULT,
            )

            // 5. WHATSAPP & PREVENTIVO
            robot.applyResultStatus("Pasta termica secca.", "89,00")
            robot.waitForTag(RepairAppTestTags.OrderWhatsAppButton, scroll = true)
            robot.clickTag(RepairAppTestTags.OrderWhatsAppButton)
            robot.waitForTag(RepairAppTestTags.OrderWhatsAppPreviewPanel)
            robot.capture(outputDir, "05-whatsapp.png")
            robot.dismissWhatsAppPreviewIfPresent()
            robot.pauseForDemo()

            robot.acceptQuote()
            robot.applySimpleStatus(
                OrderStatusAction.READY_FOR_PICKUP,
                nextAction = OrderStatusAction.CLOSE_ORDER,
            )

            // 6. SUCCESSO (Timeline chiusura)
            robot.closeOrder()
            robot.clickDashboardTab("ORDERS")
            robot.waitForTag(RepairAppTestTags.orderCard(createdOrderId), scroll = true)
            robot.clickTag(RepairAppTestTags.orderCard(createdOrderId), scroll = true)
            robot.waitForText("Chiusa")
            robot.capture(outputDir, "06-successo.png")
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
                        .firstOrNull {
                            (it.id != DemoDataSeeder.DEMO_ORDER_ID) &&
                                (it.title == summary)
                        }
                }
            order?.let { return it.id }
            Thread.sleep(250)
        }
        error("Ordine creato dal wizard non trovato")
    }

    private fun prepareScreenshotDirectory(): File {
        val directory = File(requireNotNull(context.getExternalFilesDir(null)), "social-campaign")
        directory.deleteRecursively()
        directory.mkdirs()
        return directory
    }
}

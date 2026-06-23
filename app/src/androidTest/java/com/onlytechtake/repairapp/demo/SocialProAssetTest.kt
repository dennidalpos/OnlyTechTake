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
class SocialProAssetTest {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    @get:Rule
    val cameraPermissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(android.Manifest.permission.CAMERA)

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun capturesSocialProAssets() {
        runBlocking { DemoDataSeeder.seedDemoProfile(context) }
        val outputDir = prepareScreenshotDirectory()
        val robot = ProAssetRobot(composeRule, context)

        ActivityScenario.launch(MainActivity::class.java).use {
            robot.waitForTag(RepairAppTestTags.AuthUsernameField)
            robot.loginDemo(DemoDataSeeder.DEMO_OPERATOR_USERNAME)

            // 01-stories-dashboard (Highlighting active work)
            robot.waitForTag(RepairAppTestTags.dashboardTab("HOME"))
            robot.capture(outputDir, "01-stories-dashboard.png")

            // 02-reel-intake (Highlighting fast customer intake)
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
            robot.capture(outputDir, "02-reel-intake.png")

            robot.clickTag(RepairAppTestTags.NewOrderSaveCustomerButton, scroll = true)
            robot.clickTag(RepairAppTestTags.NewOrderStep1NextButton, scroll = true)

            // 03-carousel-category (Highlighting versatility)
            robot.clickTag(RepairAppTestTags.categoryButton("demo-cat-pc"), scroll = true)
            robot.pauseForDemo()
            robot.clickTag(
                RepairAppTestTags.categoryButton(DemoDataSeeder.DEMO_CATEGORY_ID),
                scroll = true,
            )
            robot.pauseForDemo()
            robot.clickTag(RepairAppTestTags.categoryButton("demo-cat-gaming"), scroll = true)
            robot.pauseForDemo()
            robot.clickTag(RepairAppTestTags.categoryButton("demo-cat-rog-strix"), scroll = true)
            robot.pauseForDemo()
            robot.capture(outputDir, "03-carousel-category.png")
            robot.clickTag(RepairAppTestTags.NewOrderStep2NextButton, scroll = true)

            robot.typeTag(
                RepairAppTestTags.NewOrderProblemSummaryField,
                "Gaming Laptop - Overheating",
                scroll = true,
            )
            robot.typeTag(
                RepairAppTestTags.NewOrderProblemDetailsField,
                "Spegnimenti improvvisi durante sessioni di gioco.",
                scroll = true,
            )
            robot.clickTag(RepairAppTestTags.NewOrderStep3NextButton, scroll = true)

            robot.waitForText("Foto e allegati")
            robot.clickTag(RepairAppTestTags.IntakeMediaTakePhotoButton, scroll = true)
            robot.waitForTag(RepairAppTestTags.IntakeMediaCapturePhotoButton, scroll = true)
            robot.clickTag(RepairAppTestTags.IntakeMediaCapturePhotoButton, scroll = true)
            robot.waitForText("PHOTO", scroll = true)
            robot.clickTag(RepairAppTestTags.NewOrderStep4NextButton, scroll = true)

            // 04-stories-signature (Highlighting transparency/trust)
            robot.waitForText("Privacy e firma")
            robot.clickText("Accetta informativa privacy", scroll = true)
            robot.clickText("Accetta condizioni di presa in carico", scroll = true)
            robot.drawSignature(RepairAppTestTags.SignatureCanvas)
            robot.capture(outputDir, "04-stories-signature.png")
            robot.clickTag(RepairAppTestTags.NewOrderStep5NextButton, scroll = true)

            // Finalize
            robot.waitForTag(RepairAppTestTags.IntakeSubmitButton, scroll = true)
            robot.clickTag(RepairAppTestTags.IntakeSubmitButton, scroll = true)

            val createdOrderId = waitForCreatedOrderId("Gaming Laptop - Overheating")
            robot.clickDashboardTab("ORDERS")
            robot.waitForTag(RepairAppTestTags.orderCard(createdOrderId), scroll = true)
            robot.clickTag(RepairAppTestTags.orderCard(createdOrderId), scroll = true)

            // 05-carousel-status (Highlighting progress tracking)
            robot.applySimpleStatus(
                OrderStatusAction.START_PROGRESS,
                OrderStatusAction.START_DIAGNOSIS,
            )
            robot.capture(outputDir, "05-carousel-status.png")

            // 06-reel-pdf (Highlighting professionalism)
            robot.applySimpleStatus(
                OrderStatusAction.START_DIAGNOSIS,
                OrderStatusAction.RECORD_RESULT,
            )
            robot.applyResultStatus("Problema risolto con ripristino OS e pulizia.", "89,00")
            robot.waitForText("Documenti cliente", scroll = true)
            robot.capture(outputDir, "06-reel-pdf.png")
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
        val directory = File(requireNotNull(context.getExternalFilesDir(null)), "social-pro")
        directory.deleteRecursively()
        directory.mkdirs()
        return directory
    }
}

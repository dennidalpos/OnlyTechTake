package com.onlytechtake.repairapp.demo

import android.content.Context
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.onlytechtake.repairapp.MainActivity
import com.onlytechtake.repairapp.R
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
    val localeRule = LocaleTestRule("en", "US")

    @get:Rule
    val composeRule = createEmptyComposeRule()

    @get:Rule
    val cameraPermissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.RECORD_AUDIO,
        )

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun capturesStoreProScreenshots() {
        runBlocking { DemoDataSeeder.seedDemoProfile(context, localeRule.locale) }
        TestUtils.refreshAppContainer()
        val outputDir = prepareScreenshotDirectory()
        val robot = ProAssetRobot(composeRule, context)

        ActivityScenario.launch(MainActivity::class.java).use {
            Thread.sleep(5000)
            robot.capture(outputDir, "00-debug-launch.png")
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
            robot.waitForTag(RepairAppTestTags.NewOrderStep1NextButton)
            robot.clickTag(RepairAppTestTags.NewOrderStep1NextButton, scroll = true)

            // Step 2 (Category) - Skip capture
            robot.clickTag(RepairAppTestTags.categoryButton("demo-cat-pc"), scroll = true)
            robot.clickTag(
                RepairAppTestTags.categoryButton(DemoDataSeeder.DEMO_CATEGORY_ID),
                scroll = true,
            )
            robot.clickTag(RepairAppTestTags.categoryButton("demo-cat-gaming"), scroll = true)
            robot.clickTag(RepairAppTestTags.categoryButton("demo-cat-rog-strix"), scroll = true)
            robot.clickTag(RepairAppTestTags.NewOrderStep2NextButton, scroll = true)

            // Step 3 (Problem) - Skip capture
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
            robot.clickTag(RepairAppTestTags.NewOrderStep3NextButton, scroll = true)

            // Step 4: Photo - Skip capture
            robot.waitForText(R.string.intake_step_media)
            robot.clickTag(RepairAppTestTags.IntakeMediaTakePhotoButton, scroll = true)
            robot.waitForTag(RepairAppTestTags.IntakeMediaCapturePhotoButton, scroll = true)
            robot.clickTag(RepairAppTestTags.IntakeMediaCapturePhotoButton, scroll = true)
            robot.waitForText("PHOTO", scroll = true)
            robot.clickTag(RepairAppTestTags.NewOrderStep4NextButton, scroll = true)

            // 03-foto-firma-pro (Step 5 Signature)
            robot.waitForText(R.string.intake_step_privacy)
            robot.clickText(R.string.signature_accept_privacy, scroll = true)
            robot.clickText(R.string.signature_accept_terms, scroll = true)
            robot.drawSignature(RepairAppTestTags.SignatureCanvas)
            robot.capture(outputDir, "03-foto-firma-pro.png")
            robot.clickTag(RepairAppTestTags.NewOrderStep5NextButton, scroll = true)

            // Step 6: Summary and Submit
            robot.waitForTag(RepairAppTestTags.IntakeSubmitButton, scroll = true)
            robot.clickTag(RepairAppTestTags.IntakeSubmitButton, scroll = true)

            val summary = "Samsung Galaxy S23 - Schermo rotto"
            val createdOrderId = waitForCreatedOrderId()
            robot.clickDashboardTab("ORDERS")
            robot.waitForText(summary, scroll = true)
            robot.clickText(summary, scroll = true)

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

            // 05-repairing-step-pro
            robot.acceptQuote()
            robot.capture(outputDir, "05-repairing-step-pro.png")

            // 06-whatsapp-pdf-pro
            robot.waitForTag(RepairAppTestTags.OrderWhatsAppButton, scroll = true)
            robot.clickTag(RepairAppTestTags.OrderWhatsAppButton)
            robot.waitForTag(RepairAppTestTags.OrderWhatsAppPreviewPanel)
            // Scroll inside dialog or interaction to ensure visual change
            robot.typeTag(RepairAppTestTags.OrderWhatsAppMessageField, " ", scroll = false)
            Thread.sleep(1000)
            robot.capture(outputDir, "06-whatsapp-pdf-pro.png")
            robot.dismissWhatsAppPreviewIfPresent()

            // 07-timeline-chiusura-pro
            robot.applySimpleStatus(
                OrderStatusAction.READY_FOR_PICKUP,
                OrderStatusAction.CLOSE_ORDER,
            )
            robot.closeOrder("Repair successfully completed.")
            robot.clickDashboardTab("ORDERS")

            // Enable "Closed" filter to see the order we just closed
            robot.clickTag(RepairAppTestTags.OrdersFilterButton)
            robot.clickText(R.string.orders_filter_closed_discarded)
            robot.clickText(R.string.action_confirm)

            robot.waitForText(summary, scroll = true)
            robot.clickText(summary, scroll = true)
            robot.waitForText(R.string.status_closed)
            robot.capture(outputDir, "07-timeline-chiusura-pro.png")

            // 08-clienti-pro
            robot.clickDashboardTab("CUSTOMERS")
            robot.waitForTag(RepairAppTestTags.CustomerSearchField)
            robot.capture(outputDir, "08-clienti-pro.png")

            // 09-dettaglio-cliente-pro
            robot.waitForText("John Doe", scroll = true)
            robot.clickText("John Doe")
            robot.waitForText(R.string.customer_open_orders, scroll = true)
            robot.capture(outputDir, "09-dettaglio-cliente-pro.png")
        }
    }

    private fun waitForCreatedOrderId(): String {
        val summary = "Samsung Galaxy S23 - Schermo rotto"
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

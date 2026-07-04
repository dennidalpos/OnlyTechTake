package com.onlytechtake.repairapp.demo

import android.content.Context
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
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
import com.onlytechtake.repairapp.test.R as TestR

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
    private val testContext: Context
        get() = InstrumentationRegistry.getInstrumentation().context

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
            robot.typeTag(
                RepairAppTestTags.NewOrderFirstNameField,
                testContext.getString(TestR.string.demo_wizard_customer_first_name),
                scroll = true,
                fast = true,
            )
            robot.typeTag(
                RepairAppTestTags.NewOrderLastNameField,
                testContext.getString(TestR.string.demo_wizard_customer_last_name),
                scroll = true,
                fast = true,
            )
            robot.typeTag(
                RepairAppTestTags.NewOrderPhoneField,
                testContext.getString(TestR.string.demo_wizard_customer_phone),
                scroll = true,
                fast = true,
            )
            robot.typeTag(
                RepairAppTestTags.NewOrderEmailField,
                testContext.getString(TestR.string.demo_wizard_customer_email),
                scroll = true,
                fast = true,
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
                testContext.getString(TestR.string.demo_order_summary_s23),
                scroll = true,
                fast = true,
            )
            robot.typeTag(
                RepairAppTestTags.NewOrderProblemDetailsField,
                testContext.getString(TestR.string.demo_order_desc_s23),
                scroll = true,
                fast = true,
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

            val summary = testContext.getString(TestR.string.demo_order_summary_s23)
            val createdOrderId = waitForCreatedOrderId(summary)
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
            robot.applyResultStatus(
                testContext.getString(TestR.string.demo_diagnosis_result_s23),
                testContext.getString(TestR.string.demo_estimate_s23),
            )
            robot.capture(outputDir, "04-diagnosi-preventivo-pro.png")

            // 05-repairing-step-pro
            robot.acceptQuote()
            robot.capture(outputDir, "05-repairing-step-pro.png")

            // 06-whatsapp-pdf-pro
            robot.waitForTag(RepairAppTestTags.OrderWhatsAppButton, scroll = true)
            robot.clickTag(RepairAppTestTags.OrderWhatsAppButton)
            robot.waitForTag(RepairAppTestTags.OrderWhatsAppPreviewPanel)
            // Scroll inside dialog or interaction to ensure visual change
            robot.typeTag(RepairAppTestTags.OrderWhatsAppMessageField, " ", scroll = false, fast = true)
            Thread.sleep(1000)
            robot.capture(outputDir, "06-whatsapp-pdf-pro.png")
            robot.dismissWhatsAppPreviewIfPresent()

            // 07-timeline-chiusura-pro
            robot.applySimpleStatus(
                OrderStatusAction.READY_FOR_PICKUP,
                OrderStatusAction.CLOSE_ORDER,
            )
            robot.closeOrder(testContext.getString(TestR.string.demo_closure_note_completed))
            robot.clickDashboardTab("ORDERS")

            // Enable "Closed" filter to see the order we just closed
            robot.clickTag(RepairAppTestTags.OrdersFilterButton)
            robot.clickText(R.string.queue_closed)
            robot.clickText(R.string.action_confirm)

            robot.waitForText(summary, scroll = true)
            robot.clickText(summary, scroll = true)
            robot.waitForText(R.string.status_closed)
            robot.expandTimeline()
            robot.capture(outputDir, "07-timeline-chiusura-pro.png")

            // 08-clienti-pro
            robot.clickDashboardTab("CUSTOMERS")
            robot.waitForTag(RepairAppTestTags.CustomerSearchField)
            robot.capture(outputDir, "08-clienti-pro.png")

            // 09-dettaglio-cliente-pro
            val demoCustomerName = robot.getTestString(TestR.string.demo_customer_name_john)
            robot.waitForText(demoCustomerName, scroll = true)
            robot.clickText(demoCustomerName)
            robot.waitForText(R.string.customer_open_orders, scroll = true)
            robot.capture(outputDir, "09-dettaglio-cliente-pro.png")
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
        error("Ordine creato dal wizard non trovato (summary: $summary)")
    }

    private fun prepareScreenshotDirectory(): File {
        val directory = File(requireNotNull(context.getExternalFilesDir(null)), "store-pro")
        directory.deleteRecursively()
        directory.mkdirs()
        return directory
    }
}

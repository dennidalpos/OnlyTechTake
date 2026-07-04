package com.onlytechtake.repairapp.demo

import android.content.Context
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.onlytechtake.repairapp.BuildConfig
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
class SocialCampaignScreenshotTest {
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
    fun capturesSocialCampaignFlow() {
        runBlocking { DemoDataSeeder.seedDemoProfile(context) }
        TestUtils.refreshAppContainer()
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
            robot.capture(outputDir, "02-accoglienza.png")

            robot.clickTag(RepairAppTestTags.NewOrderSaveCustomerButton, scroll = true)
            robot.waitForTag(RepairAppTestTags.NewOrderStep1NextButton)
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
                robot.getTestString(TestR.string.demo_order_summary_s23),
                scroll = true,
                fast = true,
            )
            robot.typeTag(
                RepairAppTestTags.NewOrderProblemDetailsField,
                robot.getTestString(TestR.string.demo_order_desc_s23),
                scroll = true,
                fast = true,
            )
            robot.capture(outputDir, "03-step2-diagnosi.png")

            // 4. Wizard: media
            robot.clickTag(RepairAppTestTags.NewOrderStep3NextButton, scroll = true)
            robot.waitForText(R.string.intake_step_media)
            robot.clickTag(RepairAppTestTags.IntakeMediaTakePhotoButton, scroll = true)
            robot.waitForTag(RepairAppTestTags.IntakeMediaCapturePhotoButton, scroll = true)
            robot.clickTag(RepairAppTestTags.IntakeMediaCapturePhotoButton, scroll = true)
            robot.waitForText("PHOTO", scroll = true)
            robot.capture(outputDir, "03b-step3-media.png")

            // 5. Wizard: privacy e firma
            robot.clickTag(RepairAppTestTags.NewOrderStep4NextButton, scroll = true)
            robot.waitForText(R.string.intake_step_privacy)
            robot.clickText(R.string.signature_accept_privacy, scroll = true)
            robot.clickText(R.string.signature_accept_terms, scroll = true)
            robot.drawSignature(RepairAppTestTags.SignatureCanvas)
            robot.waitForTag(RepairAppTestTags.NewOrderStep5NextButton, scroll = true)
            robot.capture(outputDir, "04-step4-firma.png")

            robot.clickTag(RepairAppTestTags.NewOrderStep5NextButton, scroll = true)
            robot.waitForTag(RepairAppTestTags.IntakeSubmitButton, scroll = true)
            robot.clickTag(RepairAppTestTags.IntakeSubmitButton, scroll = true)
            val summary = robot.getTestString(TestR.string.demo_order_summary_s23)
            val createdOrderId = waitForCreatedOrderId(summary)

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
            robot.applyResultStatus(
                robot.getTestString(TestR.string.demo_diagnosis_result_overheating),
                robot.getTestString(TestR.string.demo_estimate_overheating),
            )
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

            // Enable "Closed" filter to see the order we just closed
            robot.clickTag(RepairAppTestTags.OrdersFilterButton)
            robot.clickText(R.string.queue_closed)
            robot.clickText(R.string.action_confirm)

            robot.waitForTag(RepairAppTestTags.orderCard(createdOrderId), scroll = true)
            robot.clickTag(RepairAppTestTags.orderCard(createdOrderId), scroll = true)
            robot.waitForText(R.string.status_closed)
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
        error("Ordine creato dal wizard non trovato (summary: $summary)")
    }

    private fun prepareScreenshotDirectory(): File {
        val directory = File(requireNotNull(context.getExternalFilesDir(null)), "social-campaign")
        directory.deleteRecursively()
        directory.mkdirs()
        return directory
    }
}

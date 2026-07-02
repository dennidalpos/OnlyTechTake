package com.onlytechtake.repairapp.demo

import android.content.Context
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
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

@RunWith(AndroidJUnit4::class)
class OperatorIntakeDemoScreenshotTest {
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
    fun capturesOperatorIntakeDemo() {
        runBlocking { DemoDataSeeder.seedDemoProfile(context, localeRule.locale) }
        TestUtils.refreshAppContainer()
        val outputDir = prepareScreenshotDirectory()
        val robot = ProAssetRobot(composeRule, context)

        ActivityScenario.launch(MainActivity::class.java).use {
            robot.waitForTag(RepairAppTestTags.AuthUsernameField)
            robot.capture(outputDir, "01-login.png")

            val username =
                if (BuildConfig.IS_DEMO_VERSION) DemoDataSeeder.DEMO_ADMIN_USERNAME else DemoDataSeeder.DEMO_OPERATOR_USERNAME
            robot.loginDemo(username)
            robot.capture(outputDir, "02-home.png")

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
            robot.capture(outputDir, "03-step1-customer.png")

            robot.clickTag(RepairAppTestTags.NewOrderSaveCustomerButton, scroll = true)
            robot.waitForTag(RepairAppTestTags.NewOrderStep1NextButton)
            robot.clickTag(RepairAppTestTags.NewOrderStep1NextButton, scroll = true)

            robot.clickTag(RepairAppTestTags.categoryButton("demo-cat-pc"), scroll = true)
            robot.clickTag(
                RepairAppTestTags.categoryButton(DemoDataSeeder.DEMO_CATEGORY_ID),
                scroll = true,
            )
            robot.clickTag(RepairAppTestTags.categoryButton("demo-cat-gaming"), scroll = true)
            robot.clickTag(RepairAppTestTags.categoryButton("demo-cat-rog-strix"), scroll = true)

            robot.clickTag(RepairAppTestTags.NewOrderStep2NextButton, scroll = true)
            robot.typeTag(
                RepairAppTestTags.NewOrderProblemSummaryField,
                "Samsung Galaxy S23 - Schermo rotto",
                scroll = true,
            )
            robot.typeTag(
                RepairAppTestTags.NewOrderProblemDetailsField,
                "Il cliente segnala rottura display dopo caduta.",
                scroll = true,
            )
            robot.capture(outputDir, "04-step2-problem.png")

            robot.clickTag(RepairAppTestTags.NewOrderStep3NextButton, scroll = true)
            robot.waitForText(R.string.intake_step_media)
            robot.clickTag(RepairAppTestTags.IntakeMediaTakePhotoButton, scroll = true)
            robot.waitForTag(RepairAppTestTags.IntakeMediaCapturePhotoButton, scroll = true)
            robot.clickTag(RepairAppTestTags.IntakeMediaCapturePhotoButton, scroll = true)
            robot.waitForText("PHOTO", scroll = true)
            robot.capture(outputDir, "05-step3-media.png")
            robot.clickTag(RepairAppTestTags.NewOrderStep4NextButton, scroll = true)

            robot.waitForText(R.string.intake_step_privacy)
            robot.clickText(R.string.signature_accept_privacy, scroll = true)
            robot.clickText(R.string.signature_accept_terms, scroll = true)
            robot.drawSignature(RepairAppTestTags.SignatureCanvas)
            robot.waitForTag(RepairAppTestTags.NewOrderStep5NextButton, scroll = true)
            robot.capture(outputDir, "06-step4-signature.png")

            robot.clickTag(RepairAppTestTags.NewOrderStep5NextButton, scroll = true)
            robot.waitForTag(RepairAppTestTags.IntakeSubmitButton, scroll = true)
            robot.capture(outputDir, "07-step5-summary.png")
            robot.clickTag(RepairAppTestTags.IntakeSubmitButton, scroll = true)

            val summary = "Samsung Galaxy S23 - Schermo rotto"
            val createdOrderId = waitForCreatedOrderId()
            robot.clickDashboardTab("ORDERS")
            robot.waitForText(summary, scroll = true)
            robot.clickText(summary, scroll = true)
            robot.waitForTag(RepairAppTestTags.OrderStatusLabel)
            robot.capture(outputDir, "09-order-detail-intake.png")

            // Technical Video Documentation (New Feature)
            if (robot.isTagPresent(RepairAppTestTags.IntakeMediaTakeVideoButton)) {
                robot.recordTechnicalVideo()
                robot.capture(outputDir, "09b-video-recorded.png")
            }

            robot.waitForTag(RepairAppTestTags.OrderWhatsAppButton, scroll = true)
            robot.clickTag(RepairAppTestTags.OrderWhatsAppButton)
            robot.waitForTag(RepairAppTestTags.OrderWhatsAppPreviewPanel)
            robot.capture(outputDir, "10-whatsapp-preview.png")
            robot.dismissWhatsAppPreviewIfPresent()
            robot.pauseForDemo()

            robot.applySimpleStatus(
                OrderStatusAction.START_PROGRESS,
                nextAction = OrderStatusAction.START_DIAGNOSIS,
            )
            robot.capture(outputDir, "12-in-progress.png")

            robot.applySimpleStatus(
                OrderStatusAction.START_DIAGNOSIS,
                nextAction = OrderStatusAction.RECORD_RESULT,
            )
            robot.applyResultStatus("Connettore di alimentazione danneggiato.", "149,00")
            robot.capture(outputDir, "13-diagnosis-result.png")

            robot.waitForText(R.string.order_customer_documents, scroll = true)
            robot.capture(outputDir, "14-pdf-documents.png")

            robot.acceptQuote()
            robot.applySimpleStatus(
                OrderStatusAction.READY_FOR_PICKUP,
                nextAction = OrderStatusAction.CLOSE_ORDER,
            )
            robot.capture(outputDir, "15-ready-for-pickup.png")

            robot.closeOrder()
            robot.clickDashboardTab("ORDERS")

            // Enable "Closed" filter to see the order we just closed
            robot.clickTag(RepairAppTestTags.OrdersFilterButton)
            robot.clickText(R.string.queue_closed)
            robot.clickText(R.string.action_confirm)

            robot.waitForText(summary, scroll = true)
            robot.clickText(summary, scroll = true)
            robot.waitForText(R.string.status_closed)
            robot.capture(outputDir, "16-closed-timeline.png")
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
        val directory = File(requireNotNull(context.getExternalFilesDir(null)), "operator-intake")
        directory.deleteRecursively()
        directory.mkdirs()
        return directory
    }
}

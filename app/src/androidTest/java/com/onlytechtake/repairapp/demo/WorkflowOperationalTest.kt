package com.onlytechtake.repairapp.demo

import android.content.Context
import android.content.Intent
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.onlytechtake.repairapp.MainActivity
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.core.design.RepairAppTestTags
import com.onlytechtake.repairapp.data.local.db.RepairAppDatabase
import com.onlytechtake.repairapp.data.preferences.AuthPreferences
import com.onlytechtake.repairapp.data.preferences.OwnerPreferences
import com.onlytechtake.repairapp.domain.workspace.OrderStatusAction
import com.onlytechtake.repairapp.domain.workspace.OwnerInfo
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import com.onlytechtake.repairapp.test.R as TestR

@RunWith(AndroidJUnit4::class)
class WorkflowOperationalTest {
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

    private val context: Context
        get() =
            androidx.test.platform.app.InstrumentationRegistry
                .getInstrumentation()
                .targetContext

    private fun launchMainActivity(disableAutoSeed: Boolean = false): ActivityScenario<MainActivity> {
        val intent =
            Intent(context, MainActivity::class.java).apply {
                putExtra("disable_auto_seed", disableAutoSeed)
                if (localeRule.locale.language == "en") {
                    putExtra("forceEnglish", true)
                }
            }
        return ActivityScenario.launch(intent)
    }

    @Test
    fun flow0_Setup() {
        System.setProperty("disable_auto_seed", "true")
        runBlocking {
            RepairAppDatabase.closeInstance()
            context.deleteDatabase(RepairAppDatabase.DATABASE_NAME)
            AuthPreferences(context).clear()
            OwnerPreferences(context).updateOwnerInfo(OwnerInfo())

            TestUtils.refreshAppContainer()

            val db = RepairAppDatabase.getInstance(context)
            val hasher =
                com.onlytechtake.repairapp.core.security
                    .PasswordHasher()
            val hash = hasher.hash("TempPass123!".toCharArray())
            db.userDao().upsert(
                com.onlytechtake.repairapp.data.local.entities.UserEntity(
                    id = "temp-admin",
                    displayName = "Admin",
                    username = "admin",
                    passwordHash = hash.hash,
                    passwordSalt = hash.salt,
                    role = com.onlytechtake.repairapp.data.local.entities.UserRole.ADMIN,
                    forcePasswordChange = true,
                    active = true,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                ),
            )
            TestUtils.refreshAppContainer()
        }
        val outputDir = prepareDirectory("workflow-setup")
        val robot = OperationalRobot(composeRule, context)

        launchMainActivity(disableAutoSeed = true).use {
            robot.typeTag(RepairAppTestTags.AuthUsernameField, "admin", fast = true)
            robot.typeTag(RepairAppTestTags.AuthPasswordField, "TempPass123!", fast = true)
            robot.clickTag(RepairAppTestTags.AuthSubmitButton)

            robot.waitForText(R.string.auth_security_title, scroll = false)
            robot.capture(outputDir, "01-password-change.png")
            robot.typeTag(RepairAppTestTags.AuthPasswordField, "NewAdminPass123!", fast = true)
            robot.typeTag(
                RepairAppTestTags.AuthConfirmPasswordField,
                "NewAdminPass123!",
                fast = true,
            )
            robot.clickTag(RepairAppTestTags.AuthSubmitButton)

            robot.waitForTag(RepairAppTestTags.OwnerFieldName)
            robot.typeTag(
                RepairAppTestTags.OwnerFieldName,
                robot.getTestString(TestR.string.demo_shop_name_default),
                fast = true,
            )
            robot.typeTag(
                RepairAppTestTags.OwnerFieldVat,
                robot.getTestString(TestR.string.demo_shop_vat_default),
                fast = true,
            )
            robot.typeTag(
                RepairAppTestTags.OwnerFieldAddress,
                robot.getTestString(TestR.string.demo_shop_address_default),
                fast = true,
            )
            robot.typeTag(
                RepairAppTestTags.OwnerFieldEmail,
                robot.getTestString(TestR.string.demo_shop_email_default),
                fast = true,
            )
            robot.typeTag(
                RepairAppTestTags.OwnerFieldPhone,
                robot.getTestString(TestR.string.demo_shop_phone_default),
                fast = true,
            )
            robot.capture(outputDir, "02-shop-data-compilation.png")
            robot.clickText(R.string.action_confirm)
        }
    }

    @Test
    fun flow1_Intake() {
        runBlocking { DemoDataSeeder.seedDemoProfile(context, localeRule.locale) }
        TestUtils.refreshAppContainer()
        val outputDir = prepareDirectory("workflow-intake")
        val robot = OperationalRobot(composeRule, context)

        launchMainActivity().use {
            robot.loginDemo(DemoDataSeeder.DEMO_OPERATOR_USERNAME)
            robot.intakeFlow(outputDir)
        }
    }

    @Test
    fun flow2_Management() {
        runBlocking { DemoDataSeeder.seedDemoProfile(context, localeRule.locale) }
        TestUtils.refreshAppContainer()
        val outputDir = prepareDirectory("workflow-management")
        val robot = OperationalRobot(composeRule, context)

        launchMainActivity().use {
            robot.loginDemo(DemoDataSeeder.DEMO_OPERATOR_USERNAME)
            robot.managementFlow(outputDir)
        }
    }

    @Test
    fun flow3_AdminOperators() {
        runBlocking { DemoDataSeeder.seedDemoProfile(context, localeRule.locale) }
        TestUtils.refreshAppContainer()
        val outputDir = prepareDirectory("workflow-admin-operators")
        val robot = OperationalRobot(composeRule, context)

        launchMainActivity().use {
            robot.loginDemo(DemoDataSeeder.DEMO_ADMIN_USERNAME)
            robot.adminOperatorsFlow(outputDir)
        }
    }

    @Test
    fun flow4_AdminTemplates() {
        runBlocking { DemoDataSeeder.seedDemoProfile(context, localeRule.locale) }
        TestUtils.refreshAppContainer()
        val outputDir = prepareDirectory("workflow-admin-templates")
        val robot = OperationalRobot(composeRule, context)

        launchMainActivity().use {
            robot.loginDemo(DemoDataSeeder.DEMO_ADMIN_USERNAME)
            robot.adminTemplatesFlow(outputDir)
        }
    }

    @Test
    fun flow5_Backup() {
        runBlocking { DemoDataSeeder.seedDemoProfile(context, localeRule.locale) }
        TestUtils.refreshAppContainer()
        val outputDir = prepareDirectory("workflow-backup")
        val robot = OperationalRobot(composeRule, context)

        launchMainActivity().use {
            robot.loginDemo(DemoDataSeeder.DEMO_ADMIN_USERNAME)
            robot.backupFlow(outputDir)
        }
    }

    @Test
    fun flow6_SocialPro() {
        runBlocking { DemoDataSeeder.seedDemoProfile(context, localeRule.locale) }
        TestUtils.refreshAppContainer()
        val outputDir = prepareDirectory("social-pro")
        val robot = ProAssetRobot(composeRule, context)

        launchMainActivity().use {
            robot.loginDemo(DemoDataSeeder.DEMO_OPERATOR_USERNAME)

            robot.waitForTag(RepairAppTestTags.dashboardTab("HOME"))
            robot.capture(outputDir, "01-stories-dashboard.png")

            robot.clickTag(RepairAppTestTags.HomeNewOrderButton, scroll = true)
            robot.waitForTag("quick_actions_screen")
            robot.clickTag(RepairAppTestTags.NewOrderInsertCustomerButton, scroll = true)
            robot.typeTag(
                RepairAppTestTags.NewOrderFirstNameField,
                robot.getTestString(TestR.string.demo_wizard_customer_first_name),
                scroll = true,
                fast = true,
            )
            robot.typeTag(
                RepairAppTestTags.NewOrderLastNameField,
                robot.getTestString(TestR.string.demo_wizard_customer_last_name),
                scroll = true,
                fast = true,
            )
            robot.typeTag(
                RepairAppTestTags.NewOrderPhoneField,
                robot.getTestString(TestR.string.demo_wizard_customer_phone),
                scroll = true,
                fast = true,
            )
            robot.typeTag(
                RepairAppTestTags.NewOrderEmailField,
                robot.getTestString(TestR.string.demo_wizard_customer_email),
                scroll = true,
                fast = true,
            )
            robot.capture(outputDir, "02-reel-intake.png")

            robot.clickTag(RepairAppTestTags.NewOrderSaveCustomerButton, scroll = true)
            robot.clickTag(RepairAppTestTags.NewOrderStep1NextButton, scroll = true)

            robot.clickTag(RepairAppTestTags.categoryButton("demo-cat-pc"), scroll = true)
            robot.clickTag(
                RepairAppTestTags.categoryButton(DemoDataSeeder.DEMO_CATEGORY_ID),
                scroll = true,
            )
            robot.clickTag(RepairAppTestTags.categoryButton("demo-cat-gaming"), scroll = true)
            robot.clickTag(RepairAppTestTags.categoryButton("demo-cat-rog-strix"), scroll = true)
            robot.capture(outputDir, "03-carousel-category.png")
            robot.clickTag(RepairAppTestTags.NewOrderStep2NextButton, scroll = true)

            robot.typeTag(
                RepairAppTestTags.NewOrderProblemSummaryField,
                robot.getTestString(TestR.string.demo_order_summary_overheating),
                scroll = true,
                fast = true,
            )
            robot.typeTag(
                RepairAppTestTags.NewOrderProblemDetailsField,
                robot.getTestString(TestR.string.demo_order_desc_overheating),
                scroll = true,
                fast = true,
            )
            robot.clickTag(RepairAppTestTags.NewOrderStep3NextButton, scroll = true)

            robot.waitForText(R.string.intake_step_media)
            robot.clickTag(RepairAppTestTags.IntakeMediaTakePhotoButton, scroll = true)
            robot.waitForTag(RepairAppTestTags.IntakeMediaCapturePhotoButton, scroll = true)
            robot.clickTag(RepairAppTestTags.IntakeMediaCapturePhotoButton, scroll = true)
            robot.waitForText("PHOTO", scroll = true)
            robot.clickTag(RepairAppTestTags.NewOrderStep4NextButton, scroll = true)

            robot.waitForText(R.string.intake_step_privacy)
            robot.clickText(R.string.signature_accept_privacy, scroll = true)
            robot.clickText(R.string.signature_accept_terms, scroll = true)
            robot.drawSignature(RepairAppTestTags.SignatureCanvas)
            robot.capture(outputDir, "04-stories-signature.png")
            robot.clickTag(RepairAppTestTags.NewOrderStep5NextButton, scroll = true)

            robot.waitForTag(RepairAppTestTags.IntakeSubmitButton, scroll = true)
            robot.clickTag(RepairAppTestTags.IntakeSubmitButton, scroll = true)

            val summary = robot.getTestString(TestR.string.demo_order_summary_overheating)
            waitForCreatedOrderId(summary)
            robot.clickDashboardTab("ORDERS")
            robot.waitForText(summary.substringBefore("-").trim(), scroll = true)
            robot.clickText(summary.substringBefore("-").trim(), scroll = true)

            robot.applySimpleStatus(
                OrderStatusAction.START_PROGRESS,
                OrderStatusAction.START_DIAGNOSIS,
            )
            robot.capture(outputDir, "05-carousel-status.png")

            robot.applySimpleStatus(
                OrderStatusAction.START_DIAGNOSIS,
                OrderStatusAction.RECORD_RESULT,
            )
            robot.applyResultStatus(
                robot.getTestString(TestR.string.demo_diagnosis_result_overheating),
                robot.getTestString(TestR.string.demo_estimate_overheating),
            )
            robot.waitForText(R.string.order_customer_documents, scroll = true)
            robot.capture(outputDir, "06-reel-pdf.png")
        }
    }

    @Test
    fun flow7_SocialCampaign() {
        runBlocking { DemoDataSeeder.seedDemoProfile(context, localeRule.locale) }
        TestUtils.refreshAppContainer()
        val outputDir = prepareDirectory("social-campaign")
        val robot = ProAssetRobot(composeRule, context)

        launchMainActivity().use {
            robot.waitForTag(RepairAppTestTags.AuthUsernameField)
            robot.loginDemo(DemoDataSeeder.DEMO_OPERATOR_USERNAME)
            robot.capture(outputDir, "01-home.png")

            robot.clickTag(RepairAppTestTags.HomeNewOrderButton, scroll = true)
            robot.waitForTag("quick_actions_screen")

            robot.clickTag(RepairAppTestTags.NewOrderInsertCustomerButton, scroll = true)
            robot.typeTag(
                RepairAppTestTags.NewOrderFirstNameField,
                robot.getTestString(TestR.string.demo_wizard_customer_first_name),
                scroll = true,
                fast = true,
            )
            robot.typeTag(
                RepairAppTestTags.NewOrderLastNameField,
                robot.getTestString(TestR.string.demo_wizard_customer_last_name),
                scroll = true,
                fast = true,
            )
            robot.typeTag(
                RepairAppTestTags.NewOrderPhoneField,
                robot.getTestString(TestR.string.demo_wizard_customer_phone),
                scroll = true,
                fast = true,
            )
            robot.typeTag(
                RepairAppTestTags.NewOrderEmailField,
                robot.getTestString(TestR.string.demo_wizard_customer_email),
                scroll = true,
                fast = true,
            )
            robot.capture(outputDir, "02-accoglienza.png")

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

            robot.clickTag(RepairAppTestTags.NewOrderStep3NextButton, scroll = true)
            robot.waitForText(R.string.intake_step_media)
            robot.clickTag(RepairAppTestTags.IntakeMediaTakePhotoButton, scroll = true)
            robot.waitForTag(RepairAppTestTags.IntakeMediaCapturePhotoButton, scroll = true)
            robot.clickTag(RepairAppTestTags.IntakeMediaCapturePhotoButton, scroll = true)
            robot.waitForText("PHOTO", scroll = true)
            robot.capture(outputDir, "03b-step3-media.png")

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
            composeRule.waitForIdle()
            Thread.sleep(2000)

            val summary = robot.getTestString(TestR.string.demo_order_summary_s23)
            waitForCreatedOrderId(summary)

            robot.clickDashboardTab("ORDERS")
            robot.waitForText(summary.substringBefore("-").trim(), scroll = true)
            robot.clickText(summary.substringBefore("-").trim(), scroll = true)

            robot.applySimpleStatus(
                OrderStatusAction.START_PROGRESS,
                nextAction = OrderStatusAction.START_DIAGNOSIS,
            )
            robot.applySimpleStatus(
                OrderStatusAction.START_DIAGNOSIS,
                nextAction = OrderStatusAction.RECORD_RESULT,
            )

            robot.applyResultStatus(
                robot.getTestString(TestR.string.demo_diagnosis_result_overheating),
                robot.getTestString(TestR.string.demo_estimate_overheating),
            )
            robot.waitForTag(RepairAppTestTags.OrderWhatsAppButton, scroll = true)
            robot.clickTag(RepairAppTestTags.OrderWhatsAppButton)
            robot.waitForTag(RepairAppTestTags.OrderWhatsAppPreviewPanel)
            robot.capture(outputDir, "05-whatsapp.png")
            robot.dismissWhatsAppPreviewIfPresent()

            robot.acceptQuote()
            robot.applySimpleStatus(
                OrderStatusAction.READY_FOR_PICKUP,
                nextAction = OrderStatusAction.CLOSE_ORDER,
            )

            robot.closeOrder()
            robot.clickDashboardTab("ORDERS")

            robot.clickTag(RepairAppTestTags.OrdersFilterButton)
            robot.clickText(R.string.queue_closed)
            robot.clickText(R.string.action_confirm)

            robot.waitForText(summary.substringBefore("-").trim(), scroll = true)
            robot.clickText(summary.substringBefore("-").trim(), scroll = true)
            robot.waitForText(R.string.status_closed)
            robot.capture(outputDir, "06-successo.png")
        }
    }

    @Test
    fun flow8_SocialStories() {
        runBlocking { DemoDataSeeder.seedDemoProfile(context, localeRule.locale) }
        TestUtils.refreshAppContainer()
        val outputDir = prepareDirectory("social-stories")
        val robot = ProAssetRobot(composeRule, context)

        launchMainActivity().use {
            robot.loginDemo(DemoDataSeeder.DEMO_OPERATOR_USERNAME)
            robot.capture(outputDir, "01-home-stories.png")

            robot.clickDashboardTab("NEW_ORDER")
            robot.waitForTag(RepairAppTestTags.NewOrderCustomerSearchField)
            robot.capture(outputDir, "02-customer-selection.png")

            val customerName = robot.getTestString(TestR.string.demo_customer_name_julia)

            robot.typeTag(RepairAppTestTags.NewOrderCustomerSearchField, customerName)
            robot.clickText(R.string.customer_select)
            robot.clickText(R.string.customer_select)
            robot.clickTag(RepairAppTestTags.NewOrderStep1NextButton, scroll = true)

            robot.waitForTag(RepairAppTestTags.categoryButton("demo-cat-pc"))
            robot.clickTag(RepairAppTestTags.categoryButton("demo-cat-pc"), scroll = true)
            robot.waitForTag(RepairAppTestTags.categoryButton("demo-cat-handheld"))
            robot.clickTag(RepairAppTestTags.categoryButton("demo-cat-handheld"), scroll = true)
            robot.capture(outputDir, "04-category-selection.png")

            robot.clickTag(RepairAppTestTags.NewOrderStep2NextButton, scroll = true)

            robot.typeTag(
                RepairAppTestTags.NewOrderProblemSummaryField,
                robot.getTestString(TestR.string.demo_order_summary_not_turning_on),
                scroll = true,
                fast = true,
            )
            robot.typeTag(
                RepairAppTestTags.NewOrderProblemDetailsField,
                robot.getTestString(TestR.string.demo_order_desc_not_turning_on),
                scroll = true,
                fast = true,
            )
            robot.capture(outputDir, "05-problem-details.png")

            robot.clickTag(RepairAppTestTags.NewOrderStep3NextButton, scroll = true)
            robot.waitForText(R.string.intake_step_media)
            robot.clickTag(RepairAppTestTags.IntakeMediaTakePhotoButton, scroll = true)
            robot.waitForTag(RepairAppTestTags.IntakeMediaCapturePhotoButton, scroll = true)
            robot.clickTag(RepairAppTestTags.IntakeMediaCapturePhotoButton, scroll = true)
            robot.waitForText("PHOTO", scroll = true)
            robot.capture(outputDir, "06-media-acquisition.png")

            robot.clickTag(RepairAppTestTags.NewOrderStep4NextButton, scroll = true)
            robot.waitForText(R.string.intake_step_privacy)
            robot.clickText(R.string.signature_accept_privacy, scroll = true)
            robot.clickText(R.string.signature_accept_terms, scroll = true)
            robot.drawSignature(RepairAppTestTags.SignatureCanvas)
            robot.waitForTag(RepairAppTestTags.NewOrderStep5NextButton, scroll = true)
            robot.capture(outputDir, "07-customer-signature.png")

            robot.clickTag(RepairAppTestTags.NewOrderStep5NextButton, scroll = true)
            robot.waitForTag(RepairAppTestTags.IntakeSubmitButton, scroll = true)
            robot.clickTag(RepairAppTestTags.IntakeSubmitButton, scroll = true)

            robot.clickDashboardTab("ORDERS")
            val summary = robot.getTestString(TestR.string.demo_order_summary_not_turning_on)
            robot.waitForText(summary.substringBefore("-").trim(), scroll = true)
            robot.clickText(summary.substringBefore("-").trim(), scroll = true)

            robot.clickText(R.string.order_generate_status_pdf, scroll = true)
            robot.clickTag(RepairAppTestTags.GenericConfirmButton)
            robot.pauseForDemo()

            robot.capture(outputDir, "08-share-stories.png")
            robot.clickTag(RepairAppTestTags.OrderShareButton, scroll = true)
        }
    }

    @Test
    fun flow9_StorePro() {
        runBlocking { DemoDataSeeder.seedDemoProfile(context, localeRule.locale) }
        TestUtils.refreshAppContainer()
        val outputDir = prepareDirectory("store-pro")
        val robot = ProAssetRobot(composeRule, context)

        launchMainActivity().use {
            robot.waitForTag(RepairAppTestTags.AuthUsernameField)
            robot.loginDemo(DemoDataSeeder.DEMO_OPERATOR_USERNAME)

            robot.waitForTag(RepairAppTestTags.dashboardTab("HOME"))
            robot.capture(outputDir, "01-dashboard-pro.png")

            robot.clickTag(RepairAppTestTags.HomeNewOrderButton, scroll = true)
            robot.waitForTag("quick_actions_screen")
            robot.clickTag(RepairAppTestTags.NewOrderInsertCustomerButton, scroll = true)
            robot.typeTag(
                RepairAppTestTags.NewOrderFirstNameField,
                robot.getTestString(TestR.string.demo_wizard_customer_first_name),
                scroll = true,
                fast = true,
            )
            robot.typeTag(
                RepairAppTestTags.NewOrderLastNameField,
                robot.getTestString(TestR.string.demo_wizard_customer_last_name),
                scroll = true,
                fast = true,
            )
            robot.typeTag(
                RepairAppTestTags.NewOrderPhoneField,
                robot.getTestString(TestR.string.demo_wizard_customer_phone),
                scroll = true,
                fast = true,
            )
            robot.typeTag(
                RepairAppTestTags.NewOrderEmailField,
                robot.getTestString(TestR.string.demo_wizard_customer_email),
                scroll = true,
                fast = true,
            )
            robot.capture(outputDir, "02-nuova-commessa-pro.png")

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
            robot.clickTag(RepairAppTestTags.NewOrderStep3NextButton, scroll = true)

            robot.waitForText(R.string.intake_step_media)
            robot.clickTag(RepairAppTestTags.IntakeMediaTakePhotoButton, scroll = true)
            robot.waitForTag(RepairAppTestTags.IntakeMediaCapturePhotoButton, scroll = true)
            robot.clickTag(RepairAppTestTags.IntakeMediaCapturePhotoButton, scroll = true)
            robot.waitForText("PHOTO", scroll = true)
            robot.clickTag(RepairAppTestTags.NewOrderStep4NextButton, scroll = true)

            robot.waitForText(R.string.intake_step_privacy)
            robot.clickText(R.string.signature_accept_privacy, scroll = true)
            robot.clickText(R.string.signature_accept_terms, scroll = true)
            robot.drawSignature(RepairAppTestTags.SignatureCanvas)
            robot.capture(outputDir, "03-foto-firma-pro.png")
            robot.clickTag(RepairAppTestTags.NewOrderStep5NextButton, scroll = true)

            robot.waitForTag(RepairAppTestTags.IntakeSubmitButton, scroll = true)
            robot.clickTag(RepairAppTestTags.IntakeSubmitButton, scroll = true)

            val summary = robot.getTestString(TestR.string.demo_order_summary_s23)
            waitForCreatedOrderId(summary)
            robot.clickDashboardTab("ORDERS")
            robot.waitForText(summary.substringBefore("-").trim(), scroll = true)
            robot.clickText(summary.substringBefore("-").trim(), scroll = true)

            robot.applySimpleStatus(
                OrderStatusAction.START_PROGRESS,
                OrderStatusAction.START_DIAGNOSIS,
            )
            robot.applySimpleStatus(
                OrderStatusAction.START_DIAGNOSIS,
                OrderStatusAction.RECORD_RESULT,
            )
            robot.applyResultStatus(
                robot.getTestString(TestR.string.demo_diagnosis_result_s23),
                robot.getTestString(TestR.string.demo_estimate_s23),
            )
            robot.capture(outputDir, "04-diagnosi-preventivo-pro.png")

            robot.acceptQuote()
            robot.capture(outputDir, "05-repairing-step-pro.png")

            robot.waitForTag(RepairAppTestTags.OrderWhatsAppButton, scroll = true)
            robot.clickTag(RepairAppTestTags.OrderWhatsAppButton)
            robot.waitForTag(RepairAppTestTags.OrderWhatsAppPreviewPanel)
            robot.typeTag(
                RepairAppTestTags.OrderWhatsAppMessageField,
                " ",
                scroll = false,
                fast = true,
            )
            robot.capture(outputDir, "06-whatsapp-pdf-pro.png")
            robot.dismissWhatsAppPreviewIfPresent()

            robot.applySimpleStatus(
                OrderStatusAction.READY_FOR_PICKUP,
                OrderStatusAction.CLOSE_ORDER,
            )
            robot.closeOrder(robot.getTestString(TestR.string.demo_closure_note_completed))
            robot.clickDashboardTab("ORDERS")

            robot.clickTag(RepairAppTestTags.OrdersFilterButton)
            robot.clickText(R.string.queue_closed)
            robot.clickText(R.string.action_confirm)

            robot.waitForText(summary.substringBefore("-").trim(), scroll = true)
            robot.clickText(summary.substringBefore("-").trim(), scroll = true)
            robot.waitForText(R.string.status_closed)
            robot.expandTimeline()
            robot.capture(outputDir, "07-timeline-chiusura-pro.png")

            robot.clickDashboardTab("CUSTOMERS")
            robot.waitForTag(RepairAppTestTags.CustomerSearchField)
            robot.capture(outputDir, "08-clienti-pro.png")

            val demoCustomerName = robot.getTestString(TestR.string.demo_customer_name_john)
            robot.waitForText(demoCustomerName, scroll = true)
            robot.clickText(demoCustomerName)
            robot.waitForText(R.string.customer_open_orders, scroll = true)
            robot.capture(outputDir, "09-dettaglio-cliente-pro.png")
        }
    }

    @Test
    fun flow10_StoreDemo() {
        if (!com.onlytechtake.repairapp.BuildConfig.IS_DEMO_VERSION) return
        runBlocking { DemoDataSeeder.seedDemoProfile(context, localeRule.locale) }
        TestUtils.refreshAppContainer()
        val outputDir = prepareDirectory("store-demo")
        val robot = DemoAssetRobot(composeRule, context)

        launchMainActivity().use {
            robot.waitForTag(RepairAppTestTags.AuthUsernameField)
            robot.loginDemo(DemoDataSeeder.DEMO_ADMIN_USERNAME)

            robot.waitForTag(RepairAppTestTags.dashboardTab("HOME"))
            robot.capture(outputDir, "01-dashboard-demo.png")

            robot.clickTag(RepairAppTestTags.HomeNewOrderButton, scroll = true)
            robot.waitForTag("quick_actions_screen")
            robot.capture(outputDir, "02-nuova-commessa-demo.png")

            robot.clickTag(RepairAppTestTags.NewOrderInsertCustomerButton, scroll = true)
            robot.pauseForDemo()
            robot.waitForText(
                robot
                    .getTestQuantityString(
                        TestR.plurals.demo_error_order_limit,
                        com.onlytechtake.repairapp.BuildConfig.MAX_ORDERS,
                    ).substring(0, 10),
            )
            robot.capture(outputDir, "03-limite-demo.png")

            robot.clickDashboardTab("HOME")
            robot.waitForTag(RepairAppTestTags.HomeNewOrderButton, scroll = true)
            robot.capture(outputDir, "04-passa-a-pro-demo.png")
        }
    }

    @Test
    fun flow11_SocialDemo() {
        if (!com.onlytechtake.repairapp.BuildConfig.IS_DEMO_VERSION) return
        runBlocking { DemoDataSeeder.seedDemoProfile(context, localeRule.locale) }
        TestUtils.refreshAppContainer()
        val outputDir = prepareDirectory("social-demo")
        val robot = DemoAssetRobot(composeRule, context)

        launchMainActivity().use {
            robot.waitForTag(RepairAppTestTags.AuthUsernameField)
            robot.loginDemo(DemoDataSeeder.DEMO_ADMIN_USERNAME)

            robot.waitForTag(RepairAppTestTags.dashboardTab("HOME"))
            robot.capture(outputDir, "01-stories-demo-intro.png")

            robot.clickTag(RepairAppTestTags.HomeNewOrderButton, scroll = true)
            robot.waitForTag("quick_actions_screen")
            robot.clickTag(RepairAppTestTags.NewOrderInsertCustomerButton, scroll = true)
            robot.pauseForDemo()
            robot.waitForText(
                robot
                    .getTestQuantityString(
                        TestR.plurals.demo_error_order_limit,
                        com.onlytechtake.repairapp.BuildConfig.MAX_ORDERS,
                    ).substring(0, 10),
            )
            robot.capture(outputDir, "02-reel-demo-limit.png")
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
                        .observeRecentActiveOrders(limit = 100)
                        .first()
                        .firstOrNull {
                            (it.id != DemoDataSeeder.DEMO_ORDER_ID) &&
                                it.title.equals(summary, ignoreCase = true)
                        }
                }
            order?.let { return it.id }
            Thread.sleep(250)
        }
        error("Ordine creato dal wizard non trovato (summary: $summary)")
    }

    private fun prepareDirectory(name: String): File {
        val root = File(requireNotNull(context.getExternalFilesDir(null)), "workflows")
        val directory = File(root, name)
        directory.deleteRecursively()
        directory.mkdirs()
        return directory
    }
}

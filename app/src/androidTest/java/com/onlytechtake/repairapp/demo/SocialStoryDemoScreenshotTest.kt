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
import com.onlytechtake.repairapp.domain.workspace.OrderStatusAction
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class SocialStoryDemoScreenshotTest {
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
    fun capturesOrderIntakeToClosureStories() {
        runBlocking { DemoDataSeeder.seedDemoProfile(context, localeRule.locale) }
        TestUtils.refreshAppContainer()
        val outputDir = prepareCaseDirectory(CASE_ORDER)
        val robot = ProAssetRobot(composeRule, context)

        withDemoApp {
            robot.waitForTag(RepairAppTestTags.AuthUsernameField)
            robot.capture(outputDir, "01-login.png")

            val username =
                if (BuildConfig.IS_DEMO_VERSION) DemoDataSeeder.DEMO_ADMIN_USERNAME else DemoDataSeeder.DEMO_OPERATOR_USERNAME
            robot.loginDemo(username)
            robot.waitForText(if (BuildConfig.IS_DEMO_VERSION) R.string.dashboard_role_admin else R.string.dashboard_role_operator)
            robot.capture(outputDir, "02-dashboard-home.png")

            robot.clickTag(RepairAppTestTags.HomeNewOrderButton, scroll = true)
            robot.waitForTag("quick_actions_screen")

            var formOpened = false
            repeat(3) {
                if (!formOpened) {
                    if (robot.isTagPresent(RepairAppTestTags.NewOrderInsertCustomerButton)) {
                        robot.clickTag(
                            RepairAppTestTags.NewOrderInsertCustomerButton,
                            scroll = true,
                        )
                    }
                    robot.pauseForDemo()
                    formOpened = robot.isTagPresent(RepairAppTestTags.NewOrderFirstNameField)
                }
            }

            robot.waitForTagExists(RepairAppTestTags.NewOrderFirstNameField)

            robot.typeTag(RepairAppTestTags.NewOrderFirstNameField, "Julia", scroll = true)
            robot.typeTag(RepairAppTestTags.NewOrderLastNameField, "White", scroll = true)
            robot.typeTag(RepairAppTestTags.NewOrderPhoneField, "+1 555-010-2020", scroll = true)
            robot.typeTag(
                RepairAppTestTags.NewOrderEmailField,
                "julia.white@example.invalid",
                scroll = true,
            )
            robot.capture(outputDir, "03-new-customer.png")

            robot.clickTag(RepairAppTestTags.NewOrderSaveCustomerButton, scroll = true)
            // Robust check: wait for the next button to be enabled
            robot.waitForTag(RepairAppTestTags.NewOrderStep1NextButton)
            robot.clickTag(RepairAppTestTags.NewOrderStep1NextButton, scroll = true)

            robot.clickTag(RepairAppTestTags.categoryButton("demo-cat-pc"), scroll = true)
            robot.clickTag(
                RepairAppTestTags.categoryButton(DemoDataSeeder.DEMO_CATEGORY_ID),
                scroll = true,
            )
            robot.clickTag(RepairAppTestTags.categoryButton("demo-cat-gaming"), scroll = true)
            robot.clickTag(RepairAppTestTags.categoryButton("demo-cat-rog-strix"), scroll = true)
            robot.capture(outputDir, "04-category-selection.png")

            robot.clickTag(RepairAppTestTags.NewOrderStep2NextButton, scroll = true)
            robot.typeTag(
                RepairAppTestTags.NewOrderProblemSummaryField,
                "Laptop won't turn on",
                scroll = true,
            )
            robot.typeTag(
                RepairAppTestTags.NewOrderProblemDetailsField,
                "The customer reports sudden shutdown after accidental drop.",
                scroll = true,
            )
            robot.capture(outputDir, "05-problem-details.png")

            // 4. Wizard: media
            robot.clickTag(RepairAppTestTags.NewOrderStep3NextButton, scroll = true)
            robot.waitForText(R.string.intake_step_media)
            robot.clickTag(RepairAppTestTags.IntakeMediaTakePhotoButton, scroll = true)
            robot.waitForTag(RepairAppTestTags.IntakeMediaCapturePhotoButton, scroll = true)
            robot.clickTag(RepairAppTestTags.IntakeMediaCapturePhotoButton, scroll = true)
            robot.waitForText("PHOTO", scroll = true)
            robot.capture(outputDir, "06-media-acquisition.png")

            // 5. Wizard: privacy and signature
            robot.clickTag(RepairAppTestTags.NewOrderStep4NextButton, scroll = true)
            robot.waitForText(R.string.intake_step_privacy)
            robot.clickText(R.string.signature_accept_privacy, scroll = true)
            robot.clickText(R.string.signature_accept_terms, scroll = true)
            robot.drawSignature(RepairAppTestTags.SignatureCanvas)
            robot.waitForTag(RepairAppTestTags.NewOrderStep5NextButton, scroll = true)
            robot.capture(outputDir, "07-customer-signature.png")

            robot.clickTag(RepairAppTestTags.NewOrderStep5NextButton, scroll = true)
            robot.waitForTag(RepairAppTestTags.IntakeSubmitButton, scroll = true)
            robot.capture(outputDir, "08-summary-submit.png")
            robot.clickTag(RepairAppTestTags.IntakeSubmitButton, scroll = true)

            robot.clickDashboardTab("ORDERS")
            robot.waitForText(R.string.dashboard_orders)
            robot.capture(outputDir, "09-orders-list.png")

            val summary = "Laptop won't turn on"
            robot.waitForText(summary, scroll = true)
            robot.clickText(summary, scroll = true)

            robot.waitForTag(RepairAppTestTags.OrderStatusLabel)
            robot.waitForTag(RepairAppTestTags.OrderDetailOrderReference, useUnmergedTree = true)
            robot.waitForTag(RepairAppTestTags.OrderDetailCustomerReference, useUnmergedTree = true)
            robot.waitForTag(RepairAppTestTags.orderDetailStatusStep(1), useUnmergedTree = true)
            robot.waitForText("Asus ROG Strix")
            robot.capture(outputDir, "10-order-details.png")

            robot.applySimpleStatus(
                OrderStatusAction.START_PROGRESS,
                nextAction = OrderStatusAction.START_DIAGNOSIS,
            )
            robot.applySimpleStatus(
                OrderStatusAction.START_DIAGNOSIS,
                nextAction = OrderStatusAction.RECORD_RESULT,
            )
            robot.applyResultStatus(
                summary = "Damaged power connector.",
                estimate = "149.00",
            )
            robot.capture(outputDir, "11-diagnosis-result.png")

            robot.acceptQuote()
            robot.applySimpleStatus(
                OrderStatusAction.READY_FOR_PICKUP,
                nextAction = OrderStatusAction.CLOSE_ORDER,
            )
            robot.closeOrder()
            robot.clickDashboardTab("ORDERS")

            // Enable "Closed" filter to see the order we just closed
            robot.clickTag(RepairAppTestTags.OrdersFilterButton)
            robot.clickText(R.string.queue_closed)
            robot.clickText(R.string.action_confirm)

            robot.waitForText(summary, scroll = true)
            robot.clickText(summary, scroll = true)
            robot.waitForText(R.string.status_closed)
            robot.capture(outputDir, "12-timeline-closure.png")
        }
    }

    @Test
    fun capturesCategoryAndTemplateStories() {
        runBlocking { DemoDataSeeder.seedDemoProfile(context, localeRule.locale) }
        TestUtils.refreshAppContainer()
        val outputDir = prepareCaseDirectory(CASE_CONFIG)
        val robot = ProAssetRobot(composeRule, context)

        withDemoApp {
            robot.waitForTag(RepairAppTestTags.AuthUsernameField)
            robot.loginDemo(DemoDataSeeder.DEMO_ADMIN_USERNAME)
            robot.waitForText(R.string.dashboard_role_admin)
            robot.capture(outputDir, "01-admin-dashboard.png")

            robot.clickDashboardTab("SETTINGS")
            robot.waitForText(R.string.dashboard_config)
            robot.capture(outputDir, "02-config-hub.png")

            robot.clickTag(RepairAppTestTags.ConfigHubCategoriesButton, scroll = true)
            robot.clickTag(RepairAppTestTags.ConfigCategoryNewButton, scroll = true)
            robot.typeTag(
                RepairAppTestTags.ConfigCategoryNameField,
                "Handheld Console",
                scroll = true,
            )
            robot.typeTag(
                RepairAppTestTags.ConfigCategoryDescriptionField,
                "Portable console repairs, displays and batteries.",
                scroll = true,
            )
            robot.capture(outputDir, "03-category-creation.png")
            robot.clickTag(RepairAppTestTags.ConfigCategorySaveButton, scroll = true)
            robot.waitForTag(RepairAppTestTags.GenericConfirmButton)
            robot.clickTag(RepairAppTestTags.GenericConfirmButton)
            robot.waitForTagGone(RepairAppTestTags.GenericConfirmButton)
            robot.dismissBanner()
            robot.pauseForDemo()
            robot.capture(outputDir, "04-category-saved.png")

            robot.clickTag(RepairAppTestTags.ConfigBackButton, scroll = true)
            robot.clickTag(RepairAppTestTags.ConfigHubTemplateMappingButton, scroll = true)
            robot.waitForText(R.string.config_hub_template_mapping_title)
            robot.capture(outputDir, "05-template-mappings.png")
            robot.clickTag(RepairAppTestTags.ConfigBackButton, scroll = true)
            robot.clickTag(RepairAppTestTags.ConfigHubTemplatesButton, scroll = true)
            robot.waitForText(R.string.config_hub_templates_title)
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
                "Social demo update",
                scroll = true,
            )
            robot.typeTag(
                RepairAppTestTags.ConfigTemplateBodyField,
                "Hi {{cliente}}, the order {{numero_commessa}} is now in status {{stato_commessa}}.",
                scroll = true,
            )
            robot.capture(outputDir, "07-template-creation.png")
            robot.clickTag(RepairAppTestTags.ConfigTemplateSaveButton, scroll = true)
            robot.waitForTag(RepairAppTestTags.GenericConfirmButton)
            robot.clickTag(RepairAppTestTags.GenericConfirmButton)
            robot.waitForTagGone(RepairAppTestTags.GenericConfirmButton)
            robot.dismissBanner()
            robot.pauseForDemo()
            robot.capture(outputDir, "08-template-saved.png")
        }
    }

    @Test
    fun capturesPrivateAndBusinessCustomerStories() {
        runBlocking { DemoDataSeeder.seedDemoProfile(context, localeRule.locale) }
        TestUtils.refreshAppContainer()
        val outputDir = prepareCaseDirectory(CASE_CUSTOMERS)
        val robot = ProAssetRobot(composeRule, context)

        withDemoApp {
            robot.waitForTag(RepairAppTestTags.AuthUsernameField)
            robot.loginDemo(DemoDataSeeder.DEMO_ADMIN_USERNAME)
            robot.clickDashboardTab("CUSTOMERS")
            robot.capture(outputDir, "01-customer-archive.png")

            robot.clickTag(RepairAppTestTags.CustomerNewButton, scroll = true)
            robot.clickTag(RepairAppTestTags.CustomerPrivateToggle, scroll = true)
            robot.typeTag(RepairAppTestTags.CustomerFirstNameField, "Luke", scroll = true)
            robot.typeTag(RepairAppTestTags.CustomerLastNameField, "Red", scroll = true)
            robot.typeTag(RepairAppTestTags.CustomerTaxCodeField, "REDLKU90A01F205X", scroll = true)
            robot.typeTag(RepairAppTestTags.CustomerPhoneField, "+1 555-010-3030", scroll = true)
            robot.typeTag(
                RepairAppTestTags.CustomerEmailField,
                "luke.red@example.invalid",
                scroll = true,
            )
            robot.capture(outputDir, "02-private-customer.png")
            robot.clickTag(RepairAppTestTags.CustomerSaveButton, scroll = true)
            robot.waitForTag(RepairAppTestTags.CustomerConfirmSaveButton)
            robot.clickTag(RepairAppTestTags.CustomerConfirmSaveButton)
            robot.waitForTagGone(RepairAppTestTags.CustomerConfirmSaveButton)
            robot.dismissBanner()
            robot.typeTag(RepairAppTestTags.CustomerSearchField, "Luke")
            robot.waitForText("Luke")
            robot.capture(outputDir, "03-private-saved.png")

            robot.clickTag(RepairAppTestTags.CustomerNewButton, scroll = true)
            robot.clickTag(RepairAppTestTags.CustomerBusinessToggle, scroll = true)
            robot.typeTag(RepairAppTestTags.CustomerCompanyNameField, "Demo Lab LLC", scroll = true)
            robot.typeTag(RepairAppTestTags.CustomerVatNumberField, "US123456789", scroll = true)
            robot.typeTag(RepairAppTestTags.CustomerPhoneField, "+1 555-010-4040", scroll = true)
            robot.typeTag(
                RepairAppTestTags.CustomerEmailField,
                "admin@example.invalid",
                scroll = true,
            )
            robot.typeTag(
                RepairAppTestTags.CustomerAddressField,
                "24 Demo Way, New York",
                scroll = true,
            )
            robot.capture(outputDir, "04-business-customer.png")
            robot.clickTag(RepairAppTestTags.CustomerSaveButton, scroll = true)
            robot.waitForTag(RepairAppTestTags.CustomerConfirmSaveButton)
            robot.clickTag(RepairAppTestTags.CustomerConfirmSaveButton)
            robot.waitForTagGone(RepairAppTestTags.CustomerConfirmSaveButton)
            robot.dismissBanner()
            robot.typeTag(RepairAppTestTags.CustomerSearchField, "Demo")
            robot.waitForText("Demo")
            robot.capture(outputDir, "05-business-saved.png")
        }
    }

    @Test
    fun capturesMaintenanceStories() {
        runBlocking { DemoDataSeeder.seedDemoProfile(context, localeRule.locale) }
        TestUtils.refreshAppContainer()
        val outputDir = prepareCaseDirectory(CASE_MAINTENANCE)
        val robot = ProAssetRobot(composeRule, context)

        withDemoApp {
            robot.waitForTag(RepairAppTestTags.AuthUsernameField)
            robot.loginDemo(DemoDataSeeder.DEMO_ADMIN_USERNAME)

            robot.clickDashboardTab("SETTINGS")
            robot.waitForText(R.string.dashboard_config)

            robot.clickTag(RepairAppTestTags.ConfigHubStorageButton, scroll = true)
            robot.waitForText(R.string.storage_title)
            robot.capture(outputDir, "01-storage-management.png")
            robot.clickTag(RepairAppTestTags.ConfigBackButton, scroll = true)

            robot.clickTag(RepairAppTestTags.ConfigHubBackupButton, scroll = true)
            robot.waitForText(R.string.config_backup_title)
            robot.capture(outputDir, "02-data-backup.png")

            robot.clickText(R.string.config_backup_export_xlsx, scroll = true)
            robot.waitForText(R.string.config_backup_export_xlsx)
            robot.capture(outputDir, "03-excel-export.png")
        }
    }

    @Test
    fun capturesDemoLimitStory() {
        if (!BuildConfig.IS_DEMO_VERSION) return

        runBlocking { DemoDataSeeder.seedDemoProfile(context, localeRule.locale) }
        TestUtils.refreshAppContainer()
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
                        com.onlytechtake.repairapp.test.R.plurals.demo_error_order_limit,
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
        const val CASE_ORDER = "01-order-new-customer-closure"
        const val CASE_CONFIG = "02-categories-templates"
        const val CASE_CUSTOMERS = "03-customers-private-business"
        const val CASE_MAINTENANCE = "05-maintenance-backup"
        const val CASE_DEMO_LIMIT = "06-demo-limit"
        const val SCREENSHOT_ROOT_DIR_NAME = "social-stories"
    }
}

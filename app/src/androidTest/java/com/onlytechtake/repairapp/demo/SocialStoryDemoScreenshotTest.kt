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
import com.onlytechtake.repairapp.domain.workspace.OrderStatusAction
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import com.onlytechtake.repairapp.test.R as TestR

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
    private val testContext: Context
        get() = InstrumentationRegistry.getInstrumentation().context

    @Test
    fun capturesSocialStoryFlow() {
        runBlocking { DemoDataSeeder.seedDemoProfile(context) }
        TestUtils.refreshAppContainer()
        val outputDir = prepareScreenshotDirectory()
        val robot = DemoAssetRobot(composeRule, context)

        ActivityScenario.launch(MainActivity::class.java).use {
            robot.loginDemo(DemoDataSeeder.DEMO_OPERATOR_USERNAME)
            robot.capture(outputDir, "01-home-stories.png")

            // 1. Wizard: Customer
            robot.clickDashboardTab("NEW_ORDER")
            robot.waitForTag(RepairAppTestTags.NewOrderCustomerSearchField)
            robot.capture(outputDir, "02-customer-selection.png")

            val customerName = robot.getTestString(TestR.string.demo_customer_name_julia)

            robot.typeTag(RepairAppTestTags.NewOrderCustomerSearchField, customerName)
            robot.clickText(R.string.customer_select)
            robot.clickText(R.string.customer_select)
            robot.clickTag(RepairAppTestTags.NewOrderStep1NextButton, scroll = true)

            // 2. Wizard: Category
            robot.waitForTag(RepairAppTestTags.categoryButton("demo-cat-pc"))
            robot.clickTag(RepairAppTestTags.categoryButton("demo-cat-pc"), scroll = true)
            robot.waitForTag(RepairAppTestTags.categoryButton("demo-cat-handheld"))
            robot.clickTag(RepairAppTestTags.categoryButton("demo-cat-handheld"), scroll = true)
            robot.capture(outputDir, "04-category-selection.png")

            robot.clickTag(RepairAppTestTags.NewOrderStep2NextButton, scroll = true)

            // 3. Wizard: Problem details
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

            // 6. Finalization
            robot.waitForTag(RepairAppTestTags.IntakeSubmitButton, scroll = true)
            robot.clickTag(RepairAppTestTags.IntakeSubmitButton, scroll = true)

            // Social Post creation (Showcase)
            robot.clickDashboardTab("ORDERS")
            val summary = robot.getTestString(TestR.string.demo_order_summary_not_turning_on)
            robot.waitForText(summary, scroll = true)
            robot.clickText(summary, scroll = true)

            // Generate PDF then share
            robot.clickText(R.string.order_generate_status_pdf, scroll = true)
            // Confirmation dialog for missing signatures
            robot.clickTag(RepairAppTestTags.GenericConfirmButton)
            // Wait for generation
            robot.pauseForDemo()

            robot.capture(outputDir, "08-share-stories.png")
            robot.clickTag(RepairAppTestTags.OrderShareButton, scroll = true)
        }
    }

    private fun prepareScreenshotDirectory(): File {
        val directory = File(requireNotNull(context.getExternalFilesDir(null)), "social-stories")
        directory.deleteRecursively()
        directory.mkdirs()
        return directory
    }
}

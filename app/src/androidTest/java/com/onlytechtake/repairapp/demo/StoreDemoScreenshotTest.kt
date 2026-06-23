package com.onlytechtake.repairapp.demo

import android.content.Context
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.onlytechtake.repairapp.BuildConfig
import com.onlytechtake.repairapp.MainActivity
import com.onlytechtake.repairapp.core.design.RepairAppTestTags
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class StoreDemoScreenshotTest {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    @get:Rule
    val cameraPermissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(android.Manifest.permission.CAMERA)

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun capturesStoreDemoScreenshots() {
        if (!BuildConfig.IS_DEMO_VERSION) return

        runBlocking { DemoDataSeeder.seedDemoProfile(context) }
        val outputDir = prepareScreenshotDirectory()
        val robot = DemoAssetRobot(composeRule, context)

        ActivityScenario.launch(MainActivity::class.java).use {
            robot.waitForTag(RepairAppTestTags.AuthUsernameField)
            robot.loginDemo(DemoDataSeeder.DEMO_ADMIN_USERNAME)

            // 01-dashboard-demo
            robot.waitForTag(RepairAppTestTags.dashboardTab("HOME"))
            robot.capture(outputDir, "01-dashboard-demo.png")

            // 02-nuova-commessa-demo
            robot.clickTag(RepairAppTestTags.HomeNewOrderButton, scroll = true)
            robot.waitForTag("quick_actions_screen")
            robot.capture(outputDir, "02-nuova-commessa-demo.png")

            // 03-limite-demo
            robot.clickTag(RepairAppTestTags.NewOrderInsertCustomerButton, scroll = true)
            robot.pauseForDemo()
            robot.waitForText(
                context
                    .getString(com.onlytechtake.repairapp.R.string.demo_error_order_limit)
                    .substring(0, 10),
            )
            robot.capture(outputDir, "03-limite-demo.png")

            // 04-passa-a-pro-demo (Close banner and show Home with some Pro CTA if any)
            robot.clickTag(RepairAppTestTags.MessageBannerCloseButton)
            robot.clickDashboardTab("HOME")
            // Assuming there is some "Passa a Pro" info on Home in Demo
            robot.capture(outputDir, "04-passa-a-pro-demo.png")
        }
    }

    private fun prepareScreenshotDirectory(): File {
        val directory = File(requireNotNull(context.getExternalFilesDir(null)), "store-demo")
        directory.deleteRecursively()
        directory.mkdirs()
        return directory
    }
}

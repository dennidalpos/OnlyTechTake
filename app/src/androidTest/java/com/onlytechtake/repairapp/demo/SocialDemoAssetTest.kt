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
class SocialDemoAssetTest {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    @get:Rule
    val cameraPermissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(android.Manifest.permission.CAMERA)

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun capturesSocialDemoAssets() {
        if (!BuildConfig.IS_DEMO_VERSION) return

        runBlocking { DemoDataSeeder.seedDemoProfile(context) }
        val outputDir = prepareScreenshotDirectory()
        val robot = DemoAssetRobot(composeRule, context)

        ActivityScenario.launch(MainActivity::class.java).use {
            robot.waitForTag(RepairAppTestTags.AuthUsernameField)
            robot.loginDemo(DemoDataSeeder.DEMO_ADMIN_USERNAME)

            // 01-stories-demo-intro
            robot.waitForTag(RepairAppTestTags.dashboardTab("HOME"))
            robot.capture(outputDir, "01-stories-demo-intro.png")

            // 02-reel-demo-limit (Show the limit and CTA to Pro)
            robot.clickTag(RepairAppTestTags.HomeNewOrderButton, scroll = true)
            robot.waitForTag("quick_actions_screen")
            robot.clickTag(RepairAppTestTags.NewOrderInsertCustomerButton, scroll = true)
            robot.pauseForDemo()
            robot.waitForText(
                context.getString(com.onlytechtake.repairapp.R.string.demo_error_order_limit)
                    .substring(0, 10)
            )
            robot.capture(outputDir, "02-reel-demo-limit.png")
        }
    }

    private fun prepareScreenshotDirectory(): File {
        val directory = File(requireNotNull(context.getExternalFilesDir(null)), "social-demo")
        directory.deleteRecursively()
        directory.mkdirs()
        return directory
    }
}

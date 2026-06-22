package com.onlytechtake.repairapp.demo

import android.content.Context
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.onlytechtake.repairapp.BuildConfig
import com.onlytechtake.repairapp.MainActivity
import com.onlytechtake.repairapp.core.design.RepairAppTestTags
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class ShowcaseRepresentanzaScreenshotTest {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    @get:Rule
    val cameraPermissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(android.Manifest.permission.CAMERA)

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun capturesShowcaseRepresentanzaScreenshots() {
        // Solo per Showcase mode
        if (!BuildConfig.IS_SHOWCASE_MODE_ENABLED) return

        val outputDir = prepareScreenshotDirectory()
        val robot = ShowcaseAssetRobot(composeRule, context)

        ActivityScenario.launch(MainActivity::class.java).use {
            // In Showcase mode, il login e' automatico dopo il seed.
            // Aspettiamo che appaia la Dashboard.
            robot.waitForTag(RepairAppTestTags.dashboardTab("HOME"), timeoutMillis = 60_000L)

            // 01-dashboard-showcase
            robot.capture(outputDir, "01-dashboard-showcase.png")

            // 02-commesse-popolate
            robot.clickDashboardTab("ORDERS")
            robot.waitForTag(RepairAppTestTags.orderCard("order-1"), scroll = true)
            robot.capture(outputDir, "02-commesse-popolate.png")

            // 03-dettaglio-rappresentativo
            // Scegliamo un ordine significativo, es. order-1 (recente)
            robot.clickTag(RepairAppTestTags.orderCard("order-1"), scroll = true)
            robot.waitForTag(RepairAppTestTags.OrderDetailLazyColumn)
            robot.capture(outputDir, "03-dettaglio-rappresentativo.png")

            // 04-preventivo-whatsapp-pdf
            robot.waitForTag(RepairAppTestTags.OrderWhatsAppButton, scroll = true)
            robot.clickTag(RepairAppTestTags.OrderWhatsAppButton)
            robot.pauseForDemo()
            // Optional: check if preview is there, but don't fail if it's slow
            if (robot.waitForTagIfPresent(RepairAppTestTags.OrderWhatsAppPreviewPanel, 5000L)) {
                robot.capture(outputDir, "04-preventivo-whatsapp-pdf.png")
                robot.dismissWhatsAppPreviewIfPresent()
            } else {
                robot.capture(outputDir, "04-preventivo-whatsapp-pdf-fallback.png")
            }

            // 05-timeline-finale
            robot.waitForTag(RepairAppTestTags.OrderTimelineSection, scroll = true)
            robot.capture(outputDir, "05-timeline-finale.png")
        }
    }

    private fun prepareScreenshotDirectory(): File {
        val directory =
            File(requireNotNull(context.getExternalFilesDir(null)), "showcase-representanza")
        directory.deleteRecursively()
        directory.mkdirs()
        return directory
    }
}

class ShowcaseAssetRobot(composeRule: ComposeTestRule, context: Context) :
    BaseAssetRobot(composeRule, context) {
    fun waitForTag(tag: String, timeoutMillis: Long) {
        waitForTagIfPresent(tag, timeoutMillis)
    }
}

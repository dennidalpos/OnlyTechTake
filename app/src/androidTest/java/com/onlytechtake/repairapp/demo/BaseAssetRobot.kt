package com.onlytechtake.repairapp.demo

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.click
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import androidx.test.espresso.Espresso
import androidx.test.platform.app.InstrumentationRegistry
import com.onlytechtake.repairapp.core.design.RepairAppTestTags
import java.io.File
import java.io.FileOutputStream

abstract class BaseAssetRobot(
    protected val composeRule: ComposeTestRule,
    protected val context: Context,
) {
    fun loginDemo(username: String) {
        repeat(LOGIN_ATTEMPTS) { attempt ->
            waitForTag(RepairAppTestTags.AuthUsernameField)
            typeTag(RepairAppTestTags.AuthUsernameField, username)
            pauseForDemo()
            typeTag(RepairAppTestTags.AuthPasswordField, DemoDataSeeder.DEMO_PASSWORD)
            pauseForDemo()
            clickTag(RepairAppTestTags.AuthSubmitButton)

            if (waitForTagIfPresent(
                    RepairAppTestTags.dashboardTab("HOME"),
                    LOGIN_ATTEMPT_TIMEOUT_MILLIS,
                )
            ) {
                return
            }

            if ((attempt == (LOGIN_ATTEMPTS - 1)) || !isTagPresent(RepairAppTestTags.AuthUsernameField)) {
                waitForTag(RepairAppTestTags.dashboardTab("HOME"))
            }
            pauseForDemo()
        }
    }

    fun clickDashboardTab(destination: String) {
        val tabTag = RepairAppTestTags.dashboardTab(destination)
        waitForTagExists(tabTag)
        composeRule.onNodeWithTag(tabTag).performClick()
        composeRule.waitForIdle()
        if (isTextDisplayed("Dati non salvati")) {
            clickTag(RepairAppTestTags.DashboardDiscardChangesButton)
        }
        composeRule.waitForIdle()
        Thread.sleep(2000)
    }

    fun typeTag(
        tag: String,
        text: String,
        scroll: Boolean = false,
    ) {
        waitForTagExists(tag, scroll = scroll)
        val node = composeRule.onNodeWithTag(tag)
        node.performClick()
        composeRule.waitForIdle()
        Thread.sleep(200)
        node.performTextReplacement(text)
        composeRule.waitForIdle()
        Espresso.closeSoftKeyboard()
        Thread.sleep(500)
        pauseForDemo()
    }

    fun clickTag(
        tag: String,
        scroll: Boolean = false,
        useUnmergedTree: Boolean = false,
    ) {
        waitForTag(tag, scroll = scroll, useUnmergedTree = useUnmergedTree)
        val node = composeRule.onNodeWithTag(tag, useUnmergedTree = useUnmergedTree)
        try {
            composeRule.waitUntil(timeoutMillis = 5_000) {
                runCatching { node.assertIsEnabled() }.isSuccess
            }
        } catch (_: Throwable) {
            node.assertIsEnabled()
        }
        try {
            node.performClick()
        } catch (_: Throwable) {
            try {
                node.performTouchInput { click() }
            } catch (_: Throwable) {
                node.performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick)
            }
        }
        composeRule.waitForIdle()
        pauseForDemo()
    }

    fun clickText(
        text: String,
        scroll: Boolean = false,
    ) {
        waitForText(text, scroll = scroll)
        val node = composeRule.onNodeWithText(text, substring = true)
        try {
            node.performClick()
        } catch (_: Throwable) {
            try {
                node.performTouchInput { click() }
            } catch (_: Throwable) {
                node.performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick)
            }
        }
        composeRule.waitForIdle()
        pauseForDemo()
    }

    fun waitForTag(
        tag: String,
        scroll: Boolean = false,
        useUnmergedTree: Boolean = false,
    ) {
        waitForTagExists(tag, scroll = scroll, useUnmergedTree = useUnmergedTree)
        val deadline = System.currentTimeMillis() + WAIT_TIMEOUT_MILLIS
        var attempts = 0
        while (System.currentTimeMillis() < deadline) {
            val displayed =
                runCatching {
                    val node = composeRule.onNodeWithTag(tag, useUnmergedTree = useUnmergedTree)
                    if (scroll) {
                        runCatching { node.performScrollTo() }
                        composeRule.waitForIdle()
                    }
                    node.assertIsDisplayed()
                    true
                }.getOrDefault(defaultValue = false)
            if (displayed) return
            if (scroll) {
                if ((attempts % 15) < 10) performSafeSwipeUp() else performSafeSwipeDown()
            }
            Thread.sleep(TAG_POLL_MILLIS)
            attempts++
        }
        composeRule.onNodeWithTag(tag, useUnmergedTree = useUnmergedTree).assertIsDisplayed()
    }

    fun waitForTagExists(
        tag: String,
        scroll: Boolean = false,
        useUnmergedTree: Boolean = false,
    ) {
        var found = false
        val deadline = System.currentTimeMillis() + WAIT_TIMEOUT_MILLIS
        var attempts = 0
        while (System.currentTimeMillis() < deadline) {
            found = isTagPresent(tag, useUnmergedTree)
            if (found) break
            if (scroll) {
                if ((attempts % 2) == 0) {
                    try {
                        composeRule
                            .onNodeWithTag(tag, useUnmergedTree = useUnmergedTree)
                            .performScrollTo()
                        composeRule.waitForIdle()
                        if (isTagPresent(tag, useUnmergedTree)) {
                            found = true
                            break
                        }
                    } catch (_: Throwable) {
                        if ((attempts % 15) < 10) performSafeSwipeUp() else performSafeSwipeDown()
                    }
                }
            }
            Thread.sleep(TAG_POLL_MILLIS)
            attempts++
        }
        if (!found) composeRule.onNodeWithTag(tag, useUnmergedTree = useUnmergedTree).assertExists()
    }

    fun isTagPresent(
        tag: String,
        useUnmergedTree: Boolean = false,
    ): Boolean =
        composeRule
            .onAllNodesWithTag(tag, useUnmergedTree = useUnmergedTree)
            .fetchSemanticsNodes()
            .isNotEmpty()

    private fun isTextDisplayed(text: String): Boolean = composeRule.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty()

    fun waitForText(
        text: String,
        scroll: Boolean = false,
    ) {
        var found = false
        val deadline = System.currentTimeMillis() + WAIT_TIMEOUT_MILLIS
        var attempts = 0
        while (System.currentTimeMillis() < deadline) {
            found = isTextDisplayed(text)
            if (found) break
            if (scroll) {
                if ((attempts % 2) == 0) {
                    try {
                        composeRule.onNodeWithText(text, substring = true).performScrollTo()
                        composeRule.waitForIdle()
                        if (isTextDisplayed(text)) {
                            found = true
                            break
                        }
                    } catch (_: Throwable) {
                        if ((attempts % 15) < 10) performSafeSwipeUp() else performSafeSwipeDown()
                    }
                }
            }
            Thread.sleep(TAG_POLL_MILLIS)
            attempts++
        }
        if (!found) composeRule.onNodeWithText(text, substring = true).assertExists()
    }

    fun waitForTagGone(
        tag: String,
        useUnmergedTree: Boolean = false,
        timeoutMillis: Long = WAIT_TIMEOUT_MILLIS,
    ) {
        composeRule.waitUntil(timeoutMillis = timeoutMillis) {
            composeRule
                .onAllNodesWithTag(tag, useUnmergedTree = useUnmergedTree)
                .fetchSemanticsNodes()
                .isEmpty()
        }
        composeRule.waitForIdle()
    }

    private fun waitForTagIfPresent(
        tag: String,
        timeoutMillis: Long,
        useUnmergedTree: Boolean = false,
    ): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMillis
        do {
            if (isTagPresent(tag, useUnmergedTree)) return true
            Thread.sleep(TAG_POLL_MILLIS)
        } while (System.currentTimeMillis() < deadline)
        return false
    }

    fun dismissWhatsAppPreviewIfPresent() {
        if (waitForTagIfPresent(
                RepairAppTestTags.OrderWhatsAppPreviewPanel,
                5000,
                useUnmergedTree = true,
            )
        ) {
            repeat(5) {
                if (isTagPresent(
                        RepairAppTestTags.OrderWhatsAppPreviewPanel,
                        useUnmergedTree = true,
                    )
                ) {
                    try {
                        composeRule
                            .onNodeWithTag(
                                RepairAppTestTags.OrderWhatsAppClosePreviewButton,
                                useUnmergedTree = true,
                            ).performClick()
                        composeRule.waitForIdle()
                        Thread.sleep(1000)
                    } catch (_: Throwable) {
                    }
                }
            }
            if (isTagPresent(RepairAppTestTags.OrderWhatsAppPreviewPanel, useUnmergedTree = true)) {
                try {
                    Espresso.pressBackUnconditionally()
                    composeRule.waitForIdle()
                    Thread.sleep(500)
                } catch (_: Throwable) {
                }
            }
            waitForTagGone(
                RepairAppTestTags.OrderWhatsAppPreviewPanel,
                useUnmergedTree = true,
                timeoutMillis = 3000,
            )
        }
    }

    protected fun performSafeSwipeUp() {
        val listTags =
            listOf(
                RepairAppTestTags.OrderDetailLazyColumn,
                "categories_list",
                "customer_list",
                "customer_form",
            )
        for (tag in listTags) {
            try {
                composeRule
                    .onNodeWithTag(tag)
                    .performTouchInput { swipeUp(startY = bottom * 0.8f, endY = top + 30f) }
                composeRule.waitForIdle()
                return
            } catch (_: Throwable) {
            }
        }
        try {
            composeRule
                .onAllNodes(isRoot())
                .onFirst()
                .performTouchInput { swipeUp(startY = bottom * 0.8f, endY = top + 30f) }
        } catch (_: Throwable) {
        }
        composeRule.waitForIdle()
    }

    protected fun performSafeSwipeDown() {
        val listTags =
            listOf(
                RepairAppTestTags.OrderDetailLazyColumn,
                "categories_list",
                "customer_list",
                "customer_form",
            )
        for (tag in listTags) {
            try {
                composeRule
                    .onNodeWithTag(tag)
                    .performTouchInput { swipeDown(startY = top + 30f, endY = bottom * 0.8f) }
                composeRule.waitForIdle()
                return
            } catch (_: Throwable) {
            }
        }
        try {
            composeRule
                .onAllNodes(isRoot())
                .onFirst()
                .performTouchInput { swipeDown(startY = top + 30f, endY = bottom * 0.8f) }
        } catch (_: Throwable) {
        }
        composeRule.waitForIdle()
    }

    fun drawSignature(tag: String) {
        waitForTag(tag, scroll = true)
        composeRule.onNodeWithTag(tag).performTouchInput {
            val w = width.toFloat()
            val h = height.toFloat()

            // Firma stilizzata di "Marco Bianchi" (Nome e Cognome)
            // --- MARCO ---
            // M
            down(Offset(w * 0.10f, h * 0.65f))
            moveTo(Offset(w * 0.12f, h * 0.35f))
            moveTo(Offset(w * 0.18f, h * 0.55f))
            moveTo(Offset(w * 0.24f, h * 0.35f))
            moveTo(Offset(w * 0.26f, h * 0.65f))
            // Tratto legato per "arco"
            moveTo(Offset(w * 0.28f, h * 0.60f))
            moveTo(Offset(w * 0.32f, h * 0.65f))
            moveTo(Offset(w * 0.36f, h * 0.60f))
            moveTo(Offset(w * 0.40f, h * 0.65f))
            moveTo(Offset(w * 0.44f, h * 0.60f))
            up()

            // --- BIANCHI ---
            // B
            down(Offset(w * 0.55f, h * 0.70f))
            moveTo(Offset(w * 0.55f, h * 0.30f))
            moveTo(Offset(w * 0.65f, h * 0.35f))
            moveTo(Offset(w * 0.60f, h * 0.50f))
            moveTo(Offset(w * 0.55f, h * 0.50f))
            moveTo(Offset(w * 0.68f, h * 0.60f))
            moveTo(Offset(w * 0.62f, h * 0.75f))
            moveTo(Offset(w * 0.55f, h * 0.70f))
            // Tratto legato per "ianchi"
            moveTo(Offset(w * 0.70f, h * 0.65f))
            moveTo(Offset(w * 0.75f, h * 0.70f))
            moveTo(Offset(w * 0.80f, h * 0.65f))
            moveTo(Offset(w * 0.85f, h * 0.70f))
            moveTo(Offset(w * 0.90f, h * 0.60f))
            up()

            // Sottolineatura d'autore
            down(Offset(w * 0.05f, h * 0.88f))
            moveTo(Offset(w * 0.95f, h * 0.82f))
            up()
        }
        composeRule.waitForIdle()
        Thread.sleep(1000) // Pausa per far sparire eventuali artefatti UI della gesture
    }

    fun capture(
        outputDir: File,
        fileName: String,
    ) {
        composeRule.waitForIdle()
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        pauseForScreenshot()
        val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        FileOutputStream(File(outputDir, fileName)).use { output ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        }
        bitmap.recycle()
    }

    fun pauseForDemo() {
        Thread.sleep(if (isRecordingDemo()) RECORDING_ACTION_PAUSE_MILLIS else ACTION_PAUSE_MILLIS)
    }

    private fun pauseForScreenshot() {
        Thread.sleep(if (isRecordingDemo()) RECORDING_SCREENSHOT_PAUSE_MILLIS else SCREENSHOT_PAUSE_MILLIS)
    }

    private fun isRecordingDemo(): Boolean = InstrumentationRegistry.getArguments().getString("demoRecording").toBoolean()

    companion object {
        const val WAIT_TIMEOUT_MILLIS = 30_000L
        const val LOGIN_ATTEMPTS = 2
        const val LOGIN_ATTEMPT_TIMEOUT_MILLIS = 20_000L
        const val TAG_POLL_MILLIS = 250L
        const val ACTION_PAUSE_MILLIS = 2_000L
        const val SCREENSHOT_PAUSE_MILLIS = 2_000L
        const val RECORDING_ACTION_PAUSE_MILLIS = 500L
        const val RECORDING_SCREENSHOT_PAUSE_MILLIS = 250L
    }
}

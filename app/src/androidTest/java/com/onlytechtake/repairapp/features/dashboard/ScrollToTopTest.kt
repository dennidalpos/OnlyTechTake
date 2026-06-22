package com.onlytechtake.repairapp.features.dashboard

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.onlytechtake.repairapp.MainActivity
import com.onlytechtake.repairapp.core.design.RepairAppTestTags
import com.onlytechtake.repairapp.demo.DemoDataSeeder
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScrollToTopTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setup() {
        runBlocking {
            DemoDataSeeder.seedDemoProfile(composeRule.activity.applicationContext)
        }
    }

    @Test
    fun ordersList_showsScrollToTop_afterScrollingDown() {
        // 1. Wait for Login and Perform it
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithTag(RepairAppTestTags.AuthUsernameField).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag(RepairAppTestTags.AuthUsernameField).performTextReplacement(DemoDataSeeder.DEMO_ADMIN_USERNAME)
        composeRule.onNodeWithTag(RepairAppTestTags.AuthPasswordField).performTextReplacement(DemoDataSeeder.DEMO_PASSWORD)
        composeRule.onNodeWithTag(RepairAppTestTags.AuthSubmitButton).performClick()

        // 2. Wait for Dashboard and Go to Orders tab
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithTag(RepairAppTestTags.dashboardTab("HOME")).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag(RepairAppTestTags.dashboardTab("ORDERS")).performClick()
        composeRule.waitForIdle()

        // 3. Verify ScrollToTop is NOT visible initially
        composeRule.onNodeWithTag(RepairAppTestTags.ScrollToTopButton).assertDoesNotExist()

        // 4. Scroll down the list
        composeRule.onNodeWithTag("orders_list").performTouchInput {
            swipeUp(durationMillis = 1000)
        }
        composeRule.waitForIdle()

        // 5. Verify ScrollToTop IS visible (if we scrolled enough)
        // With 8 items, it should be visible.
        composeRule.onNodeWithTag(RepairAppTestTags.ScrollToTopButton).assertIsDisplayed()

        // 6. Click ScrollToTop
        composeRule.onNodeWithTag(RepairAppTestTags.ScrollToTopButton).performClick()
        composeRule.waitForIdle()

        // 7. Verify ScrollToTop is NOT visible again (as we are at top)
        composeRule.onNodeWithTag(RepairAppTestTags.ScrollToTopButton).assertDoesNotExist()
    }
}

package com.onlytechtake.repairapp.features.dashboard

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
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
    val composeRule = createEmptyComposeRule()

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        runBlocking {
            DemoDataSeeder.seedDemoProfile(context)
        }
    }

    @Test
    fun ordersList_showsScrollToTop_afterScrollingDown() {
        ActivityScenario.launch(MainActivity::class.java).use {
            // 1. Wait for Login or Dashboard
            composeRule.waitUntil(30_000) {
                val isLoginVisible =
                    composeRule
                        .onAllNodesWithTag(RepairAppTestTags.AuthUsernameField)
                        .fetchSemanticsNodes()
                        .isNotEmpty()
                val isDashboardVisible =
                    composeRule
                        .onAllNodesWithTag(RepairAppTestTags.dashboardTab("HOME"))
                        .fetchSemanticsNodes()
                        .isNotEmpty()
                isLoginVisible || isDashboardVisible
            }

            val isLoginVisible =
                composeRule
                    .onAllNodesWithTag(RepairAppTestTags.AuthUsernameField)
                    .fetchSemanticsNodes()
                    .isNotEmpty()

            if (isLoginVisible) {
                composeRule
                    .onNodeWithTag(RepairAppTestTags.AuthUsernameField)
                    .performTextReplacement(DemoDataSeeder.DEMO_ADMIN_USERNAME)
                composeRule
                    .onNodeWithTag(RepairAppTestTags.AuthPasswordField)
                    .performTextReplacement(DemoDataSeeder.DEMO_PASSWORD)
                composeRule.onNodeWithTag(RepairAppTestTags.AuthSubmitButton).performClick()

                // 2. Wait for Dashboard after Login
                composeRule.waitUntil(30_000) {
                    composeRule
                        .onAllNodesWithTag(RepairAppTestTags.dashboardTab("HOME"))
                        .fetchSemanticsNodes()
                        .isNotEmpty()
                }
            }

            // 3. Go to Orders tab
            composeRule.onNodeWithTag(RepairAppTestTags.dashboardTab("ORDERS")).performClick()

            // Wait for items to load
            composeRule.waitUntil(30_000) {
                composeRule
                    .onAllNodesWithTag(RepairAppTestTags.orderCard(DemoDataSeeder.DEMO_ORDER_ID))
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }
            composeRule.waitForIdle()

            // 4. Verify ScrollToTop is NOT visible initially
            composeRule.onNodeWithTag(RepairAppTestTags.ScrollToTopButton).assertDoesNotExist()

            // 5. Scroll down the list
            composeRule.onNodeWithTag("orders_list").performScrollToIndex(25)
            composeRule.waitForIdle()

            // 6. Verify ScrollToTop IS visible
            composeRule.onNodeWithTag(RepairAppTestTags.ScrollToTopButton).assertIsDisplayed()

            // 7. Click ScrollToTop
            composeRule.onNodeWithTag(RepairAppTestTags.ScrollToTopButton).performClick()
            composeRule.waitForIdle()

            // 8. Verify ScrollToTop is NOT visible again
            composeRule.onNodeWithTag(RepairAppTestTags.ScrollToTopButton).assertDoesNotExist()
        }
    }
}

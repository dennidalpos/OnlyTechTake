package com.onlytechtake.repairapp.demo

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Minimal instrumental test to seed the database with demo data.
 * Useful for preparing the device for manual demo recordings.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class SeedDemoDataTest {
    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @get:Rule
    val localeRule = LocaleTestRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun seedDemoData() {
        runBlocking {
            DemoDataSeeder.seedDemoProfile(context, localeRule.locale)
        }
    }
}

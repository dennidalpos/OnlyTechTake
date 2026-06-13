package com.onlytechtake.repairapp.demo

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Minimal instrumental test to seed the database with demo data.
 * Useful for preparing the device for manual demo recordings.
 */
@RunWith(AndroidJUnit4::class)
class SeedDemoDataTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun seedDemoData() {
        runBlocking {
            DemoDataSeeder.seedDemoProfile(context)
        }
    }
}

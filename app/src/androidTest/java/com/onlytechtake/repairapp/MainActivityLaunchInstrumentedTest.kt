package com.onlytechtake.repairapp

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityLaunchInstrumentedTest {
    @Test
    fun mainActivityLaunchesOnDevice() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)

        try {
            scenario.onActivity { activity ->
                assertFalse(activity.isFinishing)
                assertFalse(activity.isDestroyed)
            }
        } finally {
            scenario.close()
        }
    }
}

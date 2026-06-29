package com.onlytechtake.repairapp.demo

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.onlytechtake.repairapp.RepairAppApplication
import com.onlytechtake.repairapp.core.di.DefaultAppContainer

object TestUtils {
    /**
     * Recreates the application container to ensure that all repositories and DAOs
     * are using the latest database instance. This is essential after seeding or
     * deleting the database during tests.
     */
    fun refreshAppContainer() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        if (context is RepairAppApplication) {
            context.appContainer.close()
            context.appContainer = DefaultAppContainer(context)
        }
    }
}

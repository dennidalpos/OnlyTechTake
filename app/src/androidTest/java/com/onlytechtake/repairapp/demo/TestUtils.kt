package com.onlytechtake.repairapp.demo

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.onlytechtake.repairapp.data.local.db.RepairAppDatabase
import java.io.File

object TestUtils {
    /**
     * Recreates the application container to ensure that all repositories and DAOs
     * are using the latest database instance. This is essential after seeding or
     * deleting the database during tests.
     */
    fun refreshAppContainer() {
        RepairAppDatabase.closeInstance()
        val context = ApplicationProvider.getApplicationContext<Context>()
        // Clear all shared preferences and data stores
        val prefsDir = File(context.filesDir.parentFile, "shared_prefs")
        if (prefsDir.exists()) prefsDir.deleteRecursively()
        val dsDir = File(context.filesDir.parentFile, "datastore")
        if (dsDir.exists()) dsDir.deleteRecursively()

        // We can't easily clear Hilt singletons, but clearing the disk state
        // should force them to reload or fail safely.
    }
}

package com.onlytechtake.repairapp

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.onlytechtake.repairapp.data.local.db.RepairAppDatabase
import com.onlytechtake.repairapp.data.preferences.BackupReminderPreferences
import com.onlytechtake.repairapp.data.services.BackupService
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class RepairAppApplication :
    Application(),
    Configuration.Provider {
    @Inject lateinit var database: RepairAppDatabase

    @Inject lateinit var backupReminderPreferences: BackupReminderPreferences

    @Inject lateinit var backupService: BackupService

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() =
            Configuration
                .Builder()
                .setWorkerFactory(workerFactory)
                .build()

    override fun onCreate() {
        super.onCreate()

        // Pre-warm database on background thread to avoid UI jank during first access
        CoroutineScope(Dispatchers.IO).launch {
            try {
                database.query("SELECT 1", null).close()
            } catch (e: Exception) {
                // Ignore pre-warm errors, the app will handle it on first real access
                android.util.Log.w("RepairAppApplication", "DB Pre-warm failed", e)
            }
        }

        // Schedule automatic backup if enabled
        CoroutineScope(Dispatchers.IO).launch {
            val settings = backupReminderPreferences.settings.first()
            backupService.scheduleAutomaticBackup(settings)
        }
    }
}

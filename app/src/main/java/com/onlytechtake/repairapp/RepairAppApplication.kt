package com.onlytechtake.repairapp

import android.app.Application
import com.onlytechtake.repairapp.core.di.AppContainer
import com.onlytechtake.repairapp.core.di.DefaultAppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class RepairAppApplication : Application() {
    lateinit var appContainer: AppContainer

    override fun onCreate() {
        super.onCreate()
        appContainer = DefaultAppContainer(this)

        // Pre-warm database on background thread to avoid UI jank during first access
        CoroutineScope(Dispatchers.IO).launch {
            try {
                appContainer.database.query("SELECT 1", null).close()
            } catch (e: Exception) {
                // Ignore pre-warm errors, the app will handle it on first real access
                android.util.Log.w("RepairAppApplication", "DB Pre-warm failed", e)
            }
        }

        // Schedule automatic backup if enabled
        CoroutineScope(Dispatchers.IO).launch {
            val settings = appContainer.backupReminderPreferences.settings.first()
            appContainer.backupService.scheduleAutomaticBackup(settings)
        }
    }
}

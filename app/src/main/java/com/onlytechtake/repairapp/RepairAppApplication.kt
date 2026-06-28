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

        // Schedule automatic backup if enabled
        CoroutineScope(Dispatchers.IO).launch {
            val settings = appContainer.backupReminderPreferences.settings.first()
            appContainer.backupService.scheduleAutomaticBackup(settings)
        }
    }
}

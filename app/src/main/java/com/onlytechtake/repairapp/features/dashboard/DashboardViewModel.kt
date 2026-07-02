package com.onlytechtake.repairapp.features.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.onlytechtake.repairapp.data.preferences.BackupReminderPreferences
import com.onlytechtake.repairapp.domain.backup.BackupReminderPolicy
import com.onlytechtake.repairapp.domain.models.AppSummary
import com.onlytechtake.repairapp.domain.repositories.RepairAppRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class DashboardViewModel(
    repository: RepairAppRepository,
    backupReminderPreferences: BackupReminderPreferences,
) : ViewModel() {
    val summary: StateFlow<AppSummary?> =
        repository
            .observeSummary()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = null,
            )

    val isBackupReminderDue: StateFlow<Boolean> =
        backupReminderPreferences.settings
            .map { settings ->
                BackupReminderPolicy.shouldShowReminder(settings, System.currentTimeMillis())
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = false,
            )

    class Factory(
        private val repository: RepairAppRepository,
        private val backupReminderPreferences: BackupReminderPreferences,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
                "Unknown ViewModel class: ${modelClass.name}"
            }
            return DashboardViewModel(repository, backupReminderPreferences) as T
        }
    }
}

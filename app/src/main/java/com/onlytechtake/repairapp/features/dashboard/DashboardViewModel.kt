package com.onlytechtake.repairapp.features.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onlytechtake.repairapp.data.preferences.BackupReminderPreferences
import com.onlytechtake.repairapp.domain.auth.AuthRepository
import com.onlytechtake.repairapp.domain.backup.BackupReminderPolicy
import com.onlytechtake.repairapp.domain.models.AppSummary
import com.onlytechtake.repairapp.domain.repositories.RepairAppRepository
import com.onlytechtake.repairapp.features.workspace.WorkspaceManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel
    @Inject
    constructor(
        repository: RepairAppRepository,
        backupReminderPreferences: BackupReminderPreferences,
        authRepository: AuthRepository,
        private val workspaceManager: WorkspaceManager,
    ) : ViewModel() {
        val currentUser = authRepository.currentUser
        val message = workspaceManager.message
        val isFormDirty = workspaceManager.isFormDirty
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

        fun clearMessage() {
            workspaceManager.clearMessage()
        }

        fun triggerReset() {
            workspaceManager.triggerReset()
        }

        fun triggerAutoSave() {
            workspaceManager.triggerAutoSave()
        }

        fun setFormDirty(dirty: Boolean) {
            workspaceManager.setFormDirty(dirty)
        }
    }

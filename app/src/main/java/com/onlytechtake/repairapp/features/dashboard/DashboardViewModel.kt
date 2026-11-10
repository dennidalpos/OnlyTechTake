package com.onlytechtake.repairapp.features.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onlytechtake.repairapp.data.preferences.BackupReminderPreferences
import com.onlytechtake.repairapp.domain.auth.AuthRepository
import com.onlytechtake.repairapp.domain.backup.BackupReminderPolicy
import com.onlytechtake.repairapp.domain.models.AppSummary
import com.onlytechtake.repairapp.domain.repositories.RepairAppRepository
import com.onlytechtake.repairapp.features.dashboard.DashboardNavRoute
import com.onlytechtake.repairapp.features.workspace.WorkspaceManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

        private val _pendingRoute = kotlinx.coroutines.flow.MutableStateFlow<DashboardNavRoute?>(null)
        val pendingRoute = _pendingRoute.asStateFlow()

        private val _pendingIsReset = kotlinx.coroutines.flow.MutableStateFlow(value = false)
        val pendingIsReset = _pendingIsReset.asStateFlow()

        private val _showUnsavedChangesDialog = kotlinx.coroutines.flow.MutableStateFlow(value = false)
        val showUnsavedChangesDialog = _showUnsavedChangesDialog.asStateFlow()

        fun setFormDirty(dirty: Boolean) {
            workspaceManager.setFormDirty(dirty)
        }

        fun setPendingNavigation(
            route: DashboardNavRoute?,
            isReset: Boolean = false,
        ) {
            _pendingRoute.value = route
            _pendingIsReset.value = isReset
            _showUnsavedChangesDialog.value = true
        }

        fun dismissUnsavedChangesDialog() {
            _showUnsavedChangesDialog.value = false
            _pendingRoute.value = null
            _pendingIsReset.value = false
        }

        fun confirmNavigation(performNavigation: (DashboardNavRoute, Boolean) -> Unit) {
            val route = _pendingRoute.value
            val isReset = _pendingIsReset.value
            _showUnsavedChangesDialog.value = false
            setFormDirty(false)
            if (route != null) {
                performNavigation(route, isReset)
            }
            _pendingRoute.value = null
            _pendingIsReset.value = false
        }
    }

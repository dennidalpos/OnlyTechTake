package com.onlytechtake.repairapp.features.dashboard.home

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.onlytechtake.repairapp.core.permissions.PermissionService
import com.onlytechtake.repairapp.data.preferences.BackupReminderPreferences
import com.onlytechtake.repairapp.data.repositories.BackupRepository
import com.onlytechtake.repairapp.domain.auth.AuthRepository
import com.onlytechtake.repairapp.domain.backup.BackupReminderPolicy
import com.onlytechtake.repairapp.domain.backup.BackupReminderSettings
import com.onlytechtake.repairapp.domain.repositories.OrderRepository
import com.onlytechtake.repairapp.domain.workspace.OrderFilter
import com.onlytechtake.repairapp.features.workspace.OperationalQueue
import com.onlytechtake.repairapp.features.workspace.buildOperationalQueues
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel
    @Inject
    constructor(
        authRepository: AuthRepository,
        orderRepository: OrderRepository,
        private val backupRepository: BackupRepository,
        backupReminderPreferences: BackupReminderPreferences,
    ) : ViewModel() {
        val currentUser = authRepository.currentUser

        val operationalQueues: StateFlow<List<OperationalQueue>?> =
            currentUser
                .flatMapLatest { user ->
                    if (user == null) {
                        flowOf(emptyList())
                    } else {
                        orderRepository.observeOrders(
                            user,
                            OrderFilter(),
                        )
                    }
                }.map(::buildOperationalQueues)
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

        val backupReminderSettings: StateFlow<BackupReminderSettings> =
            backupReminderPreferences.settings
                .stateIn(
                    viewModelScope,
                    SharingStarted.WhileSubscribed(5_000),
                    BackupReminderSettings(),
                )

        @Suppress("unused")
        val isBackupReminderDue: StateFlow<Boolean> =
            combine(
                currentUser,
                backupReminderSettings,
            ) { user, settings ->
                PermissionService.isAdmin(user) &&
                    BackupReminderPolicy.shouldShowReminder(
                        settings,
                        System.currentTimeMillis(),
                    )
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initialValue = false)

        @Suppress("unused")
        fun exportCompleteBackup(uri: Uri) {
            val user = currentUser.value ?: return
            viewModelScope.launch {
                backupRepository.exportCompleteBackup(user, uri)
            }
        }
    }

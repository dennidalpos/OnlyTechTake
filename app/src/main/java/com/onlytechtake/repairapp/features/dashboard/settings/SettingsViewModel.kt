package com.onlytechtake.repairapp.features.dashboard.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.onlytechtake.repairapp.core.templates.TemplateAnchor
import com.onlytechtake.repairapp.data.local.entities.MessageMode
import com.onlytechtake.repairapp.data.local.entities.UserPermission
import com.onlytechtake.repairapp.data.preferences.BackupReminderPreferences
import com.onlytechtake.repairapp.data.preferences.OwnerPreferences
import com.onlytechtake.repairapp.data.preferences.SecurityPreferences
import com.onlytechtake.repairapp.data.repositories.AdminUserRepository
import com.onlytechtake.repairapp.data.repositories.BackupRepository
import com.onlytechtake.repairapp.data.repositories.CatalogRepository
import com.onlytechtake.repairapp.data.repositories.DataResetRepository
import com.onlytechtake.repairapp.data.repositories.OrderFileCleanupService
import com.onlytechtake.repairapp.data.repositories.TemplateRepository
import com.onlytechtake.repairapp.data.services.DiagnosticLogService
import com.onlytechtake.repairapp.data.services.StorageStatsService
import com.onlytechtake.repairapp.domain.auth.AuthRepository
import com.onlytechtake.repairapp.domain.backup.BackupReminderPolicy
import com.onlytechtake.repairapp.domain.backup.BackupReminderSettings
import com.onlytechtake.repairapp.domain.workspace.OwnerInfo
import com.onlytechtake.repairapp.features.workspace.AuthenticatedActionRunner
import com.onlytechtake.repairapp.features.workspace.WorkspaceMessage
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModel(
    context: Context,
    private val authRepository: AuthRepository,
    private val adminUserRepository: AdminUserRepository,
    catalogRepository: CatalogRepository,
    private val templateRepository: TemplateRepository,
    backupRepository: BackupRepository,
    private val dataResetRepository: DataResetRepository,
    backupReminderPreferences: BackupReminderPreferences,
    ownerPreferences: OwnerPreferences,
    securityPreferences: SecurityPreferences,
    private val storageStatsService: StorageStatsService,
    diagnosticLogService: DiagnosticLogService,
    orderFileCleanupService: OrderFileCleanupService,
    diagnosticLogger: com.onlytechtake.repairapp.core.error.DiagnosticLogger,
    database: com.onlytechtake.repairapp.data.local.db.RepairAppDatabase,
    private val onSetFormDirty: (Boolean) -> Unit,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {
    private val _message = MutableStateFlow<WorkspaceMessage?>(null)
    val message = _message.asStateFlow()

    private val _isProcessing = MutableStateFlow(value = false)
    val isProcessing = _isProcessing.asStateFlow()

    private val _recoveryCode = MutableStateFlow<String?>(null)
    val recoveryCode = _recoveryCode.asStateFlow()

    private val actionRunner =
        AuthenticatedActionRunner(
            context = context.applicationContext,
            authRepository = authRepository,
            currentUser = authRepository.currentUser,
            scope = viewModelScope,
            showMessage = { text, isError -> _message.value = WorkspaceMessage(text, isError) },
            setLoading = { _isProcessing.value = it },
            diagnosticLogger = diagnosticLogger,
            ioDispatcher = ioDispatcher,
        )

    private val catalogDelegate = CatalogSettingsDelegate(catalogRepository, actionRunner)
    private val templateDelegate = TemplateSettingsDelegate(templateRepository, actionRunner)
    private val backupDelegate =
        BackupSettingsDelegate(
            backupRepository,
            backupReminderPreferences,
            actionRunner,
            viewModelScope,
        )
    private val securityDelegate = SecuritySettingsDelegate(securityPreferences, viewModelScope)
    private val adminDelegate = AdminSettingsDelegate(adminUserRepository, actionRunner)
    private val storageDelegate =
        StorageSettingsDelegate(
            orderFileCleanupService,
            database,
            actionRunner,
            viewModelScope,
            ::refreshStorageStats,
            ioDispatcher,
        )
    private val diagnosticDelegate =
        DiagnosticSettingsDelegate(
            diagnosticLogService,
            securityPreferences,
            actionRunner,
            viewModelScope,
            ::refreshStorageStats,
            ioDispatcher,
        )

    private val ownerDelegate =
        OwnerSettingsDelegate(ownerPreferences, actionRunner)

    fun initializeTemplatesIfNeeded() {
        viewModelScope.launch {
            val user = authRepository.currentUser.value
            templateRepository.ensureDefaults(user?.id)
        }
    }

    fun clearMessage() {
        _message.value = null
    }

    fun setFormDirty(dirty: Boolean) {
        onSetFormDirty(dirty)
    }

    private val _storageStats =
        MutableStateFlow<com.onlytechtake.repairapp.data.services.StorageStats?>(null)
    val storageStats = _storageStats.asStateFlow()

    val currentUser = authRepository.currentUser
    val users =
        adminUserRepository
            .observeUsers()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val categories =
        catalogRepository
            .observeCategories()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val templates =
        templateRepository
            .observeTemplates()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val anchorMappings =
        templateRepository.observeAnchorMappings().stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            emptyMap(),
        )
    val messageMode =
        templateRepository.observeMessageMode().stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            MessageMode.MANUAL_WHATSAPP,
        )
    val backupReminderSettings =
        backupReminderPreferences.settings.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            BackupReminderSettings(),
        )
    val securitySettings =
        securityPreferences.settings.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            com.onlytechtake.repairapp.data.preferences
                .SecuritySettings(),
        )

    val ownerInfo: kotlinx.coroutines.flow.StateFlow<OwnerInfo> =
        ownerPreferences.ownerInfo
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), OwnerInfo())

    val isBackupReminderDue =
        backupReminderPreferences.settings
            .map { settings ->
                BackupReminderPolicy.shouldShowReminder(settings, System.currentTimeMillis())
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initialValue = false)

    init {
        refreshStorageStats()
    }

    fun refreshStorageStats() {
        viewModelScope.launch {
            val stats =
                withContext(ioDispatcher) {
                    storageStatsService.getStorageStats()
                }
            _storageStats.value = stats
        }
    }

    fun cleanupCache() = storageDelegate.cleanupCache()

    fun cleanupOrphanedFiles() = storageDelegate.cleanupOrphanedFiles()

    fun clearDiagnosticLogs() = diagnosticDelegate.clearLogs()

    fun shareDiagnosticLogs() = diagnosticDelegate.shareLogs()

    fun saveCategory(
        id: String?,
        parentId: String?,
        name: String,
        description: String,
        active: Boolean,
        onSaved: (String) -> Unit = {},
    ) = catalogDelegate.saveCategory(id, parentId, name, description, active) {
        setFormDirty(dirty = false)
        onSaved(it)
    }

    fun deleteCategory(categoryId: String) = catalogDelegate.deleteCategory(categoryId)

    fun saveTemplate(
        code: String,
        title: String,
        body: String,
        active: Boolean,
        step: String? = null,
    ) = templateDelegate.saveTemplate(code, title, body, active, step).also {
        setFormDirty(dirty = false)
    }

    fun setTemplateForAnchor(
        anchor: TemplateAnchor,
        templateCode: String,
    ) = templateDelegate.setTemplateForAnchor(anchor, templateCode)

    fun resetTemplatesToDefaults() = templateDelegate.resetTemplatesToDefaults()

    fun setMessageMode(mode: MessageMode) = templateDelegate.setMessageMode(mode)

    fun setBackupReminderEnabled(enabled: Boolean) = backupDelegate.setBackupReminderEnabled(enabled)

    fun setBackupReminderIntervalDays(days: Int) = backupDelegate.setBackupReminderIntervalDays(days)

    fun setBackupEncryptionEnabled(enabled: Boolean) = securityDelegate.setBackupEncryptionEnabled(enabled)

    fun setBackupEncryptionPassword(password: String) = securityDelegate.setBackupEncryptionPassword(password)

    fun setInactivityTimeoutMinutes(minutes: Int) = securityDelegate.setInactivityTimeoutMinutes(minutes)

    fun updateOwnerInfo(info: OwnerInfo) = ownerDelegate.updateOwnerInfo(info)

    fun createOperator(
        displayName: String,
        username: String,
        password: CharArray,
    ) = adminDelegate.createOperator(displayName, username, password)

    fun setUserActive(
        userId: String,
        active: Boolean,
    ) = adminDelegate.setUserActive(userId, active)

    fun setUserPermissions(
        userId: String,
        permissions: Set<UserPermission>,
    ) = adminDelegate.setUserPermissions(userId, permissions)

    fun deleteUser(userId: String) =
        actionRunner.run { actor ->
            adminUserRepository.deleteUser(actor, userId)
        }

    fun changePassword(
        username: String,
        currentPassword: CharArray,
        newPassword: CharArray,
    ) = actionRunner.run {
        authRepository.changePassword(username, currentPassword, newPassword)
    }

    fun resetPassword(
        userId: String,
        password: CharArray,
    ) = adminDelegate.resetPassword(userId, password)

    fun resetAllData(
        confirmation: String,
        onReset: () -> Unit,
    ) = actionRunner.run(onSuccess = onReset) { actor ->
        dataResetRepository.resetAll(actor, confirmation)
    }

    fun resetOrders(
        confirmation: String,
        onReset: () -> Unit,
    ) = actionRunner.run(onSuccess = onReset) { actor ->
        dataResetRepository.resetOrders(actor, confirmation)
    }

    fun generateRecoveryKit() {
        viewModelScope.launch {
            authRepository
                .getRecoveryKit()
                .onSuccess { code ->
                    _recoveryCode.value = code
                }.onFailure { error ->
                    _message.value = WorkspaceMessage(error.message ?: "Errore", isError = true)
                }
        }
    }

    fun clearRecoveryCode() {
        _recoveryCode.value = null
    }

    fun exportCompleteBackup(uri: Uri) = backupDelegate.exportCompleteBackup(uri)

    fun exportOrdersXlsx(uri: Uri) = backupDelegate.exportOrdersXlsx(uri)

    fun restoreCompleteBackup(
        uri: Uri,
        onRestored: () -> Unit,
    ) = backupDelegate.restoreCompleteBackup(uri, onRestored)

    class Factory(
        private val context: Context,
        private val authRepository: AuthRepository,
        private val adminUserRepository: AdminUserRepository,
        private val catalogRepository: CatalogRepository,
        private val templateRepository: TemplateRepository,
        private val backupRepository: BackupRepository,
        private val dataResetRepository: DataResetRepository,
        private val backupReminderPreferences: BackupReminderPreferences,
        private val ownerPreferences: OwnerPreferences,
        private val securityPreferences: SecurityPreferences,
        private val storageStatsService: StorageStatsService,
        private val diagnosticLogService: DiagnosticLogService,
        private val orderFileCleanupService: OrderFileCleanupService,
        private val diagnosticLogger: com.onlytechtake.repairapp.core.error.DiagnosticLogger,
        private val database: com.onlytechtake.repairapp.data.local.db.RepairAppDatabase,
        private val onSetFormDirty: (Boolean) -> Unit,
        private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            SettingsViewModel(
                context,
                authRepository,
                adminUserRepository,
                catalogRepository,
                templateRepository,
                backupRepository,
                dataResetRepository,
                backupReminderPreferences,
                ownerPreferences,
                securityPreferences,
                storageStatsService,
                diagnosticLogService,
                orderFileCleanupService,
                diagnosticLogger,
                database,
                onSetFormDirty,
                ioDispatcher,
            ) as T
    }
}

package com.onlytechtake.repairapp.features.dashboard.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.onlytechtake.repairapp.core.templates.TemplateAnchor
import com.onlytechtake.repairapp.data.local.entities.MessageMode
import com.onlytechtake.repairapp.data.local.entities.UserPermission
import com.onlytechtake.repairapp.data.preferences.BackupReminderPreferences
import com.onlytechtake.repairapp.data.preferences.SecurityPreferences
import com.onlytechtake.repairapp.data.repositories.AdminUserRepository
import com.onlytechtake.repairapp.data.repositories.BackupRepository
import com.onlytechtake.repairapp.data.repositories.CatalogRepository
import com.onlytechtake.repairapp.data.repositories.OrderFileCleanupService
import com.onlytechtake.repairapp.data.repositories.TemplateRepository
import com.onlytechtake.repairapp.data.services.StorageStatsService
import com.onlytechtake.repairapp.domain.auth.AuthRepository
import com.onlytechtake.repairapp.domain.backup.BackupReminderSettings
import com.onlytechtake.repairapp.domain.workspace.OperatorInput
import com.onlytechtake.repairapp.features.workspace.AuthenticatedActionRunner
import com.onlytechtake.repairapp.features.workspace.WorkspaceMessage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModel(
    private val authRepository: AuthRepository,
    private val adminUserRepository: AdminUserRepository,
    private val catalogRepository: CatalogRepository,
    private val templateRepository: TemplateRepository,
    private val backupRepository: BackupRepository,
    private val backupReminderPreferences: BackupReminderPreferences,
    private val securityPreferences: SecurityPreferences,
    private val storageStatsService: StorageStatsService,
    private val orderFileCleanupService: OrderFileCleanupService,
    private val database: com.onlytechtake.repairapp.data.local.db.RepairAppDatabase,
    private val onSetFormDirty: (Boolean) -> Unit,
) : ViewModel() {

    private val _message = MutableStateFlow<WorkspaceMessage?>(null)
    val message = _message.asStateFlow()

    private val actionRunner = AuthenticatedActionRunner(
        authRepository = authRepository,
        currentUser = authRepository.currentUser,
        scope = viewModelScope,
        showMessage = { text, isError -> _message.value = WorkspaceMessage(text, isError) }
    )

    private val catalogDelegate = CatalogSettingsDelegate(catalogRepository, actionRunner)
    private val templateDelegate = TemplateSettingsDelegate(templateRepository, actionRunner)
    private val backupDelegate = BackupSettingsDelegate(backupRepository, backupReminderPreferences, actionRunner, viewModelScope)
    private val securityDelegate = SecuritySettingsDelegate(securityPreferences, viewModelScope)
    private val adminDelegate = AdminSettingsDelegate(adminUserRepository, actionRunner)
    private val storageDelegate = StorageSettingsDelegate(storageStatsService, orderFileCleanupService, database, actionRunner, viewModelScope, ::refreshStorageStats)

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

    private val _storageStats = MutableStateFlow<com.onlytechtake.repairapp.data.services.StorageStats?>(null)
    val storageStats = _storageStats.asStateFlow()

    private val _exportDestination = MutableStateFlow(com.onlytechtake.repairapp.data.local.entities.ExportDestination.LOCAL)
    val exportDestination = _exportDestination.asStateFlow()

    private val _isCloudAvailable = MutableStateFlow(value = false)
    val isCloudAvailable = _isCloudAvailable.asStateFlow()

    val currentUser = authRepository.currentUser
    val users = adminUserRepository.observeUsers().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val categories = catalogRepository.observeCategories().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val templates = templateRepository.observeTemplates().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val anchorMappings = templateRepository.observeAnchorMappings().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())
    val messageMode = templateRepository.observeMessageMode().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MessageMode.MANUAL_WHATSAPP)
    val backupReminderSettings = backupReminderPreferences.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BackupReminderSettings())
    val securitySettings = securityPreferences.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), com.onlytechtake.repairapp.data.preferences.SecuritySettings())

    init {
        refreshStorageStats()
        viewModelScope.launch {
            _isCloudAvailable.value = backupRepository.isCloudProviderAvailable()
            if (_isCloudAvailable.value) {
                _exportDestination.value = com.onlytechtake.repairapp.data.local.entities.ExportDestination.CLOUD
            }
        }
    }

    fun refreshStorageStats() {
        viewModelScope.launch {
            _storageStats.value = storageStatsService.getStorageStats()
        }
    }

    fun cleanupCache() = storageDelegate.cleanupCache()

    fun cleanupOrphanedFiles() = storageDelegate.cleanupOrphanedFiles()

    fun saveCategory(id: String?, parentId: String?, name: String, description: String, active: Boolean, onSaved: (String) -> Unit = {}) =
        catalogDelegate.saveCategory(id, parentId, name, description, active) {
            setFormDirty(false)
            onSaved(it)
        }

    fun deleteCategory(categoryId: String) = catalogDelegate.deleteCategory(categoryId)

    fun saveTemplate(code: String, title: String, body: String, active: Boolean, step: String? = null) =
        templateDelegate.saveTemplate(code, title, body, active, step).also {
            setFormDirty(false)
        }

    fun setTemplateForAnchor(anchor: TemplateAnchor, templateCode: String) =
        templateDelegate.setTemplateForAnchor(anchor, templateCode)

    fun setMessageMode(mode: MessageMode) = templateDelegate.setMessageMode(mode)

    fun setExportDestination(destination: com.onlytechtake.repairapp.data.local.entities.ExportDestination) {
        _exportDestination.value = destination
    }

    fun setBackupReminderEnabled(enabled: Boolean) = backupDelegate.setBackupReminderEnabled(enabled)

    fun setBackupReminderIntervalDays(days: Int) = backupDelegate.setBackupReminderIntervalDays(days)

    fun setBackupEncryptionEnabled(enabled: Boolean) = securityDelegate.setBackupEncryptionEnabled(enabled)

    fun setBackupEncryptionPassword(password: String) = securityDelegate.setBackupEncryptionPassword(password)

    fun setDatabaseEncryptionEnabled(enabled: Boolean) = securityDelegate.setDatabaseEncryptionEnabled(enabled)

    fun createOperator(displayName: String, username: String, password: CharArray) =
        adminDelegate.createOperator(displayName, username, password)

    fun setUserActive(userId: String, active: Boolean) = adminDelegate.setUserActive(userId, active)

    fun setUserPermissions(userId: String, permissions: Set<UserPermission>) =
        adminDelegate.setUserPermissions(userId, permissions)

    fun resetPassword(userId: String, password: CharArray) = adminDelegate.resetPassword(userId, password)

    fun exportConfiguration(uri: Uri) = backupDelegate.exportConfiguration(uri)

    fun exportDatabase(uri: Uri) = backupDelegate.exportDatabase(uri)

    fun exportCompleteBackup(uri: Uri) = backupDelegate.exportCompleteBackup(uri)

    fun exportOrdersXlsx(uri: Uri) = backupDelegate.exportOrdersXlsx(uri)

    fun restoreCompleteBackup(uri: Uri, onRestored: () -> Unit) =
        backupDelegate.restoreCompleteBackup(uri, onRestored)


    class Factory(
        private val authRepository: AuthRepository,
        private val adminUserRepository: AdminUserRepository,
        private val catalogRepository: CatalogRepository,
        private val templateRepository: TemplateRepository,
        private val backupRepository: BackupRepository,
        private val backupReminderPreferences: BackupReminderPreferences,
        private val securityPreferences: SecurityPreferences,
        private val storageStatsService: StorageStatsService,
        private val orderFileCleanupService: OrderFileCleanupService,
        private val database: com.onlytechtake.repairapp.data.local.db.RepairAppDatabase,
        private val onSetFormDirty: (Boolean) -> Unit,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(
                authRepository, adminUserRepository, catalogRepository,
                templateRepository, backupRepository, backupReminderPreferences,
                securityPreferences,
                storageStatsService, orderFileCleanupService,
                database,
                onSetFormDirty
            ) as T
        }
    }
}

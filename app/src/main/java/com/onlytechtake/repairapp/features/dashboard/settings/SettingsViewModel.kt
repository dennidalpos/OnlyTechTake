package com.onlytechtake.repairapp.features.dashboard.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
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
import com.onlytechtake.repairapp.domain.workspace.CategoryInput
import com.onlytechtake.repairapp.domain.workspace.OperatorInput
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
) : ViewModel() {

    private val _message = MutableStateFlow<WorkspaceMessage?>(null)
    val message = _message.asStateFlow()

    fun initializeTemplatesIfNeeded() {
        viewModelScope.launch {
            val user = authRepository.currentUser.value
            templateRepository.ensureDefaults(user?.id)
        }
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

    fun cleanupCache() {
        viewModelScope.launch {
            orderFileCleanupService.cleanupSharedCache()
            refreshStorageStats()
        }
    }

    fun saveCategory(id: String?, parentId: String?, name: String, description: String, active: Boolean, onSaved: (String) -> Unit = {}) {
        launchWithUser(onSuccess = { 
            _message.value = WorkspaceMessage("Operazione completata", false) 
        }) { actor ->
            catalogRepository.saveCategory(
                actor,
                CategoryInput(id, parentId, name, description, active)
            ).onSuccess { onSaved(it) }
        }
    }

    fun deleteCategory(categoryId: String) {
        launchWithUser(onSuccess = { 
            _message.value = WorkspaceMessage("Operazione completata", false) 
        }) { actor ->
            catalogRepository.deleteCategory(actor, categoryId)
        }
    }

    fun saveTemplate(code: String, title: String, body: String, active: Boolean, step: String? = null) {
        launchWithUser(onSuccess = { 
            _message.value = WorkspaceMessage("Operazione completata", false) 
        }) { actor ->
            templateRepository.saveTemplate(actor, code, title, body, active, step)
        }
    }

    fun setMessageMode(mode: MessageMode) {
        launchWithUser { actor ->
            templateRepository.setMessageMode(actor, mode)
        }
    }

    fun setExportDestination(destination: com.onlytechtake.repairapp.data.local.entities.ExportDestination) {
        _exportDestination.value = destination
    }

    fun setBackupReminderEnabled(enabled: Boolean) {
        viewModelScope.launch {
            backupReminderPreferences.setEnabled(enabled)
        }
    }

    fun setBackupReminderIntervalDays(days: Int) {
        viewModelScope.launch {
            backupReminderPreferences.setIntervalDays(days)
        }
    }

    fun setBackupEncryptionEnabled(enabled: Boolean) {
        viewModelScope.launch {
            securityPreferences.setBackupEncryptionEnabled(enabled)
        }
    }

    fun setBackupEncryptionPassword(password: String) {
        viewModelScope.launch {
            securityPreferences.setBackupEncryptionPassword(password)
        }
    }

    fun setDatabaseEncryptionEnabled(enabled: Boolean) {
        viewModelScope.launch {
            securityPreferences.setDatabaseEncryptionEnabled(enabled)
        }
    }

    fun createOperator(displayName: String, username: String, password: CharArray) {
        launchWithUser { actor ->
            adminUserRepository.createOperator(actor, OperatorInput(displayName, username, password))
        }
    }

    fun setUserActive(userId: String, active: Boolean) {
        launchWithUser { actor ->
            adminUserRepository.setActive(actor, userId, active)
        }
    }

    fun setUserPermissions(userId: String, permissions: Set<UserPermission>) {
        launchWithUser { actor ->
            adminUserRepository.setPermissions(actor, userId, permissions)
        }
    }

    fun resetPassword(userId: String, password: CharArray) {
        launchWithUser { actor ->
            adminUserRepository.resetPassword(actor, userId, password)
        }
    }

    fun exportConfiguration(uri: Uri) {
        launchWithUser { actor ->
            backupRepository.exportConfiguration(actor, uri)
        }
    }

    fun exportDatabase(uri: Uri) {
        launchWithUser { actor ->
            backupRepository.exportDatabase(actor, uri)
        }
    }

    fun exportCompleteBackup(uri: Uri) {
        launchWithUser { actor ->
            backupRepository.exportCompleteBackup(actor, uri)
        }
    }

    fun exportOrdersXlsx(uri: Uri) {
        launchWithUser { actor ->
            backupRepository.exportOrdersXlsx(actor, uri)
        }
    }

    fun restoreCompleteBackup(uri: Uri, onRestored: () -> Unit) {
        launchWithUser(onSuccess = onRestored) { actor ->
            backupRepository.restoreCompleteBackup(actor, uri)
        }
    }

    private fun launchWithUser(
        onSuccess: () -> Unit = {},
        block: suspend (com.onlytechtake.repairapp.domain.auth.AuthenticatedUser) -> Result<*>
    ) {
        val user = currentUser.value
        if (user != null) {
            viewModelScope.launch {
                block(user).fold(
                    onSuccess = { onSuccess() },
                    onFailure = { error -> _message.value = WorkspaceMessage(error.message ?: "Errore", true) }
                )
            }
        }
    }

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
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(
                authRepository, adminUserRepository, catalogRepository,
                templateRepository, backupRepository, backupReminderPreferences,
                securityPreferences,
                storageStatsService, orderFileCleanupService
            ) as T
        }
    }
}

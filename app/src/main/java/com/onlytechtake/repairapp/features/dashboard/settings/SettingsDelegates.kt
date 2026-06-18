package com.onlytechtake.repairapp.features.dashboard.settings

import android.net.Uri
import com.onlytechtake.repairapp.core.permissions.PermissionService
import com.onlytechtake.repairapp.core.templates.TemplateAnchor
import com.onlytechtake.repairapp.data.local.db.RepairAppDatabase
import com.onlytechtake.repairapp.data.local.entities.MessageMode
import com.onlytechtake.repairapp.data.local.entities.UserPermission
import com.onlytechtake.repairapp.data.preferences.BackupReminderPreferences
import com.onlytechtake.repairapp.data.preferences.SecurityPreferences
import com.onlytechtake.repairapp.data.repositories.*
import com.onlytechtake.repairapp.data.services.StorageStatsService
import com.onlytechtake.repairapp.domain.workspace.CategoryInput
import com.onlytechtake.repairapp.domain.workspace.OperatorInput
import com.onlytechtake.repairapp.features.workspace.AuthenticatedActionRunner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal class CatalogSettingsDelegate(
    private val catalogRepository: CatalogRepository,
    private val actionRunner: AuthenticatedActionRunner,
) {
    fun saveCategory(
        id: String?,
        parentId: String?,
        name: String,
        description: String,
        active: Boolean,
        onSaved: (String) -> Unit = {},
    ) {
        actionRunner.run(successMessage = "Operazione completata") { actor ->
            catalogRepository
                .saveCategory(
                    actor,
                    CategoryInput(id, parentId, name, description, active),
                ).onSuccess { onSaved(it) }
            Result.success(Unit)
        }
    }

    fun deleteCategory(categoryId: String) {
        actionRunner.run { actor ->
            catalogRepository.deleteCategory(actor, categoryId)
        }
    }
}

internal class TemplateSettingsDelegate(
    private val templateRepository: TemplateRepository,
    private val actionRunner: AuthenticatedActionRunner,
) {
    fun saveTemplate(
        code: String,
        title: String,
        body: String,
        active: Boolean,
        step: String? = null,
    ) {
        actionRunner.run(successMessage = "Operazione completata") { actor ->
            templateRepository.saveTemplate(actor, code, title, body, active, step)
        }
    }

    fun setTemplateForAnchor(
        anchor: TemplateAnchor,
        templateCode: String,
    ) {
        actionRunner.run(successMessage = "Associazione aggiornata") { actor ->
            templateRepository.setTemplateForAnchor(actor, anchor, templateCode)
        }
    }

    fun resetTemplatesToDefaults() {
        actionRunner.run(successMessage = "Template ripristinati") { actor ->
            templateRepository.resetToDefaults(actor)
        }
    }

    fun setMessageMode(mode: MessageMode) {
        actionRunner.run { actor ->
            templateRepository.setMessageMode(actor, mode)
        }
    }
}

internal class BackupSettingsDelegate(
    private val backupRepository: BackupRepository,
    private val backupReminderPreferences: BackupReminderPreferences,
    private val actionRunner: AuthenticatedActionRunner,
    private val scope: CoroutineScope,
) {
    fun setBackupReminderEnabled(enabled: Boolean) {
        scope.launch {
            backupReminderPreferences.setEnabled(enabled)
        }
    }

    fun setBackupReminderIntervalDays(days: Int) {
        scope.launch {
            backupReminderPreferences.setIntervalDays(days)
        }
    }

    fun exportConfiguration(uri: Uri) {
        actionRunner.run { actor ->
            backupRepository.exportConfiguration(actor, uri)
        }
    }

    fun exportDatabase(uri: Uri) {
        actionRunner.run { actor ->
            backupRepository.exportDatabase(actor, uri)
        }
    }

    fun exportCompleteBackup(uri: Uri) {
        actionRunner.run { actor ->
            backupRepository.exportCompleteBackup(actor, uri)
        }
    }

    fun exportOrdersXlsx(uri: Uri) {
        actionRunner.run { actor ->
            backupRepository.exportOrdersXlsx(actor, uri)
        }
    }

    fun restoreCompleteBackup(
        uri: Uri,
        onRestored: () -> Unit,
    ) {
        actionRunner.run(onSuccess = onRestored) { actor ->
            backupRepository.restoreCompleteBackup(actor, uri)
        }
    }
}

internal class SecuritySettingsDelegate(
    private val securityPreferences: SecurityPreferences,
    private val scope: CoroutineScope,
) {
    fun setBackupEncryptionEnabled(enabled: Boolean) {
        scope.launch {
            securityPreferences.setBackupEncryptionEnabled(enabled)
        }
    }

    fun setBackupEncryptionPassword(password: String) {
        scope.launch {
            securityPreferences.setBackupEncryptionPassword(password)
        }
    }

    fun setDatabaseEncryptionEnabled(enabled: Boolean) {
        scope.launch {
            securityPreferences.setDatabaseEncryptionEnabled(enabled)
        }
    }
}

internal class AdminSettingsDelegate(
    private val adminUserRepository: AdminUserRepository,
    private val actionRunner: AuthenticatedActionRunner,
) {
    fun createOperator(
        displayName: String,
        username: String,
        password: CharArray,
    ) {
        actionRunner.run { actor ->
            adminUserRepository.createOperator(actor, OperatorInput(displayName, username, password))
        }
    }

    fun setUserActive(
        userId: String,
        active: Boolean,
    ) {
        actionRunner.run { actor ->
            adminUserRepository.setActive(actor, userId, active)
        }
    }

    fun setUserPermissions(
        userId: String,
        permissions: Set<UserPermission>,
    ) {
        actionRunner.run { actor ->
            adminUserRepository.setPermissions(actor, userId, permissions)
        }
    }

    fun resetPassword(
        userId: String,
        password: CharArray,
    ) {
        actionRunner.run { actor ->
            adminUserRepository.resetPassword(actor, userId, password)
        }
    }
}

internal class StorageSettingsDelegate(
    private val storageStatsService: StorageStatsService,
    private val orderFileCleanupService: OrderFileCleanupService,
    private val database: RepairAppDatabase,
    private val actionRunner: AuthenticatedActionRunner,
    private val scope: CoroutineScope,
    private val onRefreshStats: () -> Unit,
) {
    fun cleanupCache() {
        scope.launch {
            orderFileCleanupService.cleanupSharedCache()
            onRefreshStats()
        }
    }

    fun cleanupOrphanedFiles() {
        actionRunner.run(successMessage = "Pulizia file orfani completata") { actor ->
            runCatching {
                PermissionService.requirePermission(actor, UserPermission.MANAGE_STORAGE)
                val paths = mutableSetOf<String>()
                paths.addAll(database.orderMediaDao().getAllPaths())
                paths.addAll(database.signatureDao().getAllPaths())
                paths.addAll(database.orderDocumentDao().getAllPaths())
                orderFileCleanupService.cleanupOrphanedFiles(paths)
                onRefreshStats()
            }
        }
    }
}

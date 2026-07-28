package com.onlytechtake.repairapp.features.dashboard.settings

import android.net.Uri
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.core.permissions.PermissionService
import com.onlytechtake.repairapp.core.templates.TemplateAnchor
import com.onlytechtake.repairapp.data.auth.AdminUserRepository
import com.onlytechtake.repairapp.data.backup.BackupRepository
import com.onlytechtake.repairapp.data.local.db.RepairAppDatabase
import com.onlytechtake.repairapp.data.orders.OrderFileCleanupService
import com.onlytechtake.repairapp.data.preferences.BackupReminderPreferences
import com.onlytechtake.repairapp.data.preferences.OwnerPreferences
import com.onlytechtake.repairapp.data.preferences.SecurityPreferences
import com.onlytechtake.repairapp.data.preferences.UiPreferences
import com.onlytechtake.repairapp.data.services.BackupService
import com.onlytechtake.repairapp.data.services.DiagnosticLogService
import com.onlytechtake.repairapp.domain.models.CategoryInput
import com.onlytechtake.repairapp.domain.models.MessageMode
import com.onlytechtake.repairapp.domain.models.OperatorInput
import com.onlytechtake.repairapp.domain.models.OwnerInfo
import com.onlytechtake.repairapp.domain.models.UserPermission
import com.onlytechtake.repairapp.domain.repositories.CatalogRepository
import com.onlytechtake.repairapp.domain.repositories.TemplateRepository
import com.onlytechtake.repairapp.features.workspace.AuthenticatedActionRunner
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class CatalogSettingsDelegate(
    private val catalogRepository: CatalogRepository,
    private val actionRunner: AuthenticatedActionRunner,
    private val scope: CoroutineScope,
) {
    fun saveCategory(
        id: String?,
        parentId: String?,
        name: String,
        description: String,
        active: Boolean,
        onSaved: (String) -> Unit = {},
    ) {
        actionRunner.run(
            scope = scope,
            successMessageRes = R.string.success_operation_completed,
            autoDismissMillis = 1000L,
        ) { actor ->
            catalogRepository
                .saveCategory(
                    actor,
                    CategoryInput(id, parentId, name, description, active),
                ).onSuccess { onSaved(it) }
            Result.success(Unit)
        }
    }

    fun deleteCategory(categoryId: String) {
        actionRunner.run(scope = scope) { actor ->
            catalogRepository.deleteCategory(actor, categoryId)
        }
    }
}

internal class TemplateSettingsDelegate(
    private val templateRepository: TemplateRepository,
    private val actionRunner: AuthenticatedActionRunner,
    private val scope: CoroutineScope,
) {
    fun saveTemplate(
        code: String,
        title: String,
        body: String,
        active: Boolean,
        step: String? = null,
    ) {
        actionRunner.run(
            scope = scope,
            successMessageRes = R.string.success_operation_completed,
            autoDismissMillis = 1000L,
        ) { actor ->
            templateRepository.saveTemplate(actor, code, title, body, active, step)
        }
    }

    fun setTemplateForAnchor(
        anchor: TemplateAnchor,
        templateCode: String,
    ) {
        actionRunner.run(
            scope = scope,
            successMessageRes = R.string.success_association_updated,
            autoDismissMillis = 1000L,
        ) { actor ->
            templateRepository.setTemplateForAnchor(actor, anchor, templateCode)
        }
    }

    fun resetTemplatesToDefaults() {
        actionRunner.run(
            scope = scope,
            successMessageRes = R.string.success_templates_restored,
            autoDismissMillis = 1000L,
        ) { actor ->
            templateRepository.resetToDefaults(actor)
        }
    }

    fun setMessageMode(mode: MessageMode) {
        actionRunner.run(scope = scope) { actor ->
            templateRepository.setMessageMode(actor, mode)
        }
    }
}

internal class BackupSettingsDelegate(
    private val backupRepository: BackupRepository,
    private val backupReminderPreferences: BackupReminderPreferences,
    private val backupService: BackupService,
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

    fun setAutomaticBackupEnabled(enabled: Boolean) {
        scope.launch {
            backupReminderPreferences.setAutomatic(enabled)
            val settings = backupReminderPreferences.settings.first()
            backupService.scheduleAutomaticBackup(settings)
        }
    }

    fun setBackupUri(uri: Uri?) {
        scope.launch {
            backupReminderPreferences.setBackupUri(uri?.toString())
            val settings = backupReminderPreferences.settings.first()
            backupService.scheduleAutomaticBackup(settings)
        }
    }

    fun setBackupScheduledTime(
        hour: Int,
        minute: Int,
    ) {
        scope.launch {
            backupReminderPreferences.setScheduledTime(hour, minute)
            val settings = backupReminderPreferences.settings.first()
            backupService.scheduleAutomaticBackup(settings)
        }
    }

    fun runBackupNow(onSuccess: () -> Unit = {}) {
        actionRunner.run(
            scope = scope,
            successMessageRes = R.string.success_operation_completed,
            autoDismissMillis = 1000L,
            onSuccess = onSuccess,
        ) { actor ->
            backupService.runAutomaticBackup(actor)
        }
    }

    fun exportCompleteBackup(uri: Uri) {
        actionRunner.run(scope = scope) { actor ->
            backupRepository.exportCompleteBackup(actor, uri)
        }
    }

    fun exportOrdersXlsx(uri: Uri) {
        actionRunner.run(scope = scope) { actor ->
            backupRepository.exportOrdersXlsx(actor, uri)
        }
    }

    fun restoreCompleteBackup(
        uri: Uri,
        onRestored: () -> Unit,
        password: String? = null,
    ) {
        actionRunner.run(scope = scope, onSuccess = onRestored) { actor ->
            backupRepository.restoreCompleteBackup(actor, uri, password)
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

    fun setInactivityTimeoutMinutes(minutes: Int) {
        scope.launch {
            securityPreferences.setInactivityTimeoutMinutes(minutes)
        }
    }
}

internal class AdminSettingsDelegate(
    private val adminUserRepository: AdminUserRepository,
    private val actionRunner: AuthenticatedActionRunner,
    private val scope: CoroutineScope,
) {
    fun createOperator(
        displayName: String,
        username: String,
        password: CharArray,
    ) {
        actionRunner.run(scope = scope) { actor ->
            adminUserRepository.createOperator(
                actor,
                OperatorInput(displayName, username, password),
            )
        }
    }

    fun setUserActive(
        userId: String,
        active: Boolean,
    ) {
        actionRunner.run(scope = scope) { actor ->
            adminUserRepository.setActive(actor, userId, active)
        }
    }

    fun setUserPermissions(
        userId: String,
        permissions: Set<UserPermission>,
    ) {
        actionRunner.run(scope = scope) { actor ->
            adminUserRepository.setPermissions(actor, userId, permissions)
        }
    }

    fun resetPassword(
        userId: String,
        password: CharArray,
    ) {
        actionRunner.run(scope = scope) { actor ->
            adminUserRepository.resetPassword(actor, userId, password)
        }
    }
}

internal class StorageSettingsDelegate(
    private val orderFileCleanupService: OrderFileCleanupService,
    private val database: RepairAppDatabase,
    private val actionRunner: AuthenticatedActionRunner,
    private val scope: CoroutineScope,
    private val onRefreshStats: () -> Unit,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    fun cleanupCache() {
        scope.launch(ioDispatcher) {
            orderFileCleanupService.cleanupSharedCache()
            onRefreshStats()
        }
    }

    fun cleanupOrphanedFiles() {
        actionRunner.run(
            scope = scope,
            successMessageRes = R.string.success_orphaned_files_cleaned,
            autoDismissMillis = 1000L,
        ) { actor ->
            PermissionService.requirePermission(actor, UserPermission.MANAGE_STORAGE)
            val paths = mutableSetOf<String>()
            paths.addAll(database.orderMediaDao().getAllPaths())
            paths.addAll(database.signatureDao().getAllPaths())
            paths.addAll(database.orderDocumentDao().getAllPaths())
            orderFileCleanupService.cleanupOrphanedFiles(paths)
            onRefreshStats()
            Result.success(Unit)
        }
    }
}

internal class DiagnosticSettingsDelegate(
    private val diagnosticLogService: DiagnosticLogService,
    private val actionRunner: AuthenticatedActionRunner,
    private val scope: CoroutineScope,
    private val onRefreshStats: () -> Unit,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    fun clearLogs() {
        scope.launch(ioDispatcher) {
            diagnosticLogService.clearLogs()
            onRefreshStats()
        }
    }

    fun shareLogs() {
        actionRunner.run(
            scope = scope,
            successMessageRes = R.string.success_operation_completed,
            autoDismissMillis = 1000L,
        ) { actor ->
            PermissionService.requirePermission(
                actor,
                UserPermission.MANAGE_STORAGE,
            )
            val zipFile = diagnosticLogService.collectAndPrepareZip(null)
            if (zipFile != null) {
                val intent =
                    diagnosticLogService.createShareIntent(zipFile).apply {
                        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                withContext(Dispatchers.Main) {
                    actionRunner.context.startActivity(intent)
                }
                Result.success(Unit)
            } else {
                Result.failure<Unit>(IllegalStateException(actionRunner.context.getString(R.string.error_log_generation_failed)))
            }
        }
    }
}

internal class OwnerSettingsDelegate(
    private val ownerPreferences: OwnerPreferences,
    private val actionRunner: AuthenticatedActionRunner,
    private val scope: CoroutineScope,
) {
    fun updateOwnerInfo(info: OwnerInfo) {
        actionRunner.run(
            scope = scope,
            successMessageRes = R.string.owner_save_success,
            autoDismissMillis = 1000L,
        ) {
            ownerPreferences.updateOwnerInfo(info)
            Result.success(Unit)
        }
    }
}

internal class UiSettingsDelegate(
    private val uiPreferences: UiPreferences,
    private val scope: CoroutineScope,
) {
    fun setDarkTheme(enabled: Boolean) {
        scope.launch {
            uiPreferences.setDarkTheme(enabled)
        }
    }
}

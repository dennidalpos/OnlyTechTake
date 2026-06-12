package com.onlytechtake.repairapp.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.onlytechtake.repairapp.domain.backup.BackupReminderPolicy
import com.onlytechtake.repairapp.domain.backup.BackupReminderSettings
import com.onlytechtake.repairapp.domain.backup.BackupReminderStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.backupReminderDataStore by preferencesDataStore(name = "backup_reminder_preferences")

class BackupReminderPreferences(
    private val context: Context,
    private val clockMillis: () -> Long = System::currentTimeMillis,
) {
    val settings: Flow<BackupReminderSettings> =
        context.backupReminderDataStore.data.map { preferences ->
            BackupReminderSettings(
                enabled = preferences[ENABLED] ?: true,
                intervalDays = BackupReminderPolicy.normalizeIntervalDays(
                    preferences[INTERVAL_DAYS] ?: BackupReminderPolicy.DEFAULT_INTERVAL_DAYS,
                ),
                lastSuccessfulBackupAtMillis = preferences[LAST_SUCCESSFUL_BACKUP_AT],
                lastBackupStatus = preferences[LAST_BACKUP_STATUS]
                    ?.let { runCatching { BackupReminderStatus.valueOf(it) }.getOrNull() }
                    ?: BackupReminderStatus.NEVER,
                lastBackupFileName = preferences[LAST_BACKUP_FILE_NAME],
            )
        }

    suspend fun setEnabled(enabled: Boolean) {
        context.backupReminderDataStore.edit { preferences ->
            preferences[ENABLED] = enabled
        }
    }

    suspend fun setIntervalDays(days: Int) {
        context.backupReminderDataStore.edit { preferences ->
            preferences[INTERVAL_DAYS] = BackupReminderPolicy.normalizeIntervalDays(days)
        }
    }

    suspend fun recordSuccessfulFullBackup(fileName: String?) {
        context.backupReminderDataStore.edit { preferences ->
            preferences[LAST_SUCCESSFUL_BACKUP_AT] = clockMillis()
            preferences[LAST_BACKUP_STATUS] = BackupReminderStatus.SUCCESS.name
            val safeFileName = fileName?.takeIf { it.isNotBlank() }
            if (safeFileName == null) {
                preferences.remove(LAST_BACKUP_FILE_NAME)
            } else {
                preferences[LAST_BACKUP_FILE_NAME] = safeFileName
            }
        }
    }

    suspend fun recordFailedFullBackup() {
        context.backupReminderDataStore.edit { preferences ->
            preferences[LAST_BACKUP_STATUS] = BackupReminderStatus.FAILED.name
        }
    }

    suspend fun currentSettings(): BackupReminderSettings = settings.first()

    private companion object {
        val ENABLED = booleanPreferencesKey("backup_reminder_enabled")
        val INTERVAL_DAYS = intPreferencesKey("backup_reminder_interval_days")
        val LAST_SUCCESSFUL_BACKUP_AT = longPreferencesKey("last_successful_full_zip_backup_at")
        val LAST_BACKUP_STATUS = stringPreferencesKey("last_backup_status")
        val LAST_BACKUP_FILE_NAME = stringPreferencesKey("last_backup_file_name")
    }
}

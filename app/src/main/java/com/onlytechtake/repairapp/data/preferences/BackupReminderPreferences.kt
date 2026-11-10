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
                intervalDays =
                    BackupReminderPolicy.normalizeIntervalDays(
                        preferences[INTERVAL_DAYS] ?: BackupReminderPolicy.DEFAULT_INTERVAL_DAYS,
                    ),
                lastSuccessfulBackupAtMillis = preferences[LAST_SUCCESSFUL_BACKUP_AT],
                lastBackupStatus =
                    preferences[LAST_BACKUP_STATUS]
                        ?.let { runCatching { BackupReminderStatus.valueOf(it) }.getOrNull() }
                        ?: BackupReminderStatus.NEVER,
                lastBackupFileName = preferences[LAST_BACKUP_FILE_NAME],
                isAutomatic = preferences[IS_AUTOMATIC] ?: false,
                backupUri = preferences[BACKUP_URI],
                scheduledHour = preferences[SCHEDULED_HOUR] ?: 3,
                scheduledMinute = preferences[SCHEDULED_MINUTE] ?: 0,
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

    suspend fun setAutomatic(automatic: Boolean) {
        context.backupReminderDataStore.edit { preferences ->
            preferences[IS_AUTOMATIC] = automatic
        }
    }

    suspend fun setBackupUri(uri: String?) {
        context.backupReminderDataStore.edit { preferences ->
            if (uri == null) {
                preferences.remove(BACKUP_URI)
            } else {
                preferences[BACKUP_URI] = uri
            }
        }
    }

    suspend fun setScheduledTime(
        hour: Int,
        minute: Int,
    ) {
        context.backupReminderDataStore.edit { preferences ->
            preferences[SCHEDULED_HOUR] = hour.coerceIn(0, 23)
            preferences[SCHEDULED_MINUTE] = minute.coerceIn(0, 59)
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

    private companion object {
        val ENABLED = booleanPreferencesKey("backup_reminder_enabled")
        val INTERVAL_DAYS = intPreferencesKey("backup_reminder_interval_days")
        val LAST_SUCCESSFUL_BACKUP_AT = longPreferencesKey("last_successful_full_zip_backup_at")
        val LAST_BACKUP_STATUS = stringPreferencesKey("last_backup_status")
        val LAST_BACKUP_FILE_NAME = stringPreferencesKey("last_backup_file_name")
        val IS_AUTOMATIC = booleanPreferencesKey("is_automatic_backup_enabled")
        val BACKUP_URI = stringPreferencesKey("backup_uri")
        val SCHEDULED_HOUR = intPreferencesKey("backup_scheduled_hour")
        val SCHEDULED_MINUTE = intPreferencesKey("backup_scheduled_minute")
    }
}

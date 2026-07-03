package com.onlytechtake.repairapp.domain.backup

import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds

enum class BackupReminderStatus {
    NEVER,
    SUCCESS,
    FAILED,
}

data class BackupReminderSettings(
    val enabled: Boolean = true,
    val intervalDays: Int = BackupReminderPolicy.DEFAULT_INTERVAL_DAYS,
    val lastSuccessfulBackupAtMillis: Long? = null,
    val lastBackupStatus: BackupReminderStatus = BackupReminderStatus.NEVER,
    val lastBackupFileName: String? = null,
    val isAutomatic: Boolean = false,
    val backupUri: String? = null,
    val scheduledHour: Int = 3,
    val scheduledMinute: Int = 0,
)

object BackupReminderPolicy {
    const val DEFAULT_INTERVAL_DAYS = 7
    private const val MIN_INTERVAL_DAYS = 1

    fun normalizeIntervalDays(days: Int): Int = days.coerceAtLeast(MIN_INTERVAL_DAYS)

    fun shouldShowReminder(
        settings: BackupReminderSettings,
        nowMillis: Long,
    ): Boolean {
        if (!settings.enabled || settings.isAutomatic) return false
        val lastSuccessfulBackup = settings.lastSuccessfulBackupAtMillis ?: return true
        val interval = normalizeIntervalDays(settings.intervalDays).days
        val elapsed = (nowMillis - lastSuccessfulBackup).milliseconds
        return elapsed >= interval
    }

    fun daysSinceLastSuccessfulBackup(
        settings: BackupReminderSettings,
        nowMillis: Long,
    ): Long? =
        settings.lastSuccessfulBackupAtMillis?.let { lastBackup ->
            (nowMillis - lastBackup).coerceAtLeast(0L).milliseconds.inWholeDays
        }

    fun recordSuccess(
        settings: BackupReminderSettings,
        backupAtMillis: Long,
        fileName: String?,
    ): BackupReminderSettings =
        settings.copy(
            lastSuccessfulBackupAtMillis = backupAtMillis,
            lastBackupStatus = BackupReminderStatus.SUCCESS,
            lastBackupFileName = fileName?.takeIf { it.isNotBlank() },
        )

    fun recordFailure(settings: BackupReminderSettings): BackupReminderSettings =
        settings.copy(lastBackupStatus = BackupReminderStatus.FAILED)
}

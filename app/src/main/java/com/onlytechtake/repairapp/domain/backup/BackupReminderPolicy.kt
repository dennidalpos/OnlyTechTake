package com.onlytechtake.repairapp.domain.backup

import java.util.concurrent.TimeUnit

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
)

object BackupReminderPolicy {
    const val DEFAULT_INTERVAL_DAYS = 7
    const val MIN_INTERVAL_DAYS = 1

    fun normalizeIntervalDays(days: Int): Int = days.coerceAtLeast(MIN_INTERVAL_DAYS)

    fun shouldShowReminder(
        settings: BackupReminderSettings,
        nowMillis: Long,
    ): Boolean {
        if (!settings.enabled) return false
        val lastSuccessfulBackup = settings.lastSuccessfulBackupAtMillis ?: return true
        val intervalMillis =
            TimeUnit.DAYS.toMillis(normalizeIntervalDays(settings.intervalDays).toLong())
        return nowMillis - lastSuccessfulBackup >= intervalMillis
    }

    fun daysSinceLastSuccessfulBackup(
        settings: BackupReminderSettings,
        nowMillis: Long,
    ): Long? =
        settings.lastSuccessfulBackupAtMillis?.let { lastBackup ->
            TimeUnit.MILLISECONDS.toDays((nowMillis - lastBackup).coerceAtLeast(0L))
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

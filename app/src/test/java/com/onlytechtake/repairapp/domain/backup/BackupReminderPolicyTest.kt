package com.onlytechtake.repairapp.domain.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class BackupReminderPolicyTest {
    @Test
    fun newInstallShowsReminderByDefault() {
        assertTrue(BackupReminderPolicy.shouldShowReminder(BackupReminderSettings(), NOW))
    }

    @Test
    fun successfulBackupHidesReminderUntilIntervalExpires() {
        val settings =
            BackupReminderPolicy.recordSuccess(
                BackupReminderSettings(),
                NOW,
                "onlytechtake-backup.zip",
            )

        assertFalse(
            BackupReminderPolicy.shouldShowReminder(
                settings,
                NOW + TimeUnit.DAYS.toMillis(6),
            ),
        )
        assertTrue(
            BackupReminderPolicy.shouldShowReminder(
                settings,
                NOW + TimeUnit.DAYS.toMillis(7),
            ),
        )
        assertEquals("onlytechtake-backup.zip", settings.lastBackupFileName)
    }

    @Test
    fun customIntervalControlsExpiry() {
        val settings =
            BackupReminderPolicy.recordSuccess(
                BackupReminderSettings(intervalDays = 3),
                NOW,
                null,
            )

        assertFalse(
            BackupReminderPolicy.shouldShowReminder(
                settings,
                NOW + TimeUnit.DAYS.toMillis(2),
            ),
        )
        assertTrue(
            BackupReminderPolicy.shouldShowReminder(
                settings,
                NOW + TimeUnit.DAYS.toMillis(3),
            ),
        )
    }

    @Test
    fun disabledReminderDoesNotShow() {
        val settings = BackupReminderSettings(enabled = false)

        assertFalse(BackupReminderPolicy.shouldShowReminder(settings, NOW))
    }

    @Test
    fun failedBackupDoesNotChangeLastSuccessfulTimestamp() {
        val successful =
            BackupReminderPolicy.recordSuccess(BackupReminderSettings(), NOW, "backup.zip")
        val failed = BackupReminderPolicy.recordFailure(successful)

        assertEquals(NOW, failed.lastSuccessfulBackupAtMillis)
        assertEquals(BackupReminderStatus.FAILED, failed.lastBackupStatus)
        assertTrue(BackupReminderPolicy.shouldShowReminder(failed, NOW + TimeUnit.DAYS.toMillis(7)))
    }

    private companion object {
        const val NOW = 1_717_171_717_000L
    }
}

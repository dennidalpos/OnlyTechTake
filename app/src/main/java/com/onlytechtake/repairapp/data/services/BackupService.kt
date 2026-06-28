package com.onlytechtake.repairapp.data.services

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.BatteryManager
import androidx.documentfile.provider.DocumentFile
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.data.preferences.BackupReminderPreferences
import com.onlytechtake.repairapp.data.repositories.BackupRepository
import com.onlytechtake.repairapp.data.services.BackupWorker
import com.onlytechtake.repairapp.domain.auth.AuthenticatedUser
import com.onlytechtake.repairapp.domain.backup.BackupReminderSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

sealed class BackupState {
    object Idle : BackupState()

    object Checking : BackupState()

    data class Running(
        val progress: Float,
    ) : BackupState()

    data class Success(
        val fileName: String,
    ) : BackupState()

    data class Error(
        val message: String,
    ) : BackupState()
}

class BackupService(
    private val context: Context,
    private val backupRepository: BackupRepository,
    private val backupReminderPreferences: BackupReminderPreferences,
) {
    private val _state = MutableStateFlow<BackupState>(BackupState.Idle)
    val state: StateFlow<BackupState> = _state.asStateFlow()

    fun getBatteryLevel(): Int {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        if (level == -1 || scale == -1) return 100
        return (level * 100 / scale.toFloat()).toInt()
    }

    fun isBatteryLow(): Boolean = getBatteryLevel() < 10

    suspend fun runAutomaticBackup(actor: AuthenticatedUser): Result<Unit> {
        val settings = backupReminderPreferences.settings.first()
        val treeUri =
            settings.backupUri?.let { Uri.parse(it) }
                ?: return Result.failure(Exception(context.getString(R.string.error_no_backup_path)))

        if (isBatteryLow()) {
            val level = getBatteryLevel()
            val error = context.getString(R.string.config_backup_auto_battery_low, level)
            _state.value = BackupState.Error(error)
            return Result.failure(Exception(error))
        }

        _state.value = BackupState.Running(0f)

        return try {
            val directory =
                DocumentFile.fromTreeUri(context, treeUri)
                    ?: throw Exception(context.getString(R.string.error_destination_unavailable))

            val fileName = "backup_${System.currentTimeMillis()}.zip"
            val file =
                directory.createFile("application/zip", fileName)
                    ?: throw Exception(context.getString(R.string.error_could_not_create_file))

            val result = backupRepository.exportCompleteBackup(actor, file.uri)
            if (result.isSuccess) {
                _state.value = BackupState.Success(fileName)
            } else {
                val error = result.exceptionOrNull()?.message ?: context.getString(R.string.error_backup_failed)
                _state.value = BackupState.Error(error)
            }
            result
        } catch (e: Exception) {
            _state.value = BackupState.Error(e.message ?: "Unknown error")
            Result.failure(e)
        }
    }

    fun scheduleAutomaticBackup(settings: BackupReminderSettings) {
        if (!settings.isAutomatic || settings.backupUri == null) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            return
        }

        val constraints =
            Constraints
                .Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .setRequiresBatteryNotLow(true)
                .build()

        val now = LocalDateTime.now()
        val scheduledTime = LocalDateTime.of(now.toLocalDate(), LocalTime.of(settings.scheduledHour, settings.scheduledMinute))

        var initialDelay = Duration.between(now, scheduledTime)
        if (initialDelay.isNegative) {
            initialDelay = initialDelay.plusDays(1)
        }

        val workRequest =
            PeriodicWorkRequestBuilder<BackupWorker>(24, TimeUnit.HOURS)
                .setConstraints(constraints)
                .setInitialDelay(initialDelay.toMillis(), TimeUnit.MILLISECONDS)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.HOURS)
                .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest,
        )
    }

    companion object {
        const val WORK_NAME = "automatic_backup_work"
    }
}

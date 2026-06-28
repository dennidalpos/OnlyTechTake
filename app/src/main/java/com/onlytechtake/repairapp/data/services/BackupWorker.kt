package com.onlytechtake.repairapp.data.services

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.onlytechtake.repairapp.RepairAppApplication

class BackupWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val application = applicationContext as? RepairAppApplication ?: return Result.failure()
        val container = application.appContainer
        val backupService = container.backupService
        val authRepository = container.authRepository

        val currentUser = authRepository.currentUser.value ?: return Result.failure()

        return try {
            val result = backupService.runAutomaticBackup(currentUser)
            if (result.isSuccess) {
                Result.success()
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            Result.failure()
        }
    }
}

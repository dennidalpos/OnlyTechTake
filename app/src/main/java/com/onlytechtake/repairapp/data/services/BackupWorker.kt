package com.onlytechtake.repairapp.data.services

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.onlytechtake.repairapp.domain.auth.AuthRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class BackupWorker
    @AssistedInject
    constructor(
        @Assisted context: Context,
        @Assisted params: WorkerParameters,
        private val backupService: BackupService,
        private val authRepository: AuthRepository,
    ) : CoroutineWorker(context, params) {
        override suspend fun doWork(): Result {
            val currentUser = authRepository.currentUser.value ?: return Result.failure()

            return try {
                val result = backupService.runAutomaticBackup(currentUser)
                if (result.isSuccess) {
                    Result.success()
                } else {
                    Result.retry()
                }
            } catch (_: Exception) {
                Result.failure()
            }
        }
    }

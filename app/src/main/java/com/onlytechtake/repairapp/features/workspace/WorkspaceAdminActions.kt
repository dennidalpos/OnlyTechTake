package com.onlytechtake.repairapp.features.workspace

import android.net.Uri
import com.onlytechtake.repairapp.data.preferences.BackupReminderPreferences
import com.onlytechtake.repairapp.data.repositories.BackupRepository
import com.onlytechtake.repairapp.data.repositories.DataResetRepository
import kotlinx.coroutines.flow.MutableStateFlow

internal class WorkspaceAdminActions(
    private val authenticatedActionRunner: AuthenticatedActionRunner,
    private val mutableUiState: MutableStateFlow<WorkspaceUiState>,
    private val backupRepository: BackupRepository,
    private val backupReminderPreferences: BackupReminderPreferences,
    private val dataResetRepository: DataResetRepository,
    private val showSuccess: (String) -> Unit,
    private val showError: (Throwable) -> Unit,
) {
    fun exportConfiguration(uri: Uri) = authenticatedActionRunner.run { actor ->
        backupRepository.exportConfiguration(actor, uri)
    }

    fun exportDatabase(uri: Uri) = authenticatedActionRunner.run { actor ->
        backupRepository.exportDatabase(actor, uri)
    }

    fun exportCompleteBackup(uri: Uri) {
        authenticatedActionRunner.runCustom { actor ->
            backupRepository.exportCompleteBackup(actor, uri).fold(
                onSuccess = {
                    backupReminderPreferences.recordSuccessfulFullBackup(backupRepository.displayName(uri))
                    showSuccess("Operazione completata")
                },
                onFailure = { error ->
                    backupReminderPreferences.recordFailedFullBackup()
                    showError(error)
                },
            )
        }
    }

    fun exportOrdersXlsx(uri: Uri) = authenticatedActionRunner.run { actor ->
        backupRepository.exportOrdersXlsx(actor, uri)
    }

    fun restoreCompleteBackup(uri: Uri, onRestored: () -> Unit) {
        authenticatedActionRunner.runCustom { actor ->
            backupRepository.restoreCompleteBackup(actor, uri).fold(
                onSuccess = {
                    mutableUiState.value = WorkspaceUiState()
                    onRestored()
                },
                onFailure = showError,
            )
        }
    }

    fun resetAllData(confirmation: String, onReset: () -> Unit) {
        authenticatedActionRunner.runCustom { actor ->
            dataResetRepository.resetAll(actor, confirmation).fold(
                onSuccess = {
                    mutableUiState.value = WorkspaceUiState()
                    onReset()
                },
                onFailure = showError,
            )
        }
    }

    fun resetOrders(confirmation: String, onReset: () -> Unit) {
        authenticatedActionRunner.runCustom { actor ->
            dataResetRepository.resetOrders(actor, confirmation).fold(
                onSuccess = {
                    mutableUiState.value = WorkspaceUiState()
                    onReset()
                },
                onFailure = showError,
            )
        }
    }
}

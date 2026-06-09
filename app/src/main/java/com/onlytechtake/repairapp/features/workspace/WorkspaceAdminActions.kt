package com.onlytechtake.repairapp.features.workspace

import android.net.Uri
import com.onlytechtake.repairapp.data.repositories.BackupRepository
import com.onlytechtake.repairapp.data.repositories.DataResetRepository
import com.onlytechtake.repairapp.data.repositories.OrderWorkflowRepository
import kotlinx.coroutines.flow.MutableStateFlow

internal class WorkspaceAdminActions(
    private val authenticatedActionRunner: AuthenticatedActionRunner,
    private val mutableUiState: MutableStateFlow<WorkspaceUiState>,
    private val backupRepository: BackupRepository,
    private val dataResetRepository: DataResetRepository,
    private val orderRepository: OrderWorkflowRepository,
    private val showError: (Throwable) -> Unit,
) {
    fun exportConfiguration(uri: Uri) = authenticatedActionRunner.run { actor ->
        backupRepository.exportConfiguration(actor, uri)
    }

    fun exportDatabase(uri: Uri) = authenticatedActionRunner.run { actor ->
        backupRepository.exportDatabase(actor, uri)
    }

    fun exportCompleteBackup(uri: Uri) = authenticatedActionRunner.run { actor ->
        backupRepository.exportCompleteBackup(actor, uri)
    }

    fun exportOrdersXlsx(uri: Uri) = authenticatedActionRunner.run { actor ->
        backupRepository.exportOrdersXlsx(actor, uri)
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

    fun archiveDiscardedClosedOrders() {
        authenticatedActionRunner.run { actor ->
            orderRepository.archiveDiscardedClosedOrders(actor).map { count ->
                "$count commesse archiviate"
            }
        }
    }

    fun deleteArchivedOrders() {
        authenticatedActionRunner.run { actor ->
            orderRepository.deleteArchivedOrders(actor).map { count ->
                "$count commesse cancellate"
            }
        }
    }
}

package com.onlytechtake.repairapp.features.workspace

import android.net.Uri
import com.onlytechtake.repairapp.data.repositories.BackupRepository
import com.onlytechtake.repairapp.data.repositories.DataResetRepository
import com.onlytechtake.repairapp.domain.auth.AuthRepository
import com.onlytechtake.repairapp.domain.auth.AuthenticatedUser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

internal class WorkspaceAdminActions(
    private val authRepository: AuthRepository,
    private val currentUser: StateFlow<AuthenticatedUser?>,
    private val mutableUiState: MutableStateFlow<WorkspaceUiState>,
    private val scope: CoroutineScope,
    private val backupRepository: BackupRepository,
    private val dataResetRepository: DataResetRepository,
    private val showSuccess: (String) -> Unit,
    private val showError: (Throwable) -> Unit,
    private val showMessage: (String, Boolean) -> Unit,
) {
    fun exportConfiguration(uri: Uri) = withUser { actor ->
        backupRepository.exportConfiguration(actor, uri)
    }

    fun exportDatabase(uri: Uri) = withUser { actor ->
        backupRepository.exportDatabase(actor, uri)
    }

    fun exportCompleteBackup(uri: Uri) = withUser { actor ->
        backupRepository.exportCompleteBackup(actor, uri)
    }

    fun resetAllData(confirmation: String, onReset: () -> Unit) {
        scope.launch {
            val actor = currentUser.value ?: return@launch showMessage("Sessione non valida", true)
            authRepository.touchSession()
            dataResetRepository.resetAll(actor, confirmation).fold(
                onSuccess = {
                    mutableUiState.value = WorkspaceUiState()
                    onReset()
                },
                onFailure = showError,
            )
        }
    }

    private fun withUser(block: suspend (AuthenticatedUser) -> Result<*>) {
        scope.launch {
            val actor = currentUser.value ?: return@launch showMessage("Sessione non valida", true)
            authRepository.touchSession()
            block(actor).fold(
                onSuccess = { showSuccess("Operazione completata") },
                onFailure = showError,
            )
        }
    }
}

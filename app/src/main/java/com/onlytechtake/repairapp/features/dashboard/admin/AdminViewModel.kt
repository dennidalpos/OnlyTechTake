package com.onlytechtake.repairapp.features.dashboard.admin

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.onlytechtake.repairapp.data.local.entities.UserPermission
import com.onlytechtake.repairapp.data.repositories.AdminUserRepository
import com.onlytechtake.repairapp.data.repositories.BackupRepository
import com.onlytechtake.repairapp.data.repositories.DataResetRepository
import com.onlytechtake.repairapp.domain.auth.AuthRepository
import com.onlytechtake.repairapp.domain.workspace.OperatorInput
import com.onlytechtake.repairapp.features.workspace.WorkspaceMessage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class AdminViewModel(
    private val authRepository: AuthRepository,
    private val adminUserRepository: AdminUserRepository,
    private val backupRepository: BackupRepository,
    private val dataResetRepository: DataResetRepository,
) : ViewModel() {

    private val _message = MutableStateFlow<WorkspaceMessage?>(null)
    val message = _message.asStateFlow()

    private val _recoveryCode = MutableStateFlow<String?>(null)
    val recoveryCode = _recoveryCode.asStateFlow()

    val currentUser = authRepository.currentUser
    val users = adminUserRepository.observeUsers().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun createOperator(displayName: String, username: String, password: CharArray) {
        launchWithUser { actor ->
            adminUserRepository.createOperator(actor, OperatorInput(displayName, username, password))
        }
    }

    fun setUserActive(userId: String, active: Boolean) {
        launchWithUser { actor ->
            adminUserRepository.setActive(actor, userId, active)
        }
    }

    fun setUserPermissions(userId: String, permissions: Set<UserPermission>) {
        launchWithUser { actor ->
            adminUserRepository.setPermissions(actor, userId, permissions)
        }
    }

    fun resetPassword(userId: String, password: CharArray) {
        launchWithUser { actor ->
            adminUserRepository.resetPassword(actor, userId, password)
        }
    }

    fun exportDatabase(uri: Uri) {
        launchWithUser { actor ->
            backupRepository.exportDatabase(actor, uri)
        }
    }

    fun exportOrdersXlsx(uri: Uri) {
        launchWithUser { actor ->
            backupRepository.exportOrdersXlsx(actor, uri)
        }
    }

    fun resetAllData(confirmation: String, onReset: () -> Unit) {
        launchWithUser(onSuccess = onReset) { actor ->
            dataResetRepository.resetAll(actor, confirmation)
        }
    }

    fun resetOrders(confirmation: String, onReset: () -> Unit) {
        launchWithUser(onSuccess = onReset) { actor ->
            dataResetRepository.resetOrders(actor, confirmation)
        }
    }

    fun generateRecoveryKit() {
        viewModelScope.launch {
            authRepository.getRecoveryKit().onSuccess { code ->
                _recoveryCode.value = code
            }.onFailure { error ->
                _message.value = WorkspaceMessage(error.message ?: "Errore", true)
            }
        }
    }

    fun clearRecoveryCode() {
        _recoveryCode.value = null
    }

    private fun launchWithUser(
        onSuccess: () -> Unit = {},
        block: suspend (com.onlytechtake.repairapp.domain.auth.AuthenticatedUser) -> Result<*>
    ) {
        val user = currentUser.value
        if (user != null) {
            viewModelScope.launch {
                block(user).fold(
                    onSuccess = { onSuccess() },
                    onFailure = { error -> _message.value = WorkspaceMessage(error.message ?: "Errore", true) }
                )
            }
        }
    }

    class Factory(
        private val authRepository: AuthRepository,
        private val adminUserRepository: AdminUserRepository,
        private val backupRepository: BackupRepository,
        private val dataResetRepository: DataResetRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AdminViewModel(authRepository, adminUserRepository, backupRepository, dataResetRepository) as T
        }
    }
}

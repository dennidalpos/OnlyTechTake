package com.onlytechtake.repairapp.features.dashboard.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.onlytechtake.repairapp.core.error.ErrorHandler
import com.onlytechtake.repairapp.data.repositories.AdminUserRepository
import com.onlytechtake.repairapp.data.repositories.DataResetRepository
import com.onlytechtake.repairapp.data.repositories.OrderWorkflowRepository
import com.onlytechtake.repairapp.domain.auth.AuthRepository
import com.onlytechtake.repairapp.domain.workspace.OperatorInput
import com.onlytechtake.repairapp.features.workspace.AuthenticatedActionRunner
import kotlinx.coroutines.flow.*

class AdminViewModel(
    private val adminUserRepository: AdminUserRepository,
    private val dataResetRepository: DataResetRepository,
    private val orderRepository: OrderWorkflowRepository,
    private val authRepository: AuthRepository,
    private val errorHandler: ErrorHandler,
) : ViewModel() {

    val users = adminUserRepository.observeUsers()
        .catch { errorHandler.handleError(it, "AdminViewModel") }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val currentUser = authRepository.currentUser

    private val actionRunner = AuthenticatedActionRunner(
        authRepository = authRepository,
        currentUser = authRepository.currentUser,
        scope = viewModelScope,
        showSuccess = { /* success feedback */ },
        showError = { errorHandler.handleError(it) },
        showMessage = { _, _ -> /* message feedback */ }
    )

    fun createOperator(displayName: String, username: String, password: String) {
        actionRunner.run { actor ->
            adminUserRepository.createOperator(actor, OperatorInput(displayName, username, password))
        }
    }

    fun setUserActive(userId: String, active: Boolean) {
        actionRunner.run { actor ->
            adminUserRepository.setActive(actor, userId, active)
        }
    }

    fun resetPassword(userId: String, password: String) {
        actionRunner.run { actor ->
            adminUserRepository.resetPassword(actor, userId, password)
        }
    }

    fun resetAllData(confirmation: String, onReset: () -> Unit) {
        actionRunner.run { actor ->
            dataResetRepository.resetAll(actor, confirmation).onSuccess { onReset() }
        }
    }

    fun archiveDiscardedClosedOrders() {
        actionRunner.run { actor ->
            orderRepository.archiveDiscardedClosedOrders(actor)
        }
    }

    fun deleteArchivedOrders() {
        actionRunner.run { actor ->
            orderRepository.deleteArchivedOrders(actor)
        }
    }

    class Factory(
        private val adminUserRepository: AdminUserRepository,
        private val dataResetRepository: DataResetRepository,
        private val orderRepository: OrderWorkflowRepository,
        private val authRepository: AuthRepository,
        private val errorHandler: ErrorHandler,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AdminViewModel(
                adminUserRepository,
                dataResetRepository,
                orderRepository,
                authRepository,
                errorHandler
            ) as T
        }
    }
}

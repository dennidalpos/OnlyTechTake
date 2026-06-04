package com.onlytechtake.repairapp.features.workspace

import com.onlytechtake.repairapp.data.repositories.AdminUserRepository
import com.onlytechtake.repairapp.data.repositories.CatalogRepository
import com.onlytechtake.repairapp.data.repositories.CustomerRepository
import com.onlytechtake.repairapp.domain.auth.AuthRepository
import com.onlytechtake.repairapp.domain.auth.AuthenticatedUser
import com.onlytechtake.repairapp.domain.workspace.CategoryInput
import com.onlytechtake.repairapp.domain.workspace.CustomerInput
import com.onlytechtake.repairapp.domain.workspace.OperatorInput
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

internal class WorkspaceCatalogActions(
    private val authRepository: AuthRepository,
    private val currentUser: StateFlow<AuthenticatedUser?>,
    private val scope: CoroutineScope,
    private val adminUserRepository: AdminUserRepository,
    private val customerRepository: CustomerRepository,
    private val catalogRepository: CatalogRepository,
    private val showSuccess: (String) -> Unit,
    private val showError: (Throwable) -> Unit,
    private val showMessage: (String, Boolean) -> Unit,
) {
    fun createOperator(displayName: String, username: String, password: String) = withUser { actor ->
        adminUserRepository.createOperator(actor, OperatorInput(displayName, username, password))
    }

    fun setUserActive(userId: String, active: Boolean) = withUser { actor ->
        adminUserRepository.setActive(actor, userId, active)
    }

    fun resetPassword(userId: String, password: String) = withUser { actor ->
        adminUserRepository.resetPassword(actor, userId, password)
    }

    fun saveCustomer(input: CustomerInput) {
        scope.launch {
            customerRepository.save(input).fold(
                onSuccess = { showSuccess("Cliente salvato") },
                onFailure = showError,
            )
        }
    }

    fun saveCategory(id: String?, name: String, description: String, active: Boolean, sortOrder: Int) =
        withUser { actor ->
            catalogRepository.saveCategory(
                actor,
                CategoryInput(
                    id = id,
                    name = name,
                    description = description,
                    active = active,
                    sortOrder = sortOrder,
                ),
            )
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

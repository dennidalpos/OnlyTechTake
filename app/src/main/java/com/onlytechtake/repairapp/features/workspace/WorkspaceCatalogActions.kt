package com.onlytechtake.repairapp.features.workspace

import com.onlytechtake.repairapp.data.repositories.AdminUserRepository
import com.onlytechtake.repairapp.data.repositories.CatalogRepository
import com.onlytechtake.repairapp.data.repositories.CustomerRepository
import com.onlytechtake.repairapp.data.local.entities.CustomerEntity
import com.onlytechtake.repairapp.domain.workspace.CategoryInput
import com.onlytechtake.repairapp.domain.workspace.CustomerInput
import com.onlytechtake.repairapp.domain.workspace.OperatorInput
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal class WorkspaceCatalogActions(
    private val authenticatedActionRunner: AuthenticatedActionRunner,
    private val scope: CoroutineScope,
    private val adminUserRepository: AdminUserRepository,
    private val customerRepository: CustomerRepository,
    private val catalogRepository: CatalogRepository,
    private val showSuccess: (String) -> Unit,
    private val showError: (Throwable) -> Unit,
) {
    fun createOperator(displayName: String, username: String, password: String) = authenticatedActionRunner.run { actor ->
        adminUserRepository.createOperator(actor, OperatorInput(displayName, username, password))
    }

    fun setUserActive(userId: String, active: Boolean) = authenticatedActionRunner.run { actor ->
        adminUserRepository.setActive(actor, userId, active)
    }

    fun resetPassword(userId: String, password: String) = authenticatedActionRunner.run { actor ->
        adminUserRepository.resetPassword(actor, userId, password)
    }

    fun saveCustomer(input: CustomerInput, onSaved: (CustomerEntity) -> Unit = {}) {
        scope.launch {
            customerRepository.save(input).fold(
                onSuccess = { customer ->
                    showSuccess("Cliente salvato")
                    onSaved(customer)
                },
                onFailure = showError,
            )
        }
    }

    fun saveCategory(id: String?, parentId: String?, name: String, description: String, active: Boolean) =
        authenticatedActionRunner.run { actor ->
            catalogRepository.saveCategory(
                actor,
                CategoryInput(
                    id = id,
                    parentId = parentId,
                    name = name,
                    description = description,
                    active = active,
                ),
            )
        }

    fun deleteCategory(categoryId: String) =
        authenticatedActionRunner.run { actor ->
            catalogRepository.deleteCategory(actor, categoryId)
        }
}

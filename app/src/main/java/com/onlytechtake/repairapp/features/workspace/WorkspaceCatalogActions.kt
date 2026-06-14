package com.onlytechtake.repairapp.features.workspace

import com.onlytechtake.repairapp.data.repositories.AdminUserRepository
import com.onlytechtake.repairapp.data.repositories.CatalogRepository
import com.onlytechtake.repairapp.data.repositories.CustomerRepository
import com.onlytechtake.repairapp.data.local.entities.CustomerEntity
import com.onlytechtake.repairapp.domain.workspace.CategoryInput
import com.onlytechtake.repairapp.domain.workspace.CustomerInput
import com.onlytechtake.repairapp.domain.workspace.OperatorInput

internal class WorkspaceCatalogActions(
    private val authenticatedActionRunner: AuthenticatedActionRunner,
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

    fun setUserPermissions(userId: String, permissions: Set<com.onlytechtake.repairapp.data.local.entities.UserPermission>) = authenticatedActionRunner.run { actor ->
        adminUserRepository.setPermissions(actor, userId, permissions)
    }

    fun resetPassword(userId: String, password: String) = authenticatedActionRunner.run { actor ->
        adminUserRepository.resetPassword(actor, userId, password)
    }

    fun saveCustomer(input: CustomerInput, onSaved: (CustomerEntity) -> Unit = {}) {
        authenticatedActionRunner.runCustom { actor ->
            com.onlytechtake.repairapp.core.permissions.PermissionService.requirePermission(actor, com.onlytechtake.repairapp.data.local.entities.UserPermission.MANAGE_CUSTOMERS)
            customerRepository.save(input).fold(
                onSuccess = { customer ->
                    showSuccess("Cliente salvato")
                    onSaved(customer)
                },
                onFailure = showError,
            )
        }
    }

    fun saveCategory(
        id: String?,
        parentId: String?,
        name: String,
        description: String,
        active: Boolean,
        onSaved: (String) -> Unit = {},
    ) {
        authenticatedActionRunner.runCustom { actor ->
            catalogRepository.saveCategory(
                actor = actor,
                input = CategoryInput(
                    id = id,
                    parentId = parentId,
                    name = name,
                    description = description,
                    active = active,
                ),
            ).fold(
                onSuccess = { categoryId ->
                    showSuccess("Operazione completata")
                    onSaved(categoryId)
                },
                onFailure = showError,
            )
        }
    }

    fun deleteCategory(categoryId: String) =
        authenticatedActionRunner.run { actor ->
            catalogRepository.deleteCategory(actor, categoryId)
        }
}

package com.onlytechtake.repairapp.data.repositories

import android.content.Context
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.core.permissions.PermissionService
import com.onlytechtake.repairapp.core.utils.capitalizeFirst
import com.onlytechtake.repairapp.data.local.dao.CustomerDao
import com.onlytechtake.repairapp.data.local.entities.CustomerEntity
import com.onlytechtake.repairapp.data.local.entities.CustomerType
import com.onlytechtake.repairapp.data.local.entities.UserPermission
import com.onlytechtake.repairapp.domain.auth.AuthenticatedUser
import com.onlytechtake.repairapp.domain.workspace.CustomerInput
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import com.onlytechtake.repairapp.domain.repositories.CustomerRepository as ICustomerRepository

class CustomerRepository(
    private val context: Context,
    private val customerDao: CustomerDao,
    private val clockMillis: () -> Long = System::currentTimeMillis,
) : ICustomerRepository {
    override fun observeRecent(): Flow<List<CustomerEntity>> = customerDao.observeRecent()

    override fun observeAll(): Flow<List<CustomerEntity>> = customerDao.observeAll()

    override fun search(query: String): Flow<List<CustomerEntity>> {
        val trimmed = query.trim()
        val sanitized =
            trimmed
                .split(" ")
                .asSequence()
                .filter { it.isNotBlank() }
                .joinToString(" ") { "$it*" }
        return customerDao.search(sanitized)
    }

    override suspend fun findById(id: String): CustomerEntity? = customerDao.findById(id)

    override suspend fun save(
        actor: AuthenticatedUser?,
        input: CustomerInput,
    ): Result<CustomerEntity> =
        runCatching {
            PermissionService.requirePermission(actor, UserPermission.MANAGE_CUSTOMERS)
            val now = clockMillis()
            val existing = input.id?.let { customerDao.findById(it) }
            require(isValid(input)) { context.getString(R.string.error_customer_required_fields) }

            val entity =
                CustomerEntity(
                    id = existing?.id ?: UUID.randomUUID().toString(),
                    type = input.type,
                    firstName =
                        input.firstName
                            .trim()
                            .capitalizeFirst()
                            .ifBlank { null },
                    lastName =
                        input.lastName
                            .trim()
                            .capitalizeFirst()
                            .ifBlank { null },
                    companyName =
                        input.companyName
                            .trim()
                            .capitalizeFirst()
                            .ifBlank { null },
                    vatNumber =
                        input.vatNumber
                            .trim()
                            .uppercase()
                            .ifBlank { null },
                    taxCode =
                        input.taxCode
                            .trim()
                            .uppercase()
                            .ifBlank { null },
                    phone = input.phone.trim().ifBlank { null },
                    email =
                        input.email
                            .trim()
                            .lowercase()
                            .ifBlank { null },
                    address =
                        input.address
                            .trim()
                            .capitalizeFirst()
                            .ifBlank { null },
                    notes =
                        input.notes
                            .trim()
                            .capitalizeFirst()
                            .ifBlank { null },
                    createdAt = existing?.createdAt ?: now,
                    updatedAt = now,
                )
            customerDao.upsert(entity)
            entity
        }

    override fun isValid(input: CustomerInput): Boolean =
        when (input.type) {
            CustomerType.PRIVATE -> input.firstName.isNotBlank() || input.lastName.isNotBlank()
            CustomerType.BUSINESS -> input.companyName.isNotBlank()
        } &&
            (input.phone.isNotBlank() || input.email.isNotBlank())
}

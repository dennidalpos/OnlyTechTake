package com.onlytechtake.repairapp.data.repositories

import com.onlytechtake.repairapp.data.local.dao.CustomerDao
import com.onlytechtake.repairapp.data.local.entities.CustomerEntity
import com.onlytechtake.repairapp.data.local.entities.CustomerType
import com.onlytechtake.repairapp.core.utils.capitalizeFirst
import com.onlytechtake.repairapp.domain.workspace.CustomerInput
import java.util.UUID
import kotlinx.coroutines.flow.Flow

class CustomerRepository(
    private val customerDao: CustomerDao,
    private val clockMillis: () -> Long = System::currentTimeMillis,
) {
    fun observeRecent(): Flow<List<CustomerEntity>> = customerDao.observeRecent()

    fun search(query: String): Flow<List<CustomerEntity>> = customerDao.search(query.trim())

    suspend fun findById(id: String): CustomerEntity? = customerDao.findById(id)

    suspend fun save(input: CustomerInput): Result<CustomerEntity> =
        runCatching {
            val now = clockMillis()
            val existing = input.id?.let { customerDao.findById(it) }
            require(isValid(input)) { "Inserisci i dati cliente obbligatori" }

            val entity = CustomerEntity(
                id = existing?.id ?: UUID.randomUUID().toString(),
                type = input.type,
                firstName = input.firstName.trim().capitalizeFirst().ifBlank { null },
                lastName = input.lastName.trim().capitalizeFirst().ifBlank { null },
                companyName = input.companyName.trim().capitalizeFirst().ifBlank { null },
                vatNumber = input.vatNumber.trim().uppercase().ifBlank { null },
                taxCode = input.taxCode.trim().uppercase().ifBlank { null },
                phone = input.phone.trim().ifBlank { null },
                email = input.email.trim().lowercase().ifBlank { null },
                address = input.address.trim().capitalizeFirst().ifBlank { null },
                notes = input.notes.trim().capitalizeFirst().ifBlank { null },
                createdAt = existing?.createdAt ?: now,
                updatedAt = now,
            )
            customerDao.upsert(entity)
            entity
        }

    private fun isValid(input: CustomerInput): Boolean =
        when (input.type) {
            CustomerType.PRIVATE -> input.firstName.isNotBlank() || input.lastName.isNotBlank()
            CustomerType.BUSINESS -> input.companyName.isNotBlank()
        } && (input.phone.isNotBlank() || input.email.isNotBlank())
}

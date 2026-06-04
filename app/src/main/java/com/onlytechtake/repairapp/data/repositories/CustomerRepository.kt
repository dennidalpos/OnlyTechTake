package com.onlytechtake.repairapp.data.repositories

import com.onlytechtake.repairapp.data.local.dao.CustomerDao
import com.onlytechtake.repairapp.data.local.entities.CustomerEntity
import com.onlytechtake.repairapp.data.local.entities.CustomerType
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
                firstName = input.firstName.trim().ifBlank { null },
                lastName = input.lastName.trim().ifBlank { null },
                companyName = input.companyName.trim().ifBlank { null },
                vatNumber = input.vatNumber.trim().ifBlank { null },
                taxCode = input.taxCode.trim().ifBlank { null },
                phone = input.phone.trim().ifBlank { null },
                email = input.email.trim().ifBlank { null },
                address = input.address.trim().ifBlank { null },
                notes = input.notes.trim().ifBlank { null },
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

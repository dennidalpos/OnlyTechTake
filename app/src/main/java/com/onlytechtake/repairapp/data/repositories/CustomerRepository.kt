package com.onlytechtake.repairapp.data.repositories

import com.onlytechtake.repairapp.core.demo.DemoMode
import com.onlytechtake.repairapp.data.local.dao.CustomerDao
import com.onlytechtake.repairapp.data.local.entities.CustomerEntity
import com.onlytechtake.repairapp.data.local.entities.CustomerType
import com.onlytechtake.repairapp.core.utils.capitalizeFirst
import com.onlytechtake.repairapp.domain.workspace.CustomerInput
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class CustomerRepository(
    private val customerDao: CustomerDao,
    private val clockMillis: () -> Long = System::currentTimeMillis,
) {
    fun observeRecent(): Flow<List<CustomerEntity>> {
        if (DemoMode.isEnabled) {
            return flowOf(listOf(DemoMode.demoCustomer))
        }
        return customerDao.observeRecent()
    }

    fun search(query: String): Flow<List<CustomerEntity>> {
        if (DemoMode.isEnabled) {
            val q = query.trim().lowercase()
            return if (DemoMode.demoCustomer.firstName?.lowercase()?.contains(q) == true ||
                DemoMode.demoCustomer.lastName?.lowercase()?.contains(q) == true
            ) {
                flowOf(listOf(DemoMode.demoCustomer))
            } else {
                flowOf(emptyList())
            }
        }
        val trimmed = query.trim()
        val sanitized = trimmed.split(" ").filter { it.isNotBlank() }.joinToString(" ") { "$it*" }
        return customerDao.search(sanitized)
    }

    suspend fun findById(id: String): CustomerEntity? {
        if (DemoMode.isEnabled && id == DemoMode.demoCustomer.id) {
            return DemoMode.demoCustomer
        }
        return customerDao.findById(id)
    }

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

package com.onlytechtake.repairapp.domain.repositories

import com.onlytechtake.repairapp.data.local.entities.CustomerEntity
import com.onlytechtake.repairapp.domain.auth.AuthenticatedUser
import com.onlytechtake.repairapp.domain.workspace.CustomerInput
import kotlinx.coroutines.flow.Flow

interface CustomerRepository {
    fun observeRecent(): Flow<List<CustomerEntity>>

    fun observeAll(): Flow<List<CustomerEntity>>

    fun search(query: String): Flow<List<CustomerEntity>>

    suspend fun findById(id: String): CustomerEntity?

    suspend fun save(
        actor: AuthenticatedUser?,
        input: CustomerInput,
    ): Result<CustomerEntity>

    suspend fun delete(
        actor: AuthenticatedUser?,
        id: String,
    ): Result<Unit>

    fun isValid(input: CustomerInput): Boolean
}

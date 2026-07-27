package com.onlytechtake.repairapp.domain.repositories

import com.onlytechtake.repairapp.domain.auth.AuthenticatedUser
import com.onlytechtake.repairapp.domain.models.Customer
import com.onlytechtake.repairapp.domain.workspace.CustomerInput
import kotlinx.coroutines.flow.Flow

interface CustomerRepository {
    fun observeRecent(): Flow<List<Customer>>

    fun observeAll(): Flow<List<Customer>>

    fun search(query: String): Flow<List<Customer>>

    suspend fun findById(id: String): Customer?

    suspend fun save(
        actor: AuthenticatedUser?,
        input: CustomerInput,
    ): Result<Customer>

    suspend fun delete(
        actor: AuthenticatedUser?,
        id: String,
    ): Result<Unit>

    fun isValid(input: CustomerInput): Boolean
}

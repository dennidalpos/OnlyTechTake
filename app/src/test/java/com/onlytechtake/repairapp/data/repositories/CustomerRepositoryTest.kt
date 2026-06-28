package com.onlytechtake.repairapp.data.repositories

import android.content.Context
import com.onlytechtake.repairapp.data.local.dao.CustomerDao
import com.onlytechtake.repairapp.data.local.entities.CustomerEntity
import com.onlytechtake.repairapp.data.local.entities.UserPermission
import com.onlytechtake.repairapp.data.local.entities.UserRole
import com.onlytechtake.repairapp.domain.auth.AuthenticatedUser
import com.onlytechtake.repairapp.domain.workspace.CustomerInput
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomerRepositoryTest {
    private val stubDao =
        object : CustomerDao {
            var lastUpserted: CustomerEntity? = null

            override fun observeRecent(): Flow<List<CustomerEntity>> = emptyFlow()

            override fun observeAll(): Flow<List<CustomerEntity>> = emptyFlow()

            override fun search(query: String): Flow<List<CustomerEntity>> = emptyFlow()

            override suspend fun findById(id: String): CustomerEntity? = null

            override suspend fun upsert(customer: CustomerEntity) {
                lastUpserted = customer
            }

            override suspend fun upsertAll(customers: List<CustomerEntity>) { // stub
            }

            override suspend fun deleteById(id: String) { // stub
            }
        }

    private val context = mockk<Context>(relaxed = true)
    private val repository = CustomerRepository(context, stubDao)
    private val manager =
        AuthenticatedUser(
            id = "manager",
            displayName = "Manager",
            username = "manager",
            role = UserRole.OPERATOR,
            permissions = setOf(UserPermission.MANAGE_CUSTOMERS),
        )
    private val operator =
        AuthenticatedUser(
            id = "operator",
            displayName = "Operator",
            username = "operator",
            role = UserRole.OPERATOR,
            permissions = emptySet(),
        )

    @Test
    fun normalizesFieldsOnSave() =
        runBlocking {
            val input =
                CustomerInput(
                    firstName = " mario ",
                    lastName = " rossi ",
                    email = " MARIO.ROSSI@EXAMPLE.COM ",
                    vatNumber = " it12345678901 ",
                    taxCode = " rssmra80a01h501z ",
                    address = " via roma 1 ",
                    notes = " alcune note ",
                )

            repository.save(manager, input)

            val saved = stubDao.lastUpserted!!
            assertEquals("Mario", saved.firstName)
            assertEquals("Rossi", saved.lastName)
            assertEquals("mario.rossi@example.com", saved.email)
            assertEquals("IT12345678901", saved.vatNumber)
            assertEquals("RSSMRA80A01H501Z", saved.taxCode)
            assertEquals("Via roma 1", saved.address)
            assertEquals("Alcune note", saved.notes)
        }

    @Test
    fun saveFailsWithoutManageCustomersPermission() =
        runBlocking {
            val result = repository.save(operator, validCustomerInput())

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is SecurityException)
            assertEquals(null, stubDao.lastUpserted)
        }

    @Test
    fun saveSucceedsWithManageCustomersPermission() =
        runBlocking {
            val result = repository.save(manager, validCustomerInput())

            assertTrue(result.isSuccess)
            assertEquals("Mario", stubDao.lastUpserted?.firstName)
        }

    private fun validCustomerInput() =
        CustomerInput(
            firstName = "Mario",
            lastName = "Rossi",
            phone = "+39123456789",
        )
}

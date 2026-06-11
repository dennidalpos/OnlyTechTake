package com.onlytechtake.repairapp.data.repositories

import com.onlytechtake.repairapp.data.local.dao.CustomerDao
import com.onlytechtake.repairapp.data.local.entities.CustomerEntity
import com.onlytechtake.repairapp.domain.workspace.CustomerInput
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class CustomerRepositoryTest {
    private val stubDao = object : CustomerDao {
        var lastUpserted: CustomerEntity? = null
        override fun observeRecent(): Flow<List<CustomerEntity>> = TODO()
        override fun observeAll(): Flow<List<CustomerEntity>> = TODO()
        override fun search(query: String): Flow<List<CustomerEntity>> = TODO()
        override suspend fun findById(id: String): CustomerEntity? = null
        override suspend fun upsert(customer: CustomerEntity) { lastUpserted = customer }
    }

    private val repository = CustomerRepository(stubDao)

    @Test
    fun normalizesFieldsOnSave() = runBlocking {
        val input = CustomerInput(
            firstName = " mario ",
            lastName = " rossi ",
            email = " MARIO.ROSSI@EXAMPLE.COM ",
            vatNumber = " it12345678901 ",
            taxCode = " rssmra80a01h501z ",
            address = " via roma 1 ",
            notes = " alcune note "
        )

        repository.save(input)

        val saved = stubDao.lastUpserted!!
        assertEquals("Mario", saved.firstName)
        assertEquals("Rossi", saved.lastName)
        assertEquals("mario.rossi@example.com", saved.email)
        assertEquals("IT12345678901", saved.vatNumber)
        assertEquals("RSSMRA80A01H501Z", saved.taxCode)
        assertEquals("Via roma 1", saved.address)
        assertEquals("Alcune note", saved.notes)
    }
}

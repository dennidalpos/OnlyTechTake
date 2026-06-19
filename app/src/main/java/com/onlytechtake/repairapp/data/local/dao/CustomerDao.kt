package com.onlytechtake.repairapp.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.onlytechtake.repairapp.data.local.entities.CustomerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {
    @Upsert
    suspend fun upsert(customer: CustomerEntity)

    @Upsert
    suspend fun upsertAll(customers: List<CustomerEntity>)

    @Query("SELECT * FROM customers ORDER BY updatedAt DESC")
    fun observeRecent(): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): CustomerEntity?

    @Query(
        """
        SELECT * FROM customers
        WHERE :query = ''
           OR rowid IN (SELECT rowid FROM customers_fts WHERE customers_fts MATCH :query || '*')
           OR id IN (SELECT customerId FROM repair_orders WHERE rowid IN (SELECT rowid FROM repair_orders_fts WHERE repair_orders_fts MATCH :query || '*'))
        ORDER BY updatedAt DESC
        """,
    )
    fun search(query: String): Flow<List<CustomerEntity>>
}

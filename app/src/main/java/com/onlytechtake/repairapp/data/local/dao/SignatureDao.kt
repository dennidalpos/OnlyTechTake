package com.onlytechtake.repairapp.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.onlytechtake.repairapp.data.local.entities.SignatureEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SignatureDao {
    @Upsert
    suspend fun upsert(signature: SignatureEntity)

    @Upsert
    suspend fun upsertAll(signatures: List<SignatureEntity>)

    @Query("SELECT * FROM signatures WHERE orderId = :orderId ORDER BY signedAt DESC")
    fun observeForOrder(orderId: String): Flow<List<SignatureEntity>>

    @Query("SELECT * FROM signatures WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): SignatureEntity?

    @Query("SELECT * FROM signatures WHERE orderId IN (:orderIds)")
    suspend fun findForOrders(orderIds: List<String>): List<SignatureEntity>

    @Query("SELECT COUNT(*) FROM signatures WHERE orderId = :orderId")
    suspend fun countForOrder(orderId: String): Int

    @Query("SELECT orderId, COUNT(*) AS signatures FROM signatures GROUP BY orderId")
    fun observeAggregates(): Flow<List<SignatureAggregate>>

    @Query("DELETE FROM signatures WHERE id = :id")
    suspend fun deleteById(id: String): Int

    @Query("DELETE FROM signatures WHERE orderId = :orderId")
    suspend fun deleteByOrderId(orderId: String)

    @Query("SELECT localPath FROM signatures")
    suspend fun getAllPaths(): List<String>
}

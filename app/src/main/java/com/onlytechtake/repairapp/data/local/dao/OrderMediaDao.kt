package com.onlytechtake.repairapp.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.onlytechtake.repairapp.data.local.entities.OrderMediaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OrderMediaDao {
    @Upsert
    suspend fun upsert(media: OrderMediaEntity)

    @Upsert
    suspend fun upsertAll(media: List<OrderMediaEntity>)

    @Query("SELECT * FROM order_media WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): OrderMediaEntity?

    @Query("SELECT * FROM order_media WHERE orderId = :orderId ORDER BY capturedAt DESC")
    fun observeForOrder(orderId: String): Flow<List<OrderMediaEntity>>

    @Query("DELETE FROM order_media WHERE id = :id")
    suspend fun deleteById(id: String): Int

    @Query("DELETE FROM order_media WHERE orderId = :orderId")
    suspend fun deleteByOrderId(orderId: String)

    @Query("SELECT localPath FROM order_media")
    suspend fun getAllPaths(): List<String>
}

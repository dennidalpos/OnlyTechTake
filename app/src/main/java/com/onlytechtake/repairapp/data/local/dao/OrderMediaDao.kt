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

    @Query("SELECT * FROM order_media WHERE orderId IN (:orderIds)")
    suspend fun findForOrders(orderIds: List<String>): List<OrderMediaEntity>

    @Query(
        """
        SELECT
            orderId,
            COUNT(CASE WHEN UPPER(mediaType) IN ('PHOTO', 'IMAGE') OR (mimeType LIKE 'image/%' AND UPPER(mediaType) != 'FINAL_PHOTO') THEN 1 END) AS images,
            COUNT(CASE WHEN NOT (UPPER(mediaType) IN ('PHOTO', 'IMAGE', 'FINAL_PHOTO') OR mimeType LIKE 'image/%') THEN 1 END) AS documents,
            COUNT(CASE WHEN UPPER(mediaType) = 'FINAL_PHOTO' THEN 1 END) AS finalPhotos
        FROM order_media
        GROUP BY orderId
        """,
    )
    fun observeAggregates(): Flow<List<OrderMediaAggregate>>

    @Query("DELETE FROM order_media WHERE id = :id")
    suspend fun deleteById(id: String): Int

    @Query("DELETE FROM order_media WHERE orderId = :orderId")
    suspend fun deleteByOrderId(orderId: String)

    @Query("SELECT localPath FROM order_media")
    suspend fun getAllPaths(): List<String>
}

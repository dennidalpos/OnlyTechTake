package com.onlytechtake.repairapp.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.onlytechtake.repairapp.data.local.entities.OrderDocumentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OrderDocumentDao {
    @Upsert
    suspend fun upsert(document: OrderDocumentEntity)

    @Query(
        """
        SELECT * FROM order_documents
        WHERE orderId = :orderId
        ORDER BY createdAt DESC
        """,
    )
    fun observeByOrder(orderId: String): Flow<List<OrderDocumentEntity>>

    @Query("SELECT * FROM order_documents WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): OrderDocumentEntity?

    @Query("UPDATE order_documents SET sharedAt = :sharedAt WHERE id = :id")
    suspend fun markShared(
        id: String,
        sharedAt: Long,
    )

    @Query("DELETE FROM order_documents WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM order_documents WHERE orderId = :orderId")
    suspend fun deleteByOrderId(orderId: String)

    @Query("SELECT localPath FROM order_documents")
    suspend fun getAllPaths(): List<String>
}

package com.onlytechtake.repairapp.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.onlytechtake.repairapp.data.local.entities.MessageLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageLogDao {
    @Upsert
    suspend fun upsert(log: MessageLogEntity)

    @Query("SELECT * FROM message_logs WHERE orderId = :orderId ORDER BY createdAt DESC")
    fun observeForOrder(orderId: String): Flow<List<MessageLogEntity>>

    @Query("SELECT * FROM message_logs WHERE orderId = :orderId ORDER BY createdAt DESC LIMIT 1")
    suspend fun findLatestForOrder(orderId: String): MessageLogEntity?

    @Query("DELETE FROM message_logs WHERE orderId = :orderId")
    suspend fun deleteByOrderId(orderId: String)
}

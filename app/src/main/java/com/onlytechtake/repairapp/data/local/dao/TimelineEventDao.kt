package com.onlytechtake.repairapp.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.onlytechtake.repairapp.data.local.entities.TimelineEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TimelineEventDao {
    @Upsert
    suspend fun upsert(event: TimelineEventEntity)

    @Upsert
    suspend fun upsertAll(events: List<TimelineEventEntity>)

    @Query("SELECT * FROM timeline_events WHERE orderId = :orderId ORDER BY createdAt DESC")
    fun observeForOrder(orderId: String): Flow<List<TimelineEventEntity>>

    @Query("DELETE FROM timeline_events WHERE orderId = :orderId")
    suspend fun deleteByOrderId(orderId: String)
}

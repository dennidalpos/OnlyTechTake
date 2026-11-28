package com.onlytechtake.repairapp.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.onlytechtake.repairapp.data.local.entities.MessageTemplateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageTemplateDao {
    @Upsert
    suspend fun upsert(template: MessageTemplateEntity)

    @Upsert
    suspend fun upsertAll(templates: List<MessageTemplateEntity>)

    @Query("SELECT * FROM message_templates ORDER BY code ASC")
    fun observeAll(): Flow<List<MessageTemplateEntity>>

    @Query("SELECT * FROM message_templates WHERE code = :code AND active = 1 LIMIT 1")
    suspend fun findActiveByCode(code: String): MessageTemplateEntity?

    @Query("SELECT * FROM message_templates WHERE code = :code LIMIT 1")
    suspend fun findByCode(code: String): MessageTemplateEntity?
}

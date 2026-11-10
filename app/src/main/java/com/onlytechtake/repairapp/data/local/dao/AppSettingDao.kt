package com.onlytechtake.repairapp.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.onlytechtake.repairapp.data.local.entities.AppSettingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppSettingDao {
    @Upsert
    suspend fun upsert(setting: AppSettingEntity)

    @Query("SELECT * FROM app_settings ORDER BY `key` ASC")
    fun observeAll(): Flow<List<AppSettingEntity>>

    @Query("SELECT * FROM app_settings WHERE `key` = :key LIMIT 1")
    suspend fun findByKey(key: String): AppSettingEntity?

    @Query("DELETE FROM app_settings WHERE `key` IN (:keys)")
    suspend fun deleteByKeys(keys: List<String>): Int
}

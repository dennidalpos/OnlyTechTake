package com.onlytechtake.repairapp.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DashboardDao {
    @Query("SELECT COUNT(*) FROM users WHERE role = 'OPERATOR'")
    fun observeOperatorCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM repair_orders")
    fun observeActiveOrderCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM customers")
    fun observeCustomerCount(): Flow<Int>
}

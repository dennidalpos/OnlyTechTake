package com.onlytechtake.repairapp.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DashboardDao {
    @Query(
        """
        SELECT
            (SELECT COUNT(*) FROM users WHERE role = 'OPERATOR') as operatorCount,
            (SELECT COUNT(*) FROM customers) as customerCount,
            (SELECT COUNT(*) FROM repair_orders) as activeOrderCount
        """,
    )
    fun observeSummary(): Flow<DashboardSummaryProjection>
}

data class DashboardSummaryProjection(
    val operatorCount: Int,
    val customerCount: Int,
    val activeOrderCount: Int,
)

package com.onlytechtake.repairapp.data.repositories

import com.onlytechtake.repairapp.data.local.dao.DashboardDao
import com.onlytechtake.repairapp.domain.models.AppSummary
import com.onlytechtake.repairapp.domain.repositories.RepairAppRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DefaultRepairAppRepository(
    private val dashboardDao: DashboardDao,
) : RepairAppRepository {
    override fun observeSummary(): Flow<AppSummary> =
        dashboardDao.observeSummary().map { projection ->
            AppSummary(
                title = "OnlyTechTake",
                operatorCount = projection.operatorCount,
                customerCount = projection.customerCount,
                activeOrderCount = projection.activeOrderCount,
            )
        }
}

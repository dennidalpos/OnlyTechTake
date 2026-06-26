package com.onlytechtake.repairapp.data.repositories

import com.onlytechtake.repairapp.data.local.dao.DashboardDao
import com.onlytechtake.repairapp.domain.models.AppSummary
import com.onlytechtake.repairapp.domain.repositories.RepairAppRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class DefaultRepairAppRepository(
    private val dashboardDao: DashboardDao,
) : RepairAppRepository {
    override fun observeSummary(): Flow<AppSummary> =
        combine(
            dashboardDao.observeOperatorCount(),
            dashboardDao.observeCustomerCount(),
            dashboardDao.observeActiveOrderCount(),
        ) { operatorCount, customerCount, activeOrderCount ->
            AppSummary(
                title = "OnlyTechTake",
                operatorCount = operatorCount,
                customerCount = customerCount,
                activeOrderCount = activeOrderCount,
            )
        }
}

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
            dashboardDao.observeUserCount(),
            dashboardDao.observeCustomerCount(),
            dashboardDao.observeActiveOrderCount(),
        ) { userCount, customerCount, activeOrderCount ->
            AppSummary(
                title = "OnlyTechTake",
                userCount = userCount,
                customerCount = customerCount,
                activeOrderCount = activeOrderCount,
            )
        }
}

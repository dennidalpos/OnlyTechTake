package com.onlytechtake.repairapp.domain.repositories

import com.onlytechtake.repairapp.domain.models.AppSummary
import kotlinx.coroutines.flow.Flow

interface RepairAppRepository {
    fun observeSummary(): Flow<AppSummary>
}

package com.onlytechtake.repairapp.core.di

import android.content.Context
import androidx.compose.runtime.staticCompositionLocalOf
import com.onlytechtake.repairapp.data.local.db.RepairAppDatabase
import com.onlytechtake.repairapp.data.repositories.DefaultRepairAppRepository
import com.onlytechtake.repairapp.domain.repositories.RepairAppRepository

class AppContainer(
    val context: Context,
) {
    private val dataContainer = DataContainer(context)
    private val authContainer by lazy { AuthContainer(context, dataContainer) }
    private val mediaContainer by lazy { MediaContainer(context, dataContainer) }
    private val orderContainer by lazy { OrderContainer(context, dataContainer, mediaContainer) }
    private val settingsContainer by lazy {
        SettingsContainer(
            context,
            dataContainer,
            authContainer,
            mediaContainer,
        )
    }

    val repairAppRepository: RepairAppRepository by lazy {
        DefaultRepairAppRepository(
            dashboardDao = dataContainer.database.dashboardDao(),
        )
    }

    val authRepository get() = authContainer.authRepository
    val adminUserRepository get() = authContainer.adminUserRepository
    val database get() = dataContainer.database

    val customerRepository get() = orderContainer.customerRepository
    val catalogRepository get() = orderContainer.catalogRepository
    val orderWorkflowRepository get() = orderContainer.orderWorkflowRepository
    val createIntakeOrderUseCase get() = orderContainer.createIntakeOrderUseCase

    val orderMediaRepository get() = mediaContainer.orderMediaRepository
    val orderDocumentRepository get() = mediaContainer.orderDocumentRepository
    val shareDocumentIntentFactory get() = mediaContainer.shareDocumentIntentFactory
    val generateOrderStatusPdfUseCase get() = orderContainer.generateOrderStatusPdfUseCase
    val orderFileCleanupService get() = mediaContainer.orderFileCleanupService

    val templateRepository get() = settingsContainer.templateRepository
    val backupRepository get() = settingsContainer.backupRepository
    val backupReminderPreferences get() = settingsContainer.backupReminderPreferences
    val securityPreferences get() = settingsContainer.securityPreferences
    val dataResetRepository get() = settingsContainer.dataResetRepository
    val storageStatsService get() = settingsContainer.storageStatsService
    val diagnosticLogService get() = settingsContainer.diagnosticLogService
    val diagnosticLogger get() = dataContainer.diagnosticLogger

    fun close() {
        RepairAppDatabase.closeInstance()
    }
}

val LocalAppContainer =
    staticCompositionLocalOf<AppContainer> {
        error("AppContainer is not available")
    }

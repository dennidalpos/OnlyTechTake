package com.onlytechtake.repairapp.core.di

import android.content.Context
import androidx.compose.runtime.staticCompositionLocalOf
import com.onlytechtake.repairapp.domain.repositories.RepairAppRepository
import com.onlytechtake.repairapp.data.repositories.DefaultRepairAppRepository

class AppContainer(context: Context) {
    private val dataContainer = DataContainer(context)
    private val authContainer = AuthContainer(context, dataContainer)
    private val mediaContainer = MediaContainer(context, dataContainer)
    private val orderContainer = OrderContainer(context, dataContainer, mediaContainer)
    private val settingsContainer = SettingsContainer(context, dataContainer, authContainer, mediaContainer)

    val repairAppRepository: RepairAppRepository = DefaultRepairAppRepository(
        dashboardDao = dataContainer.database.dashboardDao(),
    )

    val authRepository = authContainer.authRepository
    val adminUserRepository = authContainer.adminUserRepository
    
    val customerRepository = orderContainer.customerRepository
    val catalogRepository = orderContainer.catalogRepository
    val orderWorkflowRepository = orderContainer.orderWorkflowRepository
    val createIntakeOrderUseCase = orderContainer.createIntakeOrderUseCase

    val orderMediaRepository = mediaContainer.orderMediaRepository
    val orderDocumentRepository = mediaContainer.orderDocumentRepository
    val shareDocumentIntentFactory = mediaContainer.shareDocumentIntentFactory
    val generateOrderStatusPdfUseCase = orderContainer.generateOrderStatusPdfUseCase
    val orderFileCleanupService = mediaContainer.orderFileCleanupService

    val templateRepository = settingsContainer.templateRepository
    val backupRepository = settingsContainer.backupRepository
    val backupReminderPreferences = settingsContainer.backupReminderPreferences
    val securityPreferences = settingsContainer.securityPreferences
    val dataResetRepository = settingsContainer.dataResetRepository
    val storageStatsService = settingsContainer.storageStatsService

    fun close() {
        dataContainer.database.close()
    }
}

val LocalAppContainer = staticCompositionLocalOf<AppContainer> {
    error("AppContainer is not available")
}

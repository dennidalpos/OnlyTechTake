package com.onlytechtake.repairapp.core.di

import android.content.Context
import androidx.compose.runtime.staticCompositionLocalOf
import com.onlytechtake.repairapp.data.local.db.RepairAppDatabase
import com.onlytechtake.repairapp.data.repositories.AdminUserRepository
import com.onlytechtake.repairapp.data.repositories.BackupRepository
import com.onlytechtake.repairapp.data.repositories.CatalogRepository
import com.onlytechtake.repairapp.data.repositories.CustomerRepository
import com.onlytechtake.repairapp.data.repositories.DataResetRepository
import com.onlytechtake.repairapp.data.repositories.OrderDocumentRepository
import com.onlytechtake.repairapp.data.repositories.OrderFileCleanupService
import com.onlytechtake.repairapp.data.repositories.OrderMediaRepository
import com.onlytechtake.repairapp.data.repositories.OrderWorkflowRepository
import com.onlytechtake.repairapp.data.repositories.TemplateRepository
import com.onlytechtake.repairapp.data.services.DiagnosticLogService
import com.onlytechtake.repairapp.data.services.StorageStatsService
import com.onlytechtake.repairapp.data.preferences.BackupReminderPreferences
import com.onlytechtake.repairapp.data.preferences.OwnerPreferences
import com.onlytechtake.repairapp.data.preferences.SecurityPreferences
import com.onlytechtake.repairapp.core.error.DiagnosticLogger
import com.onlytechtake.repairapp.core.share.ShareDocumentIntentFactory
import com.onlytechtake.repairapp.data.local.db.RepairAppDatabase as Database
import com.onlytechtake.repairapp.demo.DataSeeder
import com.onlytechtake.repairapp.demo.DataSeederImpl
import com.onlytechtake.repairapp.domain.auth.AuthRepository
import com.onlytechtake.repairapp.domain.repositories.RepairAppRepository
import com.onlytechtake.repairapp.domain.workspace.CreateIntakeOrderUseCase
import com.onlytechtake.repairapp.domain.workspace.GenerateOrderStatusPdfUseCase

interface AppContainer {
    val context: Context
    val repairAppRepository: RepairAppRepository
    val authRepository: AuthRepository
    val adminUserRepository: AdminUserRepository
    val database: Database
    val customerRepository: CustomerRepository
    val catalogRepository: CatalogRepository
    val orderWorkflowRepository: OrderWorkflowRepository
    val createIntakeOrderUseCase: CreateIntakeOrderUseCase
    val orderMediaRepository: OrderMediaRepository
    val orderDocumentRepository: OrderDocumentRepository
    val shareDocumentIntentFactory: ShareDocumentIntentFactory
    val generateOrderStatusPdfUseCase: GenerateOrderStatusPdfUseCase
    val orderFileCleanupService: OrderFileCleanupService
    val templateRepository: TemplateRepository
    val backupRepository: BackupRepository
    val backupReminderPreferences: BackupReminderPreferences
    val ownerPreferences: OwnerPreferences
    val securityPreferences: SecurityPreferences
    val dataResetRepository: DataResetRepository
    val storageStatsService: StorageStatsService
    val diagnosticLogService: DiagnosticLogService
    val diagnosticLogger: DiagnosticLogger
    val dataSeeder: DataSeeder
    fun close()
}

class DefaultAppContainer(
    override val context: Context,
) : AppContainer {
    private val dataContainer = DataContainer(context)
    override val securityPreferences by lazy {
        com.onlytechtake.repairapp.data.preferences
            .SecurityPreferences(context.applicationContext)
    }
    private val authContainer by lazy {
        AuthContainer(
            context,
            dataContainer,
            securityPreferences,
        )
    }
    private val mediaContainer by lazy { MediaContainer(context, dataContainer) }
    private val settingsContainer by lazy {
        SettingsContainer(
            context,
            dataContainer,
            authContainer,
            mediaContainer,
            securityPreferences,
        )
    }
    private val orderContainer by lazy {
        OrderContainer(
            context,
            dataContainer,
            mediaContainer,
            settingsContainer.ownerPreferences,
        )
    }

    override val repairAppRepository: RepairAppRepository by lazy {
        com.onlytechtake.repairapp.data.repositories.DefaultRepairAppRepository(
            dashboardDao = dataContainer.database.dashboardDao(),
        )
    }

    override val authRepository get() = authContainer.authRepository
    override val adminUserRepository get() = authContainer.adminUserRepository
    override val database get() = dataContainer.database

    override val customerRepository get() = orderContainer.customerRepository
    override val catalogRepository get() = orderContainer.catalogRepository
    override val orderWorkflowRepository get() = orderContainer.orderWorkflowRepository
    override val createIntakeOrderUseCase get() = orderContainer.createIntakeOrderUseCase

    override val orderMediaRepository get() = mediaContainer.orderMediaRepository
    override val orderDocumentRepository get() = mediaContainer.orderDocumentRepository
    override val shareDocumentIntentFactory get() = mediaContainer.shareDocumentIntentFactory
    override val generateOrderStatusPdfUseCase get() = orderContainer.generateOrderStatusPdfUseCase
    override val orderFileCleanupService get() = mediaContainer.orderFileCleanupService

    override val templateRepository get() = settingsContainer.templateRepository
    override val backupRepository get() = settingsContainer.backupRepository
    override val backupReminderPreferences get() = settingsContainer.backupReminderPreferences
    override val ownerPreferences get() = settingsContainer.ownerPreferences
    override val dataResetRepository get() = settingsContainer.dataResetRepository
    override val storageStatsService get() = settingsContainer.storageStatsService
    override val diagnosticLogService get() = settingsContainer.diagnosticLogService
    override val diagnosticLogger get() = dataContainer.diagnosticLogger

    override val dataSeeder: DataSeeder by lazy { DataSeederImpl() }

    override fun close() {
        RepairAppDatabase.closeInstance()
    }
}

val LocalAppContainer =
    staticCompositionLocalOf<AppContainer> {
        error("AppContainer is not available")
    }

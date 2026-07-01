package com.onlytechtake.repairapp.core.di

import android.content.Context
import androidx.room.withTransaction
import com.onlytechtake.repairapp.core.error.DiagnosticLogger
import com.onlytechtake.repairapp.core.media.MediaOptimizer
import com.onlytechtake.repairapp.core.media.MediaStorageProvider
import com.onlytechtake.repairapp.core.security.PasswordHasher
import com.onlytechtake.repairapp.core.share.ShareDocumentIntentFactory
import com.onlytechtake.repairapp.data.local.db.RepairAppDatabase
import com.onlytechtake.repairapp.data.preferences.AuthPreferences
import com.onlytechtake.repairapp.data.preferences.BackupReminderPreferences
import com.onlytechtake.repairapp.data.preferences.OrdersPreferences
import com.onlytechtake.repairapp.data.preferences.OwnerPreferences
import com.onlytechtake.repairapp.data.preferences.SecurityPreferences
import com.onlytechtake.repairapp.data.preferences.UiPreferences
import com.onlytechtake.repairapp.data.repositories.AdminUserRepository
import com.onlytechtake.repairapp.data.repositories.BackupRepository
import com.onlytechtake.repairapp.data.repositories.CatalogRepository
import com.onlytechtake.repairapp.data.repositories.CustomerRepository
import com.onlytechtake.repairapp.data.repositories.DataResetRepository
import com.onlytechtake.repairapp.data.repositories.DefaultAuthRepository
import com.onlytechtake.repairapp.data.repositories.DefaultOrderDocumentRepository
import com.onlytechtake.repairapp.data.repositories.OrderDocumentFileStorage
import com.onlytechtake.repairapp.data.repositories.OrderDocumentRepository
import com.onlytechtake.repairapp.data.repositories.OrderFileCleanupService
import com.onlytechtake.repairapp.data.repositories.OrderMediaRepository
import com.onlytechtake.repairapp.data.repositories.OrderWorkflowRepository
import com.onlytechtake.repairapp.data.repositories.SignatureRepository
import com.onlytechtake.repairapp.data.repositories.TemplateRepository
import com.onlytechtake.repairapp.data.services.BackupService
import com.onlytechtake.repairapp.data.services.DiagnosticLogService
import com.onlytechtake.repairapp.data.services.StorageStatsService
import com.onlytechtake.repairapp.data.session.InMemorySessionStore
import com.onlytechtake.repairapp.domain.auth.AuthRepository
import com.onlytechtake.repairapp.domain.workspace.CreateIntakeOrderUseCase
import com.onlytechtake.repairapp.domain.workspace.GenerateOrderStatusPdfUseCase

class DataContainer(
    context: Context,
) {
    val database by lazy { RepairAppDatabase.getInstance(context) }
    val clockMillis: () -> Long = System::currentTimeMillis
    val diagnosticLogger by lazy { DiagnosticLogger(context) }
}

class AuthContainer(
    context: Context,
    dataContainer: DataContainer,
    securityPreferences: SecurityPreferences,
) {
    val sessionStore by lazy { InMemorySessionStore() }
    val authPreferences by lazy { AuthPreferences(context) }
    private val passwordHasher by lazy { PasswordHasher() }

    val authRepository: AuthRepository by lazy {
        DefaultAuthRepository(
            context = context,
            userDao = dataContainer.database.userDao(),
            auditLogDao = dataContainer.database.auditLogDao(),
            loginAttemptDao = dataContainer.database.loginAttemptDao(),
            appSettingDao = dataContainer.database.appSettingDao(),
            authPreferences = authPreferences,
            securityPreferences = securityPreferences,
            sessionStore = sessionStore,
            diagnosticLogger = dataContainer.diagnosticLogger,
            passwordHasher = passwordHasher,
        )
    }

    val adminUserRepository by lazy {
        AdminUserRepository(
            context = context.applicationContext,
            userDao = dataContainer.database.userDao(),
            auditLogDao = dataContainer.database.auditLogDao(),
            passwordHasher = passwordHasher,
        )
    }
}

class MediaContainer(
    context: Context,
    dataContainer: DataContainer,
) {
    val mediaStorageProvider by lazy { MediaStorageProvider(context.applicationContext) }
    val orderFileCleanupService by lazy {
        OrderFileCleanupService(
            context.applicationContext,
            mediaStorageProvider,
        )
    }
    private val mediaOptimizer by lazy {
        MediaOptimizer()
    }

    val orderMediaRepository by lazy {
        OrderMediaRepository(
            context = context.applicationContext,
            repairOrderDao = dataContainer.database.repairOrderDao(),
            orderMediaDao = dataContainer.database.orderMediaDao(),
            timelineEventDao = dataContainer.database.timelineEventDao(),
            fileCleanupService = orderFileCleanupService,
            mediaStorageProvider = mediaStorageProvider,
            database = dataContainer.database,
            mediaOptimizer = mediaOptimizer,
        )
    }

    val orderDocumentFileStorage by lazy { OrderDocumentFileStorage(mediaStorageProvider) }

    val orderDocumentRepository: OrderDocumentRepository by lazy {
        DefaultOrderDocumentRepository(
            context = context,
            dao = dataContainer.database.orderDocumentDao(),
            fileCleanupService = orderFileCleanupService,
        )
    }

    val shareDocumentIntentFactory by lazy { ShareDocumentIntentFactory(context) }
}

class OrderContainer(
    context: Context,
    dataContainer: DataContainer,
    mediaContainer: MediaContainer,
    private val ownerPreferences: OwnerPreferences,
) {
    val orderWorkflowRepository: OrderWorkflowRepository by lazy {
        OrderWorkflowRepository(
            context = context.applicationContext,
            repairOrderDao = dataContainer.database.repairOrderDao(),
            customerDao = dataContainer.database.customerDao(),
            categoryDao = dataContainer.database.orderCategoryDao(),
            userDao = dataContainer.database.userDao(),
            timelineEventDao = dataContainer.database.timelineEventDao(),
            orderMediaDao = dataContainer.database.orderMediaDao(),
            signatureDao = dataContainer.database.signatureDao(),
            messageLogDao = dataContainer.database.messageLogDao(),
            auditLogDao = dataContainer.database.auditLogDao(),
            database = dataContainer.database,
            fileCleanupService = mediaContainer.orderFileCleanupService,
        )
    }

    val customerRepository: CustomerRepository by lazy {
        CustomerRepository(
            context = context.applicationContext,
            customerDao = dataContainer.database.customerDao(),
            repairOrderDao = dataContainer.database.repairOrderDao(),
        )
    }

    val catalogRepository by lazy {
        CatalogRepository(
            context = context.applicationContext,
            categoryDao = dataContainer.database.orderCategoryDao(),
        )
    }

    private val signatureRepository by lazy {
        SignatureRepository(
            context = context.applicationContext,
            repairOrderDao = dataContainer.database.repairOrderDao(),
            signatureDao = dataContainer.database.signatureDao(),
            templateDao = dataContainer.database.messageTemplateDao(),
            settingDao = dataContainer.database.appSettingDao(),
            timelineEventDao = dataContainer.database.timelineEventDao(),
            fileCleanupService = mediaContainer.orderFileCleanupService,
            mediaStorageProvider = mediaContainer.mediaStorageProvider,
        )
    }

    val createIntakeOrderUseCase by lazy {
        CreateIntakeOrderUseCase(
            context = context.applicationContext,
            customerRepository = customerRepository,
            orderRepository = orderWorkflowRepository,
            orderMediaRepository = mediaContainer.orderMediaRepository,
            signatureRepository = signatureRepository,
        ) { block -> dataContainer.database.withTransaction { block() } }
    }

    val generateOrderStatusPdfUseCase by lazy {
        GenerateOrderStatusPdfUseCase(
            context = context.applicationContext,
            workflowRepository = orderWorkflowRepository,
            catalogRepository = catalogRepository,
            documentRepository = mediaContainer.orderDocumentRepository,
            userDao = dataContainer.database.userDao(),
            fileStorage = mediaContainer.orderDocumentFileStorage,
            ownerPreferences = ownerPreferences,
        )
    }
}

class SettingsContainer(
    context: Context,
    dataContainer: DataContainer,
    authContainer: AuthContainer,
    mediaContainer: MediaContainer,
    private val securityPreferences: SecurityPreferences,
) {
    val ownerPreferences by lazy { OwnerPreferences(context.applicationContext) }
    val backupReminderPreferences by lazy { BackupReminderPreferences(context.applicationContext) }
    val ordersPreferences by lazy { OrdersPreferences(context.applicationContext) }
    val uiPreferences by lazy { UiPreferences(context.applicationContext) }
    val storageStatsService by lazy {
        StorageStatsService(
            context.applicationContext,
            mediaContainer.mediaStorageProvider,
        )
    }
    val diagnosticLogService by lazy {
        DiagnosticLogService(
            context.applicationContext,
            dataContainer.diagnosticLogger,
        )
    }

    val backupService by lazy {
        BackupService(
            context = context.applicationContext,
            backupRepository = backupRepository,
            backupReminderPreferences = backupReminderPreferences,
        )
    }

    val templateRepository by lazy {
        TemplateRepository(
            context = context.applicationContext,
            templateDao = dataContainer.database.messageTemplateDao(),
            orderDao = dataContainer.database.repairOrderDao(),
            customerDao = dataContainer.database.customerDao(),
            categoryDao = dataContainer.database.orderCategoryDao(),
            settingDao = dataContainer.database.appSettingDao(),
            messageLogDao = dataContainer.database.messageLogDao(),
            timelineEventDao = dataContainer.database.timelineEventDao(),
            auditLogDao = dataContainer.database.auditLogDao(),
            ownerPreferences = ownerPreferences,
        )
    }

    val backupRepository by lazy {
        BackupRepository(
            context = context.applicationContext,
            categoryDao = dataContainer.database.orderCategoryDao(),
            templateDao = dataContainer.database.messageTemplateDao(),
            settingDao = dataContainer.database.appSettingDao(),
            auditLogDao = dataContainer.database.auditLogDao(),
            backupReminderPreferences = backupReminderPreferences,
            securityPreferences = securityPreferences,
            orderExportDao = dataContainer.database.orderExportDao(),
            database = dataContainer.database,
            authPreferences = authContainer.authPreferences,
            sessionStore = authContainer.sessionStore,
            mediaStorageProvider = mediaContainer.mediaStorageProvider,
        )
    }

    val dataResetRepository by lazy {
        DataResetRepository(
            context = context.applicationContext,
            database = dataContainer.database,
            auditLogDao = dataContainer.database.auditLogDao(),
            authPreferences = authContainer.authPreferences,
            sessionStore = authContainer.sessionStore,
            fileCleanupService = mediaContainer.orderFileCleanupService,
            mediaStorageProvider = mediaContainer.mediaStorageProvider,
        )
    }
}

package com.onlytechtake.repairapp.core.di

import android.content.Context
import androidx.room.withTransaction
import com.onlytechtake.repairapp.core.media.MediaStorageProvider
import com.onlytechtake.repairapp.core.security.PasswordHasher
import com.onlytechtake.repairapp.core.share.ShareDocumentIntentFactory
import com.onlytechtake.repairapp.data.local.db.RepairAppDatabase
import com.onlytechtake.repairapp.data.preferences.AuthPreferences
import com.onlytechtake.repairapp.data.preferences.BackupReminderPreferences
import com.onlytechtake.repairapp.data.preferences.SecurityPreferences
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
}

class AuthContainer(
    context: Context,
    dataContainer: DataContainer,
) {
    val sessionStore by lazy { InMemorySessionStore() }
    val authPreferences by lazy { AuthPreferences(context) }
    private val passwordHasher by lazy { PasswordHasher() }

    val authRepository: AuthRepository by lazy {
        DefaultAuthRepository(
            userDao = dataContainer.database.userDao(),
            auditLogDao = dataContainer.database.auditLogDao(),
            loginAttemptDao = dataContainer.database.loginAttemptDao(),
            appSettingDao = dataContainer.database.appSettingDao(),
            authPreferences = authPreferences,
            sessionStore = sessionStore,
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
        com.onlytechtake.repairapp.core.media
            .MediaOptimizer()
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
) {
    val orderWorkflowRepository by lazy {
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

    val customerRepository by lazy {
        CustomerRepository(
            customerDao = dataContainer.database.customerDao(),
        )
    }

    val catalogRepository by lazy {
        CatalogRepository(
            context = context.applicationContext,
            categoryDao = dataContainer.database.orderCategoryDao(),
        )
    }

    val signatureRepository by lazy {
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
        )
    }
}

class SettingsContainer(
    context: Context,
    dataContainer: DataContainer,
    authContainer: AuthContainer,
    mediaContainer: MediaContainer,
) {
    val backupReminderPreferences by lazy { BackupReminderPreferences(context.applicationContext) }
    val securityPreferences by lazy { SecurityPreferences(context.applicationContext) }
    val storageStatsService by lazy {
        StorageStatsService(
            context.applicationContext,
            mediaContainer.mediaStorageProvider,
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
            storageStatsService = storageStatsService,
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

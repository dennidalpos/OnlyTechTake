package com.onlytechtake.repairapp.core.di

import android.content.Context
import com.onlytechtake.repairapp.core.security.PasswordHasher
import com.onlytechtake.repairapp.core.share.ShareDocumentIntentFactory
import com.onlytechtake.repairapp.data.local.db.RepairAppDatabase
import com.onlytechtake.repairapp.data.preferences.AuthPreferences
import com.onlytechtake.repairapp.data.preferences.BackupReminderPreferences
import com.onlytechtake.repairapp.data.repositories.*
import com.onlytechtake.repairapp.data.services.StorageStatsService
import com.onlytechtake.repairapp.data.session.InMemorySessionStore
import com.onlytechtake.repairapp.domain.auth.AuthRepository
import com.onlytechtake.repairapp.domain.workspace.CreateIntakeOrderUseCase
import com.onlytechtake.repairapp.domain.workspace.GenerateOrderStatusPdfUseCase
import androidx.room.withTransaction

class DataContainer(context: Context) {
    val database = RepairAppDatabase.getInstance(context)
    val clockMillis: () -> Long = System::currentTimeMillis
}

class AuthContainer(context: Context, dataContainer: DataContainer) {
    val sessionStore = InMemorySessionStore()
    val authPreferences = AuthPreferences(context)
    private val passwordHasher = PasswordHasher()

    val authRepository: AuthRepository = DefaultAuthRepository(
        userDao = dataContainer.database.userDao(),
        auditLogDao = dataContainer.database.auditLogDao(),
        loginAttemptDao = dataContainer.database.loginAttemptDao(),
        authPreferences = authPreferences,
        sessionStore = sessionStore,
        passwordHasher = passwordHasher,
    )

    val adminUserRepository = AdminUserRepository(
        context = context.applicationContext,
        userDao = dataContainer.database.userDao(),
        auditLogDao = dataContainer.database.auditLogDao(),
        passwordHasher = passwordHasher,
    )
}

class MediaContainer(context: Context, dataContainer: DataContainer) {
    val orderFileCleanupService = OrderFileCleanupService(context.applicationContext)
    
    val orderMediaRepository = OrderMediaRepository(
        context = context.applicationContext,
        repairOrderDao = dataContainer.database.repairOrderDao(),
        orderMediaDao = dataContainer.database.orderMediaDao(),
        timelineEventDao = dataContainer.database.timelineEventDao(),
        fileCleanupService = orderFileCleanupService,
        database = dataContainer.database,
    )

    val orderDocumentFileStorage = OrderDocumentFileStorage(context.applicationContext)

    val orderDocumentRepository: OrderDocumentRepository = DefaultOrderDocumentRepository(
        dao = dataContainer.database.orderDocumentDao(),
        fileCleanupService = orderFileCleanupService,
    )

    val shareDocumentIntentFactory = ShareDocumentIntentFactory(context)

    // Temporary repo for PDF generator until all use OrderContainer's one
    private val workflowRepo = OrderWorkflowRepository(
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
    )

    val generateOrderStatusPdfUseCase = GenerateOrderStatusPdfUseCase(
        context = context.applicationContext,
        workflowRepository = workflowRepo,
        catalogRepository = CatalogRepository(
            context = context.applicationContext,
            categoryDao = dataContainer.database.orderCategoryDao(),
        ),
        documentRepository = orderDocumentRepository,
        userDao = dataContainer.database.userDao(),
        fileStorage = orderDocumentFileStorage,
    )
}

class OrderContainer(context: Context, dataContainer: DataContainer, mediaContainer: MediaContainer) {
    val orderWorkflowRepository = OrderWorkflowRepository(
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
    )

    val customerRepository = CustomerRepository(
        customerDao = dataContainer.database.customerDao(),
    )

    val catalogRepository = CatalogRepository(
        context = context.applicationContext,
        categoryDao = dataContainer.database.orderCategoryDao(),
    )

    val signatureRepository = SignatureRepository(
        context = context.applicationContext,
        repairOrderDao = dataContainer.database.repairOrderDao(),
        signatureDao = dataContainer.database.signatureDao(),
        templateDao = dataContainer.database.messageTemplateDao(),
        timelineEventDao = dataContainer.database.timelineEventDao(),
        fileCleanupService = mediaContainer.orderFileCleanupService,
    )

    val createIntakeOrderUseCase = CreateIntakeOrderUseCase(
        context = context.applicationContext,
        customerRepository = customerRepository,
        orderRepository = orderWorkflowRepository,
        orderMediaRepository = mediaContainer.orderMediaRepository,
        signatureRepository = signatureRepository,
    ) { block -> dataContainer.database.withTransaction { block() } }
}

class SettingsContainer(context: Context, dataContainer: DataContainer, authContainer: AuthContainer, mediaContainer: MediaContainer) {
    val backupReminderPreferences = BackupReminderPreferences(context.applicationContext)
    val storageStatsService = StorageStatsService(context.applicationContext)

    val templateRepository = TemplateRepository(
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

    val backupRepository = BackupRepository(
        context = context.applicationContext,
        categoryDao = dataContainer.database.orderCategoryDao(),
        templateDao = dataContainer.database.messageTemplateDao(),
        settingDao = dataContainer.database.appSettingDao(),
        auditLogDao = dataContainer.database.auditLogDao(),
        orderExportDao = dataContainer.database.orderExportDao(),
        database = dataContainer.database,
        authPreferences = authContainer.authPreferences,
        sessionStore = authContainer.sessionStore,
        storageStatsService = storageStatsService,
    )

    val dataResetRepository = DataResetRepository(
        context = context.applicationContext,
        database = dataContainer.database,
        auditLogDao = dataContainer.database.auditLogDao(),
        authPreferences = authContainer.authPreferences,
        sessionStore = authContainer.sessionStore,
        fileCleanupService = mediaContainer.orderFileCleanupService,
    )
}

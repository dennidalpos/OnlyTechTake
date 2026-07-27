package com.onlytechtake.repairapp.core.di

import android.content.Context
import com.onlytechtake.repairapp.core.error.DiagnosticLogger
import com.onlytechtake.repairapp.core.media.MediaOptimizer
import com.onlytechtake.repairapp.core.media.MediaStorageProvider
import com.onlytechtake.repairapp.core.security.PasswordHasher
import com.onlytechtake.repairapp.data.local.dao.*
import com.onlytechtake.repairapp.data.local.db.RepairAppDatabase
import com.onlytechtake.repairapp.data.preferences.*
import com.onlytechtake.repairapp.data.repositories.*
import com.onlytechtake.repairapp.data.session.InMemorySessionStore
import com.onlytechtake.repairapp.domain.auth.AuthRepository
import com.onlytechtake.repairapp.domain.repositories.OrderRepository
import com.onlytechtake.repairapp.domain.repositories.RepairAppRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import com.onlytechtake.repairapp.domain.repositories.CustomerRepository as CustomerRepositoryInterface

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    @Provides
    @Singleton
    fun provideAuthRepository(
        @ApplicationContext context: Context,
        userDao: UserDao,
        auditLogDao: AuditLogDao,
        loginAttemptDao: LoginAttemptDao,
        appSettingDao: AppSettingDao,
        authPreferences: AuthPreferences,
        securityPreferences: SecurityPreferences,
        sessionStore: InMemorySessionStore,
        diagnosticLogger: DiagnosticLogger,
        passwordHasher: PasswordHasher,
    ): AuthRepository =
        DefaultAuthRepository(
            context = context,
            userDao = userDao,
            auditLogDao = auditLogDao,
            loginAttemptDao = loginAttemptDao,
            appSettingDao = appSettingDao,
            authPreferences = authPreferences,
            securityPreferences = securityPreferences,
            sessionStore = sessionStore,
            diagnosticLogger = diagnosticLogger,
            passwordHasher = passwordHasher,
        )

    @Provides
    @Singleton
    fun provideAdminUserRepository(
        @ApplicationContext context: Context,
        userDao: UserDao,
        auditLogDao: AuditLogDao,
        passwordHasher: PasswordHasher,
    ): AdminUserRepository =
        AdminUserRepository(
            context = context,
            userDao = userDao,
            auditLogDao = auditLogDao,
            passwordHasher = passwordHasher,
        )

    @Provides
    @Singleton
    fun provideRepairAppRepository(
        dashboardDao: DashboardDao,
    ): RepairAppRepository = DefaultRepairAppRepository(dashboardDao)

    @Provides
    @Singleton
    fun provideCustomerRepository(
        @ApplicationContext context: Context,
        customerDao: CustomerDao,
        repairOrderDao: RepairOrderDao,
    ): com.onlytechtake.repairapp.domain.repositories.CustomerRepository =
        CustomerRepository(
            context = context,
            customerDao = customerDao,
            repairOrderDao = repairOrderDao,
        )

    @Provides
    @Singleton
    fun provideCatalogRepository(
        @ApplicationContext context: Context,
        categoryDao: OrderCategoryDao,
    ): CatalogRepository = CatalogRepository(context, categoryDao)

    @Provides
    @Singleton
    fun provideOrderRepository(
        @ApplicationContext context: Context,
        repairOrderDao: RepairOrderDao,
        customerDao: CustomerDao,
        categoryDao: OrderCategoryDao,
        timelineEventDao: TimelineEventDao,
        signatureDao: SignatureDao,
        messageLogDao: MessageLogDao,
        auditLogDao: AuditLogDao,
        database: RepairAppDatabase,
        fileCleanupService: OrderFileCleanupService,
    ): OrderRepository =
        OrderWorkflowRepository(
            context = context,
            repairOrderDao = repairOrderDao,
            customerDao = customerDao,
            categoryDao = categoryDao,
            timelineEventDao = timelineEventDao,
            signatureDao = signatureDao,
            messageLogDao = messageLogDao,
            auditLogDao = auditLogDao,
            database = database,
            fileCleanupService = fileCleanupService,
        )

    @Provides
    @Singleton
    fun provideOrderMediaRepository(
        @ApplicationContext context: Context,
        repairOrderDao: RepairOrderDao,
        orderMediaDao: OrderMediaDao,
        timelineEventDao: TimelineEventDao,
        fileCleanupService: OrderFileCleanupService,
        mediaStorageProvider: MediaStorageProvider,
        database: RepairAppDatabase,
        mediaOptimizer: MediaOptimizer,
    ): OrderMediaRepository =
        OrderMediaRepository(
            context = context,
            repairOrderDao = repairOrderDao,
            orderMediaDao = orderMediaDao,
            timelineEventDao = timelineEventDao,
            fileCleanupService = fileCleanupService,
            mediaStorageProvider = mediaStorageProvider,
            database = database,
            mediaOptimizer = mediaOptimizer,
        )

    @Provides
    @Singleton
    fun provideOrderDocumentRepository(
        @ApplicationContext context: Context,
        dao: OrderDocumentDao,
        fileCleanupService: OrderFileCleanupService,
    ): OrderDocumentRepository = DefaultOrderDocumentRepository(context, dao, fileCleanupService)

    @Provides
    @Singleton
    fun provideTemplateRepository(
        @ApplicationContext context: Context,
        templateDao: MessageTemplateDao,
        orderDao: RepairOrderDao,
        customerDao: CustomerDao,
        categoryDao: OrderCategoryDao,
        settingDao: AppSettingDao,
        messageLogDao: MessageLogDao,
        timelineEventDao: TimelineEventDao,
        auditLogDao: AuditLogDao,
        ownerPreferences: OwnerPreferences,
    ): TemplateRepository =
        TemplateRepository(
            context = context,
            templateDao = templateDao,
            orderDao = orderDao,
            customerDao = customerDao,
            categoryDao = categoryDao,
            settingDao = settingDao,
            messageLogDao = messageLogDao,
            timelineEventDao = timelineEventDao,
            auditLogDao = auditLogDao,
            ownerPreferences = ownerPreferences,
        )

    @Provides
    @Singleton
    fun provideBackupRepository(
        @ApplicationContext context: Context,
        categoryDao: OrderCategoryDao,
        templateDao: MessageTemplateDao,
        settingDao: AppSettingDao,
        auditLogDao: AuditLogDao,
        backupReminderPreferences: BackupReminderPreferences,
        securityPreferences: SecurityPreferences,
        orderExportDao: OrderExportDao,
        database: RepairAppDatabase,
        authPreferences: AuthPreferences,
        sessionStore: InMemorySessionStore,
        mediaStorageProvider: MediaStorageProvider,
    ): BackupRepository =
        BackupRepository(
            context = context,
            categoryDao = categoryDao,
            templateDao = templateDao,
            settingDao = settingDao,
            auditLogDao = auditLogDao,
            backupReminderPreferences = backupReminderPreferences,
            securityPreferences = securityPreferences,
            orderExportDao = orderExportDao,
            database = database,
            authPreferences = authPreferences,
            sessionStore = sessionStore,
            mediaStorageProvider = mediaStorageProvider,
        )

    @Provides
    @Singleton
    fun provideDataResetRepository(
        @ApplicationContext context: Context,
        database: RepairAppDatabase,
        auditLogDao: AuditLogDao,
        authPreferences: AuthPreferences,
        sessionStore: InMemorySessionStore,
        fileCleanupService: OrderFileCleanupService,
        mediaStorageProvider: MediaStorageProvider,
    ): DataResetRepository =
        DataResetRepository(
            context = context,
            database = database,
            auditLogDao = auditLogDao,
            authPreferences = authPreferences,
            sessionStore = sessionStore,
            fileCleanupService = fileCleanupService,
            mediaStorageProvider = mediaStorageProvider,
        )

    @Provides
    @Singleton
    fun provideSignatureRepository(
        @ApplicationContext context: Context,
        repairOrderDao: RepairOrderDao,
        signatureDao: SignatureDao,
        templateDao: MessageTemplateDao,
        settingDao: AppSettingDao,
        timelineEventDao: TimelineEventDao,
        fileCleanupService: OrderFileCleanupService,
        mediaStorageProvider: MediaStorageProvider,
    ): SignatureRepository =
        SignatureRepository(
            context = context,
            repairOrderDao = repairOrderDao,
            signatureDao = signatureDao,
            templateDao = templateDao,
            settingDao = settingDao,
            timelineEventDao = timelineEventDao,
            fileCleanupService = fileCleanupService,
            mediaStorageProvider = mediaStorageProvider,
        )
}

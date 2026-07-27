package com.onlytechtake.repairapp.core.di

import android.content.Context
import com.onlytechtake.repairapp.core.error.DiagnosticLogger
import com.onlytechtake.repairapp.core.media.MediaOptimizer
import com.onlytechtake.repairapp.core.media.MediaStorageProvider
import com.onlytechtake.repairapp.core.security.PasswordHasher
import com.onlytechtake.repairapp.core.share.ShareDocumentIntentFactory
import com.onlytechtake.repairapp.data.preferences.AuthPreferences
import com.onlytechtake.repairapp.data.preferences.BackupReminderPreferences
import com.onlytechtake.repairapp.data.preferences.OrdersPreferences
import com.onlytechtake.repairapp.data.preferences.OwnerPreferences
import com.onlytechtake.repairapp.data.preferences.SecurityPreferences
import com.onlytechtake.repairapp.data.preferences.UiPreferences
import com.onlytechtake.repairapp.data.repositories.OrderDocumentFileStorage
import com.onlytechtake.repairapp.data.repositories.OrderFileCleanupService
import com.onlytechtake.repairapp.data.services.BackupService
import com.onlytechtake.repairapp.data.services.DiagnosticLogService
import com.onlytechtake.repairapp.data.services.StorageStatsService
import com.onlytechtake.repairapp.data.session.InMemorySessionStore
import com.onlytechtake.repairapp.demo.DataSeeder
import com.onlytechtake.repairapp.demo.DataSeederImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideDiagnosticLogger(
        @ApplicationContext context: Context,
    ): DiagnosticLogger = DiagnosticLogger(context)

    @Provides
    @Singleton
    fun provideAuthPreferences(
        @ApplicationContext context: Context,
    ): AuthPreferences = AuthPreferences(context)

    @Provides
    @Singleton
    fun provideSecurityPreferences(
        @ApplicationContext context: Context,
    ): SecurityPreferences = SecurityPreferences(context)

    @Provides
    @Singleton
    fun provideOwnerPreferences(
        @ApplicationContext context: Context,
    ): OwnerPreferences = OwnerPreferences(context)

    @Provides
    @Singleton
    fun provideBackupReminderPreferences(
        @ApplicationContext context: Context,
    ): BackupReminderPreferences = BackupReminderPreferences(context)

    @Provides
    @Singleton
    fun provideOrdersPreferences(
        @ApplicationContext context: Context,
    ): OrdersPreferences = OrdersPreferences(context)

    @Provides
    @Singleton
    fun provideUiPreferences(
        @ApplicationContext context: Context,
    ): UiPreferences = UiPreferences(context)

    @Provides
    @Singleton
    fun provideSessionStore(): InMemorySessionStore = InMemorySessionStore()

    @Provides
    @Singleton
    fun providePasswordHasher(): PasswordHasher = PasswordHasher()

    @Provides
    @Singleton
    fun provideMediaStorageProvider(
        @ApplicationContext context: Context,
    ): MediaStorageProvider = MediaStorageProvider(context)

    @Provides
    @Singleton
    fun provideOrderFileCleanupService(
        @ApplicationContext context: Context,
        mediaStorageProvider: MediaStorageProvider,
    ): OrderFileCleanupService = OrderFileCleanupService(context, mediaStorageProvider)

    @Provides
    @Singleton
    fun provideMediaOptimizer(): MediaOptimizer = MediaOptimizer()

    @Provides
    @Singleton
    fun provideOrderDocumentFileStorage(mediaStorageProvider: MediaStorageProvider): OrderDocumentFileStorage = OrderDocumentFileStorage(mediaStorageProvider)

    @Provides
    @Singleton
    fun provideShareDocumentIntentFactory(
        @ApplicationContext context: Context,
    ): ShareDocumentIntentFactory = ShareDocumentIntentFactory(context)

    @Provides
    @Singleton
    fun provideStorageStatsService(
        @ApplicationContext context: Context,
        mediaStorageProvider: MediaStorageProvider,
    ): StorageStatsService = StorageStatsService(context, mediaStorageProvider)

    @Provides
    @Singleton
    fun provideDiagnosticLogService(
        @ApplicationContext context: Context,
        diagnosticLogger: DiagnosticLogger,
    ): DiagnosticLogService = DiagnosticLogService(context, diagnosticLogger)

    @Provides
    @Singleton
    fun provideBackupService(
        @ApplicationContext context: Context,
        backupRepository: com.onlytechtake.repairapp.data.repositories.BackupRepository,
        backupReminderPreferences: BackupReminderPreferences,
        securityPreferences: SecurityPreferences,
    ): BackupService =
        BackupService(
            context = context,
            backupRepository = backupRepository,
            backupReminderPreferences = backupReminderPreferences,
            securityPreferences = securityPreferences,
        )

    @Provides
    @Singleton
    fun provideDataSeeder(): DataSeeder = DataSeederImpl()

    @Provides
    fun provideClockMillis(): () -> Long = { System.currentTimeMillis() }
}

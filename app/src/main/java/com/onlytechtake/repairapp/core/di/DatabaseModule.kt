package com.onlytechtake.repairapp.core.di

import android.content.Context
import com.onlytechtake.repairapp.data.local.dao.AppSettingDao
import com.onlytechtake.repairapp.data.local.dao.AuditLogDao
import com.onlytechtake.repairapp.data.local.dao.CustomerDao
import com.onlytechtake.repairapp.data.local.dao.DashboardDao
import com.onlytechtake.repairapp.data.local.dao.LoginAttemptDao
import com.onlytechtake.repairapp.data.local.dao.MessageLogDao
import com.onlytechtake.repairapp.data.local.dao.MessageTemplateDao
import com.onlytechtake.repairapp.data.local.dao.OrderCategoryDao
import com.onlytechtake.repairapp.data.local.dao.OrderDocumentDao
import com.onlytechtake.repairapp.data.local.dao.OrderExportDao
import com.onlytechtake.repairapp.data.local.dao.OrderMediaDao
import com.onlytechtake.repairapp.data.local.dao.RepairOrderDao
import com.onlytechtake.repairapp.data.local.dao.SignatureDao
import com.onlytechtake.repairapp.data.local.dao.TimelineEventDao
import com.onlytechtake.repairapp.data.local.dao.UserDao
import com.onlytechtake.repairapp.data.local.db.RepairAppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): RepairAppDatabase = RepairAppDatabase.getInstance(context)

    @Provides
    fun provideUserDao(db: RepairAppDatabase): UserDao = db.userDao()

    @Provides
    fun provideCustomerDao(db: RepairAppDatabase): CustomerDao = db.customerDao()

    @Provides
    fun provideOrderCategoryDao(db: RepairAppDatabase): OrderCategoryDao = db.orderCategoryDao()

    @Provides
    fun provideRepairOrderDao(db: RepairAppDatabase): RepairOrderDao = db.repairOrderDao()

    @Provides
    fun provideTimelineEventDao(db: RepairAppDatabase): TimelineEventDao = db.timelineEventDao()

    @Provides
    fun provideOrderMediaDao(db: RepairAppDatabase): OrderMediaDao = db.orderMediaDao()

    @Provides
    fun provideSignatureDao(db: RepairAppDatabase): SignatureDao = db.signatureDao()

    @Provides
    fun provideMessageTemplateDao(db: RepairAppDatabase): MessageTemplateDao = db.messageTemplateDao()

    @Provides
    fun provideMessageLogDao(db: RepairAppDatabase): MessageLogDao = db.messageLogDao()

    @Provides
    fun provideAppSettingDao(db: RepairAppDatabase): AppSettingDao = db.appSettingDao()

    @Provides
    fun provideAuditLogDao(db: RepairAppDatabase): AuditLogDao = db.auditLogDao()

    @Provides
    fun provideOrderExportDao(db: RepairAppDatabase): OrderExportDao = db.orderExportDao()

    @Provides
    fun provideOrderDocumentDao(db: RepairAppDatabase): OrderDocumentDao = db.orderDocumentDao()

    @Provides
    fun provideLoginAttemptDao(db: RepairAppDatabase): LoginAttemptDao = db.loginAttemptDao()

    @Provides
    fun provideDashboardDao(db: RepairAppDatabase): DashboardDao = db.dashboardDao()
}

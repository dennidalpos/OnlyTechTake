package com.onlytechtake.repairapp.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.onlytechtake.repairapp.data.local.dao.AppSettingDao
import com.onlytechtake.repairapp.data.local.dao.AuditLogDao
import com.onlytechtake.repairapp.data.local.dao.CustomerDao
import com.onlytechtake.repairapp.data.local.dao.DashboardDao
import com.onlytechtake.repairapp.data.local.dao.MessageLogDao
import com.onlytechtake.repairapp.data.local.dao.MessageTemplateDao
import com.onlytechtake.repairapp.data.local.dao.OrderExportDao
import com.onlytechtake.repairapp.data.local.dao.OrderCategoryDao
import com.onlytechtake.repairapp.data.local.dao.OrderDocumentDao
import com.onlytechtake.repairapp.data.local.dao.OrderMediaDao
import com.onlytechtake.repairapp.data.local.dao.RepairOrderDao
import com.onlytechtake.repairapp.data.local.dao.SignatureDao
import com.onlytechtake.repairapp.data.local.dao.TimelineEventDao
import com.onlytechtake.repairapp.data.local.dao.UserDao
import com.onlytechtake.repairapp.data.local.entities.AppSettingEntity
import com.onlytechtake.repairapp.data.local.entities.AuditLogEntity
import com.onlytechtake.repairapp.data.local.entities.CustomerEntity
import com.onlytechtake.repairapp.data.local.entities.CustomerFtsEntity
import com.onlytechtake.repairapp.data.local.entities.LoginAttemptEntity
import com.onlytechtake.repairapp.data.local.entities.MessageLogEntity
import com.onlytechtake.repairapp.data.local.entities.MessageTemplateEntity
import com.onlytechtake.repairapp.data.local.entities.OrderCategoryEntity
import com.onlytechtake.repairapp.data.local.entities.OrderDocumentEntity
import com.onlytechtake.repairapp.data.local.entities.OrderMediaEntity
import com.onlytechtake.repairapp.data.local.entities.RepairOrderEntity
import com.onlytechtake.repairapp.data.local.entities.RepairOrderFtsEntity
import com.onlytechtake.repairapp.data.local.entities.SignatureEntity
import com.onlytechtake.repairapp.data.local.entities.TimelineEventEntity
import com.onlytechtake.repairapp.data.local.entities.UserEntity

import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

@Database(
    entities = [
        UserEntity::class,
        CustomerEntity::class,
        CustomerFtsEntity::class,
        OrderCategoryEntity::class,
        RepairOrderEntity::class,
        RepairOrderFtsEntity::class,
        TimelineEventEntity::class,
        MessageTemplateEntity::class,
        MessageLogEntity::class,
        AppSettingEntity::class,
        AuditLogEntity::class,
        OrderMediaEntity::class,
        SignatureEntity::class,
        OrderDocumentEntity::class,
        LoginAttemptEntity::class,
    ],
    version = RepairAppDatabase.DATABASE_VERSION,
    exportSchema = true,
)
@TypeConverters(RoomConverters::class)
abstract class RepairAppDatabase : RoomDatabase() {
    abstract fun dashboardDao(): DashboardDao
    abstract fun userDao(): UserDao
    abstract fun customerDao(): CustomerDao
    abstract fun orderCategoryDao(): OrderCategoryDao
    abstract fun repairOrderDao(): RepairOrderDao
    abstract fun timelineEventDao(): TimelineEventDao
    abstract fun orderMediaDao(): OrderMediaDao
    abstract fun signatureDao(): SignatureDao
    abstract fun messageTemplateDao(): MessageTemplateDao
    abstract fun messageLogDao(): MessageLogDao
    abstract fun appSettingDao(): AppSettingDao
    abstract fun auditLogDao(): AuditLogDao
    abstract fun orderExportDao(): OrderExportDao
    abstract fun orderDocumentDao(): OrderDocumentDao
    abstract fun loginAttemptDao(): com.onlytechtake.repairapp.data.local.dao.LoginAttemptDao

    companion object {
        const val DATABASE_NAME = "repair_app.db"
        const val DATABASE_VERSION = 4

        @Volatile
        private var instance: RepairAppDatabase? = null

        fun getInstance(context: Context): RepairAppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    RepairAppDatabase::class.java,
                    DATABASE_NAME,
                )
                    .openHelperFactory(SupportOpenHelperFactory("".toByteArray()))
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { instance = it }
            }

        fun closeInstance() {
            synchronized(this) {
                instance?.close()
                instance = null
            }
        }
    }
}

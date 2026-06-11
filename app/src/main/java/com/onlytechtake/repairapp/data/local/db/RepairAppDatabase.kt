package com.onlytechtake.repairapp.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
import com.onlytechtake.repairapp.data.local.entities.MessageLogEntity
import com.onlytechtake.repairapp.data.local.entities.MessageTemplateEntity
import com.onlytechtake.repairapp.data.local.entities.OrderCategoryEntity
import com.onlytechtake.repairapp.data.local.entities.OrderDocumentEntity
import com.onlytechtake.repairapp.data.local.entities.OrderMediaEntity
import com.onlytechtake.repairapp.data.local.entities.RepairOrderEntity
import com.onlytechtake.repairapp.data.local.entities.SignatureEntity
import com.onlytechtake.repairapp.data.local.entities.TimelineEventEntity
import com.onlytechtake.repairapp.data.local.entities.UserEntity

@Database(
    entities = [
        UserEntity::class,
        CustomerEntity::class,
        OrderCategoryEntity::class,
        RepairOrderEntity::class,
        TimelineEventEntity::class,
        MessageTemplateEntity::class,
        MessageLogEntity::class,
        AppSettingEntity::class,
        AuditLogEntity::class,
        OrderMediaEntity::class,
        SignatureEntity::class,
        OrderDocumentEntity::class,
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

    companion object {
        const val DATABASE_NAME = "repair_app.db"
        const val DATABASE_VERSION = 3

        @Volatile
        private var instance: RepairAppDatabase? = null

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE repair_orders ADD COLUMN customerActionStatus TEXT")
                db.execSQL("ALTER TABLE repair_orders ADD COLUMN closureReason TEXT")
                db.execSQL("ALTER TABLE repair_orders ADD COLUMN closeNote TEXT")
                db.execSQL("ALTER TABLE repair_orders ADD COLUMN closedByUserId TEXT")
                db.execSQL("ALTER TABLE repair_orders ADD COLUMN categoryPathSnapshot TEXT")
                db.execSQL("ALTER TABLE repair_orders ADD COLUMN lastCustomerContactAt INTEGER")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS order_documents (
                        id TEXT NOT NULL PRIMARY KEY,
                        orderId TEXT NOT NULL,
                        type TEXT NOT NULL,
                        localPath TEXT NOT NULL,
                        fileName TEXT NOT NULL,
                        mimeType TEXT NOT NULL,
                        sizeBytes INTEGER NOT NULL,
                        includesCustomerSignature INTEGER NOT NULL,
                        createdByUserId TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        sharedAt INTEGER,
                        FOREIGN KEY(orderId) REFERENCES repair_orders(id) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(createdByUserId) REFERENCES users(id) ON UPDATE NO ACTION ON DELETE RESTRICT
                    )
                """.trimIndent())

                db.execSQL("CREATE INDEX IF NOT EXISTS index_order_documents_orderId ON order_documents (orderId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_order_documents_createdByUserId ON order_documents (createdByUserId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_order_documents_createdAt ON order_documents (createdAt)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_repair_orders_closedByUserId ON repair_orders (closedByUserId)")
            }
        }

        fun getInstance(context: Context): RepairAppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    RepairAppDatabase::class.java,
                    DATABASE_NAME,
                )
                    .addMigrations(MIGRATION_2_3)
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

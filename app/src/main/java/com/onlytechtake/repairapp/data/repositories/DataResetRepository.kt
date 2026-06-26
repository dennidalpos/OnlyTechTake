package com.onlytechtake.repairapp.data.repositories

import android.content.Context
import androidx.room.withTransaction
import com.onlytechtake.repairapp.core.media.MediaStorageProvider
import com.onlytechtake.repairapp.core.permissions.PermissionService
import com.onlytechtake.repairapp.data.local.dao.AuditLogDao
import com.onlytechtake.repairapp.data.local.db.RepairAppDatabase
import com.onlytechtake.repairapp.data.local.entities.UserPermission
import com.onlytechtake.repairapp.data.preferences.AuthPreferences
import com.onlytechtake.repairapp.data.session.InMemorySessionStore
import com.onlytechtake.repairapp.domain.auth.AuthenticatedUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class DataResetRepository(
    private val context: Context,
    private val database: RepairAppDatabase,
    private val auditLogDao: AuditLogDao,
    private val authPreferences: AuthPreferences,
    private val sessionStore: InMemorySessionStore,
    private val fileCleanupService: OrderFileCleanupService,
    private val mediaStorageProvider: MediaStorageProvider,
) {
    private val eventWriter = OrderWorkflowEventWriter(context, auditLogDao = auditLogDao)

    /**
     * Factory reset: clears EVERYTHING.
     */
    suspend fun resetAll(
        actor: AuthenticatedUser,
        @Suppress("UNUSED_PARAMETER") confirmation: String,
    ): Result<Unit> =
        runCatching {
            PermissionService.requirePermission(actor, UserPermission.DELETE_DATA)

            withContext(Dispatchers.IO) {
                eventWriter.dataReset(actor.id, System.currentTimeMillis())

                database.withTransaction {
                    val db = database.openHelper.writableDatabase
                    // Break self-referencing hierarchies before deletion to avoid FK violations (code 1811)
                    db.execSQL("UPDATE order_categories SET parentId = NULL")

                    TABLES_ORDERED_FOR_DELETION.forEach { table ->
                        db.execSQL("DELETE FROM $table")
                    }
                }

                authPreferences.clear()
                sessionStore.clear()

                // Clear files specifically instead of nuking everything in filesDir
                // This prevents deleting datastore files while they are being closed
                mediaStorageProvider.getBaseMediaDir().deleteRecursively()
                fileCleanupService.cleanupSharedCache()

                // Optional: delete other specific folders if needed, but NOT the root filesDir
                File(context.filesDir, "logs").deleteRecursively()
            }
        }

    /**
     * Modular reset: clears only orders and related data.
     */
    suspend fun resetOrders(
        actor: AuthenticatedUser,
        @Suppress("UNUSED_PARAMETER") confirmation: String,
    ): Result<Unit> =
        runCatching {
            PermissionService.requirePermission(actor, UserPermission.DELETE_DATA)

            withContext(Dispatchers.IO) {
                database.withTransaction {
                    val db = database.openHelper.writableDatabase
                    ORDER_RELATED_TABLES.forEach { table ->
                        db.execSQL("DELETE FROM $table")
                    }
                }

                // Clear centralized media related to orders and intake
                File(mediaStorageProvider.getBaseMediaDir(), "order_media").deleteRecursively()
                File(mediaStorageProvider.getBaseMediaDir(), "intake_media").deleteRecursively()
                File(mediaStorageProvider.getBaseMediaDir(), "signatures").deleteRecursively()
                File(mediaStorageProvider.getBaseMediaDir(), "documents").deleteRecursively()

                fileCleanupService.cleanupSharedCache()

                eventWriter.orderCleanup(actor.id, "all_orders_deleted", System.currentTimeMillis())
            }
        }

    private companion object {
        val TABLES_ORDERED_FOR_DELETION =
            listOf(
                "audit_logs",
                "message_logs",
                "order_documents",
                "order_media",
                "signatures",
                "timeline_events",
                "repair_orders",
                "customers",
                "order_categories",
                "message_templates",
                "app_settings",
                "login_attempts",
                "users",
            )

        val ORDER_RELATED_TABLES =
            listOf(
                "message_logs",
                "order_documents",
                "order_media",
                "signatures",
                "timeline_events",
                "repair_orders",
            )
    }
}

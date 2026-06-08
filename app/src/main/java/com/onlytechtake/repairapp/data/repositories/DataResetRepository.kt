package com.onlytechtake.repairapp.data.repositories

import android.content.Context
import com.onlytechtake.repairapp.data.local.dao.AuditLogDao
import com.onlytechtake.repairapp.data.local.db.RepairAppDatabase
import com.onlytechtake.repairapp.data.local.entities.UserRole
import com.onlytechtake.repairapp.data.preferences.AuthPreferences
import com.onlytechtake.repairapp.data.session.InMemorySessionStore
import com.onlytechtake.repairapp.domain.auth.AuthenticatedUser
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DataResetRepository(
    private val context: Context,
    private val database: RepairAppDatabase,
    private val auditLogDao: AuditLogDao,
    private val authPreferences: AuthPreferences,
    private val sessionStore: InMemorySessionStore,
) {
    private val eventWriter = OrderWorkflowEventWriter(auditLogDao = auditLogDao)

    suspend fun resetAll(actor: AuthenticatedUser, confirmation: String): Result<Unit> =
        runCatching {
            require(actor.role == UserRole.ADMIN) { "Solo admin" }
            require(confirmation == RESET_CONFIRMATION) { "Digitare RESET per confermare" }

            withContext(Dispatchers.IO) {
                eventWriter.dataReset(actor.id, System.currentTimeMillis())
                clearDatabaseTables()
                authPreferences.clear()
                clearDirectory(File(context.filesDir, "orders"))
                clearDirectory(File(context.filesDir, "order_media"))
                clearDirectory(File(context.filesDir, "intake_media"))
                clearDirectory(File(context.filesDir, "signatures"))
                clearDirectory(context.cacheDir)
                sessionStore.clear()
            }
        }

    private fun clearDatabaseTables() {
        val db = database.openHelper.writableDatabase
        db.execSQL("PRAGMA foreign_keys=OFF")
        try {
            db.beginTransaction()
            try {
                RESET_TABLES.forEach { table -> db.execSQL("DELETE FROM $table") }
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
        } finally {
            db.execSQL("PRAGMA foreign_keys=ON")
        }
    }

    private fun clearDirectory(directory: File) {
        if (!directory.exists()) return
        directory.listFiles().orEmpty().forEach { child ->
            child.deleteRecursively()
        }
    }

    private companion object {
        const val RESET_CONFIRMATION = "RESET"
        val RESET_TABLES = listOf(
            "message_logs",
            "order_media",
            "signatures",
            "timeline_events",
            "audit_logs",
            "repair_orders",
            "customers",
            "order_categories",
            "message_templates",
            "app_settings",
            "users",
        )
    }
}

package com.onlytechtake.repairapp.data.repositories

import android.content.Context
import com.onlytechtake.repairapp.data.local.dao.AuditLogDao
import com.onlytechtake.repairapp.data.local.db.RepairAppDatabase
import com.onlytechtake.repairapp.data.local.entities.UserRole
import com.onlytechtake.repairapp.data.preferences.AuthPreferences
import com.onlytechtake.repairapp.data.session.InMemorySessionStore
import com.onlytechtake.repairapp.domain.auth.AuthenticatedUser
import java.io.File

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

            eventWriter.dataReset(actor.id, System.currentTimeMillis())
            database.clearAllTables()
            authPreferences.clear()
            clearDirectory(File(context.filesDir, "orders"))
            clearDirectory(File(context.filesDir, "order_media"))
            clearDirectory(File(context.filesDir, "intake_media"))
            clearDirectory(File(context.filesDir, "signatures"))
            clearDirectory(context.cacheDir)
            sessionStore.clear()
        }

    private fun clearDirectory(directory: File) {
        if (!directory.exists()) return
        directory.listFiles().orEmpty().forEach { child ->
            child.deleteRecursively()
        }
    }

    private companion object {
        const val RESET_CONFIRMATION = "RESET"
    }
}

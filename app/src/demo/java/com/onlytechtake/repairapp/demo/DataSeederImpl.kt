package com.onlytechtake.repairapp.demo

import android.content.Context
import androidx.room.withTransaction
import com.onlytechtake.repairapp.core.demo.DemoMode
import com.onlytechtake.repairapp.core.security.PasswordHasher
import com.onlytechtake.repairapp.data.local.db.RepairAppDatabase
import com.onlytechtake.repairapp.data.local.entities.UserEntity
import com.onlytechtake.repairapp.data.local.entities.UserPermission
import com.onlytechtake.repairapp.data.local.entities.UserRole
import com.onlytechtake.repairapp.data.preferences.AuthPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Implementation of [DataSeeder] for the Demo flavor.
 * Seeds the database with minimal demo data if requested.
 */
class DataSeederImpl : DataSeeder {
    override suspend fun seed(context: Context) =
        withContext(Dispatchers.IO) {
            DemoSeeder.seedDemoData(context)
        }

    override val defaultPassword: String = DemoSeeder.DEFAULT_PASSWORD
}

object DemoSeeder {
    const val DEFAULT_PASSWORD = "DemoPassword123"

    suspend fun seedDemoData(context: Context) {
        val authPreferences = AuthPreferences(context)
        val db = RepairAppDatabase.getInstance(context)
        val now = System.currentTimeMillis()
        val passwordHasher = PasswordHasher()
        val pwdHash = passwordHasher.hash(DEFAULT_PASSWORD.toCharArray())

        db.withTransaction {
            // Seed Admin
            db.userDao().upsert(
                UserEntity(
                    id = "admin",
                    displayName = DemoMode.getAdminDisplayName(context),
                    username = "admin",
                    passwordHash = pwdHash.hash,
                    passwordSalt = pwdHash.salt,
                    role = UserRole.ADMIN,
                    permissions = UserPermission.entries.toSet(),
                    active = true,
                    createdAt = now,
                    updatedAt = now,
                ),
            )
            // Seed Demo Content
            db.customerDao().upsert(DemoMode.getDemoCustomer(context))
            db.orderCategoryDao().upsertAll(DemoMode.getDemoCategories(context))
        }

        authPreferences.markConfigured()
    }
}

package com.onlytechtake.repairapp.core.di

import android.content.Context
import com.onlytechtake.repairapp.core.error.DiagnosticLogger
import com.onlytechtake.repairapp.core.share.ShareDocumentIntentFactory
import com.onlytechtake.repairapp.data.local.db.RepairAppDatabase
import com.onlytechtake.repairapp.data.preferences.BackupReminderPreferences
import com.onlytechtake.repairapp.data.preferences.OwnerPreferences
import com.onlytechtake.repairapp.data.preferences.SecurityPreferences
import com.onlytechtake.repairapp.data.repositories.AdminUserRepository
import com.onlytechtake.repairapp.data.repositories.BackupRepository
import com.onlytechtake.repairapp.data.repositories.CatalogRepository
import com.onlytechtake.repairapp.data.repositories.CustomerRepository
import com.onlytechtake.repairapp.data.repositories.DataResetRepository
import com.onlytechtake.repairapp.data.repositories.OrderDocumentRepository
import com.onlytechtake.repairapp.data.repositories.OrderFileCleanupService
import com.onlytechtake.repairapp.data.repositories.OrderMediaRepository
import com.onlytechtake.repairapp.data.repositories.OrderWorkflowRepository
import com.onlytechtake.repairapp.data.repositories.TemplateRepository
import com.onlytechtake.repairapp.data.services.DiagnosticLogService
import com.onlytechtake.repairapp.data.services.StorageStatsService
import com.onlytechtake.repairapp.demo.DataSeeder
import com.onlytechtake.repairapp.domain.auth.AuthRepository
import com.onlytechtake.repairapp.domain.auth.AuthState
import com.onlytechtake.repairapp.domain.auth.AuthenticatedUser
import com.onlytechtake.repairapp.domain.auth.LoginResult
import com.onlytechtake.repairapp.domain.repositories.RepairAppRepository
import com.onlytechtake.repairapp.domain.workspace.CreateIntakeOrderUseCase
import com.onlytechtake.repairapp.domain.workspace.GenerateOrderStatusPdfUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf

class FakeAppContainer(
    override val context: Context,
) : AppContainer {
    override val repairAppRepository: RepairAppRepository get() = error("Not implemented")
    override val authRepository: AuthRepository =
        object : AuthRepository {
            override val currentUser: StateFlow<AuthenticatedUser?> = MutableStateFlow(null)

            override fun observeAuthState(): Flow<AuthState> = flowOf(AuthState())

            override suspend fun createFirstAdmin(
                displayName: String,
                username: String,
                password: CharArray,
            ): AuthenticatedUser = error("Not implemented")

            override suspend fun login(
                username: String,
                password: CharArray,
            ): LoginResult = LoginResult.Success(mockk_fallback())

            override suspend fun loginDemo(): LoginResult = LoginResult.Success(mockk_fallback())

            override suspend fun changePassword(
                username: String,
                currentPassword: CharArray,
                newPassword: CharArray,
            ): Result<Unit> = Result.success(Unit)

            override suspend fun getRecoveryKit(): Result<String> = Result.success("FAKE-CODE")

            override suspend fun resetAdminWithRecoveryCode(
                code: String,
                newPassword: CharArray,
            ): Result<Unit> = Result.success(Unit)

            override fun touchSession() {}

            override suspend fun logout() {}

            private fun <T> mockk_fallback(): T = error("Fake method not fully implemented")
        }
    override val adminUserRepository: AdminUserRepository get() = error("Not implemented")
    override val database: RepairAppDatabase get() = error("Not implemented")
    override val customerRepository: CustomerRepository get() = error("Not implemented")
    override val catalogRepository: CatalogRepository get() = error("Not implemented")
    override val orderWorkflowRepository: OrderWorkflowRepository get() = error("Not implemented")
    override val createIntakeOrderUseCase: CreateIntakeOrderUseCase get() = error("Not implemented")
    override val orderMediaRepository: OrderMediaRepository get() = error("Not implemented")
    override val orderDocumentRepository: OrderDocumentRepository get() = error("Not implemented")
    override val shareDocumentIntentFactory: ShareDocumentIntentFactory get() = error("Not implemented")
    override val generateOrderStatusPdfUseCase: GenerateOrderStatusPdfUseCase get() = error("Not implemented")
    override val orderFileCleanupService: OrderFileCleanupService get() = error("Not implemented")
    override val templateRepository: TemplateRepository get() = error("Not implemented")
    override val backupRepository: BackupRepository get() = error("Not implemented")
    override val backupReminderPreferences: BackupReminderPreferences get() = error("Not implemented")
    override val ownerPreferences: OwnerPreferences get() = error("Not implemented")
    override val securityPreferences: SecurityPreferences get() = error("Not implemented")
    override val dataResetRepository: DataResetRepository get() = error("Not implemented")
    override val storageStatsService: StorageStatsService get() = error("Not implemented")
    override val diagnosticLogService: DiagnosticLogService get() = error("Not implemented")
    override val diagnosticLogger: DiagnosticLogger get() = error("Not implemented")
    override val dataSeeder: DataSeeder get() = error("Not implemented")

    override fun close() {}
}

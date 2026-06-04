package com.onlytechtake.repairapp.core.di

import android.content.Context
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.room.withTransaction
import com.onlytechtake.repairapp.core.permissions.PermissionManager
import com.onlytechtake.repairapp.core.security.PasswordHasher
import com.onlytechtake.repairapp.data.local.db.RepairAppDatabase
import com.onlytechtake.repairapp.data.preferences.AuthPreferences
import com.onlytechtake.repairapp.data.repositories.AdminUserRepository
import com.onlytechtake.repairapp.data.repositories.BackupRepository
import com.onlytechtake.repairapp.data.repositories.CatalogRepository
import com.onlytechtake.repairapp.data.repositories.CustomerRepository
import com.onlytechtake.repairapp.data.repositories.DefaultAuthRepository
import com.onlytechtake.repairapp.data.repositories.DefaultRepairAppRepository
import com.onlytechtake.repairapp.data.repositories.DataResetRepository
import com.onlytechtake.repairapp.data.repositories.OrderMediaRepository
import com.onlytechtake.repairapp.data.repositories.OrderWorkflowRepository
import com.onlytechtake.repairapp.data.repositories.SignatureRepository
import com.onlytechtake.repairapp.data.repositories.TemplateRepository
import com.onlytechtake.repairapp.data.session.InMemorySessionStore
import com.onlytechtake.repairapp.domain.auth.AuthRepository
import com.onlytechtake.repairapp.domain.repositories.RepairAppRepository
import com.onlytechtake.repairapp.domain.workspace.CreateIntakeOrderUseCase

class AppContainer(context: Context) {
    private val database = RepairAppDatabase.getInstance(context)
    private val sessionStore = InMemorySessionStore()
    private val authPreferences = AuthPreferences(context)
    private val passwordHasher = PasswordHasher()
    val permissionManager = PermissionManager()

    val repairAppRepository: RepairAppRepository = DefaultRepairAppRepository(
        dashboardDao = database.dashboardDao(),
    )

    val authRepository: AuthRepository = DefaultAuthRepository(
        userDao = database.userDao(),
        auditLogDao = database.auditLogDao(),
        authPreferences = authPreferences,
        sessionStore = sessionStore,
        passwordHasher = passwordHasher,
    )

    val adminUserRepository = AdminUserRepository(
        userDao = database.userDao(),
        auditLogDao = database.auditLogDao(),
        passwordHasher = passwordHasher,
    )

    val customerRepository = CustomerRepository(
        customerDao = database.customerDao(),
    )

    val catalogRepository = CatalogRepository(
        categoryDao = database.orderCategoryDao(),
    )

    val orderWorkflowRepository = OrderWorkflowRepository(
        repairOrderDao = database.repairOrderDao(),
        customerDao = database.customerDao(),
        categoryDao = database.orderCategoryDao(),
        userDao = database.userDao(),
        timelineEventDao = database.timelineEventDao(),
        orderMediaDao = database.orderMediaDao(),
        signatureDao = database.signatureDao(),
        messageLogDao = database.messageLogDao(),
        auditLogDao = database.auditLogDao(),
        database = database,
    )

    val orderMediaRepository = OrderMediaRepository(
        context = context.applicationContext,
        repairOrderDao = database.repairOrderDao(),
        orderMediaDao = database.orderMediaDao(),
        timelineEventDao = database.timelineEventDao(),
        database = database,
    )

    val templateRepository = TemplateRepository(
        templateDao = database.messageTemplateDao(),
        orderDao = database.repairOrderDao(),
        customerDao = database.customerDao(),
        settingDao = database.appSettingDao(),
        messageLogDao = database.messageLogDao(),
        timelineEventDao = database.timelineEventDao(),
        auditLogDao = database.auditLogDao(),
    )

    val signatureRepository = SignatureRepository(
        context = context.applicationContext,
        repairOrderDao = database.repairOrderDao(),
        signatureDao = database.signatureDao(),
        templateDao = database.messageTemplateDao(),
        timelineEventDao = database.timelineEventDao(),
    )

    val createIntakeOrderUseCase = CreateIntakeOrderUseCase(
        customerRepository = customerRepository,
        orderRepository = orderWorkflowRepository,
        orderMediaRepository = orderMediaRepository,
        signatureRepository = signatureRepository,
        runAtomically = { block -> database.withTransaction { block() } },
    )

    val backupRepository = BackupRepository(
        context = context.applicationContext,
        categoryDao = database.orderCategoryDao(),
        templateDao = database.messageTemplateDao(),
        settingDao = database.appSettingDao(),
        auditLogDao = database.auditLogDao(),
    )

    val dataResetRepository = DataResetRepository(
        context = context.applicationContext,
        database = database,
        auditLogDao = database.auditLogDao(),
        authPreferences = authPreferences,
        sessionStore = sessionStore,
    )
}

val LocalAppContainer = staticCompositionLocalOf<AppContainer> {
    error("AppContainer is not available")
}

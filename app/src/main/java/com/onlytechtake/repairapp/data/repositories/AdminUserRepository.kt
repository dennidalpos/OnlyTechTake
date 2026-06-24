package com.onlytechtake.repairapp.data.repositories

import android.content.Context
import com.onlytechtake.repairapp.BuildConfig
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.core.permissions.PermissionService
import com.onlytechtake.repairapp.core.security.PasswordHasher
import com.onlytechtake.repairapp.data.local.dao.AuditLogDao
import com.onlytechtake.repairapp.data.local.dao.UserDao
import com.onlytechtake.repairapp.data.local.entities.AuditAction
import com.onlytechtake.repairapp.data.local.entities.AuditLogEntity
import com.onlytechtake.repairapp.data.local.entities.UserEntity
import com.onlytechtake.repairapp.data.local.entities.UserPermission
import com.onlytechtake.repairapp.data.local.entities.UserRole
import com.onlytechtake.repairapp.domain.auth.AuthenticatedUser
import com.onlytechtake.repairapp.domain.workspace.OperatorInput
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class AdminUserRepository(
    private val context: Context,
    private val userDao: UserDao,
    private val auditLogDao: AuditLogDao,
    private val passwordHasher: PasswordHasher,
    private val clockMillis: () -> Long = System::currentTimeMillis,
) {
    fun observeUsers(): Flow<List<UserEntity>> = userDao.observeAll()

    fun observeActiveUsers(): Flow<List<UserEntity>> = userDao.observeActiveUsers()

    suspend fun createOperator(
        actor: AuthenticatedUser,
        input: OperatorInput,
    ): Result<Unit> =
        runCatching {
            PermissionService.requirePermission(actor, UserPermission.MANAGE_USERS)
            require(input.displayName.isNotBlank()) { context.getString(R.string.error_name_required) }
            require(input.username.isNotBlank()) { context.getString(R.string.error_username_required) }
            require(input.password.size >= MIN_PASSWORD_LENGTH) { context.getString(R.string.error_password_too_short) }
            require(userDao.findByUsername(input.username.trim()) == null) { context.getString(R.string.error_username_exists) }

            if (BuildConfig.IS_DEMO_VERSION) {
                if (MAX_DEMO_OPERATORS <= 0) {
                    throw IllegalStateException(context.getString(R.string.demo_error_operator_not_available))
                }

                if (userDao.countActiveByRole(UserRole.OPERATOR) >= MAX_DEMO_OPERATORS) {
                    throw IllegalStateException(
                        context.resources.getQuantityString(
                            R.plurals.demo_error_operator_limit,
                            MAX_DEMO_OPERATORS,
                            MAX_DEMO_OPERATORS,
                        ),
                    )
                }
            }

            val now = clockMillis()
            val passwordHash =
                try {
                    passwordHasher.hash(input.password)
                } finally {
                    input.password.fill('\u0000')
                }
            val user =
                UserEntity(
                    id = UUID.randomUUID().toString(),
                    displayName = input.displayName.trim(),
                    username = input.username.trim(),
                    passwordHash = passwordHash.hash,
                    passwordSalt = passwordHash.salt,
                    role = UserRole.OPERATOR,
                    permissions =
                        setOf(
                            UserPermission.CREATE_ORDERS,
                            UserPermission.EDIT_OWN_ORDERS,
                            UserPermission.CLOSE_ORDERS,
                            UserPermission.MANAGE_CUSTOMERS,
                            UserPermission.MANAGE_CATEGORIES,
                            UserPermission.MANAGE_PAYMENTS,
                        ),
                    active = true,
                    forcePasswordChange = true,
                    createdAt = now,
                    updatedAt = now,
                )

            userDao.upsert(user)
            audit(actor.id, AuditAction.USER_CREATED, """{"userId":"${user.id}"}""", now)
        }

    suspend fun updateUser(
        actor: AuthenticatedUser,
        userId: String,
        displayName: String,
        username: String,
    ): Result<Unit> =
        runCatching {
            PermissionService.requirePermission(actor, UserPermission.MANAGE_USERS)
            val existing = requireNotNull(userDao.findById(userId)) { context.getString(R.string.error_user_not_found) }
            val trimmedUsername = username.trim()
            require(displayName.isNotBlank()) { context.getString(R.string.error_name_required) }
            require(trimmedUsername.isNotBlank()) { context.getString(R.string.error_username_required) }
            val sameUsernameUser = userDao.findByUsername(trimmedUsername)
            require(sameUsernameUser == null || sameUsernameUser.id == userId) { context.getString(R.string.error_username_exists) }

            val now = clockMillis()
            userDao.upsert(
                existing.copy(
                    displayName = displayName.trim(),
                    username = trimmedUsername,
                    updatedAt = now,
                ),
            )
            audit(actor.id, AuditAction.USER_UPDATED, """{"userId":"$userId"}""", now)
        }

    suspend fun resetPassword(
        actor: AuthenticatedUser,
        userId: String,
        password: CharArray,
    ): Result<Unit> =
        runCatching {
            PermissionService.requirePermission(actor, UserPermission.MANAGE_USERS)
            require(password.size >= MIN_PASSWORD_LENGTH) { context.getString(R.string.error_password_too_short) }
            val existing = requireNotNull(userDao.findById(userId)) { context.getString(R.string.error_user_not_found) }
            val hash =
                try {
                    passwordHasher.hash(password)
                } finally {
                    password.fill('\u0000')
                }
            val now = clockMillis()
            userDao.upsert(
                existing.copy(
                    passwordHash = hash.hash,
                    passwordSalt = hash.salt,
                    forcePasswordChange = true,
                    updatedAt = now,
                ),
            )
            audit(actor.id, AuditAction.PASSWORD_RESET, """{"userId":"$userId"}""", now)
        }

    suspend fun setActive(
        actor: AuthenticatedUser,
        userId: String,
        active: Boolean,
    ): Result<Unit> =
        runCatching {
            PermissionService.requirePermission(actor, UserPermission.MANAGE_USERS)
            val existing = requireNotNull(userDao.findById(userId)) { context.getString(R.string.error_user_not_found) }
            if (!active && existing.role == UserRole.ADMIN) {
                throw IllegalStateException(context.getString(R.string.error_admin_cannot_be_deactivated))
            }

            val now = clockMillis()
            userDao.upsert(existing.copy(active = active, updatedAt = now))
            audit(
                actor.id,
                AuditAction.USER_UPDATED,
                """{"userId":"$userId","active":$active}""",
                now,
            )
        }

    suspend fun deleteUser(
        actor: AuthenticatedUser,
        userId: String,
    ): Result<Unit> =
        runCatching {
            PermissionService.requirePermission(actor, UserPermission.MANAGE_USERS)
            val existing = requireNotNull(userDao.findById(userId)) { context.getString(R.string.error_user_not_found) }
            if (existing.role == UserRole.ADMIN) {
                throw IllegalStateException(context.getString(R.string.error_admin_cannot_be_deleted))
            }

            userDao.deleteById(userId)
            audit(
                actor.id,
                AuditAction.USER_UPDATED,
                """{"userId":"$userId","action":"deleted"}""",
                clockMillis(),
            )
        }

    suspend fun setPermissions(
        actor: AuthenticatedUser,
        userId: String,
        permissions: Set<UserPermission>,
    ): Result<Unit> =
        runCatching {
            PermissionService.requirePermission(actor, UserPermission.MANAGE_USERS)
            val existing = requireNotNull(userDao.findById(userId)) { context.getString(R.string.error_user_not_found) }
            require(existing.role != UserRole.ADMIN) { context.getString(R.string.error_admin_permissions_immutable) }

            val now = clockMillis()
            userDao.upsert(existing.copy(permissions = permissions, updatedAt = now))
            audit(
                actor.id,
                AuditAction.USER_UPDATED,
                """{"userId":"$userId","permissions":${permissions.map { it.name }}}""",
                now,
            )
        }

    private suspend fun audit(
        actorUserId: String,
        action: AuditAction,
        detailsJson: String,
        now: Long,
    ) {
        auditLogDao.upsert(
            AuditLogEntity(
                id = UUID.randomUUID().toString(),
                action = action,
                actorUserId = actorUserId,
                orderId = null,
                customerId = null,
                detailsJson = detailsJson,
                createdAt = now,
            ),
        )
    }

    private companion object {
        const val MIN_PASSWORD_LENGTH = 8
        const val MAX_DEMO_OPERATORS = 0
    }
}

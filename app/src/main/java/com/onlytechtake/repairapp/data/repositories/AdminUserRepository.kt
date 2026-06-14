package com.onlytechtake.repairapp.data.repositories

import android.content.Context
import com.onlytechtake.repairapp.BuildConfig
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.core.security.PasswordHasher
import com.onlytechtake.repairapp.data.local.dao.AuditLogDao
import com.onlytechtake.repairapp.data.local.dao.UserDao
import com.onlytechtake.repairapp.data.local.entities.AuditAction
import com.onlytechtake.repairapp.data.local.entities.AuditLogEntity
import com.onlytechtake.repairapp.data.local.entities.UserEntity
import com.onlytechtake.repairapp.data.local.entities.UserPermission
import com.onlytechtake.repairapp.data.local.entities.UserRole
import com.onlytechtake.repairapp.core.permissions.PermissionService
import com.onlytechtake.repairapp.domain.auth.AuthenticatedUser
import com.onlytechtake.repairapp.domain.workspace.OperatorInput
import java.util.UUID
import kotlinx.coroutines.flow.Flow

class AdminUserRepository(
    private val context: Context,
    private val userDao: UserDao,
    private val auditLogDao: AuditLogDao,
    private val passwordHasher: PasswordHasher,
    private val clockMillis: () -> Long = System::currentTimeMillis,
) {
    fun observeUsers(): Flow<List<UserEntity>> = userDao.observeAll()

    fun observeActiveUsers(): Flow<List<UserEntity>> = userDao.observeActiveUsers()

    suspend fun createOperator(actor: AuthenticatedUser, input: OperatorInput): Result<Unit> =
        runCatching {
            PermissionService.requirePermission(actor, UserPermission.MANAGE_USERS)
            require(input.displayName.isNotBlank()) { "Nome obbligatorio" }
            require(input.username.isNotBlank()) { "Username obbligatorio" }
            require(input.password.size >= MIN_PASSWORD_LENGTH) { "Password troppo corta" }
            require(userDao.findByUsername(input.username.trim()) == null) { "Username gia' esistente" }

            if (BuildConfig.IS_DEMO_VERSION) {
                val operatorCount = userDao.countActiveByRole(UserRole.OPERATOR)
                if (operatorCount >= MAX_DEMO_OPERATORS) {
                    throw IllegalStateException(context.getString(R.string.demo_error_operator_limit, MAX_DEMO_OPERATORS))
                }
            }

            val now = clockMillis()
            val passwordHash = try {
                passwordHasher.hash(input.password)
            } finally {
                input.password.fill('\u0000')
            }
            val user = UserEntity(
                id = UUID.randomUUID().toString(),
                displayName = input.displayName.trim(),
                username = input.username.trim(),
                passwordHash = passwordHash.hash,
                passwordSalt = passwordHash.salt,
                role = UserRole.OPERATOR,
                permissions = setOf(
                    UserPermission.CREATE_ORDERS,
                    UserPermission.EDIT_OWN_ORDERS,
                    UserPermission.MANAGE_CUSTOMERS,
                ),
                active = true,
                forcePasswordChange = true,
                createdAt = now,
                updatedAt = now,
            )

            userDao.upsert(user)
            audit(actor.id, AuditAction.USER_CREATED, """{"userId":"${user.id}"}""", now)
        }

    suspend fun updateUser(actor: AuthenticatedUser, userId: String, displayName: String, username: String): Result<Unit> =
        runCatching {
            PermissionService.requirePermission(actor, UserPermission.MANAGE_USERS)
            val existing = requireNotNull(userDao.findById(userId)) { "Utente non trovato" }
            val trimmedUsername = username.trim()
            require(displayName.isNotBlank()) { "Nome obbligatorio" }
            require(trimmedUsername.isNotBlank()) { "Username obbligatorio" }
            val sameUsernameUser = userDao.findByUsername(trimmedUsername)
            require(sameUsernameUser == null || sameUsernameUser.id == userId) { "Username gia' esistente" }

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

    suspend fun resetPassword(actor: AuthenticatedUser, userId: String, password: CharArray): Result<Unit> =
        runCatching {
            PermissionService.requirePermission(actor, UserPermission.MANAGE_USERS)
            require(password.size >= MIN_PASSWORD_LENGTH) { "Password troppo corta" }
            val existing = requireNotNull(userDao.findById(userId)) { "Utente non trovato" }
            val hash = try {
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

    suspend fun setActive(actor: AuthenticatedUser, userId: String, active: Boolean): Result<Unit> =
        runCatching {
            PermissionService.requirePermission(actor, UserPermission.MANAGE_USERS)
            val existing = requireNotNull(userDao.findById(userId)) { "Utente non trovato" }
            if (!active && existing.role == UserRole.ADMIN) {
                val activeAdmins = userDao.countActiveByRole(UserRole.ADMIN)
                if (activeAdmins <= 1) {
                    throw IllegalStateException("L'ultimo account Admin attivo non può essere disattivato")
                }
            }

            val now = clockMillis()
            userDao.upsert(existing.copy(active = active, updatedAt = now))
            audit(actor.id, AuditAction.USER_UPDATED, """{"userId":"$userId","active":$active}""", now)
        }

    suspend fun setPermissions(actor: AuthenticatedUser, userId: String, permissions: Set<UserPermission>): Result<Unit> =
        runCatching {
            PermissionService.requirePermission(actor, UserPermission.MANAGE_USERS)
            val existing = requireNotNull(userDao.findById(userId)) { "Utente non trovato" }
            require(existing.role != UserRole.ADMIN) { "I permessi dell'admin non possono essere modificati" }

            val now = clockMillis()
            userDao.upsert(existing.copy(permissions = permissions, updatedAt = now))
            audit(actor.id, AuditAction.USER_UPDATED, """{"userId":"$userId","permissions":${permissions.map { it.name }}}""", now)
        }

    private suspend fun audit(actorUserId: String, action: AuditAction, detailsJson: String, now: Long) {
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
        const val MAX_DEMO_OPERATORS = 2
    }
}

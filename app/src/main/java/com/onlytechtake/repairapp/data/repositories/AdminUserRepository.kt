package com.onlytechtake.repairapp.data.repositories

import com.onlytechtake.repairapp.core.security.PasswordHasher
import com.onlytechtake.repairapp.data.local.dao.AuditLogDao
import com.onlytechtake.repairapp.data.local.dao.UserDao
import com.onlytechtake.repairapp.data.local.entities.AuditAction
import com.onlytechtake.repairapp.data.local.entities.AuditLogEntity
import com.onlytechtake.repairapp.data.local.entities.UserEntity
import com.onlytechtake.repairapp.data.local.entities.UserRole
import com.onlytechtake.repairapp.domain.auth.AuthenticatedUser
import com.onlytechtake.repairapp.domain.workspace.OperatorInput
import java.util.UUID
import kotlinx.coroutines.flow.Flow

class AdminUserRepository(
    private val userDao: UserDao,
    private val auditLogDao: AuditLogDao,
    private val passwordHasher: PasswordHasher,
    private val clockMillis: () -> Long = System::currentTimeMillis,
) {
    fun observeUsers(): Flow<List<UserEntity>> = userDao.observeAll()

    fun observeActiveUsers(): Flow<List<UserEntity>> = userDao.observeActiveUsers()

    suspend fun createOperator(actor: AuthenticatedUser, input: OperatorInput): Result<Unit> =
        runCatching {
            require(actor.role == UserRole.ADMIN) { "Solo admin" }
            require(input.displayName.isNotBlank()) { "Nome obbligatorio" }
            require(input.username.isNotBlank()) { "Username obbligatorio" }
            require(input.password.length >= MIN_PASSWORD_LENGTH) { "Password troppo corta" }
            require(userDao.findByUsername(input.username.trim()) == null) { "Username gia' esistente" }

            val now = clockMillis()
            val passwordHash = passwordHasher.hash(input.password.toCharArray())
            val user = UserEntity(
                id = UUID.randomUUID().toString(),
                displayName = input.displayName.trim(),
                username = input.username.trim(),
                passwordHash = passwordHash.hash,
                passwordSalt = passwordHash.salt,
                role = UserRole.OPERATOR,
                active = true,
                createdAt = now,
                updatedAt = now,
            )

            userDao.upsert(user)
            audit(actor.id, AuditAction.USER_CREATED, """{"userId":"${user.id}"}""", now)
        }

    suspend fun updateUser(actor: AuthenticatedUser, userId: String, displayName: String, username: String): Result<Unit> =
        runCatching {
            require(actor.role == UserRole.ADMIN) { "Solo admin" }
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

    suspend fun resetPassword(actor: AuthenticatedUser, userId: String, password: String): Result<Unit> =
        runCatching {
            require(actor.role == UserRole.ADMIN) { "Solo admin" }
            require(password.length >= MIN_PASSWORD_LENGTH) { "Password troppo corta" }
            val existing = requireNotNull(userDao.findById(userId)) { "Utente non trovato" }
            val hash = passwordHasher.hash(password.toCharArray())
            val now = clockMillis()
            userDao.upsert(
                existing.copy(
                    passwordHash = hash.hash,
                    passwordSalt = hash.salt,
                    updatedAt = now,
                ),
            )
            audit(actor.id, AuditAction.PASSWORD_RESET, """{"userId":"$userId"}""", now)
        }

    suspend fun setActive(actor: AuthenticatedUser, userId: String, active: Boolean): Result<Unit> =
        runCatching {
            require(actor.role == UserRole.ADMIN) { "Solo admin" }
            val existing = requireNotNull(userDao.findById(userId)) { "Utente non trovato" }
            if (!active && existing.role == UserRole.ADMIN) {
                require(userDao.countActiveByRole(UserRole.ADMIN) > 1) { "Ultimo admin non disattivabile" }
            }

            val now = clockMillis()
            userDao.upsert(existing.copy(active = active, updatedAt = now))
            audit(actor.id, AuditAction.USER_UPDATED, """{"userId":"$userId","active":$active}""", now)
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
    }
}

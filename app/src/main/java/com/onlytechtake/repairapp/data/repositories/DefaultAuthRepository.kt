package com.onlytechtake.repairapp.data.repositories

import com.onlytechtake.repairapp.core.demo.DemoMode
import com.onlytechtake.repairapp.core.security.PasswordHasher
import com.onlytechtake.repairapp.data.local.dao.AuditLogDao
import com.onlytechtake.repairapp.data.local.dao.LoginAttemptDao
import com.onlytechtake.repairapp.data.local.dao.UserDao
import com.onlytechtake.repairapp.data.local.entities.AuditAction
import com.onlytechtake.repairapp.data.local.entities.AuditLogEntity
import com.onlytechtake.repairapp.data.local.entities.LoginAttemptEntity
import com.onlytechtake.repairapp.data.local.entities.UserEntity
import com.onlytechtake.repairapp.data.local.entities.UserRole
import com.onlytechtake.repairapp.data.preferences.AuthPreferences
import com.onlytechtake.repairapp.data.session.InMemorySessionStore
import com.onlytechtake.repairapp.domain.auth.AuthRepository
import com.onlytechtake.repairapp.domain.auth.AuthState
import com.onlytechtake.repairapp.domain.auth.AuthenticatedUser
import com.onlytechtake.repairapp.domain.auth.AuthPolicy
import com.onlytechtake.repairapp.domain.auth.LoginResult
import java.util.UUID
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.first
import kotlin.time.Duration.Companion.milliseconds

import kotlinx.coroutines.flow.onStart

class DefaultAuthRepository(
    private val userDao: UserDao,
    private val auditLogDao: AuditLogDao,
    private val loginAttemptDao: LoginAttemptDao,
    private val authPreferences: AuthPreferences,
    private val sessionStore: InMemorySessionStore,
    private val passwordHasher: PasswordHasher,
    private val clockMillis: () -> Long = System::currentTimeMillis,
) : AuthRepository {
    override val currentUser = sessionStore.currentUser

    override fun observeAuthState(): Flow<AuthState> =
        combine(
            userDao.observeCountByRole(UserRole.ADMIN),
            sessionStore.state,
            authPreferences.lastUserId,
            inactivityTicker(),
        ) { adminCount, session, lastUserId, now ->
            val activeUser = if ((session.user != null) && (sessionStore.isExpired(now, AuthPolicy.INACTIVITY_TIMEOUT_MILLIS))) {
                sessionStore.clear()
                null
            } else {
                session.user
            }
            AuthState(
                initialized = true,
                configured = adminCount > 0,
                currentUser = activeUser,
                lastUsername = lastUserId?.let { userDao.findById(it)?.username }.orEmpty(),
            )
        }.onStart {
            // Artificial delay to allow the brand animation to complete at least one cycle (1500ms)
            delay(1500)
        }

    override suspend fun createFirstAdmin(
        displayName: String,
        username: String,
        password: CharArray,
    ): AuthenticatedUser {
        check(userDao.observeCountByRole(UserRole.ADMIN).first() == 0) {
            "First admin already exists"
        }
        require(displayName.isNotBlank()) { "Display name is required" }
        require(username.isNotBlank()) { "Username is required" }
        require(password.size >= AuthPolicy.MIN_PASSWORD_LENGTH) { "Password too short" }

        val passwordHash = passwordHasher.hash(password)
        val now = clockMillis()
        val user = UserEntity(
            id = UUID.randomUUID().toString(),
            displayName = displayName.trim(),
            username = username.trim(),
            passwordHash = passwordHash.hash,
            passwordSalt = passwordHash.salt,
            role = UserRole.ADMIN,
            permissions = com.onlytechtake.repairapp.data.local.entities.UserPermission.entries.toSet(),
            active = true,
            createdAt = now,
            updatedAt = now,
        )

        userDao.upsert(user)
        authPreferences.markConfigured()

        val authenticatedUser = user.toAuthenticatedUser()
        sessionStore.setCurrentUser(authenticatedUser, now)
        authPreferences.recordLogin(user.id, now)
        writeAudit(AuditAction.ADMIN_LOGIN, user.id, now)

        return authenticatedUser
    }

    override suspend fun login(username: String, password: CharArray): LoginResult {
        if (DemoMode.isEnabled) {
            return loginDemo()
        }
        val normalizedUsername = username.trim()
        val now = clockMillis()
        if (isLockedOut(normalizedUsername, now)) return LoginResult.LockedOut

        val user = userDao.findByUsername(normalizedUsername) ?: run {
            recordFailedAttempt(normalizedUsername, now)
            return LoginResult.InvalidCredentials
        }
        if (!user.active) return LoginResult.InactiveUser

        val passwordMatches = passwordHasher.verify(
            password = password,
            expectedHash = user.passwordHash,
            salt = user.passwordSalt,
        )
        if (!passwordMatches) {
            recordFailedAttempt(normalizedUsername, now)
            writeAudit(AuditAction.LOGIN_FAILED, user.id, now)
            return LoginResult.InvalidCredentials
        }

        val authenticatedUser = user.toAuthenticatedUser()
        sessionStore.setCurrentUser(authenticatedUser, now)
        authPreferences.recordLogin(user.id, now)
        loginAttemptDao.upsert(LoginAttemptEntity(username = normalizedUsername, successful = true, attemptAt = now))
        
        if (user.role == UserRole.ADMIN) {
            writeAudit(AuditAction.ADMIN_LOGIN, user.id, now)
        }

        return LoginResult.Success(authenticatedUser)
    }

    override suspend fun loginDemo(): LoginResult {
        DemoMode.setEnabled(enabled = true)
        val demoUser = AuthenticatedUser(
            id = "admin",
            displayName = "Demo Admin",
            username = "admin",
            role = UserRole.ADMIN,
            permissions = com.onlytechtake.repairapp.data.local.entities.UserPermission.entries.toSet(),
        )
        sessionStore.setCurrentUser(demoUser, clockMillis())
        return LoginResult.Success(demoUser)
    }

    override suspend fun logout() {
        DemoMode.setEnabled(enabled = false)
        sessionStore.clear()
    }

    override fun touchSession() {
        sessionStore.touch(clockMillis())
    }

    private suspend fun writeAudit(action: AuditAction, actorUserId: String, createdAt: Long) {
        auditLogDao.upsert(
            AuditLogEntity(
                id = UUID.randomUUID().toString(),
                action = action,
                actorUserId = actorUserId,
                orderId = null,
                customerId = null,
                detailsJson = null,
                createdAt = createdAt,
            ),
        )
    }

    private fun UserEntity.toAuthenticatedUser(): AuthenticatedUser =
        AuthenticatedUser(
            id = id,
            displayName = displayName,
            username = username,
            role = role,
            permissions = permissions,
        )

    private fun inactivityTicker(): Flow<Long> =
        flow {
            while (true) {
                emit(clockMillis())
                delay(INACTIVITY_CHECK_INTERVAL_MILLIS.milliseconds)
            }
        }

    private suspend fun isLockedOut(username: String, now: Long): Boolean {
        val since = now - AuthPolicy.LOGIN_LOCKOUT_MILLIS
        val failedAttempts = loginAttemptDao.countFailedAttempts(username, since)
        return failedAttempts >= AuthPolicy.MAX_FAILED_LOGIN_ATTEMPTS
    }

    private suspend fun recordFailedAttempt(username: String, now: Long) {
        loginAttemptDao.upsert(LoginAttemptEntity(username = username, successful = false, attemptAt = now))
    }

    private companion object {
        const val INACTIVITY_CHECK_INTERVAL_MILLIS = 60 * 1000L
    }
}

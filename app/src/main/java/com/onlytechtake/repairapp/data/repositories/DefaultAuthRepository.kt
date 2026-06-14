package com.onlytechtake.repairapp.data.repositories

import com.onlytechtake.repairapp.core.demo.DemoMode
import com.onlytechtake.repairapp.core.security.PasswordHasher
import com.onlytechtake.repairapp.data.local.dao.AppSettingDao
import com.onlytechtake.repairapp.data.local.dao.AuditLogDao
import com.onlytechtake.repairapp.data.local.dao.LoginAttemptDao
import com.onlytechtake.repairapp.data.local.dao.UserDao
import com.onlytechtake.repairapp.data.local.entities.AppSettingEntity
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
    private val appSettingDao: AppSettingDao,
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

        try {
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
                forcePasswordChange = false,
                createdAt = now,
                updatedAt = now,
            )

            userDao.upsert(user)
            authPreferences.markConfigured()

            val authenticatedUser = user.toAuthenticatedUser()
            sessionStore.setCurrentUser(authenticatedUser, now)
            authPreferences.recordLogin(user.id, now)
            writeAudit(AuditAction.ADMIN_LOGIN, user.id, null, now)

            return authenticatedUser
        } finally {
            password.fill('\u0000')
        }
    }

    override suspend fun login(username: String, password: CharArray): LoginResult {
        if (DemoMode.isEnabled) {
            return loginDemo()
        }
        val normalizedUsername = username.trim()
        val now = clockMillis()
        if (isLockedOut(normalizedUsername, now)) return LoginResult.LockedOut

        try {
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
                writeAudit(AuditAction.LOGIN_FAILED, user.id, null, now)
                return LoginResult.InvalidCredentials
            }

            val authenticatedUser = user.toAuthenticatedUser()

            if (user.forcePasswordChange) {
                return LoginResult.PasswordChangeRequired(authenticatedUser)
            }

            sessionStore.setCurrentUser(authenticatedUser, now)
            authPreferences.recordLogin(user.id, now)
            loginAttemptDao.upsert(LoginAttemptEntity(username = normalizedUsername, successful = true, attemptAt = now))

            if (user.role == UserRole.ADMIN) {
                writeAudit(AuditAction.ADMIN_LOGIN, user.id, null, now)
            }

            return LoginResult.Success(authenticatedUser)
        } finally {
            password.fill('\u0000')
        }
    }

    override suspend fun loginDemo(): LoginResult {
        if (!DemoMode.canEnable()) return LoginResult.InvalidCredentials

        DemoMode.setEnabled(enabled = true)
        val demoUser = AuthenticatedUser(
            id = "admin",
            displayName = "Demo Admin",
            username = "admin",
            role = UserRole.ADMIN,
            permissions = com.onlytechtake.repairapp.data.local.entities.UserPermission.entries.toSet(),
            forcePasswordChange = false,
        )
        sessionStore.setCurrentUser(demoUser, clockMillis())
        return LoginResult.Success(demoUser)
    }

    override suspend fun changePassword(
        username: String,
        currentPassword: CharArray,
        newPassword: CharArray,
    ): Result<Unit> = runCatching {
        val user = requireNotNull(userDao.findByUsername(username)) { "Utente non trovato" }
        val matches = passwordHasher.verify(currentPassword, user.passwordHash, user.passwordSalt)
        require(matches) { "Password attuale non corretta" }
        require(newPassword.size >= AuthPolicy.MIN_PASSWORD_LENGTH) { "Nuova password troppo corta" }

        val newHash = passwordHasher.hash(newPassword)
        val now = clockMillis()
        userDao.upsert(
            user.copy(
                passwordHash = newHash.hash,
                passwordSalt = newHash.salt,
                forcePasswordChange = false,
                updatedAt = now,
            )
        )
        writeAudit(AuditAction.PASSWORD_RESET, user.id, null, now)
    }.also {
        currentPassword.fill('\u0000')
        newPassword.fill('\u0000')
    }

    override suspend fun getRecoveryKit(): Result<String> = runCatching {
        // Only existing admins or the flow of first admin creation should call this
        // Generate a random 12-character code
        val code = java.util.UUID.randomUUID().toString().replace("-", "").take(12).uppercase()
        val hash = passwordHasher.hash(code.toCharArray())
        val now = clockMillis()
        val actorId = sessionStore.currentUser.value?.id

        appSettingDao.upsert(
            AppSettingEntity(
                key = "master_recovery_code_hash",
                value = hash.hash,
                updatedByUserId = actorId,
                updatedAt = now
            )
        )
        appSettingDao.upsert(
            AppSettingEntity(
                key = "master_recovery_code_salt",
                value = hash.salt,
                updatedByUserId = actorId,
                updatedAt = now
            )
        )
        code
    }

    override suspend fun resetAdminWithRecoveryCode(code: String, newPassword: CharArray): Result<Unit> = runCatching {
        val hashSetting = appSettingDao.findByKey("master_recovery_code_hash") ?: throw IllegalStateException("Recovery Kit non configurato")
        val saltSetting = appSettingDao.findByKey("master_recovery_code_salt") ?: throw IllegalStateException("Recovery Kit non configurato")
        
        val matches = passwordHasher.verify(code.toCharArray(), hashSetting.value, saltSetting.value)
        require(matches) { "Codice di recupero non valido" }
        require(newPassword.size >= AuthPolicy.MIN_PASSWORD_LENGTH) { "Nuova password troppo corta" }

        // Find all admins and reset their password, or just the first one?
        // Let's reset all admins to the new password to be sure.
        val admins = userDao.findAllAdmins()
        require(admins.isNotEmpty()) { "Nessun account admin trovato" }
        
        val newHash = passwordHasher.hash(newPassword)
        val now = clockMillis()
        admins.forEach { admin ->
            userDao.upsert(
                admin.copy(
                    passwordHash = newHash.hash,
                    passwordSalt = newHash.salt,
                    forcePasswordChange = false,
                    updatedAt = now
                )
            )
        }
        
        // Invalidate recovery code after use (monouso)
        // appSettingDao.deleteByKey("master_recovery_code_hash") // We don't have deleteByKey yet, let's just empty it
        appSettingDao.upsert(hashSetting.copy(value = "", updatedAt = now))

        writeAudit(AuditAction.PASSWORD_RESET, "SYSTEM_RECOVERY", """{"resetAdmins":${admins.size}}""", now)
    }.also {
        newPassword.fill('\u0000')
    }

    override suspend fun logout() {
        DemoMode.setEnabled(enabled = false)
        sessionStore.clear()
    }

    override fun touchSession() {
        sessionStore.touch(clockMillis())
    }

    private suspend fun writeAudit(action: AuditAction, actorUserId: String, detailsJson: String?, createdAt: Long) {
        auditLogDao.upsert(
            AuditLogEntity(
                id = UUID.randomUUID().toString(),
                action = action,
                actorUserId = actorUserId,
                orderId = null,
                customerId = null,
                detailsJson = detailsJson,
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
            forcePasswordChange = forcePasswordChange,
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

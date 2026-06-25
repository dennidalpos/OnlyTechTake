package com.onlytechtake.repairapp.data.repositories

import android.content.Context
import com.onlytechtake.repairapp.R
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
import com.onlytechtake.repairapp.domain.auth.AuthPolicy
import com.onlytechtake.repairapp.domain.auth.AuthRepository
import com.onlytechtake.repairapp.domain.auth.AuthState
import com.onlytechtake.repairapp.domain.auth.AuthenticatedUser
import com.onlytechtake.repairapp.domain.auth.LoginResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds

class DefaultAuthRepository(
    private val context: Context,
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
            val activeUser =
                if ((session.user != null) &&
                    (
                        sessionStore.isExpired(
                            now,
                            AuthPolicy.INACTIVITY_TIMEOUT_MILLIS,
                        )
                    )
                ) {
                    sessionStore.clear()
                    null
                } else {
                    session.user
                }

            // Simple non-blocking state creation
            AuthState(
                initialized = true,
                configured = adminCount > 0,
                currentUser = activeUser,
                lastUsername = lastUserId ?: "",
            )
        }.onStart {
            // Artificial delay for splash/animation
            delay(1500.milliseconds)
        }.catch { e ->
            android.util.Log.e("AuthRepository", "Flow error", e)
            emit(
                AuthState(
                    initialized = true,
                    initializationError = e.message ?: context.getString(R.string.error_critical_db_error),
                ),
            )
        }

    override suspend fun createFirstAdmin(
        displayName: String,
        username: String,
        password: CharArray,
    ): AuthenticatedUser =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            check(userDao.observeCountByRole(UserRole.ADMIN).first() == 0) {
                context.getString(R.string.error_first_admin_exists)
            }
            require(displayName.isNotBlank()) { context.getString(R.string.error_name_required) }
            require(username.isNotBlank()) { context.getString(R.string.error_username_required) }
            require(password.size >= AuthPolicy.MIN_PASSWORD_LENGTH) { context.getString(R.string.error_password_too_short) }

            try {
                val passwordHash = passwordHasher.hash(password)
                val now = clockMillis()
                val user =
                    UserEntity(
                        id = UUID.randomUUID().toString(),
                        displayName = displayName.trim(),
                        username = username.trim(),
                        passwordHash = passwordHash.hash,
                        passwordSalt = passwordHash.salt,
                        role = UserRole.ADMIN,
                        permissions =
                            com.onlytechtake.repairapp.data.local.entities.UserPermission.entries
                                .toSet(),
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

                authenticatedUser
            } finally {
                password.fill('\u0000')
            }
        }

    override suspend fun login(
        username: String,
        password: CharArray,
    ): LoginResult =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            if (DemoMode.isEnabled) {
                return@withContext loginDemo()
            }
            val normalizedUsername = username.trim()
            val now = clockMillis()
            if (isLockedOut(normalizedUsername, now)) return@withContext LoginResult.LockedOut

            try {
                val user =
                    userDao.findByUsername(normalizedUsername) ?: run {
                        recordFailedAttempt(normalizedUsername, now)
                        return@withContext LoginResult.InvalidCredentials
                    }
                if (!user.active) return@withContext LoginResult.InactiveUser

                val passwordMatches =
                    passwordHasher.verify(
                        password = password,
                        expectedHash = user.passwordHash,
                        salt = user.passwordSalt,
                    )
                if (!passwordMatches) {
                    recordFailedAttempt(normalizedUsername, now)
                    writeAudit(AuditAction.LOGIN_FAILED, user.id, null, now)
                    return@withContext LoginResult.InvalidCredentials
                }

                val authenticatedUser = user.toAuthenticatedUser()

                if (user.forcePasswordChange) {
                    return@withContext LoginResult.PasswordChangeRequired(authenticatedUser)
                }

                sessionStore.setCurrentUser(authenticatedUser, now)
                authPreferences.recordLogin(user.id, now)
                loginAttemptDao.upsert(
                    LoginAttemptEntity(
                        username = normalizedUsername,
                        successful = true,
                        attemptAt = now,
                    ),
                )

                if (user.role == UserRole.ADMIN) {
                    writeAudit(AuditAction.ADMIN_LOGIN, user.id, null, now)
                }

                LoginResult.Success(authenticatedUser)
            } finally {
                password.fill('\u0000')
            }
        }

    override suspend fun loginDemo(): LoginResult {
        if (!DemoMode.canEnable()) return LoginResult.InvalidCredentials

        DemoMode.setEnabled(enabled = true)
        val demoUser =
            AuthenticatedUser(
                id = "admin",
                displayName = context.getString(R.string.label_demo_admin),
                username = "admin",
                role = UserRole.ADMIN,
                permissions =
                    com.onlytechtake.repairapp.data.local.entities.UserPermission.entries
                        .toSet(),
                forcePasswordChange = false,
            )
        sessionStore.setCurrentUser(demoUser, clockMillis())
        return LoginResult.Success(demoUser)
    }

    override suspend fun changePassword(
        username: String,
        currentPassword: CharArray,
        newPassword: CharArray,
    ): Result<Unit> =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            runCatching {
                val user = requireNotNull(userDao.findByUsername(username)) { context.getString(R.string.error_user_not_found) }
                val matches =
                    passwordHasher.verify(currentPassword, user.passwordHash, user.passwordSalt)
                require(matches) { context.getString(R.string.error_current_password_incorrect) }
                require(newPassword.size >= AuthPolicy.MIN_PASSWORD_LENGTH) { context.getString(R.string.error_new_password_too_short) }

                val newHash = passwordHasher.hash(newPassword)
                val now = clockMillis()
                userDao.upsert(
                    user.copy(
                        passwordHash = newHash.hash,
                        passwordSalt = newHash.salt,
                        forcePasswordChange = false,
                        updatedAt = now,
                    ),
                )
                writeAudit(AuditAction.PASSWORD_RESET, user.id, null, now)
            }.also {
                currentPassword.fill('\u0000')
                newPassword.fill('\u0000')
            }
        }

    override suspend fun getRecoveryKit(): Result<String> =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            runCatching {
                // Only existing admins or the flow of first admin creation should call this
                // Generate a random 12-character code
                val code =
                    UUID
                        .randomUUID()
                        .toString()
                        .replace("-", "")
                        .take(12)
                        .uppercase()
                val hash = passwordHasher.hash(code.toCharArray())
                val now = clockMillis()
                val actorId = sessionStore.currentUser.value?.id

                appSettingDao.upsert(
                    AppSettingEntity(
                        key = "master_recovery_code_hash",
                        value = hash.hash,
                        updatedByUserId = actorId,
                        updatedAt = now,
                    ),
                )
                appSettingDao.upsert(
                    AppSettingEntity(
                        key = "master_recovery_code_salt",
                        value = hash.salt,
                        updatedByUserId = actorId,
                        updatedAt = now,
                    ),
                )
                code
            }
        }

    override suspend fun resetAdminWithRecoveryCode(
        code: String,
        newPassword: CharArray,
    ): Result<Unit> =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            runCatching {
                val hashSetting =
                    appSettingDao.findByKey("master_recovery_code_hash") ?: throw IllegalStateException(
                        context.getString(R.string.error_recovery_kit_not_configured),
                    )
                val saltSetting =
                    appSettingDao.findByKey("master_recovery_code_salt") ?: throw IllegalStateException(
                        context.getString(R.string.error_recovery_kit_not_configured),
                    )

                val matches =
                    passwordHasher.verify(code.toCharArray(), hashSetting.value, saltSetting.value)
                require(matches) { context.getString(R.string.error_recovery_code_invalid) }
                require(newPassword.size >= AuthPolicy.MIN_PASSWORD_LENGTH) { context.getString(R.string.error_new_password_too_short) }

                // Find all admins and reset their password, or just the first one?
                // Let's reset all admins to the new password to be sure.
                val admins = userDao.findAllAdmins()
                require(admins.isNotEmpty()) { context.getString(R.string.error_no_admin_found) }

                val newHash = passwordHasher.hash(newPassword)
                val now = clockMillis()
                admins.forEach { admin ->
                    userDao.upsert(
                        admin.copy(
                            passwordHash = newHash.hash,
                            passwordSalt = newHash.salt,
                            forcePasswordChange = false,
                            updatedAt = now,
                        ),
                    )
                }

                // Invalidate recovery code after use (monouso)
                // appSettingDao.deleteByKey("master_recovery_code_hash") // We don't have deleteByKey yet, let's just empty it
                appSettingDao.upsert(hashSetting.copy(value = "", updatedAt = now))

                writeAudit(
                    AuditAction.PASSWORD_RESET,
                    "SYSTEM_RECOVERY",
                    """{"resetAdmins":${admins.size}}""",
                    now,
                )
            }.also {
                newPassword.fill('\u0000')
            }
        }

    override suspend fun logout() {
        DemoMode.setEnabled(enabled = false)
        sessionStore.clear()
    }

    override fun touchSession() {
        sessionStore.touch(clockMillis())
    }

    private suspend fun writeAudit(
        action: AuditAction,
        actorUserId: String,
        detailsJson: String?,
        createdAt: Long,
    ) {
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

    private suspend fun isLockedOut(
        username: String,
        now: Long,
    ): Boolean {
        val since = now - AuthPolicy.LOGIN_LOCKOUT_MILLIS
        val failedAttempts = loginAttemptDao.countFailedAttempts(username, since)
        return failedAttempts >= AuthPolicy.MAX_FAILED_LOGIN_ATTEMPTS
    }

    private suspend fun recordFailedAttempt(
        username: String,
        now: Long,
    ) {
        loginAttemptDao.upsert(
            LoginAttemptEntity(
                username = username,
                successful = false,
                attemptAt = now,
            ),
        )
    }

    private companion object {
        const val INACTIVITY_CHECK_INTERVAL_MILLIS = 60 * 1000L
    }
}

package com.onlytechtake.repairapp.data.repositories

import android.content.Context
import com.onlytechtake.repairapp.BuildConfig
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.core.security.PasswordHash
import com.onlytechtake.repairapp.core.security.PasswordHasher
import com.onlytechtake.repairapp.data.local.dao.AuditLogDao
import com.onlytechtake.repairapp.data.local.dao.UserDao
import com.onlytechtake.repairapp.data.local.entities.UserEntity
import com.onlytechtake.repairapp.data.local.entities.UserPermission
import com.onlytechtake.repairapp.data.local.entities.UserRole
import com.onlytechtake.repairapp.domain.auth.AuthenticatedUser
import com.onlytechtake.repairapp.domain.workspace.OperatorInput
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AdminUserRepositoryTest {
    private val context = mockk<Context>(relaxed = true)
    private val userDao = mockk<UserDao>()
    private val auditLogDao = mockk<AuditLogDao>(relaxed = true)
    private val passwordHasher = mockk<PasswordHasher>()
    private lateinit var repository: AdminUserRepository

    @Before
    fun setUp() {
        repository = AdminUserRepository(context, userDao, auditLogDao, passwordHasher)
    }

    @Test
    fun setActiveFailsForAdmin() =
        runBlocking {
            val actor = mockAdmin()
            val admin =
                UserEntity(
                    id = "admin-1",
                    displayName = "Admin",
                    username = "admin",
                    passwordHash = "h",
                    passwordSalt = "s",
                    role = UserRole.ADMIN,
                    active = true,
                    createdAt = 0,
                    updatedAt = 0,
                )

            coEvery { userDao.findById("admin-1") } returns admin
            every { context.getString(R.string.error_admin_cannot_be_deactivated) } returns
                "L'account Admin non può essere disattivato"

            val result = repository.setActive(actor, "admin-1", false)

            assertTrue(result.isFailure)
            assertEquals(
                "L'account Admin non può essere disattivato",
                result.exceptionOrNull()?.message,
            )
        }

    @Test
    fun createOperatorRespectsFlavorLimits() =
        runBlocking {
            val actor = mockAdmin()
            val input = OperatorInput("Operatore", "operatore", "password-sicura".toCharArray())
            coEvery { userDao.findByUsername("operatore") } returns null
            every { context.getString(R.string.demo_error_operator_not_available) } returns
                "La versione Demo non consente la creazione di operatori. Usa solo l'account admin locale."

            if (!BuildConfig.IS_DEMO_VERSION) {
                coEvery { passwordHasher.hash(any()) } returns PasswordHash("hash", "salt")
                coEvery { userDao.upsert(any()) } returns Unit
            }

            val result = repository.createOperator(actor, input)

            if (BuildConfig.IS_DEMO_VERSION) {
                assertTrue(result.isFailure)
                assertEquals(
                    "La versione Demo non consente la creazione di operatori. Usa solo l'account admin locale.",
                    result.exceptionOrNull()?.message,
                )
                coVerify(exactly = 0) { userDao.upsert(any()) }
            } else {
                assertTrue(result.isSuccess)
                coVerify(exactly = 1) { userDao.upsert(any()) }
            }
        }

    private fun mockAdmin() =
        AuthenticatedUser(
            id = "admin-1",
            displayName = "Admin",
            username = "admin",
            role = UserRole.ADMIN,
            permissions = UserPermission.entries.toSet(),
        )
}

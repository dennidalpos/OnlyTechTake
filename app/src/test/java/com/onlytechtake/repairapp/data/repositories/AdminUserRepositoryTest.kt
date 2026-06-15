package com.onlytechtake.repairapp.data.repositories

import android.content.Context
import com.onlytechtake.repairapp.core.security.PasswordHasher
import com.onlytechtake.repairapp.data.local.dao.AuditLogDao
import com.onlytechtake.repairapp.data.local.dao.UserDao
import com.onlytechtake.repairapp.data.local.entities.UserEntity
import com.onlytechtake.repairapp.data.local.entities.UserPermission
import com.onlytechtake.repairapp.data.local.entities.UserRole
import com.onlytechtake.repairapp.domain.auth.AuthenticatedUser
import io.mockk.coEvery
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
    fun setActiveFailsForAdmin() = runBlocking {
        val actor = mockAdmin()
        val admin = UserEntity(
            id = "admin-1",
            displayName = "Admin",
            username = "admin",
            passwordHash = "h",
            passwordSalt = "s",
            role = UserRole.ADMIN,
            active = true,
            createdAt = 0,
            updatedAt = 0
        )
        
        coEvery { userDao.findById("admin-1") } returns admin
        
        val result = repository.setActive(actor, "admin-1", false)
        
        assertTrue(result.isFailure)
        assertEquals("L'account Admin non può essere disattivato", result.exceptionOrNull()?.message)
    }

    private fun mockAdmin() = AuthenticatedUser(
        id = "admin-1",
        displayName = "Admin",
        username = "admin",
        role = UserRole.ADMIN,
        permissions = UserPermission.entries.toSet()
    )
}

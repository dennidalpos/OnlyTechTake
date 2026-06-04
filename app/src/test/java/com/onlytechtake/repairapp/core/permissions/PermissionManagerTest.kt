package com.onlytechtake.repairapp.core.permissions

import com.onlytechtake.repairapp.data.local.entities.UserRole
import com.onlytechtake.repairapp.domain.auth.AuthenticatedUser
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PermissionManagerTest {
    private val permissionManager = PermissionManager()

    @Test
    fun adminHasAllPermissions() {
        val admin = user(role = UserRole.ADMIN)

        AppPermission.entries.forEach { permission ->
            assertTrue(permissionManager.hasPermission(admin, permission))
        }
    }

    @Test
    fun operatorCannotUseAdminOnlyPermissions() {
        val operator = user(role = UserRole.OPERATOR)

        assertFalse(permissionManager.hasPermission(operator, AppPermission.ManageUsers))
        assertFalse(permissionManager.hasPermission(operator, AppPermission.DeleteOrders))
        assertTrue(permissionManager.hasPermission(operator, AppPermission.CreateOrders))
    }

    @Test
    fun anonymousUserHasNoPermissions() {
        assertFalse(permissionManager.hasPermission(null, AppPermission.CreateOrders))
    }

    private fun user(role: UserRole): AuthenticatedUser =
        AuthenticatedUser(
            id = "user-id",
            displayName = "User",
            username = "user",
            role = role,
        )
}

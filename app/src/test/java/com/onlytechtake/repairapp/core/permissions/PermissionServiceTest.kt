package com.onlytechtake.repairapp.core.permissions

import com.onlytechtake.repairapp.data.local.entities.UserPermission
import com.onlytechtake.repairapp.data.local.entities.UserRole
import com.onlytechtake.repairapp.domain.auth.AuthenticatedUser
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PermissionServiceTest {

    @Test
    fun adminHasAllPermissions() {
        val admin = AuthenticatedUser("1", "Admin", "admin", UserRole.ADMIN, emptySet())
        assertTrue(PermissionService.hasPermission(admin, UserPermission.DELETE_DATA))
        assertTrue(PermissionService.hasPermission(admin, UserPermission.CREATE_ORDERS))
    }

    @Test
    fun operatorOnlyHasAssignedPermissions() {
        val operator = AuthenticatedUser(
            "2", "Op", "op", UserRole.OPERATOR,
            setOf(UserPermission.CREATE_ORDERS, UserPermission.EDIT_OWN_ORDERS)
        )
        
        assertTrue(PermissionService.hasPermission(operator, UserPermission.CREATE_ORDERS))
        assertTrue(PermissionService.hasPermission(operator, UserPermission.EDIT_OWN_ORDERS))
        assertFalse(PermissionService.hasPermission(operator, UserPermission.DELETE_DATA))
    }

    @Test(expected = SecurityException::class)
    fun requirePermissionThrowsWhenMissing() {
        val operator = AuthenticatedUser("2", "Op", "op", UserRole.OPERATOR, emptySet())
        PermissionService.requirePermission(operator, UserPermission.DELETE_DATA)
    }

    @Test
    fun requirePermissionDoesNotThrowWhenPresent() {
        val admin = AuthenticatedUser("1", "Admin", "admin", UserRole.ADMIN, emptySet())
        PermissionService.requirePermission(admin, UserPermission.DELETE_DATA)
    }
}

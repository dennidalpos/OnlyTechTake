package com.onlytechtake.repairapp.core.permissions

import com.onlytechtake.repairapp.domain.auth.AuthenticatedUser
import com.onlytechtake.repairapp.domain.models.UserPermission
import com.onlytechtake.repairapp.domain.models.UserRole

object PermissionService {
    fun hasPermission(
        user: AuthenticatedUser?,
        permission: UserPermission,
    ): Boolean {
        if (user == null) return false
        if (user.role == UserRole.ADMIN) return true
        return user.permissions.contains(permission)
    }

    fun requirePermission(
        user: AuthenticatedUser?,
        permission: UserPermission,
    ) {
        if (!hasPermission(user, permission)) {
            throw SecurityException("Permesso mancante: ${permission.name}")
        }
    }

    fun isAdmin(user: AuthenticatedUser?): Boolean = user?.role == UserRole.ADMIN
}

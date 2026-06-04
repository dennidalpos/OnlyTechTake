package com.onlytechtake.repairapp.core.permissions

import com.onlytechtake.repairapp.data.local.entities.UserRole
import com.onlytechtake.repairapp.domain.auth.AuthenticatedUser

enum class AppPermission {
    ManageUsers,
    ManageSettings,
    ManageTemplates,
    ExportData,
    DeleteOrders,
    CreateCustomers,
    CreateOrders,
    ManageOwnOrders,
    ViewAllOrders,
    SendManualWhatsApp,
}

class PermissionManager {
    fun hasPermission(user: AuthenticatedUser?, permission: AppPermission): Boolean {
        if (user == null) return false

        return when (user.role) {
            UserRole.ADMIN -> true
            UserRole.OPERATOR -> permission in operatorPermissions
        }
    }

    private companion object {
        val operatorPermissions = setOf(
            AppPermission.CreateCustomers,
            AppPermission.CreateOrders,
            AppPermission.ManageOwnOrders,
            AppPermission.ViewAllOrders,
            AppPermission.SendManualWhatsApp,
        )
    }
}

package com.onlytechtake.repairapp.domain.workspace

import com.onlytechtake.repairapp.core.permissions.PermissionService
import com.onlytechtake.repairapp.data.local.entities.OrderStatus
import com.onlytechtake.repairapp.data.local.entities.UserPermission
import com.onlytechtake.repairapp.domain.auth.AuthenticatedUser

object OrderClosurePermissionPolicy {
    fun requireClosePermissionIfNeeded(actor: AuthenticatedUser, targetStatus: OrderStatus) {
        if (targetStatus in OrderStatusPolicy.closedStatuses) {
            PermissionService.requirePermission(actor, UserPermission.CLOSE_ORDERS)
        }
    }
}

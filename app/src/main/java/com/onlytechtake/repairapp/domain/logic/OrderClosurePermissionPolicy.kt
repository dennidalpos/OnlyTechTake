package com.onlytechtake.repairapp.domain.logic

import com.onlytechtake.repairapp.core.permissions.PermissionService
import com.onlytechtake.repairapp.domain.auth.AuthenticatedUser
import com.onlytechtake.repairapp.domain.models.OrderStatus
import com.onlytechtake.repairapp.domain.models.UserPermission

object OrderClosurePermissionPolicy {
    fun requireClosePermissionIfNeeded(
        actor: AuthenticatedUser,
        targetStatus: OrderStatus,
    ) {
        if (targetStatus in OrderStatusPolicy.closedStatuses) {
            PermissionService.requirePermission(actor, UserPermission.CLOSE_ORDERS)
        }
    }
}

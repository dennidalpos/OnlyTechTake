package com.onlytechtake.repairapp.domain.workspace

import com.onlytechtake.repairapp.domain.auth.AuthenticatedUser
import com.onlytechtake.repairapp.domain.models.OrderStatus
import com.onlytechtake.repairapp.domain.models.UserPermission
import com.onlytechtake.repairapp.domain.models.UserRole
import org.junit.Assert.assertThrows
import org.junit.Test

class OrderClosurePermissionPolicyTest {
    @Test
    fun nonClosingTargetDoesNotRequireClosePermission() {
        OrderClosurePermissionPolicy.requireClosePermissionIfNeeded(
            actor = operator(permissions = setOf(UserPermission.EDIT_OWN_ORDERS)),
            targetStatus = OrderStatus.IN_PROGRESS,
        )
    }

    @Test
    fun closingTargetRequiresCloseOrdersPermission() {
        assertThrows(SecurityException::class.java) {
            OrderClosurePermissionPolicy.requireClosePermissionIfNeeded(
                actor = operator(permissions = setOf(UserPermission.EDIT_OWN_ORDERS)),
                targetStatus = OrderStatus.CLOSED,
            )
        }
    }

    @Test
    fun closingTargetAllowsExplicitCloseOrdersPermission() {
        OrderClosurePermissionPolicy.requireClosePermissionIfNeeded(
            actor =
                operator(
                    permissions =
                        setOf(
                            UserPermission.EDIT_OWN_ORDERS,
                            UserPermission.CLOSE_ORDERS,
                        ),
                ),
            targetStatus = OrderStatus.CLOSED,
        )
    }

    @Test
    fun adminCanCloseWithoutExplicitPermissionSet() {
        OrderClosurePermissionPolicy.requireClosePermissionIfNeeded(
            actor =
                AuthenticatedUser(
                    id = "admin-1",
                    displayName = "Admin",
                    username = "admin",
                    role = UserRole.ADMIN,
                ),
            targetStatus = OrderStatus.CLOSED,
        )
    }

    private fun operator(permissions: Set<UserPermission>) =
        AuthenticatedUser(
            id = "operator-1",
            displayName = "Operatore",
            username = "operatore",
            role = UserRole.OPERATOR,
            permissions = permissions,
        )
}

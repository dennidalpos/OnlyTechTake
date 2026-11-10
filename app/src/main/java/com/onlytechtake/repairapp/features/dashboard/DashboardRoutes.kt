package com.onlytechtake.repairapp.features.dashboard

import kotlinx.serialization.Serializable

sealed interface DashboardNavRoute {
    @Serializable
    data object Home : DashboardNavRoute

    @Serializable
    data object Customers : DashboardNavRoute

    @Serializable
    data class Orders(
        val status: String? = null,
        val statuses: List<String>? = null,
        val customerId: String? = null,
    ) : DashboardNavRoute

    @Serializable
    data object NewOrder : DashboardNavRoute

    @Serializable
    data class OrderDetail(
        val orderId: String,
    ) : DashboardNavRoute

    @Serializable
    data class Settings(
        val section: String? = null,
        val fromIntake: Boolean = false,
    ) : DashboardNavRoute

    @Serializable
    data object Admin : DashboardNavRoute
}

package com.onlytechtake.repairapp.features.dashboard

object DashboardRoutes {
    const val HOME = "home"
    const val CUSTOMERS = "customers"
    const val ORDERS = "orders?status={status}&statuses={statuses}&customerId={customerId}"
    const val NEW_ORDER = "new_order"
    const val ORDER_DETAIL = "order_detail/{orderId}"
    const val SETTINGS = "settings?section={section}&fromIntake={fromIntake}"
    const val ADMIN = "admin"

    fun orderDetail(orderId: String) = "order_detail/$orderId"

    fun orders(
        status: String? = null,
        statuses: List<String>? = null,
        customerId: String? = null,
    ): String {
        val params =
            listOfNotNull(
                status?.let { "status=$it" },
                statuses?.let { "statuses=${it.joinToString(",")}" },
                customerId?.let { "customerId=$it" },
            )
        return if (params.isEmpty()) "orders" else "orders?${params.joinToString("&")}"
    }

    fun settings(
        section: String? = null,
        fromIntake: Boolean = false,
    ): String {
        val params =
            listOfNotNull(
                section?.let { "section=$it" },
                if (fromIntake) "fromIntake=true" else null,
            )
        return if (params.isEmpty()) "settings" else "settings?${params.joinToString("&")}"
    }
}

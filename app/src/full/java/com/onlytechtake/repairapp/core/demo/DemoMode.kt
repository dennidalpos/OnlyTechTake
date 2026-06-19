package com.onlytechtake.repairapp.core.demo

import com.onlytechtake.repairapp.data.local.entities.CustomerEntity
import com.onlytechtake.repairapp.data.local.entities.OrderCategoryEntity

object DemoMode {
    const val isEnabled: Boolean = false

    fun canEnable(): Boolean = false

    fun setEnabled(enabled: Boolean) {
        // No-op in full version
    }

    val demoCustomer: CustomerEntity get() = throw IllegalStateException("DemoMode non disponibile")
    val demoCategories: List<OrderCategoryEntity> get() = throw IllegalStateException("DemoMode non disponibile")
}

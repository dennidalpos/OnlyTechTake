package com.onlytechtake.repairapp.core.demo

import com.onlytechtake.repairapp.data.local.entities.CustomerEntity
import com.onlytechtake.repairapp.data.local.entities.OrderCategoryEntity
import com.onlytechtake.repairapp.data.local.entities.RepairOrderEntity

object DemoMode {
    const val isEnabled: Boolean = false

    fun canEnable(): Boolean = false

    fun setEnabled(enabled: Boolean) {
        // No-op in full version
    }

    val demoCustomer: CustomerEntity get() = throw IllegalStateException("DemoMode non disponibile")
    val demoCategory: OrderCategoryEntity get() = throw IllegalStateException("DemoMode non disponibile")
    val demoOrder: RepairOrderEntity get() = throw IllegalStateException("DemoMode non disponibile")
    val demoCategories =
        listOf(
            OrderCategoryEntity(
                "cat-smartphone",
                null,
                0,
                "Smartphone",
                "Riparazione telefoni cellulari",
                true
            ),
            OrderCategoryEntity("cat-apple", "cat-smartphone", 1, "Apple", "Dispositivi iOS", true),
            OrderCategoryEntity(
                "cat-iphone",
                "cat-apple",
                2,
                "iPhone",
                "Tutti i modelli iPhone",
                true
            ),
            OrderCategoryEntity(
                "cat-iphone-14-pro",
                "cat-iphone",
                3,
                "iPhone 14 Pro",
                "Riparazione display",
                true
            ),
            OrderCategoryEntity(
                "cat-pc",
                null,
                0,
                "Computer & Laptop",
                "Riparazioni hardware e software PC",
                true
            ),
            OrderCategoryEntity(
                "cat-notebook",
                "cat-pc",
                1,
                "Notebook",
                "Portatili e Ultrabook",
                true
            ),
            OrderCategoryEntity(
                "cat-windows-laptop",
                "cat-notebook",
                2,
                "Windows Laptop",
                "Asus, HP, Dell",
                true
            ),
            OrderCategoryEntity(
                "cat-asus-zenbook",
                "cat-windows-laptop",
                3,
                "Asus Zenbook OLED",
                "Riparazione hardware",
                true
            ),
        )
}

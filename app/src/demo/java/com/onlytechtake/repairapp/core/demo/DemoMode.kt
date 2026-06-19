package com.onlytechtake.repairapp.core.demo

import com.onlytechtake.repairapp.BuildConfig
import com.onlytechtake.repairapp.data.local.entities.CustomerEntity
import com.onlytechtake.repairapp.data.local.entities.CustomerType
import com.onlytechtake.repairapp.data.local.entities.OrderCategoryEntity

object DemoMode {
    var isEnabled: Boolean = false
        private set

    fun canEnable(): Boolean = BuildConfig.IS_DEMO_MODE_ENABLED

    fun setEnabled(enabled: Boolean) {
        if (canEnable()) {
            isEnabled = enabled
        }
    }

    val demoCustomer =
        CustomerEntity(
            id = "demo-customer-1",
            type = CustomerType.PRIVATE,
            firstName = "Marco",
            lastName = "Rossi",
            companyName = null,
            vatNumber = null,
            taxCode = null,
            phone = "+39 333 000 0000",
            email = "marco.rossi@example.com",
            address = "Via Roma 1, Milano",
            notes = "Cliente abituale",
            createdAt = 1735689600000L, // 2025-01-01
            updatedAt = 1735689600000L,
        )

    val demoCategories =
        listOf(
            OrderCategoryEntity(
                id = "cat-smartphone",
                parentId = null,
                level = 0,
                name = "Smartphone",
                description = "Riparazione telefoni cellulari",
                active = true,
            ),
            OrderCategoryEntity(
                id = "cat-apple",
                parentId = "cat-smartphone",
                level = 1,
                name = "Apple",
                description = "iPhone, iPad, Watch",
                active = true,
            ),
            OrderCategoryEntity(
                id = "cat-iphone",
                parentId = "cat-apple",
                level = 2,
                name = "iPhone",
                description = "Tutti i modelli",
                active = true,
            ),
            OrderCategoryEntity(
                id = "cat-iphone-13",
                parentId = "cat-iphone",
                level = 3,
                name = "iPhone 13",
                description = "Riparazione Display",
                active = true,
            ),
            OrderCategoryEntity(
                id = "cat-console",
                parentId = null,
                level = 0,
                name = "Console",
                description = "Gaming hardware",
                active = true,
            ),
            OrderCategoryEntity(
                id = "cat-sony",
                parentId = "cat-console",
                level = 1,
                name = "Sony",
                description = "PlayStation World",
                active = true,
            ),
            OrderCategoryEntity(
                id = "cat-ps5",
                parentId = "cat-sony",
                level = 2,
                name = "PS5",
                description = "Console Next-Gen",
                active = true,
            ),
            OrderCategoryEntity(
                id = "cat-dualsense",
                parentId = "cat-ps5",
                level = 3,
                name = "DualSense",
                description = "Riparazione Drift",
                active = true,
            ),
        )
}

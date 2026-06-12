package com.onlytechtake.repairapp.core.demo

import com.onlytechtake.repairapp.BuildConfig
import com.onlytechtake.repairapp.data.local.entities.*
import java.util.UUID

object DemoMode {
    var isEnabled: Boolean = false
        private set

    fun canEnable(): Boolean = BuildConfig.IS_DEMO_MODE_ENABLED

    fun setEnabled(enabled: Boolean) {
        if (canEnable()) {
            isEnabled = enabled
        }
    }

    val demoCustomer = CustomerEntity(
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
        updatedAt = 1735689600000L
    )

    val demoCategory = OrderCategoryEntity(
        id = "cat-smartphone",
        parentId = null,
        level = 0,
        name = "Smartphone",
        description = "Riparazione telefoni cellulari",
        active = true
    )

    val demoOrder = RepairOrderEntity(
        id = "demo-order-1",
        orderNumber = "OTT-2026-001",
        customerId = demoCustomer.id,
        title = "iPhone 13",
        categoryId = "cat-smartphone",
        status = OrderStatus.RESULT,
        priority = OrderPriority.NORMAL,
        assignedToUserId = "admin",
        createdByUserId = "admin",
        description = "Schermo rotto, touch parzialmente funzionante",
        diagnosisOutcome = DiagnosisOutcome.REPAIRABLE,
        estimateAmountCents = 8900L,
        customerActionStatus = CustomerActionStatus.WAITING_ACCEPTANCE,
        internalNotes = "Display da sostituire, preventivo approvato",
        createdAt = 1767225600000L, // 2026-01-01
        updatedAt = 1767225600000L
    )
}

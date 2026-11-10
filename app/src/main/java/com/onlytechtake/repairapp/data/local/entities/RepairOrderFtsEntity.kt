package com.onlytechtake.repairapp.data.local.entities

import androidx.room.Entity
import androidx.room.Fts4

@Fts4(contentEntity = RepairOrderEntity::class)
@Entity(tableName = "repair_orders_fts")
data class RepairOrderFtsEntity(
    val orderNumber: String,
    val title: String,
    val description: String?,
)

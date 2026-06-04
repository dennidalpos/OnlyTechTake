package com.onlytechtake.repairapp.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "order_categories",
    indices = [
        Index(value = ["name"], unique = true),
        Index(value = ["active"]),
        Index(value = ["sortOrder"]),
    ],
)
data class OrderCategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String?,
    @ColumnInfo(defaultValue = "1") val active: Boolean,
    @ColumnInfo(defaultValue = "0") val sortOrder: Int,
)

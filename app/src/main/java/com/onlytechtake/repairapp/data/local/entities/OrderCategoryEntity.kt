package com.onlytechtake.repairapp.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "order_categories",
    foreignKeys = [
        ForeignKey(
            entity = OrderCategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["parentId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index(value = ["parentId", "name"], unique = true),
        Index(value = ["parentId"]),
        Index(value = ["level"]),
        Index(value = ["active"]),
    ],
)
data class OrderCategoryEntity(
    @PrimaryKey val id: String,
    val parentId: String?,
    @ColumnInfo(defaultValue = "0") val level: Int,
    val name: String,
    val description: String?,
    @ColumnInfo(defaultValue = "1") val active: Boolean,
)

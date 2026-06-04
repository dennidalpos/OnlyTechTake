package com.onlytechtake.repairapp.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "customers",
    indices = [
        Index(value = ["type"]),
        Index(value = ["firstName"]),
        Index(value = ["lastName"]),
        Index(value = ["companyName"]),
        Index(value = ["phone"]),
        Index(value = ["email"]),
        Index(value = ["vatNumber"]),
        Index(value = ["taxCode"]),
    ],
)
data class CustomerEntity(
    @PrimaryKey val id: String,
    val type: CustomerType,
    val firstName: String?,
    val lastName: String?,
    val companyName: String?,
    val vatNumber: String?,
    val taxCode: String?,
    val phone: String?,
    val email: String?,
    val address: String?,
    val notes: String?,
    val createdAt: Long,
    val updatedAt: Long,
)

package com.onlytechtake.repairapp.data.local.entities

import androidx.room.Entity
import androidx.room.Fts4

@Fts4(contentEntity = CustomerEntity::class)
@Entity(tableName = "customers_fts")
data class CustomerFtsEntity(
    val firstName: String?,
    val lastName: String?,
    val companyName: String?,
    val phone: String?,
    val email: String?,
    val vatNumber: String?,
    val taxCode: String?,
)

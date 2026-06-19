package com.onlytechtake.repairapp.core.utils

import android.content.Context
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.data.local.entities.CustomerEntity
import com.onlytechtake.repairapp.data.local.entities.CustomerType

fun CustomerEntity?.displayName(context: Context): String =
    if (this == null) {
        context.getString(R.string.orders_customer_unavailable)
    } else {
        displayName(
            unnamedPrivate = context.getString(R.string.customer_unnamed_private),
            unnamedBusiness = context.getString(R.string.customer_unnamed_business),
        )
    }

fun CustomerEntity?.displayName(
    unnamedPrivate: String,
    unnamedBusiness: String,
): String =
    if (this == null) {
        ""
    } else {
        when (type) {
            CustomerType.PRIVATE ->
                listOfNotNull(firstName, lastName)
                    .joinToString(" ")
                    .ifBlank { unnamedPrivate }

            CustomerType.BUSINESS ->
                companyName
                    .orEmpty()
                    .ifBlank { unnamedBusiness }
        }
    }

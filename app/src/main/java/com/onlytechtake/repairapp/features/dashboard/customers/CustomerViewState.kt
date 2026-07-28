package com.onlytechtake.repairapp.features.dashboard.customers

import com.onlytechtake.repairapp.domain.models.Customer
import com.onlytechtake.repairapp.domain.models.CustomerInput
import com.onlytechtake.repairapp.domain.models.CustomerType

data class CustomerViewState(
    val selectedCustomer: Customer? = null,
    val showForm: Boolean = false,
    val editingCustomerId: String? = null,
    val isBusiness: Boolean = false,
    val firstName: String = "",
    val lastName: String = "",
    val companyName: String = "",
    val phone: String = "",
    val email: String = "",
    val taxCode: String = "",
    val vatNumber: String = "",
    val address: String = "",
    val notes: String = "",
) {
    val isEditing: Boolean get() = editingCustomerId != null

    fun toInput() =
        CustomerInput(
            id = editingCustomerId,
            type = if (isBusiness) CustomerType.BUSINESS else CustomerType.PRIVATE,
            firstName = firstName,
            lastName = lastName,
            companyName = companyName,
            phone = phone,
            email = email,
            taxCode = taxCode,
            vatNumber = vatNumber,
            address = address,
            notes = notes,
        )

    fun isValid(): Boolean {
        val hasContact = phone.isNotBlank() || email.isNotBlank()
        val hasName = if (isBusiness) companyName.isNotBlank() else firstName.isNotBlank() && lastName.isNotBlank()
        return hasContact && hasName
    }
}

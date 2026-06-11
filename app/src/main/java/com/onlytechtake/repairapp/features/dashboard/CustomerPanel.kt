package com.onlytechtake.repairapp.features.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.platform.LocalContext
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.core.design.RepairAppTestTags
import com.onlytechtake.repairapp.core.utils.displayName
import com.onlytechtake.repairapp.core.utils.label
import com.onlytechtake.repairapp.data.local.entities.CustomerEntity
import com.onlytechtake.repairapp.data.local.entities.CustomerType
import com.onlytechtake.repairapp.domain.workspace.CustomerInput
import com.onlytechtake.repairapp.features.workspace.WorkspaceViewModel
import com.onlytechtake.repairapp.features.workspace.ButtonTone
import com.onlytechtake.repairapp.features.workspace.label

@Composable
fun CustomerPanel(viewModel: WorkspaceViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val customers by viewModel.customerSuggestions.collectAsState()
    var showForm by rememberSaveable { mutableStateOf(false) }
    var confirmSave by rememberSaveable { mutableStateOf(false) }
    var editingCustomerId by rememberSaveable { mutableStateOf<String?>(null) }
    var isBusiness by rememberSaveable { mutableStateOf(false) }
    var firstName by rememberSaveable { mutableStateOf("") }
    var lastName by rememberSaveable { mutableStateOf("") }
    var companyName by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var taxCode by rememberSaveable { mutableStateOf("") }
    var vatNumber by rememberSaveable { mutableStateOf("") }
    var address by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    val input = CustomerInput(
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
    val canSave = (phone.isNotBlank() || email.isNotBlank()) &&
        if (isBusiness) companyName.isNotBlank() else firstName.isNotBlank() && lastName.isNotBlank()

    fun clearForm() {
        editingCustomerId = null
        isBusiness = false
        firstName = ""
        lastName = ""
        companyName = ""
        phone = ""
        email = ""
        taxCode = ""
        vatNumber = ""
        address = ""
        notes = ""
    }

    fun openNewCustomerForm() {
        clearForm()
        showForm = true
    }

    fun editCustomer(customer: CustomerEntity) {
        editingCustomerId = customer.id
        isBusiness = customer.type == CustomerType.BUSINESS
        firstName = customer.firstName.orEmpty()
        lastName = customer.lastName.orEmpty()
        companyName = customer.companyName.orEmpty()
        phone = customer.phone.orEmpty()
        email = customer.email.orEmpty()
        taxCode = customer.taxCode.orEmpty()
        vatNumber = customer.vatNumber.orEmpty()
        address = customer.address.orEmpty()
        notes = customer.notes.orEmpty()
        showForm = true
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(DashboardSpacing.screenPadding),
        verticalArrangement = Arrangement.spacedBy(DashboardSpacing.listGap),
    ) {
        item {
            SectionTitle(stringResource(R.string.customer_title))
                Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(DashboardSpacing.cardPadding),
                    verticalArrangement = Arrangement.spacedBy(DashboardSpacing.formGap),
                ) {
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = uiState.customerSearchQuery,
                        onValueChange = viewModel::setCustomerSearchQuery,
                        label = { Text(stringResource(R.string.customer_search_label)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                    )
                    TouchButton(
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(R.string.customer_new),
                        testTag = RepairAppTestTags.CustomerNewButton,
                        onClick = ::openNewCustomerForm,
                    )
                }
            }
        }

        if (showForm) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(DashboardSpacing.cardPadding),
                        verticalArrangement = Arrangement.spacedBy(DashboardSpacing.formGap),
                    ) {
                        Text(
                            text = if (editingCustomerId == null) stringResource(R.string.customer_new) else stringResource(R.string.customer_edit),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        CustomerTypeToggle(isBusiness = isBusiness, onBusinessChange = { isBusiness = it })
                        if (isBusiness) {
                            Field(
                                companyName,
                                { companyName = it },
                                stringResource(R.string.customer_field_company_name),
                                required = true,
                                showError = companyName.isBlank(),
                                singleLine = true,
                                testTag = RepairAppTestTags.CustomerCompanyNameField,
                            )
                            Field(vatNumber, { vatNumber = it }, stringResource(R.string.customer_field_vat_number), testTag = RepairAppTestTags.CustomerVatNumberField)
                        } else {
                            Field(
                                firstName,
                                { firstName = it },
                                stringResource(R.string.customer_field_first_name),
                                required = true,
                                showError = firstName.isBlank(),
                                singleLine = true,
                                testTag = RepairAppTestTags.CustomerFirstNameField,
                            )
                            Field(
                                lastName,
                                { lastName = it },
                                stringResource(R.string.customer_field_last_name),
                                required = true,
                                showError = lastName.isBlank(),
                                singleLine = true,
                                testTag = RepairAppTestTags.CustomerLastNameField,
                            )
                        }
                        Field(taxCode, { taxCode = it }, stringResource(R.string.customer_field_tax_code), testTag = RepairAppTestTags.CustomerTaxCodeField)
                        Field(
                            phone,
                            { phone = it },
                            stringResource(R.string.customer_field_phone),
                            required = true,
                            showError = phone.isBlank() && email.isBlank(),
                            singleLine = true,
                            testTag = RepairAppTestTags.CustomerPhoneField,
                        )
                        Field(
                            email,
                            { email = it },
                            stringResource(R.string.customer_field_email),
                            required = true,
                            showError = phone.isBlank() && email.isBlank(),
                            singleLine = true,
                            testTag = RepairAppTestTags.CustomerEmailField,
                        )
                        Field(address, { address = it }, stringResource(R.string.customer_field_address), testTag = RepairAppTestTags.CustomerAddressField)
                        Field(notes, { notes = it }, stringResource(R.string.customer_field_notes), testTag = RepairAppTestTags.CustomerNotesField)
                        ActionRow {
                            TouchButton(
                                text = if (editingCustomerId == null) stringResource(R.string.customer_save) else stringResource(R.string.customer_update),
                                enabled = canSave,
                                testTag = RepairAppTestTags.CustomerSaveButton,
                                onClick = { confirmSave = true },
                            )
                            TouchButton(
                                text = stringResource(R.string.customer_cancel),
                                tone = ButtonTone.NEUTRAL,
                                onClick = {
                                    showForm = false
                                    confirmSave = false
                                    clearForm()
                                },
                            )
                        }
                    }
                }
            }
        }

        item { SectionTitle(if (uiState.customerSearchQuery.isBlank()) stringResource(R.string.customer_recent) else stringResource(R.string.customer_search_results)) }
        if (customers.isEmpty()) {
            item {
                CustomerEmptyState(
                    hasQuery = uiState.customerSearchQuery.isNotBlank(),
                    onNewCustomer = ::openNewCustomerForm,
                )
            }
        } else {
            items(customers) { customer ->
                CustomerCard(
                    customer = customer,
                    onEdit = { editCustomer(customer) },
                )
            }
        }
    }

    if (confirmSave) {
        AlertDialog(
            onDismissRequest = { confirmSave = false },
            title = { Text(if (editingCustomerId == null) stringResource(R.string.customer_confirm_save_title) else stringResource(R.string.customer_confirm_update_title)) },
            text = { Text(if (editingCustomerId == null) stringResource(R.string.customer_confirm_save_text) else stringResource(R.string.customer_confirm_update_text)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmSave = false
                        viewModel.saveCustomer(input)
                        clearForm()
                        showForm = false
                        viewModel.setCustomerSearchQuery("")
                    },
                    modifier = Modifier.testTag(RepairAppTestTags.CustomerConfirmSaveButton),
                ) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmSave = false }) {
                    Text(stringResource(R.string.customer_cancel))
                }
            },
        )
    }
}

@Composable
private fun CustomerCard(customer: CustomerEntity, onEdit: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(DashboardSpacing.cardPadding),
            verticalArrangement = Arrangement.spacedBy(DashboardSpacing.compactGap),
        ) {
            val context = LocalContext.current
            Text(
                customer.displayName(context),
                style = MaterialTheme.typography.titleMedium
            )
            Text(customer.type.label(context))
            listOfNotNull(customer.phone, customer.email).takeIf { it.isNotEmpty() }?.let {
                Text(it.joinToString(" - "))
            }
            TouchButton(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.customer_edit),
                tone = ButtonTone.NEUTRAL,
                onClick = onEdit,
            )
        }
    }
}

@Composable
private fun CustomerEmptyState(hasQuery: Boolean, onNewCustomer: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(DashboardSpacing.cardPadding),
            verticalArrangement = Arrangement.spacedBy(DashboardSpacing.formGap),
        ) {
            Text(
                text = if (hasQuery) stringResource(R.string.customer_no_results) else stringResource(R.string.customer_empty),
                style = MaterialTheme.typography.bodyMedium,
            )
            TouchButton(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.customer_new),
                onClick = onNewCustomer,
            )
        }
    }
}

package com.onlytechtake.repairapp.features.dashboard.customers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.activity.compose.BackHandler
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.core.design.RepairAppTestTags
import com.onlytechtake.repairapp.core.utils.displayName
import com.onlytechtake.repairapp.core.utils.label
import com.onlytechtake.repairapp.data.local.entities.CustomerEntity
import com.onlytechtake.repairapp.data.local.entities.CustomerType
import com.onlytechtake.repairapp.domain.workspace.CustomerInput
import com.onlytechtake.repairapp.features.dashboard.common.ActionRow
import com.onlytechtake.repairapp.features.dashboard.common.CustomerTypeToggle
import com.onlytechtake.repairapp.features.dashboard.common.DashboardSpacing
import com.onlytechtake.repairapp.features.dashboard.common.Field
import com.onlytechtake.repairapp.features.dashboard.common.NoteFieldWithDialog
import com.onlytechtake.repairapp.features.dashboard.common.SectionTitle
import com.onlytechtake.repairapp.features.dashboard.common.StandardCard
import com.onlytechtake.repairapp.features.dashboard.common.TouchButton
import com.onlytechtake.repairapp.features.workspace.ButtonTone
import com.onlytechtake.repairapp.features.workspace.WorkspaceViewModel

@Composable
fun CustomerPanel(viewModel: WorkspaceViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val customers by viewModel.customerSuggestions.collectAsState()
    var showForm by rememberSaveable { mutableStateOf(value = false) }
    var confirmSave by rememberSaveable { mutableStateOf(value = false) }
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

    BackHandler(enabled = showForm) {
        showForm = false
        clearForm()
        viewModel.setFormDirty(false)
    }

    androidx.compose.runtime.LaunchedEffect(
        firstName, lastName, companyName, phone, email, taxCode, vatNumber, address, notes, showForm
    ) {
        val isDirty = showForm && (firstName.isNotBlank() || lastName.isNotBlank() || companyName.isNotBlank() ||
            phone.isNotBlank() || email.isNotBlank() || taxCode.isNotBlank() ||
            vatNumber.isNotBlank() || address.isNotBlank() || notes.isNotBlank())
        viewModel.setFormDirty(isDirty)
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

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = DashboardSpacing.maxContentWidth)
                .imePadding(),
            contentPadding = PaddingValues(DashboardSpacing.screenPadding),
            verticalArrangement = Arrangement.spacedBy(DashboardSpacing.listGap),
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SectionTitle(stringResource(R.string.customer_title))
                    TouchButton(
                        text = stringResource(R.string.customer_new),
                        testTag = RepairAppTestTags.CustomerNewButton,
                        onClick = ::openNewCustomerForm,
                    )
                }
            }

            item {
                StandardCard {
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
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            ),
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Search,
                                capitalization = KeyboardCapitalization.Words
                            ),
                            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                        )
                    }
                }
            }

            if (showForm) {
                item {
                    StandardCard(highlight = true) {
                        Column(
                            modifier = Modifier.padding(DashboardSpacing.cardPadding),
                            verticalArrangement = Arrangement.spacedBy(DashboardSpacing.formGap),
                        ) {
                            Text(
                                text = if (editingCustomerId == null) stringResource(R.string.customer_new) else stringResource(R.string.customer_edit),
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary
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
                                    capitalization = KeyboardCapitalization.Words,
                                    testTag = RepairAppTestTags.CustomerCompanyNameField,
                                )
                                Field(vatNumber, { vatNumber = it }, stringResource(R.string.customer_field_vat_number), showOptional = true, capitalization = KeyboardCapitalization.Characters, testTag = RepairAppTestTags.CustomerVatNumberField)
                            } else {
                                Field(
                                    firstName,
                                    { firstName = it },
                                    stringResource(R.string.customer_field_first_name),
                                    required = true,
                                    showError = firstName.isBlank(),
                                    singleLine = true,
                                    capitalization = KeyboardCapitalization.Words,
                                    testTag = RepairAppTestTags.CustomerFirstNameField,
                                )
                                Field(
                                    lastName,
                                    { lastName = it },
                                    stringResource(R.string.customer_field_last_name),
                                    required = true,
                                    showError = lastName.isBlank(),
                                    singleLine = true,
                                    capitalization = KeyboardCapitalization.Words,
                                    testTag = RepairAppTestTags.CustomerLastNameField,
                                )
                            }
                            Field(taxCode, { taxCode = it }, stringResource(R.string.customer_field_tax_code), showOptional = true, capitalization = KeyboardCapitalization.Characters, testTag = RepairAppTestTags.CustomerTaxCodeField)
                            Field(
                                phone,
                                { phone = it },
                                stringResource(R.string.customer_field_phone),
                                required = true,
                                showError = phone.isBlank() && email.isBlank(),
                                singleLine = true,
                                keyboardType = KeyboardType.Phone,
                                testTag = RepairAppTestTags.CustomerPhoneField,
                            )
                            Field(
                                email,
                                { email = it },
                                stringResource(R.string.customer_field_email),
                                required = true,
                                showError = phone.isBlank() && email.isBlank(),
                                singleLine = true,
                                keyboardType = KeyboardType.Email,
                                capitalization = KeyboardCapitalization.None,
                                testTag = RepairAppTestTags.CustomerEmailField,
                            )
                            Field(address, { address = it }, stringResource(R.string.customer_field_address), showOptional = true, testTag = RepairAppTestTags.CustomerAddressField)
                            NoteFieldWithDialog(notes, { notes = it }, testTag = RepairAppTestTags.CustomerNotesField)
                            ActionRow {
                                TouchButton(
                                    text = if (editingCustomerId == null) stringResource(R.string.customer_save) else stringResource(R.string.customer_update),
                                    enabled = canSave,
                                    testTag = RepairAppTestTags.CustomerSaveButton,
                                    onClick = { confirmSave = true }
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
                    ) {
                        editCustomer(customer)
                    }
                }
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
                        viewModel.setFormDirty(false)
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
    StandardCard(onClick = onEdit) {
        Row(
            modifier = Modifier.padding(DashboardSpacing.cardPadding).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                val context = LocalContext.current
                Text(
                    customer.displayName(context),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = listOfNotNull(customer.type.label(context), customer.phone).joinToString(" - "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            androidx.compose.material3.Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun CustomerEmptyState(hasQuery: Boolean, onNewCustomer: () -> Unit) {
    StandardCard {
        Column(
            modifier = Modifier.padding(DashboardSpacing.cardPadding),
            verticalArrangement = Arrangement.spacedBy(DashboardSpacing.formGap),
        ) {
            Text(
                text = if (hasQuery) stringResource(R.string.customer_no_results) else stringResource(R.string.customer_empty),
                style = MaterialTheme.typography.bodyMedium,
            )
            if (hasQuery) {
                TouchButton(
                    text = stringResource(R.string.customer_new),
                    onClick = onNewCustomer
                )
            }
        }
    }
}

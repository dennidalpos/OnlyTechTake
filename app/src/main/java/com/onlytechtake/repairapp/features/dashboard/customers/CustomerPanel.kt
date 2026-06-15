package com.onlytechtake.repairapp.features.dashboard.customers

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.activity.compose.BackHandler
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.core.design.RepairAppTestTags
import com.onlytechtake.repairapp.core.utils.displayName
import com.onlytechtake.repairapp.core.utils.label
import com.onlytechtake.repairapp.data.local.entities.CustomerEntity
import com.onlytechtake.repairapp.data.local.entities.CustomerType
import com.onlytechtake.repairapp.domain.workspace.CustomerInput
import com.onlytechtake.repairapp.features.dashboard.common.*
import com.onlytechtake.repairapp.features.workspace.ButtonTone
import com.onlytechtake.repairapp.navigation.LocalWindowSizeClass

@Composable
fun CustomerPanel(
    viewModel: CustomersViewModel,
    onOrderClick: (String) -> Unit,
) {
    val windowSizeClass = LocalWindowSizeClass.current
    val isWide = windowSizeClass.widthSizeClass >= WindowWidthSizeClass.Medium

    var selectedCustomer by remember { mutableStateOf<CustomerEntity?>(null) }
    var showForm by rememberSaveable { mutableStateOf(value = false) }
    var editingCustomerId by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmSave by rememberSaveable { mutableStateOf(value = false) }
    
    // Form state
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

    if (isWide) {
        Row(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f)) {
                CustomerListContent(
                    viewModel = viewModel,
                    onCustomerClick = { selectedCustomer = it },
                    onNewCustomer = {
                        clearForm()
                        showForm = true
                    }
                )
            }
            VerticalDivider()
            Box(Modifier.weight(1.5f)) {
                if (showForm) {
                    CustomerForm(
                        isEditing = editingCustomerId != null,
                        isBusiness = isBusiness,
                        onBusinessChange = { isBusiness = it },
                        firstName = firstName,
                        onFirstNameChange = { firstName = it },
                        lastName = lastName,
                        onLastNameChange = { lastName = it },
                        companyName = companyName,
                        onCompanyNameChange = { companyName = it },
                        vatNumber = vatNumber,
                        onVatNumberChange = { vatNumber = it },
                        taxCode = taxCode,
                        onTaxCodeChange = { taxCode = it },
                        phone = phone,
                        onPhoneChange = { phone = it },
                        email = email,
                        onEmailChange = { email = it },
                        address = address,
                        onAddressChange = { address = it },
                        notes = notes,
                        onNotesChange = { notes = it },
                        onSave = { confirmSave = true },
                        onCancel = {
                            showForm = false
                            clearForm()
                        }
                    )
                } else if (selectedCustomer != null) {
                    CustomerDetailContent(
                        customer = selectedCustomer!!,
                        viewModel = viewModel,
                        onEdit = {
                            editCustomer(selectedCustomer!!)
                        },
                        onOrderClick = onOrderClick,
                        onClose = { selectedCustomer = null }
                    )
                } else {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Seleziona un cliente per vedere i dettagli", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    } else {
        Box(Modifier.fillMaxSize()) {
            if (showForm) {
                CustomerForm(
                    isEditing = editingCustomerId != null,
                    isBusiness = isBusiness,
                    onBusinessChange = { isBusiness = it },
                    firstName = firstName,
                    onFirstNameChange = { firstName = it },
                    lastName = lastName,
                    onLastNameChange = { lastName = it },
                    companyName = companyName,
                    onCompanyNameChange = { companyName = it },
                    vatNumber = vatNumber,
                    onVatNumberChange = { vatNumber = it },
                    taxCode = taxCode,
                    onTaxCodeChange = { taxCode = it },
                    phone = phone,
                    onPhoneChange = { phone = it },
                    email = email,
                    onEmailChange = { email = it },
                    address = address,
                    onAddressChange = { address = it },
                    notes = notes,
                    onNotesChange = { notes = it },
                    onSave = { confirmSave = true },
                    onCancel = {
                        showForm = false
                        clearForm()
                    }
                )
            } else {
                CustomerListContent(
                    viewModel = viewModel,
                    onCustomerClick = { selectedCustomer = it },
                    onNewCustomer = {
                        clearForm()
                        showForm = true
                    }
                )
            }
        }

        selectedCustomer?.let { customer ->
            CustomerDetailDialog(
                customer = customer,
                viewModel = viewModel,
                onDismiss = { selectedCustomer = null },
                onEdit = {
                    editCustomer(customer)
                    selectedCustomer = null
                },
                onOrderClick = onOrderClick
            )
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
                        viewModel.saveCustomer(input) {
                            clearForm()
                            showForm = false
                        }
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
private fun CustomerListContent(
    viewModel: CustomersViewModel,
    onCustomerClick: (CustomerEntity) -> Unit,
    onNewCustomer: () -> Unit
) {
    val customers by viewModel.customerSuggestions.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val focusManager = LocalFocusManager.current

    LazyColumn(
        modifier = Modifier.fillMaxSize().imePadding(),
        contentPadding = PaddingValues(DashboardSpacing.screenPadding),
        verticalArrangement = Arrangement.spacedBy(DashboardSpacing.listGap)
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
                    onClick = onNewCustomer,
                )
            }
        }

        item {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = searchQuery,
                onValueChange = viewModel::setSearchQuery,
                label = { Text(stringResource(R.string.customer_search_label)) },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Search,
                    capitalization = KeyboardCapitalization.Words
                ),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
            )
        }

        item { SectionTitle(if (searchQuery.isBlank()) stringResource(R.string.customer_recent) else stringResource(R.string.customer_search_results)) }
        
        if (customers.isEmpty()) {
            item {
                CustomerEmptyState(hasQuery = searchQuery.isNotBlank(), onNewCustomer = onNewCustomer)
            }
        } else {
            items(customers) { customer ->
                CustomerCard(customer = customer, onClick = { onCustomerClick(customer) })
            }
        }
    }
}

@Composable
private fun CustomerForm(
    isEditing: Boolean,
    isBusiness: Boolean,
    onBusinessChange: (Boolean) -> Unit,
    firstName: String,
    onFirstNameChange: (String) -> Unit,
    lastName: String,
    onLastNameChange: (String) -> Unit,
    companyName: String,
    onCompanyNameChange: (String) -> Unit,
    vatNumber: String,
    onVatNumberChange: (String) -> Unit,
    taxCode: String,
    onTaxCodeChange: (String) -> Unit,
    phone: String,
    onPhoneChange: (String) -> Unit,
    email: String,
    onEmailChange: (String) -> Unit,
    address: String,
    onAddressChange: (String) -> Unit,
    notes: String,
    onNotesChange: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    val canSave = (phone.isNotBlank() || email.isNotBlank()) &&
        if (isBusiness) companyName.isNotBlank() else firstName.isNotBlank() && lastName.isNotBlank()

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(DashboardSpacing.screenPadding),
        verticalArrangement = Arrangement.spacedBy(DashboardSpacing.formGap)
    ) {
        Text(
            text = if (!isEditing) stringResource(R.string.customer_new) else stringResource(R.string.customer_edit),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary
        )
        CustomerTypeToggle(isBusiness = isBusiness, onBusinessChange = onBusinessChange)
        if (isBusiness) {
            Field(companyName, onCompanyNameChange, stringResource(R.string.customer_field_company_name), required = true)
            Field(vatNumber, onVatNumberChange, stringResource(R.string.customer_field_vat_number), showOptional = true)
        } else {
            Field(firstName, onFirstNameChange, stringResource(R.string.customer_field_first_name), required = true)
            Field(lastName, onLastNameChange, stringResource(R.string.customer_field_last_name), required = true)
        }
        Field(taxCode, onTaxCodeChange, stringResource(R.string.customer_field_tax_code), showOptional = true)
        Field(phone, onPhoneChange, stringResource(R.string.customer_field_phone), required = true, keyboardType = KeyboardType.Phone)
        Field(email, onEmailChange, stringResource(R.string.customer_field_email), required = true, keyboardType = KeyboardType.Email)
        Field(address, onAddressChange, stringResource(R.string.customer_field_address), showOptional = true)
        NoteFieldWithDialog(notes, onNotesChange)
        
        ActionRow {
            TouchButton(
                text = stringResource(R.string.customer_save),
                enabled = canSave,
                testTag = RepairAppTestTags.CustomerSaveButton,
                onClick = onSave
            )
            TouchButton(text = stringResource(R.string.customer_cancel), tone = ButtonTone.NEUTRAL, onClick = onCancel)
        }
    }
}

@Composable
private fun CustomerDetailContent(
    customer: CustomerEntity,
    viewModel: CustomersViewModel,
    onEdit: () -> Unit,
    onOrderClick: (String) -> Unit,
    onClose: () -> Unit
) {
    val openOrders by viewModel.observeOrdersForCustomer(customer.id, openOnly = true).collectAsState(emptyList())
    val allOrders by viewModel.observeOrdersForCustomer(customer.id, openOnly = false).collectAsState(emptyList())
    val closedOrders = allOrders.filter { it.order.status == com.onlytechtake.repairapp.data.local.entities.OrderStatus.CLOSED }
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(DashboardSpacing.screenPadding),
        verticalArrangement = Arrangement.spacedBy(DashboardSpacing.listGap)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(text = customer.displayName(context), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(text = customer.type.label(context), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                }
                Row {
                    TouchButton(text = "Chiudi", tone = ButtonTone.NEUTRAL, onClick = onClose)
                    Spacer(Modifier.width(8.dp))
                    TouchButton(text = "Modifica", onClick = onEdit)
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                customer.phone?.let { Text("Tel: $it", style = MaterialTheme.typography.bodyLarge) }
                customer.email?.let { Text("Email: $it", style = MaterialTheme.typography.bodyLarge) }
                customer.address?.let { Text("Indirizzo: $it", style = MaterialTheme.typography.bodyMedium) }
            }
        }
        if (!customer.notes.isNullOrBlank()) {
            item { OperatorText(customer.notes) }
        }
        item { SectionTitle("Commesse Aperte (${openOrders.size})") }
        if (openOrders.isEmpty()) {
            item { Text("Nessuna commessa aperta", color = MaterialTheme.colorScheme.outline) }
        } else {
            items(openOrders) { item -> OrderSummaryRow(item, onOrderClick) }
        }
        item { SectionTitle("Storico Chiuse (${closedOrders.size})") }
        if (closedOrders.isEmpty()) {
            item { Text("Nessuna commessa chiusa", color = MaterialTheme.colorScheme.outline) }
        } else {
            items(closedOrders) { item -> OrderSummaryRow(item, onOrderClick) }
        }
    }
}

@Composable
fun CustomerDetailDialog(
    customer: CustomerEntity,
    viewModel: CustomersViewModel,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onOrderClick: (String) -> Unit,
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.95f).widthIn(max = 600.dp),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            CustomerDetailContent(customer, viewModel, onEdit, onOrderClick, onDismiss)
        }
    }
}

@Composable
fun OrderSummaryRow(item: com.onlytechtake.repairapp.domain.workspace.OrderListItem, onOrderClick: (String) -> Unit) {
    StandardCard(onClick = { onOrderClick(item.order.id) }) {
        Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(text = "${item.order.orderNumber}: ${item.order.title}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Text(text = item.order.status.label(LocalContext.current), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
            Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun CustomerCard(customer: CustomerEntity, onClick: () -> Unit) {
    StandardCard(onClick = onClick) {
        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                val context = LocalContext.current
                Text(customer.displayName(context), style = MaterialTheme.typography.titleMedium)
                Text(text = listOfNotNull(customer.type.label(context), customer.phone).joinToString(" - "), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun CustomerEmptyState(hasQuery: Boolean, onNewCustomer: () -> Unit) {
    StandardCard {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text = if (hasQuery) "Nessun risultato" else "Nessun cliente recente")
            if (hasQuery) TouchButton(text = "Nuovo Cliente", onClick = onNewCustomer)
        }
    }
}

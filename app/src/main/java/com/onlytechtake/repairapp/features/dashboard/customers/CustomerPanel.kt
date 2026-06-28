package com.onlytechtake.repairapp.features.dashboard.customers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.core.design.RepairAppIconSizes
import com.onlytechtake.repairapp.core.design.RepairAppShapes
import com.onlytechtake.repairapp.core.design.RepairAppSpacing
import com.onlytechtake.repairapp.core.design.RepairAppTestTags
import com.onlytechtake.repairapp.core.design.components.AppButton
import com.onlytechtake.repairapp.core.design.components.AppButtonSize
import com.onlytechtake.repairapp.core.design.components.AppButtonVariant
import com.onlytechtake.repairapp.core.design.components.AppDialog
import com.onlytechtake.repairapp.core.design.components.AppIconButton
import com.onlytechtake.repairapp.core.design.components.AppScreenContainer
import com.onlytechtake.repairapp.core.design.components.AppStatusBadge
import com.onlytechtake.repairapp.core.design.components.AppTextField
import com.onlytechtake.repairapp.core.design.components.AppVerticalDivider
import com.onlytechtake.repairapp.core.design.components.BannerType
import com.onlytechtake.repairapp.core.design.components.DismissibleAppMessageBanner
import com.onlytechtake.repairapp.core.design.components.StandardCard
import com.onlytechtake.repairapp.core.design.repairColors
import com.onlytechtake.repairapp.core.utils.displayName
import com.onlytechtake.repairapp.core.utils.labelRes
import com.onlytechtake.repairapp.data.local.entities.CustomerEntity
import com.onlytechtake.repairapp.data.local.entities.CustomerType
import com.onlytechtake.repairapp.data.local.entities.OrderStatus
import com.onlytechtake.repairapp.domain.workspace.CustomerInput
import com.onlytechtake.repairapp.features.dashboard.common.AppEmptyState
import com.onlytechtake.repairapp.features.dashboard.common.CustomerTypeToggle
import com.onlytechtake.repairapp.features.dashboard.common.NoteFieldWithDialog
import com.onlytechtake.repairapp.features.dashboard.common.OperatorText
import com.onlytechtake.repairapp.features.dashboard.common.SectionTitle
import com.onlytechtake.repairapp.navigation.LocalWindowSizeClass
import kotlinx.coroutines.launch

@Composable
fun CustomerPanel(
    viewModel: CustomersViewModel,
    sharedViewModel: com.onlytechtake.repairapp.features.workspace.SharedWorkspaceViewModel,
    onOrderClick: (String) -> Unit,
) {
    val windowSizeClass = LocalWindowSizeClass.current
    val isWide = windowSizeClass.widthSizeClass >= WindowWidthSizeClass.Medium
    val message by viewModel.message.collectAsState()

    var selectedCustomer by remember { mutableStateOf<CustomerEntity?>(null) }
    var showForm by rememberSaveable { mutableStateOf(value = false) }
    var editingCustomerId by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmSave by rememberSaveable { mutableStateOf(value = false) }

    // Form state
    var isBusiness by rememberSaveable { mutableStateOf(value = false) }
    var firstName by rememberSaveable { mutableStateOf("") }
    var lastName by rememberSaveable { mutableStateOf("") }
    var companyName by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var taxCode by rememberSaveable { mutableStateOf("") }
    var vatNumber by rememberSaveable { mutableStateOf("") }
    var address by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }

    val input =
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
        viewModel.setFormDirty(dirty = false)
    }

    val resetSignal by sharedViewModel.resetSignal.collectAsState()
    LaunchedEffect(resetSignal) {
        if (resetSignal > 0) {
            clearForm()
            showForm = false
            selectedCustomer = null
        }
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

    Column(Modifier.fillMaxSize()) {
        message?.let {
            DismissibleAppMessageBanner(
                message = it.text,
                type = if (it.isError) BannerType.Error else BannerType.Info,
                onDismiss = viewModel::clearMessage,
                modifier = Modifier.padding(RepairAppSpacing.compactGap),
            )
        }

        Box(Modifier.weight(1f)) {
            if (isWide) {
                Row(Modifier.fillMaxSize()) {
                    Box(Modifier.weight(1f)) {
                        CustomerListContent(
                            viewModel = viewModel,
                            onCustomerClick = { selectedCustomer = it },
                        ) {
                            clearForm()
                            showForm = true
                        }
                    }
                    AppVerticalDivider()
                    Box(Modifier.weight(1.5f)) {
                        if (showForm) {
                            CustomerForm(
                                isEditing = editingCustomerId != null,
                                isBusiness = isBusiness,
                                onBusinessChange = {
                                    isBusiness = it
                                    viewModel.setFormDirty(dirty = true)
                                },
                                firstName = firstName,
                                onFirstNameChange = {
                                    firstName = it
                                    viewModel.setFormDirty(dirty = true)
                                },
                                lastName = lastName,
                                onLastNameChange = {
                                    lastName = it
                                    viewModel.setFormDirty(dirty = true)
                                },
                                companyName = companyName,
                                onCompanyNameChange = {
                                    companyName = it
                                    viewModel.setFormDirty(dirty = true)
                                },
                                vatNumber = vatNumber,
                                onVatNumberChange = {
                                    vatNumber = it
                                    viewModel.setFormDirty(dirty = true)
                                },
                                taxCode = taxCode,
                                onTaxCodeChange = {
                                    taxCode = it
                                    viewModel.setFormDirty(dirty = true)
                                },
                                phone = phone,
                                onPhoneChange = {
                                    phone = it
                                    viewModel.setFormDirty(dirty = true)
                                },
                                email = email,
                                onEmailChange = {
                                    email = it
                                    viewModel.setFormDirty(dirty = true)
                                },
                                address = address,
                                onAddressChange = {
                                    address = it
                                    viewModel.setFormDirty(dirty = true)
                                },
                                notes = notes,
                                onNotesChange = {
                                    notes = it
                                    viewModel.setFormDirty(dirty = true)
                                },
                                onSave = { confirmSave = true },
                            ) {
                                showForm = false
                                clearForm()
                            }
                        } else if (selectedCustomer != null) {
                            CustomerDetailContent(
                                customer = selectedCustomer!!,
                                viewModel = viewModel,
                                onEdit = {
                                    editCustomer(selectedCustomer!!)
                                },
                                onOrderClick = onOrderClick,
                            )
                        } else {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                AppEmptyState(
                                    title = stringResource(R.string.customer_select_detail_title),
                                    body = stringResource(R.string.customer_select_detail_hint),
                                )
                            }
                        }
                    }
                }
            } else {
                if (showForm) {
                    CustomerForm(
                        isEditing = editingCustomerId != null,
                        isBusiness = isBusiness,
                        onBusinessChange = {
                            isBusiness = it
                            viewModel.setFormDirty(dirty = true)
                        },
                        firstName = firstName,
                        onFirstNameChange = {
                            firstName = it
                            viewModel.setFormDirty(dirty = true)
                        },
                        lastName = lastName,
                        onLastNameChange = {
                            lastName = it
                            viewModel.setFormDirty(dirty = true)
                        },
                        companyName = companyName,
                        onCompanyNameChange = {
                            companyName = it
                            viewModel.setFormDirty(dirty = true)
                        },
                        vatNumber = vatNumber,
                        onVatNumberChange = {
                            vatNumber = it
                            viewModel.setFormDirty(dirty = true)
                        },
                        taxCode = taxCode,
                        onTaxCodeChange = {
                            taxCode = it
                            viewModel.setFormDirty(dirty = true)
                        },
                        phone = phone,
                        onPhoneChange = {
                            phone = it
                            viewModel.setFormDirty(dirty = true)
                        },
                        email = email,
                        onEmailChange = {
                            email = it
                            viewModel.setFormDirty(dirty = true)
                        },
                        address = address,
                        onAddressChange = {
                            address = it
                            viewModel.setFormDirty(dirty = true)
                        },
                        notes = notes,
                        onNotesChange = {
                            notes = it
                            viewModel.setFormDirty(dirty = true)
                        },
                        onSave = { confirmSave = true },
                    ) {
                        showForm = false
                        clearForm()
                    }
                } else {
                    CustomerListContent(
                        viewModel = viewModel,
                        onCustomerClick = { selectedCustomer = it },
                    ) {
                        clearForm()
                        showForm = true
                    }
                }
            }
        }

        if (!isWide) {
            selectedCustomer?.let { customer ->
                AppDialog(
                    onDismissRequest = { selectedCustomer = null },
                    confirmButton = {
                        AppButton(
                            text = stringResource(R.string.customer_detail_edit),
                            onClick = {
                                editCustomer(customer)
                                selectedCustomer = null
                            },
                            variant = AppButtonVariant.Primary,
                            size = AppButtonSize.Compact,
                        )
                    },
                    dismissButton = {
                        AppButton(
                            text = stringResource(R.string.action_close),
                            onClick = { selectedCustomer = null },
                            variant = AppButtonVariant.Secondary,
                            size = AppButtonSize.Compact,
                        )
                    },
                    content = {
                        CustomerDetailContent(
                            customer = customer,
                            viewModel = viewModel,
                            onEdit = {}, // Handled by dialog button
                            onOrderClick = onOrderClick,
                        )
                    },
                )
            }
        }
    }

    if (confirmSave) {
        AppDialog(
            onDismissRequest = { confirmSave = false },
            title =
                if (editingCustomerId == null) {
                    stringResource(R.string.customer_confirm_save_title)
                } else {
                    stringResource(
                        R.string.customer_confirm_update_title,
                    )
                },
            text =
                if (editingCustomerId == null) {
                    stringResource(R.string.customer_confirm_save_text)
                } else {
                    stringResource(
                        R.string.customer_confirm_update_text,
                    )
                },
            confirmButton = {
                AppButton(
                    text = stringResource(R.string.action_confirm),
                    onClick = {
                        confirmSave = false
                        viewModel.saveCustomer(input) {
                            clearForm()
                            showForm = false
                        }
                    },
                    variant = AppButtonVariant.Primary,
                    size = AppButtonSize.Compact,
                    testTag = RepairAppTestTags.CustomerConfirmSaveButton,
                )
            },
            dismissButton = {
                AppButton(
                    text = stringResource(R.string.customer_cancel),
                    onClick = { confirmSave = false },
                    variant = AppButtonVariant.Secondary,
                    size = AppButtonSize.Compact,
                )
            },
        )
    }
}

@Composable
private fun CustomerListContent(
    viewModel: CustomersViewModel,
    onCustomerClick: (CustomerEntity) -> Unit,
    onNewCustomer: () -> Unit,
) {
    val customers by viewModel.customerSuggestions.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val showAll by viewModel.showAll.collectAsState()
    val focusManager = LocalFocusManager.current
    val listState = rememberLazyListState()
    val showScrollToTop by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 3 }
    }
    val scope = rememberCoroutineScope()

    AppScreenContainer(scrollable = false) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(RepairAppSpacing.compactGap),
        ) {
            AppTextField(
                modifier =
                    Modifier
                        .weight(1f)
                        .height(56.dp),
                value = searchQuery,
                onValueChange = {
                    viewModel.setSearchQuery(it)
                    if (it.isNotBlank()) viewModel.setShowAll(show = false)
                },
                label = stringResource(R.string.label_search_customer),
                singleLine = true,
                imeAction = ImeAction.Search,
                keyboardType = KeyboardType.Text,
                capitalization = KeyboardCapitalization.Words,
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                testTag = RepairAppTestTags.CustomerSearchField,
            )
            Surface(
                onClick = onNewCustomer,
                color = MaterialTheme.colorScheme.primary,
                shape = RepairAppShapes.button,
                modifier =
                    Modifier
                        .height(56.dp)
                        .width(56.dp)
                        .testTag(RepairAppTestTags.CustomerNewButton),
                tonalElevation = 0.dp,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.customer_new),
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(RepairAppIconSizes.medium),
                    )
                }
            }
        }

        Spacer(Modifier.height(RepairAppSpacing.compactGap))

        if (searchQuery.isBlank()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text =
                        if (showAll) {
                            stringResource(R.string.label_customers)
                        } else {
                            stringResource(
                                R.string.customer_recent,
                            )
                        },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                AppButton(
                    text =
                        if (showAll) {
                            stringResource(R.string.customer_recent)
                        } else {
                            stringResource(
                                R.string.customer_show_all,
                            )
                        },
                    onClick = { viewModel.setShowAll(!showAll) },
                    variant = AppButtonVariant.Ghost,
                    size = AppButtonSize.Compact,
                )
            }
        } else {
            SectionTitle(stringResource(R.string.customer_search_results))
        }

        Box(modifier = Modifier.weight(1f)) {
            LazyColumn(
                state = listState,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .testTag("customer_list")
                        .imePadding(),
                verticalArrangement = Arrangement.spacedBy(RepairAppSpacing.cardGap),
                contentPadding =
                    PaddingValues(
                        top = RepairAppSpacing.compactGap,
                        bottom = RepairAppSpacing.bottomContentPadding,
                    ),
            ) {
                if (customers.isEmpty()) {
                    item {
                        CustomerEmptyState(hasQuery = searchQuery.isNotBlank())
                    }
                } else {
                    items(customers, key = { it.id }) { customer ->
                        CustomerCard(customer = customer) { onCustomerClick(customer) }
                    }
                }
            }

            if (showScrollToTop) {
                AppButton(
                    text = stringResource(R.string.customer_scroll_to_top),
                    leadingIcon = Icons.Default.KeyboardArrowUp,
                    modifier =
                        Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = RepairAppSpacing.screenVertical)
                            .testTag(RepairAppTestTags.ScrollToTopButton),
                    onClick = {
                        scope.launch { listState.animateScrollToItem(0) }
                    },
                    variant = AppButtonVariant.Secondary,
                    size = AppButtonSize.Compact,
                )
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
    val canSave =
        (phone.isNotBlank() || email.isNotBlank()) &&
            if (isBusiness) companyName.isNotBlank() else firstName.isNotBlank() && lastName.isNotBlank()

    AppScreenContainer(modifier = Modifier.testTag("customer_form")) {
        Text(
            text = if (!isEditing) stringResource(R.string.customer_new) else stringResource(R.string.customer_edit),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(Modifier.height(RepairAppSpacing.sectionGap))

        CustomerTypeToggle(isBusiness = isBusiness, onBusinessChange = onBusinessChange)

        Spacer(Modifier.height(RepairAppSpacing.sectionGap))

        Column(verticalArrangement = Arrangement.spacedBy(RepairAppSpacing.fieldGap)) {
            if (isBusiness) {
                AppTextField(
                    companyName,
                    onCompanyNameChange,
                    stringResource(R.string.customer_field_company_name),
                    required = true,
                    testTag = RepairAppTestTags.CustomerCompanyNameField,
                )
                AppTextField(
                    vatNumber,
                    onVatNumberChange,
                    stringResource(R.string.customer_field_vat_number),
                    showOptional = true,
                    testTag = RepairAppTestTags.CustomerVatNumberField,
                )
            } else {
                AppTextField(
                    firstName,
                    onFirstNameChange,
                    stringResource(R.string.customer_field_first_name),
                    required = true,
                    testTag = RepairAppTestTags.CustomerFirstNameField,
                )
                AppTextField(
                    lastName,
                    onLastNameChange,
                    stringResource(R.string.customer_field_last_name),
                    required = true,
                    testTag = RepairAppTestTags.CustomerLastNameField,
                )
            }
            AppTextField(
                taxCode,
                onTaxCodeChange,
                stringResource(R.string.customer_field_tax_code),
                showOptional = true,
                testTag = RepairAppTestTags.CustomerTaxCodeField,
            )
            AppTextField(
                phone,
                onPhoneChange,
                stringResource(R.string.customer_field_phone),
                required = true,
                keyboardType = KeyboardType.Phone,
                testTag = RepairAppTestTags.CustomerPhoneField,
            )
            AppTextField(
                email,
                onEmailChange,
                stringResource(R.string.customer_field_email),
                required = true,
                keyboardType = KeyboardType.Email,
                testTag = RepairAppTestTags.CustomerEmailField,
            )
            AppTextField(
                address,
                onAddressChange,
                stringResource(R.string.customer_field_address),
                showOptional = true,
                testTag = RepairAppTestTags.CustomerAddressField,
            )

            Spacer(Modifier.height(RepairAppSpacing.compactGap))
            SectionTitle(stringResource(R.string.customer_field_notes))
            NoteFieldWithDialog(
                notes,
                onNotesChange,
                testTag = RepairAppTestTags.CustomerNotesField,
            )
        }

        Spacer(Modifier.height(RepairAppSpacing.sectionGap))

        AppButton(
            text = if (isEditing) stringResource(R.string.customer_update) else stringResource(R.string.customer_save),
            onClick = onSave,
            enabled = canSave,
            variant = AppButtonVariant.Primary,
            size = AppButtonSize.FullWidth,
            testTag = RepairAppTestTags.CustomerSaveButton,
        )
        Spacer(Modifier.height(RepairAppSpacing.cardGap))
        AppButton(
            text = stringResource(R.string.customer_cancel),
            onClick = onCancel,
            variant = AppButtonVariant.Ghost,
            size = AppButtonSize.FullWidth,
        )
    }
}

@Composable
private fun CustomerDetailContent(
    customer: CustomerEntity,
    viewModel: CustomersViewModel,
    onEdit: () -> Unit,
    onOrderClick: (String) -> Unit,
) {
    val openOrders by viewModel
        .observeOrdersForCustomer(customer.id, openOnly = true)
        .collectAsState(emptyList())
    val allOrders by viewModel
        .observeOrdersForCustomer(customer.id, openOnly = false)
        .collectAsState(emptyList())
    val closedOrders =
        allOrders.filter { it.order.status == OrderStatus.CLOSED }
    val context = LocalContext.current

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(RepairAppSpacing.cardGap),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = customer.displayName(context),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                AppStatusBadge(
                    text = stringResource(customer.type.labelRes),
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
            AppIconButton(
                imageVector = Icons.Default.Edit,
                contentDescription = stringResource(R.string.customer_detail_edit),
                onClick = onEdit,
                tint = MaterialTheme.colorScheme.primary,
            )
        }

        StandardCard {
            Column(
                modifier = Modifier.padding(RepairAppSpacing.cardPadding),
                verticalArrangement = Arrangement.spacedBy(RepairAppSpacing.compactGap),
            ) {
                customer.phone?.let {
                    AppKeyValueRow(
                        label = stringResource(R.string.customer_field_phone),
                        value = it,
                    )
                }
                customer.email?.let {
                    AppKeyValueRow(
                        label = stringResource(R.string.customer_field_email),
                        value = it,
                    )
                }
                customer.address?.let {
                    AppKeyValueRow(
                        label = stringResource(R.string.customer_field_address),
                        value = it,
                    )
                }
            }
        }

        if (!customer.notes.isNullOrBlank()) {
            SectionTitle(stringResource(R.string.customer_field_notes))
            OperatorText(customer.notes)
        }

        SectionTitle(stringResource(R.string.customer_open_orders, openOrders.size))
        if (openOrders.isEmpty()) {
            Text(
                stringResource(R.string.customer_no_open_orders),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline,
            )
        } else {
            openOrders.forEach { item -> OrderSummaryRow(item, onOrderClick) }
        }

        SectionTitle(stringResource(R.string.customer_closed_orders, closedOrders.size))
        if (closedOrders.isEmpty()) {
            Text(
                stringResource(R.string.customer_no_closed_orders),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline,
            )
        } else {
            closedOrders.forEach { item -> OrderSummaryRow(item, onOrderClick) }
        }
    }
}

@Composable
fun AppKeyValueRow(
    label: String,
    value: String,
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
fun OrderSummaryRow(
    item: com.onlytechtake.repairapp.domain.workspace.OrderListItem,
    onOrderClick: (String) -> Unit,
) {
    StandardCard(
        onClick = { onOrderClick(item.order.id) },
        modifier = Modifier.padding(bottom = RepairAppSpacing.tinyGap),
    ) {
        Row(
            modifier =
                Modifier
                    .padding(RepairAppSpacing.compactGap)
                    .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(RepairAppSpacing.tinyGap)) {
                Text(
                    text = "${item.order.orderNumber}: ${item.order.title}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                val status = item.order.status
                val containerColor =
                    when (status) {
                        OrderStatus.CLOSED -> MaterialTheme.colorScheme.errorContainer
                        OrderStatus.OPEN -> MaterialTheme.colorScheme.secondaryContainer
                        else -> MaterialTheme.repairColors.warningContainer
                    }
                val contentColor =
                    when (status) {
                        OrderStatus.CLOSED -> MaterialTheme.colorScheme.onErrorContainer
                        OrderStatus.OPEN -> MaterialTheme.colorScheme.onSecondaryContainer
                        else -> MaterialTheme.repairColors.onWarningContainer
                    }

                AppStatusBadge(
                    text = stringResource(status.labelRes),
                    containerColor = containerColor,
                    contentColor = contentColor,
                )
            }
            Icon(
                Icons.Default.Info,
                contentDescription = null,
                modifier = Modifier.size(RepairAppIconSizes.small),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
            )
        }
    }
}

@Composable
private fun CustomerCard(
    customer: CustomerEntity,
    onClick: () -> Unit,
) {
    StandardCard(
        modifier = Modifier.testTag("customer_card_${customer.id}"),
        onClick = onClick,
    ) {
        Row(
            modifier =
                Modifier
                    .padding(RepairAppSpacing.cardPadding)
                    .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                val context = LocalContext.current
                Text(
                    customer.displayName(context),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text =
                        listOfNotNull(
                            stringResource(customer.type.labelRes),
                            customer.phone,
                        ).joinToString(" - "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
            )
        }
    }
}

@Composable
private fun CustomerEmptyState(hasQuery: Boolean) {
    AppEmptyState(
        title =
            if (hasQuery) {
                stringResource(R.string.customer_no_results_title)
            } else {
                stringResource(
                    R.string.customer_search_empty_title,
                )
            },
        body = if (hasQuery) stringResource(R.string.customer_no_results) else stringResource(R.string.customer_search_hint),
    )
}

@Preview(showBackground = true, name = "Light Mode")
@Preview(
    showBackground = true,
    name = "Dark Mode",
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun CustomerPanelPreview() {
    MaterialTheme {
        CustomerPanel(
            viewModel =
                androidx.lifecycle.viewmodel.compose
                    .viewModel(),
            sharedViewModel =
                androidx.lifecycle.viewmodel.compose
                    .viewModel(),
        ) {}
    }
}

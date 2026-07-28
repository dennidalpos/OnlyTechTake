package com.onlytechtake.repairapp.features.dashboard.customers

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.core.design.RepairAppSpacing
import com.onlytechtake.repairapp.core.design.RepairAppTestTags
import com.onlytechtake.repairapp.core.design.RepairAppTheme
import com.onlytechtake.repairapp.core.design.components.AppButton
import com.onlytechtake.repairapp.core.design.components.AppButtonSize
import com.onlytechtake.repairapp.core.design.components.AppButtonVariant
import com.onlytechtake.repairapp.core.design.components.AppDialog
import com.onlytechtake.repairapp.core.design.components.AppVerticalDivider
import com.onlytechtake.repairapp.core.design.components.BannerType
import com.onlytechtake.repairapp.core.design.components.DismissibleAppMessageBanner
import com.onlytechtake.repairapp.core.utils.displayName
import com.onlytechtake.repairapp.features.dashboard.common.AppEmptyState
import com.onlytechtake.repairapp.navigation.LocalWindowSizeClass

@Composable
fun CustomerPanel(
    viewModel: CustomersViewModel,
    onOrderClick: (String) -> Unit,
) {
    val windowSizeClass = LocalWindowSizeClass.current
    val isWide = windowSizeClass.widthSizeClass >= WindowWidthSizeClass.Medium
    val message by viewModel.message.collectAsState()
    val state by viewModel.viewState.collectAsState()

    var confirmSave by rememberSaveable { mutableStateOf(value = false) }
    var confirmDelete by rememberSaveable { mutableStateOf(value = false) }

    Column(Modifier.fillMaxSize()) {
        message?.let {
            DismissibleAppMessageBanner(
                message = it.text,
                type = if (it.isError) BannerType.Error else BannerType.Success,
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
                            onCustomerClick = viewModel::onSelectedCustomerChange,
                            onNewCustomer = { viewModel.onShowFormChange(true) },
                        )
                    }
                    AppVerticalDivider()
                    Box(Modifier.weight(1.5f)) {
                        if (state.showForm) {
                            CustomerFormContent(
                                state = state,
                                onBusinessChange = viewModel::onBusinessChange,
                                onFirstNameChange = viewModel::onFirstNameChange,
                                onLastNameChange = viewModel::onLastNameChange,
                                onCompanyNameChange = viewModel::onCompanyNameChange,
                                onVatNumberChange = viewModel::onVatNumberChange,
                                onTaxCodeChange = viewModel::onTaxCodeChange,
                                onPhoneChange = viewModel::onPhoneChange,
                                onEmailChange = viewModel::onEmailChange,
                                onAddressChange = viewModel::onAddressChange,
                                onNotesChange = viewModel::onNotesChange,
                                onSave = { confirmSave = true },
                                onCancel = { viewModel.onShowFormChange(false) },
                            )
                        } else if (state.selectedCustomer != null) {
                            CustomerDetailContent(
                                customer = state.selectedCustomer!!,
                                viewModel = viewModel,
                                onEdit = { viewModel.editCustomer(state.selectedCustomer!!) },
                                onDelete = { confirmDelete = true },
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
                if (state.showForm) {
                    CustomerFormContent(
                        state = state,
                        onBusinessChange = viewModel::onBusinessChange,
                        onFirstNameChange = viewModel::onFirstNameChange,
                        onLastNameChange = viewModel::onLastNameChange,
                        onCompanyNameChange = viewModel::onCompanyNameChange,
                        onVatNumberChange = viewModel::onVatNumberChange,
                        onTaxCodeChange = viewModel::onTaxCodeChange,
                        onPhoneChange = viewModel::onPhoneChange,
                        onEmailChange = viewModel::onEmailChange,
                        onAddressChange = viewModel::onAddressChange,
                        onNotesChange = viewModel::onNotesChange,
                        onSave = { confirmSave = true },
                        onCancel = { viewModel.onShowFormChange(false) },
                    )
                } else {
                    CustomerListContent(
                        viewModel = viewModel,
                        onCustomerClick = viewModel::onSelectedCustomerChange,
                        onNewCustomer = { viewModel.onShowFormChange(true) },
                    )
                }
            }
        }

        if (!isWide) {
            state.selectedCustomer?.let { customer ->
                AppDialog(
                    onDismissRequest = { viewModel.onSelectedCustomerChange(null) },
                    confirmButton = {
                        AppButton(
                            text = stringResource(R.string.customer_detail_edit),
                            onClick = {
                                viewModel.editCustomer(customer)
                                viewModel.onSelectedCustomerChange(null)
                            },
                            variant = AppButtonVariant.Primary,
                            size = AppButtonSize.Compact,
                        )
                    },
                    dismissButton = {
                        AppButton(
                            text = stringResource(R.string.action_close),
                            onClick = { viewModel.onSelectedCustomerChange(null) },
                            variant = AppButtonVariant.Secondary,
                            size = AppButtonSize.Compact,
                        )
                    },
                    content = {
                        CustomerDetailContent(
                            customer = customer,
                            viewModel = viewModel,
                            onEdit = {},
                            onDelete = {
                                confirmDelete = true
                                viewModel.onSelectedCustomerChange(null)
                            },
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
            title = if (!state.isEditing) stringResource(R.string.customer_confirm_save_title) else stringResource(R.string.customer_confirm_update_title),
            text = if (!state.isEditing) stringResource(R.string.customer_confirm_save_text) else stringResource(R.string.customer_confirm_update_text),
            confirmButton = {
                AppButton(
                    text = stringResource(R.string.action_confirm),
                    onClick = {
                        confirmSave = false
                        viewModel.saveCustomer(state.toInput()) {
                            viewModel.onShowFormChange(false)
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

    if (confirmDelete) {
        val context = LocalContext.current
        AppDialog(
            onDismissRequest = { confirmDelete = false },
            title = stringResource(R.string.action_delete_confirm_title, state.selectedCustomer?.displayName(context).orEmpty()),
            text = stringResource(R.string.action_delete_confirm_text, state.selectedCustomer?.displayName(context).orEmpty()),
            confirmButton = {
                AppButton(
                    text = stringResource(R.string.content_description_delete),
                    onClick = {
                        confirmDelete = false
                        state.selectedCustomer?.id?.let { id ->
                            viewModel.deleteCustomer(id) {
                                viewModel.onSelectedCustomerChange(null)
                            }
                        }
                    },
                    variant = AppButtonVariant.Danger,
                    size = AppButtonSize.Compact,
                    testTag = RepairAppTestTags.GenericConfirmButton,
                )
            },
            dismissButton = {
                AppButton(
                    text = stringResource(R.string.customer_cancel),
                    onClick = { confirmDelete = false },
                    variant = AppButtonVariant.Secondary,
                    size = AppButtonSize.Compact,
                )
            },
        )
    }
}

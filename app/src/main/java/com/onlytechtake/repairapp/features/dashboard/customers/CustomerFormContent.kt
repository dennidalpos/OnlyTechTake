package com.onlytechtake.repairapp.features.dashboard.customers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.core.design.RepairAppSpacing
import com.onlytechtake.repairapp.core.design.RepairAppTestTags
import com.onlytechtake.repairapp.core.design.components.AppButton
import com.onlytechtake.repairapp.core.design.components.AppButtonSize
import com.onlytechtake.repairapp.core.design.components.AppButtonVariant
import com.onlytechtake.repairapp.core.design.components.AppScreenContainer
import com.onlytechtake.repairapp.core.design.components.AppTextField
import com.onlytechtake.repairapp.features.dashboard.common.CustomerTypeToggle
import com.onlytechtake.repairapp.features.dashboard.common.NoteFieldWithDialog
import com.onlytechtake.repairapp.features.dashboard.common.SectionTitle

@Composable
fun CustomerFormContent(
    state: CustomerViewState,
    onBusinessChange: (Boolean) -> Unit,
    onFirstNameChange: (String) -> Unit,
    onLastNameChange: (String) -> Unit,
    onCompanyNameChange: (String) -> Unit,
    onVatNumberChange: (String) -> Unit,
    onTaxCodeChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onAddressChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    AppScreenContainer(modifier = Modifier.testTag("customer_form")) {
        Text(
            text = if (!state.isEditing) stringResource(R.string.customer_new) else stringResource(R.string.customer_edit),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(Modifier.height(RepairAppSpacing.sectionGap))

        CustomerTypeToggle(isBusiness = state.isBusiness, onBusinessChange = onBusinessChange)

        Spacer(Modifier.height(RepairAppSpacing.sectionGap))

        Column(verticalArrangement = Arrangement.spacedBy(RepairAppSpacing.fieldGap)) {
            if (state.isBusiness) {
                AppTextField(
                    state.companyName,
                    onCompanyNameChange,
                    stringResource(R.string.customer_field_company_name),
                    required = true,
                    testTag = RepairAppTestTags.CustomerCompanyNameField,
                )
                AppTextField(
                    state.vatNumber,
                    onVatNumberChange,
                    stringResource(R.string.customer_field_vat_number),
                    showOptional = true,
                    testTag = RepairAppTestTags.CustomerVatNumberField,
                )
            } else {
                AppTextField(
                    state.firstName,
                    onFirstNameChange,
                    stringResource(R.string.customer_field_first_name),
                    required = true,
                    testTag = RepairAppTestTags.CustomerFirstNameField,
                )
                AppTextField(
                    state.lastName,
                    onLastNameChange,
                    stringResource(R.string.customer_field_last_name),
                    required = true,
                    testTag = RepairAppTestTags.CustomerLastNameField,
                )
            }
            AppTextField(
                state.taxCode,
                onTaxCodeChange,
                stringResource(R.string.customer_field_tax_code),
                showOptional = true,
                testTag = RepairAppTestTags.CustomerTaxCodeField,
            )
            AppTextField(
                state.phone,
                onPhoneChange,
                stringResource(R.string.customer_field_phone),
                required = true,
                keyboardType = KeyboardType.Phone,
                testTag = RepairAppTestTags.CustomerPhoneField,
            )
            AppTextField(
                state.email,
                onEmailChange,
                stringResource(R.string.customer_field_email),
                required = true,
                keyboardType = KeyboardType.Email,
                testTag = RepairAppTestTags.CustomerEmailField,
            )
            AppTextField(
                state.address,
                onAddressChange,
                stringResource(R.string.customer_field_address),
                showOptional = true,
                testTag = RepairAppTestTags.CustomerAddressField,
            )

            Spacer(Modifier.height(RepairAppSpacing.compactGap))
            SectionTitle(stringResource(R.string.customer_field_notes))
            NoteFieldWithDialog(
                state.notes,
                onNotesChange,
                testTag = RepairAppTestTags.CustomerNotesField,
            )
        }

        Spacer(Modifier.height(RepairAppSpacing.sectionGap))

        AppButton(
            text = if (state.isEditing) stringResource(R.string.customer_update) else stringResource(R.string.customer_save),
            onClick = onSave,
            enabled = state.isValid(),
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

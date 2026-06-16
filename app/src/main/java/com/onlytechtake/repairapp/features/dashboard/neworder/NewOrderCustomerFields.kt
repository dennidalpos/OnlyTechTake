package com.onlytechtake.repairapp.features.dashboard.neworder

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
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
import com.onlytechtake.repairapp.core.design.components.AppTextField
import com.onlytechtake.repairapp.core.design.components.StandardCard
import com.onlytechtake.repairapp.features.dashboard.common.CustomerTypeToggle
import com.onlytechtake.repairapp.core.design.RepairAppSpacing
import com.onlytechtake.repairapp.features.dashboard.common.NoteFieldWithDialog
import com.onlytechtake.repairapp.features.dashboard.common.OperatorText
import com.onlytechtake.repairapp.core.design.components.AppButton
import com.onlytechtake.repairapp.core.design.components.AppButtonVariant
import com.onlytechtake.repairapp.core.design.components.AppButtonSize

@Composable
fun CustomerSuggestionCard(customer: CustomerEntity, onClick: () -> Unit) {
    val context = LocalContext.current
    var expanded by rememberSaveable(customer.id) { mutableStateOf(value = false) }
    StandardCard(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(RepairAppSpacing.compactGap),
            verticalArrangement = Arrangement.spacedBy(RepairAppSpacing.compactGap),
        ) {
            Text(
                customer.displayName(context),
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = listOfNotNull(customer.type.label(context), customer.phone).joinToString(" - "),
                style = MaterialTheme.typography.bodySmall
            )
            if (expanded) {
                val details = listOfNotNull(
                    customer.email?.let { "${stringResource(R.string.customer_field_email)}: $it" },
                    customer.address?.let { "${stringResource(R.string.customer_field_address)}: $it" },
                    customer.vatNumber?.let { "${stringResource(R.string.customer_field_vat_number)}: $it" },
                    customer.taxCode?.let { "${stringResource(R.string.customer_field_tax_code)}: $it" },
                    customer.notes?.let { "${stringResource(R.string.customer_field_notes)}: $it" },
                ).joinToString("\n")
                if (details.isNotBlank()) OperatorText(details)
            }
            AppButton(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.customer_select),
                variant = if (expanded) AppButtonVariant.Success else AppButtonVariant.Secondary,
                size = AppButtonSize.Compact,
                onClick = {
                    if (expanded) onClick() else expanded = true
                },
            )
        }
    }
}

@Composable
fun IntakeCustomerFields(
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
    nextAction: KeyboardActions,
    doneAction: KeyboardActions,
) {
    val contactMissing = phone.isBlank() && email.isBlank()

    Column(verticalArrangement = Arrangement.spacedBy(RepairAppSpacing.formGap)) {
        CustomerTypeToggle(isBusiness = isBusiness, onBusinessChange = onBusinessChange)
        if (isBusiness) {
            AppTextField(companyName, onCompanyNameChange, stringResource(R.string.customer_field_company_name), required = true, showError = companyName.isBlank(), singleLine = true, imeAction = ImeAction.Next, keyboardActions = nextAction, capitalization = KeyboardCapitalization.Words)
            AppTextField(vatNumber, onVatNumberChange, stringResource(R.string.customer_field_vat_number), showOptional = true, singleLine = true, imeAction = ImeAction.Next, keyboardActions = nextAction, capitalization = KeyboardCapitalization.Characters)
        } else {
            AppTextField(firstName, onFirstNameChange, stringResource(R.string.customer_field_first_name), required = true, showError = firstName.isBlank(), singleLine = true, imeAction = ImeAction.Next, keyboardActions = nextAction, capitalization = KeyboardCapitalization.Words, testTag = RepairAppTestTags.NewOrderFirstNameField)
            AppTextField(lastName, onLastNameChange, stringResource(R.string.customer_field_last_name), required = true, showError = lastName.isBlank(), singleLine = true, imeAction = ImeAction.Next, keyboardActions = nextAction, capitalization = KeyboardCapitalization.Words, testTag = RepairAppTestTags.NewOrderLastNameField)
        }
        AppTextField(taxCode, onTaxCodeChange, stringResource(R.string.customer_field_tax_code), showOptional = true, singleLine = true, imeAction = ImeAction.Next, keyboardActions = nextAction, capitalization = KeyboardCapitalization.Characters)
        AppTextField(phone, onPhoneChange, stringResource(R.string.customer_field_phone), required = true, showError = contactMissing, singleLine = true, imeAction = ImeAction.Next, keyboardActions = nextAction, keyboardType = KeyboardType.Phone, testTag = RepairAppTestTags.NewOrderPhoneField)
        AppTextField(email, onEmailChange, stringResource(R.string.customer_field_email), required = true, showError = contactMissing, singleLine = true, imeAction = ImeAction.Next, keyboardActions = nextAction, keyboardType = KeyboardType.Email, capitalization = KeyboardCapitalization.None, testTag = RepairAppTestTags.NewOrderEmailField)
        AppTextField(address, onAddressChange, stringResource(R.string.customer_field_address), showOptional = true, singleLine = true, imeAction = ImeAction.Done, keyboardActions = doneAction, capitalization = KeyboardCapitalization.Words)
        NoteFieldWithDialog(notes, onNotesChange, testTag = RepairAppTestTags.NewOrderNotesField)
    }
}


package com.onlytechtake.repairapp.features.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.core.design.RepairAppTestTags
import com.onlytechtake.repairapp.core.utils.displayName
import com.onlytechtake.repairapp.core.utils.label
import com.onlytechtake.repairapp.data.local.entities.CustomerEntity
import com.onlytechtake.repairapp.features.workspace.ButtonTone

@Composable
fun CustomerSuggestionCard(customer: CustomerEntity, onClick: () -> Unit) {
    val context = LocalContext.current
    var expanded by rememberSaveable(customer.id) { mutableStateOf(false) }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(DashboardSpacing.cardPadding),
            verticalArrangement = Arrangement.spacedBy(DashboardSpacing.compactGap),
        ) {
            Text(
                customer.displayName(context),
                style = MaterialTheme.typography.titleSmall
            )
            Text(listOfNotNull(customer.type.label(context), customer.phone, customer.email).joinToString(" - "))
            if (expanded) {
                val details = listOfNotNull(
                    customer.address?.let { "${stringResource(R.string.customer_field_address)}: $it" },
                    customer.vatNumber?.let { "${stringResource(R.string.customer_field_vat_number)}: $it" },
                    customer.taxCode?.let { "${stringResource(R.string.customer_field_tax_code)}: $it" },
                    customer.notes?.let { "${stringResource(R.string.customer_field_notes)}: $it" },
                ).joinToString("\n")
                if (details.isNotBlank()) OperatorText(details)
            }
            TouchButton(
                modifier = Modifier.fillMaxWidth(),
                text = if (expanded) stringResource(R.string.customer_select) else stringResource(R.string.customer_details),
                tone = if (expanded) ButtonTone.SUCCESS else ButtonTone.NEUTRAL,
                onClick = {
                    if (expanded) onClick() else expanded = true
                },
            )
        }
    }
}

@Composable
fun NewCustomerFields(
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
    canCreate: Boolean,
    onConfirm: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val nextAction = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
    val doneAction = KeyboardActions(
        onDone = {
            keyboardController?.hide()
            focusManager.clearFocus()
        },
    )

    Column(verticalArrangement = Arrangement.spacedBy(DashboardSpacing.formGap)) {
        Text(stringResource(R.string.customer_new), style = MaterialTheme.typography.titleMedium)
        IntakeCustomerFields(
            isBusiness = isBusiness,
            onBusinessChange = onBusinessChange,
            firstName = firstName,
            onFirstNameChange = onFirstNameChange,
            lastName = lastName,
            onLastNameChange = onLastNameChange,
            companyName = companyName,
            onCompanyNameChange = onCompanyNameChange,
            vatNumber = vatNumber,
            onVatNumberChange = onVatNumberChange,
            taxCode = taxCode,
            onTaxCodeChange = onTaxCodeChange,
            phone = phone,
            onPhoneChange = onPhoneChange,
            email = email,
            onEmailChange = onEmailChange,
            address = address,
            onAddressChange = onAddressChange,
            notes = notes,
            onNotesChange = onNotesChange,
            nextAction = nextAction,
            doneAction = doneAction,
        )
        TouchButton(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(R.string.customer_create_and_order),
            enabled = canCreate,
            onClick = onConfirm,
        )
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

    Column(verticalArrangement = Arrangement.spacedBy(DashboardSpacing.formGap)) {
        CustomerTypeToggle(isBusiness = isBusiness, onBusinessChange = onBusinessChange)
        if (isBusiness) {
            Field(companyName, onCompanyNameChange, stringResource(R.string.customer_field_company_name), required = true, showError = companyName.isBlank(), singleLine = true, imeAction = ImeAction.Next, keyboardActions = nextAction)
            Field(vatNumber, onVatNumberChange, stringResource(R.string.customer_field_vat_number), singleLine = true, imeAction = ImeAction.Next, keyboardActions = nextAction)
        } else {
            Field(firstName, onFirstNameChange, stringResource(R.string.customer_field_first_name), required = true, showError = firstName.isBlank(), singleLine = true, imeAction = ImeAction.Next, keyboardActions = nextAction, testTag = RepairAppTestTags.NewOrderFirstNameField)
            Field(lastName, onLastNameChange, stringResource(R.string.customer_field_last_name), required = true, showError = lastName.isBlank(), singleLine = true, imeAction = ImeAction.Next, keyboardActions = nextAction, testTag = RepairAppTestTags.NewOrderLastNameField)
        }
        Field(taxCode, onTaxCodeChange, stringResource(R.string.customer_field_tax_code), singleLine = true, imeAction = ImeAction.Next, keyboardActions = nextAction)
        Field(phone, onPhoneChange, stringResource(R.string.customer_field_phone), required = true, showError = contactMissing, singleLine = true, imeAction = ImeAction.Next, keyboardActions = nextAction, testTag = RepairAppTestTags.NewOrderPhoneField)
        Field(email, onEmailChange, stringResource(R.string.customer_field_email), required = true, showError = contactMissing, singleLine = true, imeAction = ImeAction.Next, keyboardActions = nextAction, testTag = RepairAppTestTags.NewOrderEmailField)
        Field(address, onAddressChange, stringResource(R.string.customer_field_address), singleLine = true, imeAction = ImeAction.Next, keyboardActions = nextAction)
        Field(notes, onNotesChange, stringResource(R.string.customer_field_notes), imeAction = ImeAction.Done, keyboardActions = doneAction)
    }
}

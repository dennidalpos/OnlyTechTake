package com.onlytechtake.repairapp.features.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.onlytechtake.repairapp.data.local.entities.CustomerEntity
import com.onlytechtake.repairapp.features.workspace.label

@Composable
fun CustomerSuggestionCard(customer: CustomerEntity, onClick: () -> Unit) {
    var expanded by rememberSaveable(customer.id) { mutableStateOf(false) }
    androidx.compose.material3.Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(DashboardSpacing.cardPadding),
            verticalArrangement = Arrangement.spacedBy(DashboardSpacing.compactGap),
        ) {
            Text(customer.displayName(), style = MaterialTheme.typography.titleSmall)
            Text(listOfNotNull(customer.type.label(), customer.phone, customer.email).joinToString(" - "))
            if (expanded) {
                val details = listOfNotNull(
                    customer.address?.let { "Indirizzo: $it" },
                    customer.vatNumber?.let { "P.IVA: $it" },
                    customer.taxCode?.let { "Codice fiscale: $it" },
                    customer.notes?.let { "Note: $it" },
                ).joinToString("\n")
                if (details.isNotBlank()) OperatorText(details)
            }
            TouchButton(
                modifier = Modifier.fillMaxWidth(),
                text = if (expanded) "Seleziona cliente" else "Dettagli",
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
    val contactMissing = phone.isBlank() && email.isBlank()

    Column(verticalArrangement = Arrangement.spacedBy(DashboardSpacing.formGap)) {
        Text("Nuovo cliente", style = MaterialTheme.typography.titleMedium)
        CustomerTypeToggle(isBusiness = isBusiness, onBusinessChange = onBusinessChange)
        if (isBusiness) {
            Field(companyName, onCompanyNameChange, "Ragione sociale", required = true, showError = companyName.isBlank(), singleLine = true, imeAction = ImeAction.Next, keyboardActions = nextAction)
            Field(vatNumber, onVatNumberChange, "P.IVA", singleLine = true, imeAction = ImeAction.Next, keyboardActions = nextAction)
        } else {
            Field(firstName, onFirstNameChange, "Nome", required = true, showError = firstName.isBlank(), singleLine = true, imeAction = ImeAction.Next, keyboardActions = nextAction)
            Field(lastName, onLastNameChange, "Cognome", required = true, showError = lastName.isBlank(), singleLine = true, imeAction = ImeAction.Next, keyboardActions = nextAction)
        }
        Field(taxCode, onTaxCodeChange, "Codice fiscale", singleLine = true, imeAction = ImeAction.Next, keyboardActions = nextAction)
        Field(phone, onPhoneChange, "Telefono", required = true, showError = contactMissing, singleLine = true, imeAction = ImeAction.Next, keyboardActions = nextAction)
        Field(email, onEmailChange, "Email", required = true, showError = contactMissing, singleLine = true, imeAction = ImeAction.Next, keyboardActions = nextAction)
        Field(address, onAddressChange, "Indirizzo", singleLine = true, imeAction = ImeAction.Next, keyboardActions = nextAction)
        Field(notes, onNotesChange, "Note cliente", imeAction = ImeAction.Done, keyboardActions = doneAction)
        TouchButton(
            modifier = Modifier.fillMaxWidth(),
            text = "Crea cliente e commessa",
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
            Field(companyName, onCompanyNameChange, "Ragione sociale", required = true, showError = companyName.isBlank(), singleLine = true, imeAction = ImeAction.Next, keyboardActions = nextAction)
            Field(vatNumber, onVatNumberChange, "P.IVA", singleLine = true, imeAction = ImeAction.Next, keyboardActions = nextAction)
        } else {
            Field(firstName, onFirstNameChange, "Nome", required = true, showError = firstName.isBlank(), singleLine = true, imeAction = ImeAction.Next, keyboardActions = nextAction)
            Field(lastName, onLastNameChange, "Cognome", required = true, showError = lastName.isBlank(), singleLine = true, imeAction = ImeAction.Next, keyboardActions = nextAction)
        }
        Field(taxCode, onTaxCodeChange, "Codice fiscale", singleLine = true, imeAction = ImeAction.Next, keyboardActions = nextAction)
        Field(phone, onPhoneChange, "Telefono", required = true, showError = contactMissing, singleLine = true, imeAction = ImeAction.Next, keyboardActions = nextAction)
        Field(email, onEmailChange, "Email", required = true, showError = contactMissing, singleLine = true, imeAction = ImeAction.Next, keyboardActions = nextAction)
        Field(address, onAddressChange, "Indirizzo", singleLine = true, imeAction = ImeAction.Next, keyboardActions = nextAction)
        Field(notes, onNotesChange, "Note cliente", imeAction = ImeAction.Done, keyboardActions = doneAction)
    }
}

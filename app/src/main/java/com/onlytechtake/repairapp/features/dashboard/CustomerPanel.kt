package com.onlytechtake.repairapp.features.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.ui.unit.dp
import com.onlytechtake.repairapp.data.local.entities.CustomerEntity
import com.onlytechtake.repairapp.data.local.entities.CustomerType
import com.onlytechtake.repairapp.domain.workspace.CustomerInput
import com.onlytechtake.repairapp.features.workspace.WorkspaceViewModel
import com.onlytechtake.repairapp.features.workspace.label

@Composable
fun CustomerPanel(viewModel: WorkspaceViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val customers by viewModel.customerSuggestions.collectAsState()
    var showForm by rememberSaveable { mutableStateOf(false) }
    var confirmSave by rememberSaveable { mutableStateOf(false) }
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

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            SectionTitle("Clienti")
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Archivio clienti", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = uiState.customerSearchQuery,
                        onValueChange = viewModel::setCustomerSearchQuery,
                        label = { Text("Cerca nome, telefono, email, P.IVA, codice fiscale o commessa") },
                        singleLine = true,
                    )
                    TouchButton(
                        modifier = Modifier.fillMaxWidth(),
                        text = "Nuovo cliente",
                        onClick = { showForm = true },
                    )
                }
            }
        }

        if (showForm) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Nuovo cliente", style = MaterialTheme.typography.titleMedium)
                        CustomerTypeToggle(isBusiness = isBusiness, onBusinessChange = { isBusiness = it })
                        if (isBusiness) {
                            Field(companyName, { companyName = it }, "Ragione sociale", required = true, showError = companyName.isBlank(), singleLine = true)
                            Field(vatNumber, { vatNumber = it }, "P.IVA")
                        } else {
                            Field(firstName, { firstName = it }, "Nome", required = true, showError = firstName.isBlank(), singleLine = true)
                            Field(lastName, { lastName = it }, "Cognome", required = true, showError = lastName.isBlank(), singleLine = true)
                        }
                        Field(taxCode, { taxCode = it }, "Codice fiscale")
                        Field(phone, { phone = it }, "Telefono", required = true, showError = phone.isBlank() && email.isBlank(), singleLine = true)
                        Field(email, { email = it }, "Email", required = true, showError = phone.isBlank() && email.isBlank(), singleLine = true)
                        Field(address, { address = it }, "Indirizzo")
                        Field(notes, { notes = it }, "Note")
                        ActionRow {
                            TouchButton(
                                text = "Salva cliente",
                                enabled = canSave,
                                onClick = { confirmSave = true },
                            )
                            TouchButton(
                                text = "Annulla",
                                tone = ButtonTone.NEUTRAL,
                                onClick = { showForm = false },
                            )
                        }
                    }
                }
            }
        }

        item { SectionTitle(if (uiState.customerSearchQuery.isBlank()) "Clienti recenti" else "Risultati ricerca") }
        if (customers.isEmpty()) {
            item {
                CustomerEmptyState(
                    hasQuery = uiState.customerSearchQuery.isNotBlank(),
                    onNewCustomer = { showForm = true },
                )
            }
        } else {
            items(customers) { customer ->
                CustomerCard(customer)
            }
        }
    }

    if (confirmSave) {
        AlertDialog(
            onDismissRequest = { confirmSave = false },
            title = { Text("Salvare cliente?") },
            text = { Text("Salvare il cliente?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmSave = false
                        viewModel.saveCustomer(input)
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
                        showForm = false
                        viewModel.setCustomerSearchQuery("")
                    },
                ) {
                    Text("Conferma")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmSave = false }) {
                    Text("Annulla")
                }
            },
        )
    }
}

@Composable
private fun CustomerCard(customer: CustomerEntity) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(customer.displayName(), style = MaterialTheme.typography.titleMedium)
            Text("${customer.type.label()} - ${customer.phone.orEmpty()} ${customer.email.orEmpty()}")
        }
    }
}

@Composable
private fun CustomerEmptyState(hasQuery: Boolean, onNewCustomer: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = if (hasQuery) "Nessun cliente trovato per questa ricerca." else "Nessun cliente registrato.",
                style = MaterialTheme.typography.bodyMedium,
            )
            TouchButton(
                modifier = Modifier.fillMaxWidth(),
                text = "Nuovo cliente",
                onClick = onNewCustomer,
            )
        }
    }
}

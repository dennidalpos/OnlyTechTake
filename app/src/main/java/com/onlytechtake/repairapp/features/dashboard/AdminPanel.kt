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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.onlytechtake.repairapp.features.workspace.WorkspaceViewModel

@Composable
fun AdminPanel(
    viewModel: WorkspaceViewModel,
    isAdmin: Boolean,
    onResetComplete: () -> Unit,
) {
    val users by viewModel.users.collectAsState()
    var displayName by rememberSaveable { mutableStateOf("") }
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var resetPasswords by rememberSaveable { mutableStateOf(emptyMap<String, String>()) }
    var resetConfirmation by rememberSaveable { mutableStateOf("") }
    var confirmTitle by remember { mutableStateOf<String?>(null) }
    var confirmText by remember { mutableStateOf("") }
    var confirmAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    fun requestConfirm(title: String, text: String, action: () -> Unit) {
        confirmTitle = title
        confirmText = text
        confirmAction = action
    }

    if (!isAdmin) {
        Text(modifier = Modifier.padding(16.dp), text = "Sezione disponibile solo per admin.")
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            SectionTitle("Admin")
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Crea operatore", style = MaterialTheme.typography.titleMedium)
                    Field(displayName, { displayName = it }, "Nome")
                    Field(username, { username = it }, "Username")
                    Field(
                        value = password,
                        onValueChange = { password = it },
                        label = "Password temporanea",
                        singleLine = true,
                        keyboardType = KeyboardType.Password,
                        visualTransformation = PasswordVisualTransformation(),
                    )
                    TouchButton(
                        text = "Crea operatore",
                        onClick = {
                            requestConfirm("Creare operatore?", "Il nuovo operatore verra' salvato nel database locale.") {
                                viewModel.createOperator(displayName, username, password)
                                displayName = ""
                                username = ""
                                password = ""
                            }
                        },
                    )
                }
            }
        }
        item { SectionTitle("Utenti") }
        if (users.isEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Nessun utente configurato.", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        } else {
            items(users) { user ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("${user.displayName} - ${user.username} (${user.role.name})", style = MaterialTheme.typography.titleMedium)
                        Text(if (user.active) "Attivo" else "Disattivo")
                        Field(
                            value = resetPasswords[user.id].orEmpty(),
                            onValueChange = { value ->
                                resetPasswords = resetPasswords + (user.id to value)
                            },
                            label = "Nuova password temporanea",
                            singleLine = true,
                            keyboardType = KeyboardType.Password,
                            visualTransformation = PasswordVisualTransformation(),
                        )
                        ActionRow {
                            Button(
                                onClick = {
                                    requestConfirm("Cambiare stato utente?", "Lo stato di ${user.displayName} verra' aggiornato.") {
                                        viewModel.setUserActive(user.id, !user.active)
                                    }
                                },
                            ) {
                                Text(if (user.active) "Disattiva" else "Attiva")
                            }
                            Button(
                                onClick = {
                                    val resetPassword = resetPasswords[user.id].orEmpty()
                                    requestConfirm("Resettare password?", "La password temporanea inserita verra' salvata per ${user.displayName}.") {
                                        viewModel.resetPassword(user.id, resetPassword)
                                        resetPasswords = resetPasswords - user.id
                                    }
                                },
                            ) {
                                Text("Reset password")
                            }
                        }
                    }
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Reset totale dati", style = MaterialTheme.typography.titleMedium)
                    Text("Cancella dati locali, preferenze e file privati dell'app. Dopo il reset serve configurare un nuovo admin.")
                    Field(resetConfirmation, { resetConfirmation = it }, "Digita RESET")
                    TouchButton(
                        text = "Reset totale",
                        enabled = resetConfirmation == "RESET",
                        tone = ButtonTone.DANGER,
                        onClick = {
                            requestConfirm("Reset totale?", "Questa azione elimina i dati locali dell'app e riporta al primo setup admin.") {
                                viewModel.resetAllData(resetConfirmation, onResetComplete)
                                resetConfirmation = ""
                            }
                        },
                    )
                }
            }
        }
    }

    confirmTitle?.let { title ->
        AlertDialog(
            onDismissRequest = {
                confirmTitle = null
                confirmAction = null
            },
            title = { Text(title) },
            text = { Text(confirmText) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmAction?.invoke()
                        confirmTitle = null
                        confirmAction = null
                    },
                ) {
                    Text("Conferma")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        confirmTitle = null
                        confirmAction = null
                    },
                ) {
                    Text("Annulla")
                }
            },
        )
    }
}

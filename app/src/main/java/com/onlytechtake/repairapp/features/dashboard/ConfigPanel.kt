package com.onlytechtake.repairapp.features.dashboard

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.unit.dp
import com.onlytechtake.repairapp.data.local.entities.MessageMode
import com.onlytechtake.repairapp.features.workspace.WorkspaceDestination
import com.onlytechtake.repairapp.features.workspace.WorkspaceViewModel

@Composable
fun ConfigPanel(
    viewModel: WorkspaceViewModel,
    isAdmin: Boolean,
    onBackupRestored: () -> Unit,
) {
    val categories by viewModel.categories.collectAsState()
    val templates by viewModel.templates.collectAsState()
    val messageMode by viewModel.messageMode.collectAsState()

    var section by rememberSaveable { mutableStateOf<ConfigSection?>(null) }
    var categoryId by rememberSaveable { mutableStateOf<String?>(null) }
    var categoryName by rememberSaveable { mutableStateOf("") }
    var categoryDescription by rememberSaveable { mutableStateOf("") }
    var categoryActive by rememberSaveable { mutableStateOf(true) }
    var categorySortOrder by rememberSaveable { mutableStateOf("0") }
    var templateCode by rememberSaveable { mutableStateOf("whatsapp_aggiornamento") }
    var templateTitle by rememberSaveable { mutableStateOf("WhatsApp aggiornamento") }
    var templateBody by rememberSaveable { mutableStateOf("Aggiornamento {{numero_commessa}}: {{stato_commessa}}") }
    var templateActive by rememberSaveable { mutableStateOf(true) }
    var confirmTitle by remember { mutableStateOf<String?>(null) }
    var confirmText by remember { mutableStateOf("") }
    var confirmAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    val exportConfigLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let(viewModel::exportConfiguration)
    }
    val exportDbLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        uri?.let(viewModel::exportDatabase)
    }
    val exportZipLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        uri?.let(viewModel::exportCompleteBackup)
    }
    val restoreZipLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.restoreCompleteBackup(it, onBackupRestored) }
    }
    val exportOrdersXlsxLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(XLSX_MIME_TYPE)) { uri ->
        uri?.let(viewModel::exportOrdersXlsx)
    }

    fun requestConfirm(title: String, text: String, action: () -> Unit) {
        confirmTitle = title
        confirmText = text
        confirmAction = action
    }

    fun loadNewCategory() {
        categoryId = null
        categoryName = ""
        categoryDescription = ""
        categoryActive = true
        categorySortOrder = "0"
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(DashboardSpacing.screenPadding),
        verticalArrangement = Arrangement.spacedBy(DashboardSpacing.listGap),
    ) {
        item {
            SectionTitle("Config")
            section?.let {
                TouchButton(text = "Torna a Config", onClick = { section = null })
            }
        }

        when (section) {
            null -> {
                item {
                    ConfigHubCard("Categorie", "Tipi di commessa attivi o archiviati.") {
                        section = ConfigSection.CATEGORIES
                    }
                }
                item {
                    ConfigHubCard(
                        title = "Template",
                        subtitle = if (isAdmin) "Privacy, condizioni e messaggi WhatsApp." else "Area riservata admin.",
                        enabled = isAdmin,
                    ) {
                        section = ConfigSection.TEMPLATES
                    }
                }
                item {
                    ConfigHubCard("WhatsApp", "Preparazione manuale messaggi cliente.") {
                        section = ConfigSection.WHATSAPP
                    }
                }
                item {
                    ConfigHubCard("Utenti e reset", "Gestione utenti e azzeramento dati admin.") {
                        viewModel.navigateTo(WorkspaceDestination.ADMIN)
                    }
                }
                item {
                    ConfigHubCard("Backup", "Export config, database, ZIP e XLSX.") {
                        section = ConfigSection.EXPORT
                    }
                }
            }

            ConfigSection.CATEGORIES -> {
                item {
                    SectionTitle("Categorie")
                    if (!isAdmin) {
                        Text("Area riservata admin. Le categorie sono disponibili solo in consultazione.")
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(DashboardSpacing.formGap)) {
                            Field(categoryName, { categoryName = it }, "Nome categoria")
                            Field(categoryDescription, { categoryDescription = it }, "Descrizione")
                            Field(categorySortOrder, { categorySortOrder = it }, "Ordine")
                            ActionRow {
                                TouchButton(
                                    text = if (categoryActive) "Attiva" else "Archiviata",
                                    tone = if (categoryActive) ButtonTone.SUCCESS else ButtonTone.DANGER,
                                    onClick = { categoryActive = !categoryActive },
                                )
                                TouchButton(text = "Nuova", tone = ButtonTone.NEUTRAL, onClick = ::loadNewCategory)
                                TouchButton(
                                    text = "Salva categoria",
                                    onClick = {
                                        requestConfirm("Salvare categoria?", "La categoria verra' aggiornata nel catalogo locale.") {
                                            viewModel.saveCategory(
                                                id = categoryId,
                                                name = categoryName,
                                                description = categoryDescription,
                                                active = categoryActive,
                                                sortOrder = categorySortOrder.toIntOrNull() ?: 0,
                                            )
                                            loadNewCategory()
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
                items(categories) { category ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = isAdmin) {
                                categoryId = category.id
                                categoryName = category.name
                                categoryDescription = category.description.orEmpty()
                                categoryActive = category.active
                                categorySortOrder = category.sortOrder.toString()
                            },
                    ) {
                        Column(
                            modifier = Modifier.padding(DashboardSpacing.cardPadding),
                            verticalArrangement = Arrangement.spacedBy(DashboardSpacing.compactGap),
                        ) {
                            Text(category.name, style = MaterialTheme.typography.titleMedium)
                            Text(if (category.active) "Attiva" else "Archiviata")
                            category.description?.takeIf { it.isNotBlank() }?.let { Text(it) }
                        }
                    }
                }
                if (categories.isEmpty()) {
                    item {
                        Text("Nessuna categoria configurata.", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            ConfigSection.TEMPLATES -> {
                item {
                    SectionTitle("Template")
                    Field(templateCode, { templateCode = it }, "Codice")
                    Field(templateTitle, { templateTitle = it }, "Titolo")
                    Field(templateBody, { templateBody = it }, "Testo con {{variabili}}")
                    ActionRow {
                        TouchButton(
                            text = if (templateActive) "Attivo" else "Archiviato",
                            tone = if (templateActive) ButtonTone.SUCCESS else ButtonTone.DANGER,
                            onClick = { templateActive = !templateActive },
                        )
                        TouchButton(
                            text = "Salva template",
                            onClick = {
                                requestConfirm("Salvare template?", "Il template verra' salvato con una nuova versione.") {
                                    viewModel.saveTemplate(templateCode, templateTitle, templateBody, active = templateActive)
                                }
                            },
                        )
                    }
                }
                items(templates) { template ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                templateCode = template.code
                                templateTitle = template.title
                                templateBody = template.body
                                templateActive = template.active
                            },
                    ) {
                        Column(
                            modifier = Modifier.padding(DashboardSpacing.cardPadding),
                            verticalArrangement = Arrangement.spacedBy(DashboardSpacing.compactGap),
                        ) {
                            Text("${template.code} v${template.version}", style = MaterialTheme.typography.titleMedium)
                            Text(template.title)
                            Text(if (template.active) "Attivo" else "Archiviato")
                        }
                    }
                }
                if (templates.isEmpty()) {
                    item {
                        Text("Nessun template configurato.", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            ConfigSection.WHATSAPP -> {
                item {
                    SectionTitle("WhatsApp")
                    Text("Attuale: ${messageMode.name}")
                    ActionRow {
                        TouchButton(
                            text = "Manuale",
                            onClick = {
                                requestConfirm("Impostare modalita manuale?", "La modalita WhatsApp verra' salvata.") {
                                    viewModel.setMessageMode(MessageMode.MANUAL_WHATSAPP)
                                }
                            },
                        )
                    }
                    Text("I messaggi vengono preparati nell'app e inviati manualmente dall'operatore.")
                }
            }

            ConfigSection.EXPORT -> {
                item {
                    SectionTitle("Backup")
                    if (!isAdmin) {
                        Text("Area riservata admin.")
                    } else {
                        Text("Gli export e i backup ripristinati non sono cifrati dall'app: usare solo supporti o cartelle protette.")
                        ActionRow {
                            TouchButton(
                                text = "Export config",
                                onClick = {
                                    requestConfirm("Esportare configurazione?", "Verra' creato un file JSON non cifrato tramite selettore di sistema.") {
                                        exportConfigLauncher.launch("onlytechtake-config.json")
                                    }
                                },
                            )
                            TouchButton(
                                text = "Export DB",
                                onClick = {
                                    requestConfirm("Esportare database?", "Verra' creato un file database non cifrato tramite selettore di sistema.") {
                                        exportDbLauncher.launch("repair_app.db")
                                    }
                                },
                            )
                            TouchButton(
                                text = "Export commesse XLSX",
                                onClick = {
                                    requestConfirm("Esportare commesse?", "Verra' creato un file XLSX non cifrato tramite selettore di sistema.") {
                                        exportOrdersXlsxLauncher.launch("onlytechtake-commesse.xlsx")
                                    }
                                },
                            )
                            TouchButton(
                                text = "Export ZIP",
                                onClick = {
                                    requestConfirm("Esportare backup completo?", "Verra' creato un file ZIP non cifrato tramite selettore di sistema.") {
                                        exportZipLauncher.launch("onlytechtake-backup.zip")
                                    }
                                },
                            )
                            TouchButton(
                                text = "Ripristina ZIP",
                                tone = ButtonTone.WARNING,
                                onClick = {
                                    requestConfirm(
                                        title = "Ripristinare backup?",
                                        text = "Il backup ZIP sostituira' database e file locali dell'app. Dopo il ripristino sara' richiesto un nuovo login.",
                                    ) {
                                        restoreZipLauncher.launch(arrayOf("application/zip", "application/octet-stream", "*/*"))
                                    }
                                },
                            )
                        }
                    }
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

@Composable
private fun ConfigHubCard(
    title: String,
    subtitle: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick),
    ) {
        Column(
            modifier = Modifier.padding(DashboardSpacing.cardPadding),
            verticalArrangement = Arrangement.spacedBy(DashboardSpacing.compactGap),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle)
        }
    }
}

private enum class ConfigSection {
    CATEGORIES,
    TEMPLATES,
    WHATSAPP,
    EXPORT,
}

private const val XLSX_MIME_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"

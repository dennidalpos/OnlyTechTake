package com.onlytechtake.repairapp.features.dashboard

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.onlytechtake.repairapp.core.design.RepairAppTestTags
import com.onlytechtake.repairapp.data.local.entities.MessageMode
import com.onlytechtake.repairapp.data.local.entities.OrderCategoryEntity
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
    var categoryParentId by rememberSaveable { mutableStateOf<String?>(null) }
    var currentCategoryParentId by rememberSaveable { mutableStateOf<String?>(null) }
    var categoryName by rememberSaveable { mutableStateOf("") }
    var categoryDescription by rememberSaveable { mutableStateOf("") }
    var categoryActive by rememberSaveable { mutableStateOf(true) }
    var templateCode by rememberSaveable { mutableStateOf("whatsapp_aggiornamento") }
    var loadedTemplateCode by rememberSaveable { mutableStateOf<String?>(null) }
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
        categoryParentId = currentCategoryParentId
        categoryName = ""
        categoryDescription = ""
        categoryActive = true
    }

    fun selectCategoryForEdit(category: OrderCategoryEntity) {
        categoryId = category.id
        categoryParentId = category.parentId
        categoryName = category.name
        categoryDescription = category.description.orEmpty()
        categoryActive = category.active
    }

    fun openCategoryBranch(categoryId: String?) {
        currentCategoryParentId = categoryId
        loadNewCategory()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(DashboardSpacing.screenPadding),
        verticalArrangement = Arrangement.spacedBy(DashboardSpacing.listGap),
    ) {
        item {
            SectionTitle("Config")
            section?.let {
                TouchButton(
                    text = "Torna a Config",
                    testTag = RepairAppTestTags.ConfigBackButton,
                    onClick = { section = null },
                )
            }
        }

        when (section) {
            null -> {
                item {
                    ConfigHubCard(
                        title = "Categorie",
                        subtitle = "Catalogo commesse",
                        testTag = RepairAppTestTags.ConfigHubCategoriesButton,
                    ) {
                        section = ConfigSection.CATEGORIES
                    }
                }
                item {
                    ConfigHubCard(
                        title = "Template",
                        subtitle = if (isAdmin) "Messaggi e documenti" else "Area admin",
                        enabled = isAdmin,
                        testTag = RepairAppTestTags.ConfigHubTemplatesButton,
                    ) {
                        section = ConfigSection.TEMPLATES
                    }
                }
                item {
                    ConfigHubCard("WhatsApp", "Invio manuale") {
                        section = ConfigSection.WHATSAPP
                    }
                }
                item {
                    ConfigHubCard("Utenti e reset", "Area admin") {
                        viewModel.navigateTo(WorkspaceDestination.ADMIN)
                    }
                }
                item {
                    ConfigHubCard("Backup", "Export e ripristino") {
                        section = ConfigSection.EXPORT
                    }
                }
            }

            ConfigSection.CATEGORIES -> {
                val currentParent = currentCategoryParentId?.let { id -> categories.firstOrNull { it.id == id } }
                val visibleCategories = categories.filter { it.parentId == currentCategoryParentId }
                item {
                    SectionTitle("Categorie")
                    if (!isAdmin) {
                        Text("Area riservata admin. Le categorie sono disponibili solo in consultazione.")
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(DashboardSpacing.formGap)) {
                            Text(
                                text = "Livello ${currentParent?.level?.plus(1) ?: 0}",
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Text(
                                text = categoryBreadcrumb(categories, currentCategoryParentId),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            ActionRow {
                                TouchButton(
                                    text = "Radice",
                                    tone = if (currentCategoryParentId == null) ButtonTone.SUCCESS else ButtonTone.NEUTRAL,
                                    onClick = { openCategoryBranch(null) },
                                )
                                if (currentParent?.parentId != null) {
                                    TouchButton(
                                        text = "Su",
                                        tone = ButtonTone.NEUTRAL,
                                        onClick = { openCategoryBranch(currentParent.parentId) },
                                    )
                                } else if (currentCategoryParentId != null) {
                                    TouchButton(
                                        text = "Su",
                                        tone = ButtonTone.NEUTRAL,
                                        onClick = { openCategoryBranch(null) },
                                    )
                                }
                            }
                            Field(categoryName, { categoryName = it }, "Nome categoria", testTag = RepairAppTestTags.ConfigCategoryNameField)
                            Field(categoryDescription, { categoryDescription = it }, "Descrizione", testTag = RepairAppTestTags.ConfigCategoryDescriptionField)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(if (categoryActive) "Categoria attiva" else "Categoria archiviata")
                                Switch(
                                    checked = categoryActive,
                                    onCheckedChange = { categoryActive = it },
                                )
                            }
                            ActionRow {
                                TouchButton(
                                    text = "Nuova",
                                    tone = ButtonTone.NEUTRAL,
                                    testTag = RepairAppTestTags.ConfigCategoryNewButton,
                                    onClick = ::loadNewCategory,
                                )
                                TouchButton(
                                    text = "Salva categoria",
                                    testTag = RepairAppTestTags.ConfigCategorySaveButton,
                                    onClick = {
                                        requestConfirm("Salvare categoria?", "La categoria verra' aggiornata nel catalogo locale.") {
                                            viewModel.saveCategory(
                                                id = categoryId,
                                                parentId = categoryParentId,
                                                name = categoryName,
                                                description = categoryDescription,
                                                active = categoryActive,
                                            )
                                            loadNewCategory()
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
                items(visibleCategories) { category ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = isAdmin) {
                                selectCategoryForEdit(category)
                            },
                    ) {
                        Column(
                            modifier = Modifier.padding(DashboardSpacing.cardPadding),
                            verticalArrangement = Arrangement.spacedBy(DashboardSpacing.compactGap),
                        ) {
                            Text(category.name, style = MaterialTheme.typography.titleMedium)
                            Text("Livello ${category.level} - ${if (category.active) "Attiva" else "Archiviata"}")
                            category.description?.takeIf { it.isNotBlank() }?.let { Text(it) }
                            if (isAdmin) {
                                ActionRow {
                                    TouchButton(
                                        text = "Modifica",
                                        tone = ButtonTone.NEUTRAL,
                                        onClick = { selectCategoryForEdit(category) },
                                    )
                                    TouchButton(
                                        text = "Apri ramo",
                                        tone = ButtonTone.INFO,
                                        onClick = { openCategoryBranch(category.id) },
                                    )
                                }
                            }
                        }
                    }
                }
                if (visibleCategories.isEmpty()) {
                    item {
                        Text("Nessuna categoria in questo ramo.", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            ConfigSection.TEMPLATES -> {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(DashboardSpacing.formGap)) {
                        SectionTitle("Template")
                        Field(templateCode, { templateCode = it }, "Codice", testTag = RepairAppTestTags.ConfigTemplateCodeField)
                        Field(templateTitle, { templateTitle = it }, "Titolo", testTag = RepairAppTestTags.ConfigTemplateTitleField)
                        Field(templateBody, { templateBody = it }, "Testo", testTag = RepairAppTestTags.ConfigTemplateBodyField)
                        Column(verticalArrangement = Arrangement.spacedBy(DashboardSpacing.compactGap)) {
                            Text("Variabili", style = MaterialTheme.typography.titleSmall)
                            OperatorText("{{numero_commessa}}  {{stato_commessa}}  {{categoria_percorso}}  {{cliente}}")
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(if (templateActive) "Template attivo" else "Template archiviato")
                            Switch(
                                checked = templateActive,
                                onCheckedChange = { templateActive = it },
                            )
                        }
                        ActionRow {
                            TouchButton(
                                text = "Salva template",
                                testTag = RepairAppTestTags.ConfigTemplateSaveButton,
                                onClick = {
                                    val riskyTemplateChange = isRiskyTemplateChange(loadedTemplateCode, templateCode, templateActive)
                                    requestConfirm(
                                        "Salvare template?",
                                        if (riskyTemplateChange) {
                                            "La modifica puo' interrompere i flussi che usano questo codice. Le firme salvate non cambiano."
                                        } else {
                                            "Verra' salvata una nuova versione."
                                        },
                                    ) {
                                        viewModel.saveTemplate(templateCode, templateTitle, templateBody, active = templateActive)
                                        loadedTemplateCode = templateCode
                                    }
                                },
                            )
                        }
                    }
                }
                items(templates) { template ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                templateCode = template.code
                                loadedTemplateCode = template.code
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
                    Column(verticalArrangement = Arrangement.spacedBy(DashboardSpacing.formGap)) {
                        SectionTitle("WhatsApp")
                        Text("Modalita': ${messageMode.name}")
                        ActionRow {
                            TouchButton(
                                text = "Manuale",
                                onClick = {
                                    requestConfirm("Impostare modalita' manuale?", "La modalita' WhatsApp verra' salvata.") {
                                        viewModel.setMessageMode(MessageMode.MANUAL_WHATSAPP)
                                    }
                                },
                            )
                        }
                    }
                }
            }

            ConfigSection.EXPORT -> {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(DashboardSpacing.formGap)) {
                        SectionTitle("Backup")
                        if (!isAdmin) {
                            Text("Area riservata admin.")
                        } else {
                            Text("File non cifrati: usare solo destinazioni protette.")
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
    testTag: String? = null,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(testTag?.let { Modifier.testTag(it) } ?: Modifier)
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

private val systemTemplateCodes = setOf(
    "privacy_breve",
    "condizioni_presa_in_carico",
    "whatsapp_presa_in_carico",
    "whatsapp_aggiornamento",
    "whatsapp_diagnosi",
    "whatsapp_risultato",
    "whatsapp_preventivo_intervento",
    "whatsapp_non_riparabile",
    "whatsapp_riparazione",
    "whatsapp_lavorazione",
    "whatsapp_completamento",
    "whatsapp_pronto_ritiro",
)

private fun isRiskyTemplateChange(loadedCode: String?, currentCode: String, active: Boolean): Boolean =
    (loadedCode != null && loadedCode != currentCode) || (currentCode in systemTemplateCodes && !active)

private fun categoryBreadcrumb(categories: List<OrderCategoryEntity>, parentId: String?): String {
    if (parentId == null) return "Radice"
    val byId = categories.associateBy { it.id }
    val names = mutableListOf<String>()
    val visited = mutableSetOf<String>()
    var current = byId[parentId]
    while (current != null && visited.add(current.id)) {
        names += current.name
        current = current.parentId?.let(byId::get)
    }
    return (listOf("Radice") + names.asReversed()).joinToString(" > ")
}

package com.onlytechtake.repairapp.features.dashboard.config

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.core.utils.formatUiDateTime
import com.onlytechtake.repairapp.core.design.RepairAppTestTags
import com.onlytechtake.repairapp.data.local.entities.ExportDestination
import com.onlytechtake.repairapp.data.local.entities.MessageMode
import com.onlytechtake.repairapp.data.local.entities.OrderCategoryEntity
import com.onlytechtake.repairapp.domain.backup.BackupReminderPolicy
import com.onlytechtake.repairapp.domain.backup.BackupReminderSettings
import com.onlytechtake.repairapp.domain.backup.BackupReminderStatus
import com.onlytechtake.repairapp.features.dashboard.common.ActionRow
import com.onlytechtake.repairapp.features.dashboard.common.DashboardSpacing
import com.onlytechtake.repairapp.features.dashboard.common.Field
import com.onlytechtake.repairapp.features.dashboard.common.OperatorText
import com.onlytechtake.repairapp.features.dashboard.common.SectionTitle
import com.onlytechtake.repairapp.features.dashboard.common.StandardCard
import com.onlytechtake.repairapp.features.dashboard.common.TouchButton
import com.onlytechtake.repairapp.features.dashboard.common.TouchButtonWithIcon
import com.onlytechtake.repairapp.features.workspace.ButtonTone
import com.onlytechtake.repairapp.features.workspace.WorkspaceDestination
import com.onlytechtake.repairapp.features.workspace.WorkspaceViewModel

@Composable
fun ConfigPanel(
    viewModel: WorkspaceViewModel,
    isAdmin: Boolean,
    onBackupRestored: () -> Unit,
    initialSection: String? = null,
) {
    val categories by viewModel.categories.collectAsState()
    val templates by viewModel.templates.collectAsState()
    val messageMode by viewModel.messageMode.collectAsState()
    val exportDestination by viewModel.exportDestination.collectAsState()
    val isCloudAvailable by viewModel.isCloudAvailable.collectAsState()
    val backupReminderSettings by viewModel.backupReminderSettings.collectAsState()

    val confirmCategorySaveTitle = stringResource(R.string.config_category_confirm_save_title)
    val confirmCategorySaveText = stringResource(R.string.config_category_confirm_save_text)
    val confirmCategoryDeleteTitle = stringResource(R.string.config_category_confirm_delete_title)
    val confirmCategoryDeleteText = stringResource(R.string.config_category_confirm_delete_text)
    val confirmCategoryDeleteSpecificText = stringResource(R.string.config_category_confirm_delete_specific_text)
    val confirmTemplateSaveTitle = stringResource(R.string.config_template_confirm_save_title)
    val confirmTemplateSaveRisky = stringResource(R.string.config_template_confirm_save_risky)
    val confirmTemplateSaveNewVersion = stringResource(R.string.config_template_confirm_save_new_version)
    val confirmWaManualTitle = stringResource(R.string.config_whatsapp_confirm_manual_title)
    val confirmWaManualText = stringResource(R.string.config_whatsapp_confirm_manual_text)
    val confirmExportXlsxTitle = stringResource(R.string.config_backup_confirm_export_xlsx_title)
    val confirmExportXlsxTextTemplate = stringResource(R.string.config_backup_confirm_export_xlsx_text)
    val confirmExportZipTitle = stringResource(R.string.config_backup_confirm_export_zip_title)
    val confirmExportZipTextTemplate = stringResource(R.string.config_backup_confirm_export_zip_text)
    val confirmRestoreZipTitle = stringResource(R.string.config_backup_confirm_restore_zip_title)
    val confirmRestoreZipText = stringResource(R.string.config_backup_confirm_restore_zip_text)
    val destMsgLocal = stringResource(R.string.order_export_local)
    val destMsgCloud = stringResource(R.string.order_export_cloud)

    val section = initialSection?.let { ConfigSection.valueOf(it) }
    var categoryId by rememberSaveable { mutableStateOf<String?>(null) }
    var categoryParentId by rememberSaveable { mutableStateOf<String?>(null) }
    var currentCategoryParentId by rememberSaveable { mutableStateOf<String?>(null) }
    var categoryName by rememberSaveable { mutableStateOf("") }
    var categoryDescription by rememberSaveable { mutableStateOf("") }
    var categoryActive by rememberSaveable { mutableStateOf(value = true) }
    var templateCode by rememberSaveable { mutableStateOf("whatsapp_aggiornamento") }
    var loadedTemplateCode by rememberSaveable { mutableStateOf<String?>(null) }
    var templateTitle by rememberSaveable { mutableStateOf("WhatsApp aggiornamento") }
    var templateBody by rememberSaveable { mutableStateOf("Aggiornamento {{numero_commessa}}: {{stato_commessa}}") }
    var templateActive by rememberSaveable { mutableStateOf(value = true) }
    var backupIntervalDays by rememberSaveable { mutableStateOf(backupReminderSettings.intervalDays.toString()) }
    var confirmTitle by remember { mutableStateOf<String?>(null) }
    var confirmText by remember { mutableStateOf("") }
    var confirmAction by remember { mutableStateOf<(() -> Unit)?>(null) }

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

    LaunchedEffect(backupReminderSettings.intervalDays) {
        backupIntervalDays = backupReminderSettings.intervalDays.toString()
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

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = DashboardSpacing.maxContentWidth)
                .imePadding(),
            contentPadding = PaddingValues(DashboardSpacing.screenPadding),
            verticalArrangement = Arrangement.spacedBy(DashboardSpacing.listGap),
        ) {
            item {
                SectionTitle(stringResource(R.string.dashboard_config))
            }

            when (section) {
                null -> {
                    item {
                        ConfigHubCard(
                            title = stringResource(R.string.config_hub_categories_title),
                            subtitle = stringResource(R.string.config_hub_categories_subtitle),
                            testTag = RepairAppTestTags.ConfigHubCategoriesButton,
                        ) {
                            viewModel.navigateTo(WorkspaceDestination.CONFIG_CATEGORIES)
                        }
                    }
                    item {
                        ConfigHubCard(
                            title = stringResource(R.string.config_hub_templates_title),
                            subtitle = if (isAdmin) stringResource(R.string.config_hub_templates_subtitle_admin) else stringResource(R.string.config_hub_templates_subtitle_user),
                            enabled = isAdmin,
                            testTag = RepairAppTestTags.ConfigHubTemplatesButton,
                        ) {
                            viewModel.navigateTo(WorkspaceDestination.CONFIG_TEMPLATES)
                        }
                    }
                    item {
                        ConfigHubCard(stringResource(R.string.config_hub_whatsapp_title), stringResource(R.string.config_hub_whatsapp_subtitle)) {
                            viewModel.navigateTo(WorkspaceDestination.CONFIG_WHATSAPP)
                        }
                    }
                    item {
                        ConfigHubCard(stringResource(R.string.config_hub_admin_title), stringResource(R.string.config_hub_admin_subtitle)) {
                            viewModel.navigateTo(WorkspaceDestination.ADMIN)
                        }
                    }
                    if (isAdmin) {
                        item {
                            ConfigHubCard(
                                stringResource(R.string.config_hub_backup_title),
                                stringResource(R.string.config_hub_backup_subtitle),
                                testTag = RepairAppTestTags.ConfigHubBackupButton,
                            ) {
                                viewModel.navigateTo(WorkspaceDestination.CONFIG_EXPORT)
                            }
                        }
                    }
                }

                ConfigSection.CATEGORIES -> {
                    val currentParent = currentCategoryParentId?.let { id -> categories.firstOrNull { it.id == id } }
                    val visibleCategories = categories.filter { it.parentId == currentCategoryParentId }
                    item {
                        SectionTitle(stringResource(R.string.config_hub_categories_title))
                        StandardCard(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            highlight = true,
                        ) {
                            Column(
                                modifier = Modifier.padding(DashboardSpacing.cardPadding),
                                verticalArrangement = Arrangement.spacedBy(DashboardSpacing.formGap),
                            ) {
                                Text(
                                    text = if (categoryId == null) {
                                        if (currentCategoryParentId == null) "Nuova Categoria principale"
                                        else "Nuova Sottocategoria"
                                    } else {
                                        "Modifica Categoria"
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                CategoryBreadcrumb(categories, currentCategoryParentId)
                                ActionRow {
                                    TouchButton(
                                        text = stringResource(R.string.config_category_root),
                                        tone = if (currentCategoryParentId == null) ButtonTone.SUCCESS else ButtonTone.NEUTRAL,
                                        onClick = { openCategoryBranch(null) },
                                    )
                                    if (currentParent?.parentId != null) {
                                        TouchButton(
                                            text = stringResource(R.string.config_category_up),
                                            tone = ButtonTone.NEUTRAL,
                                            onClick = { openCategoryBranch(currentParent.parentId) },
                                        )
                                    } else if (currentCategoryParentId != null) {
                                        TouchButton(
                                            text = stringResource(R.string.config_category_up),
                                            tone = ButtonTone.NEUTRAL,
                                            onClick = { openCategoryBranch(null) },
                                        )
                                    }
                                }
                                Field(
                                    categoryName,
                                    { categoryName = it },
                                    stringResource(R.string.config_category_name_label),
                                    testTag = RepairAppTestTags.ConfigCategoryNameField
                                )
                                Field(
                                    categoryDescription,
                                    { categoryDescription = it },
                                    stringResource(R.string.config_category_description_label),
                                    showOptional = true,
                                    testTag = RepairAppTestTags.ConfigCategoryDescriptionField
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(if (categoryActive) stringResource(R.string.config_category_active) else stringResource(R.string.config_category_archived))
                                    Switch(
                                        checked = categoryActive,
                                        enabled = isAdmin,
                                        onCheckedChange = { categoryActive = it },
                                    )
                                }
                                ActionRow {
                                    TouchButtonWithIcon(
                                        text = stringResource(R.string.config_category_new),
                                        icon = Icons.Default.Add,
                                        tone = ButtonTone.NEUTRAL,
                                        testTag = RepairAppTestTags.ConfigCategoryNewButton,
                                        onClick = ::loadNewCategory,
                                    )
                                    if ((categoryId != null) && isAdmin) {
                                        TouchButtonWithIcon(
                                            text = stringResource(R.string.config_category_open_branch),
                                            icon = Icons.AutoMirrored.Filled.ArrowForward,
                                            tone = ButtonTone.NEUTRAL,
                                            onClick = { openCategoryBranch(categoryId) },
                                        )
                                    }
                                    if ((categoryId == null) || isAdmin) {
                                        TouchButtonWithIcon(
                                            text = stringResource(R.string.config_category_save),
                                            icon = Icons.Default.Check,
                                            testTag = RepairAppTestTags.ConfigCategorySaveButton,
                                            onClick = {
                                                requestConfirm(confirmCategorySaveTitle, confirmCategorySaveText) {
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
                                    if ((categoryId != null) && isAdmin) {
                                        TouchButtonWithIcon(
                                            text = stringResource(R.string.config_category_delete),
                                            icon = Icons.Default.Delete,
                                            tone = ButtonTone.DANGER,
                                            testTag = RepairAppTestTags.ConfigCategoryDeleteButton,
                                            onClick = {
                                                requestConfirm(confirmCategoryDeleteTitle, confirmCategoryDeleteText) {
                                                    viewModel.deleteCategory(categoryId!!)
                                                    loadNewCategory()
                                                }
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }
                    items(visibleCategories) { category ->
                        StandardCard(
                            onClick = { openCategoryBranch(category.id) },
                        ) {
                            Column(
                                modifier = Modifier.padding(DashboardSpacing.cardPadding),
                                verticalArrangement = Arrangement.spacedBy(DashboardSpacing.compactGap),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                        LevelDot(category.level)
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(category.name, style = MaterialTheme.typography.titleMedium)
                                            Text(
                                                text = (if (category.level == 0) stringResource(R.string.config_category_level_main) else stringResource(R.string.config_category_level_sub, category.level)) + " - " + (if (category.active) stringResource(R.string.config_category_active) else stringResource(R.string.config_category_archived)),
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        if (isAdmin) {
                                            IconButton(
                                                onClick = { selectCategoryForEdit(category) },
                                            ) {
                                                Icon(Icons.Default.Edit, contentDescription = "Modifica")
                                            }
                                        }
                                        IconButton(
                                            onClick = { openCategoryBranch(category.id) },
                                        ) {
                                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Apri")
                                        }
                                        if (isAdmin) {
                                            IconButton(onClick = {
                                                requestConfirm(
                                                    confirmCategoryDeleteTitle,
                                                    confirmCategoryDeleteSpecificText.format(category.name)
                                                ) {
                                                    viewModel.deleteCategory(category.id)
                                                    loadNewCategory()
                                                }
                                            }) {
                                                Icon(Icons.Default.Delete, contentDescription = "Elimina", tint = MaterialTheme.colorScheme.error)
                                            }
                                        }
                                    }
                                }
                                category.description?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                            }
                        }
                    }
                    if (visibleCategories.isEmpty()) {
                        item {
                            Text(stringResource(R.string.config_category_empty_branch), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                ConfigSection.TEMPLATES -> {
                    item {
                        StandardCard(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            highlight = true,
                        ) {
                            Column(modifier = Modifier.padding(DashboardSpacing.cardPadding), verticalArrangement = Arrangement.spacedBy(DashboardSpacing.formGap)) {
                                SectionTitle(stringResource(R.string.config_hub_templates_title))
                                Field(templateCode, { templateCode = it }, stringResource(R.string.config_template_code_label), testTag = RepairAppTestTags.ConfigTemplateCodeField)
                                Field(templateTitle, { templateTitle = it }, stringResource(R.string.config_template_title_label), testTag = RepairAppTestTags.ConfigTemplateTitleField)
                                Field(templateBody, { templateBody = it }, stringResource(R.string.config_template_body_label), testTag = RepairAppTestTags.ConfigTemplateBodyField)
                                Column(verticalArrangement = Arrangement.spacedBy(DashboardSpacing.compactGap)) {
                                    Text(stringResource(R.string.config_template_variables), style = MaterialTheme.typography.titleSmall)
                                    OperatorText("{{numero_commessa}}  {{stato_commessa}}  {{categoria_percorso}}  {{cliente}}")
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(if (templateActive) stringResource(R.string.config_template_active) else stringResource(R.string.config_template_archived))
                                    Switch(
                                        checked = templateActive,
                                        onCheckedChange = { templateActive = it },
                                    )
                                }
                                ActionRow {
                                    TouchButton(
                                        text = stringResource(R.string.config_template_save),
                                        testTag = RepairAppTestTags.ConfigTemplateSaveButton,
                                        onClick = {
                                            val riskyTemplateChange = isRiskyTemplateChange(loadedTemplateCode, templateCode, templateActive)
                                            requestConfirm(
                                                confirmTemplateSaveTitle,
                                                if (riskyTemplateChange) confirmTemplateSaveRisky else confirmTemplateSaveNewVersion,
                                            ) {
                                                viewModel.saveTemplate(templateCode, templateTitle, templateBody, active = templateActive)
                                                loadedTemplateCode = templateCode
                                            }
                                        },
                                    )
                                }
                            }
                        }
                    }
                    items(templates) { template ->
                        StandardCard(
                            onClick = {
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
                                Text(stringResource(R.string.config_template_version_label, template.code, template.version), style = MaterialTheme.typography.titleMedium)
                                Text(template.title)
                                Text(if (template.active) stringResource(R.string.config_template_active) else stringResource(R.string.config_template_archived))
                            }
                        }
                    }
                    if (templates.isEmpty()) {
                        item {
                            Text(stringResource(R.string.config_template_empty), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                ConfigSection.WHATSAPP -> {
                    item {
                        StandardCard(highlight = true) {
                            Column(modifier = Modifier.padding(DashboardSpacing.cardPadding), verticalArrangement = Arrangement.spacedBy(DashboardSpacing.formGap)) {
                                SectionTitle(stringResource(R.string.config_hub_whatsapp_title))
                                Text(stringResource(R.string.config_whatsapp_mode, messageMode.name))
                                ActionRow {
                                    TouchButton(
                                        text = stringResource(R.string.config_whatsapp_manual),
                                        onClick = {
                                            requestConfirm(confirmWaManualTitle, confirmWaManualText) {
                                                viewModel.setMessageMode(MessageMode.MANUAL_WHATSAPP)
                                            }
                                        },
                                    )
                                }
                            }
                        }
                    }
                }

                ConfigSection.EXPORT -> {
                    item {
                        StandardCard(highlight = true) {
                            Column(modifier = Modifier.padding(DashboardSpacing.cardPadding), verticalArrangement = Arrangement.spacedBy(DashboardSpacing.formGap)) {
                                SectionTitle(stringResource(R.string.config_backup_title))
                                if (!isAdmin) {
                                    Text(stringResource(R.string.admin_only_section))
                                } else {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(stringResource(R.string.config_backup_reminder_enabled))
                                        Switch(
                                            checked = backupReminderSettings.enabled,
                                            onCheckedChange = viewModel::setBackupReminderEnabled,
                                            modifier = Modifier.testTag(RepairAppTestTags.ConfigBackupReminderSwitch),
                                        )
                                    }
                                    Field(
                                        value = backupIntervalDays,
                                        onValueChange = { raw ->
                                            val digits = raw.filter { it.isDigit() }.take(3)
                                            backupIntervalDays = digits
                                            digits.toIntOrNull()?.let(viewModel::setBackupReminderIntervalDays)
                                        },
                                        label = stringResource(R.string.config_backup_interval_days),
                                        singleLine = true,
                                        keyboardType = KeyboardType.Number,
                                        testTag = RepairAppTestTags.ConfigBackupIntervalField,
                                    )
                                    OperatorText(backupReminderInfo(backupReminderSettings))
                                    Text(stringResource(R.string.config_backup_unencrypted_warning))
                                    Text(stringResource(R.string.config_backup_destination_label), style = MaterialTheme.typography.titleSmall)
                                    ActionRow {
                                        TouchButton(
                                            text = stringResource(R.string.config_backup_destination_local),
                                            tone = if (exportDestination == ExportDestination.LOCAL) ButtonTone.PRIMARY else ButtonTone.NEUTRAL,
                                            onClick = { viewModel.setExportDestination(ExportDestination.LOCAL) }
                                        )
                                        TouchButton(
                                            text = stringResource(R.string.config_backup_destination_cloud),
                                            tone = if (exportDestination == ExportDestination.CLOUD) ButtonTone.PRIMARY else ButtonTone.NEUTRAL,
                                            enabled = isCloudAvailable,
                                            onClick = { viewModel.setExportDestination(ExportDestination.CLOUD) }
                                        )
                                    }
                                    if (!isCloudAvailable) {
                                        OperatorText("Nessun provider cloud (Drive, OneDrive, Dropbox) rilevato. L'esportazione avverra' solo in locale.")
                                    }
                                    Text(
                                        text = if (exportDestination == ExportDestination.LOCAL)
                                            stringResource(R.string.config_backup_destination_local_desc)
                                        else
                                            stringResource(R.string.config_backup_destination_cloud_desc),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    OperatorText(stringResource(R.string.config_backup_full_zip_includes))
                                    OperatorText(stringResource(R.string.config_backup_android_archive_note))

                                    ActionRow {
                                        TouchButton(
                                            text = stringResource(R.string.backup_run_full_zip),
                                            testTag = RepairAppTestTags.ConfigBackupRunZipButton,
                                            onClick = {
                                                val destMsg = if (exportDestination == ExportDestination.LOCAL) destMsgLocal else destMsgCloud
                                                requestConfirm(confirmExportZipTitle, confirmExportZipTextTemplate.format(destMsg)) {
                                                    exportZipLauncher.launch("onlytechtake-backup.zip")
                                                }
                                            },
                                        )
                                        TouchButton(
                                            text = stringResource(R.string.config_backup_restore_zip),
                                            tone = ButtonTone.WARNING,
                                            onClick = {
                                                requestConfirm(
                                                    title = confirmRestoreZipTitle,
                                                    text = confirmRestoreZipText,
                                                ) {
                                                    restoreZipLauncher.launch(arrayOf("application/zip", "application/octet-stream", "*/*"))
                                                }
                                            },
                                        )
                                    }

                                    SectionTitle(stringResource(R.string.config_backup_orders_export_title))
                                    Text(stringResource(R.string.config_backup_orders_export_desc), style = MaterialTheme.typography.bodyMedium)
                                    ActionRow {
                                        TouchButton(
                                            text = stringResource(R.string.config_backup_export_xlsx),
                                            onClick = {
                                                val destMsg = if (exportDestination == ExportDestination.LOCAL) destMsgLocal else destMsgCloud
                                                requestConfirm(confirmExportXlsxTitle, confirmExportXlsxTextTemplate.format(destMsg)) {
                                                    exportOrdersXlsxLauncher.launch("onlytechtake-commesse.xlsx")
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
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        confirmTitle = null
                        confirmAction = null
                    },
                ) {
                    Text(stringResource(R.string.customer_cancel))
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
    StandardCard(
        modifier = testTag?.let { Modifier.testTag(it) } ?: Modifier,
        onClick = if (enabled) onClick else null,
    ) {
        Column(
            modifier = Modifier.padding(DashboardSpacing.cardPadding),
            verticalArrangement = Arrangement.spacedBy(DashboardSpacing.compactGap),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun backupReminderInfo(settings: BackupReminderSettings): String {
    val status = when {
        settings.lastBackupStatus == BackupReminderStatus.FAILED ->
            stringResource(R.string.backup_status_failed)
        settings.lastSuccessfulBackupAtMillis == null ->
            stringResource(R.string.backup_status_never)
        else -> {
            val days = BackupReminderPolicy.daysSinceLastSuccessfulBackup(settings, System.currentTimeMillis()) ?: 0L
            stringResource(R.string.backup_status_days_ago, days)
        }
    }
    val lastSuccessful = settings.lastSuccessfulBackupAtMillis
        ?.let { formatUiDateTime(it) }
        ?: stringResource(R.string.config_backup_last_success_none)
    val fileName = settings.lastBackupFileName
        ?: stringResource(R.string.config_backup_last_file_none)
    return stringResource(
        R.string.config_backup_reminder_info,
        status,
        lastSuccessful,
        settings.intervalDays,
        fileName,
    )
}

fun colorForLevel(level: Int): Color = when (level) {
    0 -> Color(0xFF1976D2)
    1 -> Color(0xFF388E3C)
    2 -> Color(0xFFF57C00)
    else -> Color(0xFF7B1FA2)
}

@Composable
private fun LevelDot(level: Int) {
    Box(
        modifier = Modifier
            .size(12.dp)
            .background(colorForLevel(level), CircleShape)
    )
}

@Composable
private fun CategoryBreadcrumb(categories: List<OrderCategoryEntity>, parentId: String?) {
    val byId = categories.associateBy { it.id }
    val nodes = mutableListOf<OrderCategoryEntity>()
    var current = parentId?.let(byId::get)
    while (current != null) {
        nodes.add(0, current)
        current = current.parentId?.let(byId::get)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Text("Radice", style = MaterialTheme.typography.bodySmall)
        nodes.forEach { node ->
            Text(">", style = MaterialTheme.typography.bodySmall)
            LevelDot(node.level)
            Text(node.name, style = MaterialTheme.typography.bodySmall)
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

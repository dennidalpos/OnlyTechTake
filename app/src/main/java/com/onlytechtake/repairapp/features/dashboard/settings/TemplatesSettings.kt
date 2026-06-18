package com.onlytechtake.repairapp.features.dashboard.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.core.design.RepairAppTestTags
import com.onlytechtake.repairapp.core.design.RepairAppShapes
import com.onlytechtake.repairapp.core.design.RepairAppSpacing
import com.onlytechtake.repairapp.core.design.components.*
import com.onlytechtake.repairapp.core.templates.TemplateAnchor
import com.onlytechtake.repairapp.data.local.entities.MessageChannel
import com.onlytechtake.repairapp.data.local.entities.MessageTemplateEntity
import com.onlytechtake.repairapp.features.dashboard.common.ActionRow
import com.onlytechtake.repairapp.features.dashboard.common.SectionTitle

@Composable
fun ColumnScope.TemplatesSettings(
    viewModel: SettingsViewModel,
    isAdmin: Boolean,
    onRequestConfirm: (String, String, () -> Unit) -> Unit,
    onBack: () -> Unit,
) {
    val templates by viewModel.templates.collectAsState()
    val anchorMappings by viewModel.anchorMappings.collectAsState()
    var editingTemplate by remember { mutableStateOf<MessageTemplateEntity?>(null) }
    var selectingAnchorForTemplate by remember { mutableStateOf<TemplateAnchor?>(null) }
    var searchQuery by rememberSaveable { mutableStateOf("") }

    val confirmTitle = stringResource(R.string.config_template_confirm_save_title)
    val confirmRisky = stringResource(R.string.config_template_confirm_save_risky)
    val confirmNewVersion = stringResource(R.string.config_template_confirm_save_new_version)
    val resetTitle = stringResource(R.string.config_template_reset_confirm_title)
    val resetText = stringResource(R.string.config_template_reset_confirm_text)
    val usedTemplateCodes = remember(anchorMappings) { anchorMappings.values.toSet() }

    val filteredTemplates = remember(templates, searchQuery) {
        if (searchQuery.isBlank()) templates
        else templates.filter { 
            it.title.contains(searchQuery, ignoreCase = true) || 
            it.code.contains(searchQuery, ignoreCase = true) 
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .widthIn(max = RepairAppSpacing.maxContentWidthMedium)
            .testTag("templates_list"),
        contentPadding = PaddingValues(RepairAppSpacing.screenHorizontal),
        verticalArrangement = Arrangement.spacedBy(RepairAppSpacing.cardGap),
    ) {
        item {
            SectionTitle(
                text = stringResource(R.string.config_hub_templates_title),
                tooltip = stringResource(R.string.config_template_tooltip)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(RepairAppSpacing.compactGap)
            ) {
                AppTextField(
                    modifier = Modifier.weight(1f),
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = stringResource(R.string.config_category_search_label), // Reuse search label
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = if (searchQuery.isNotEmpty()) {
                        {
                            AppIconButton(
                                imageVector = Icons.Default.Clear,
                                contentDescription = stringResource(R.string.content_description_clear),
                                onClick = { searchQuery = "" }
                            )
                        }
                    } else null,
                )

                if (isAdmin) {
                    Surface(
                        onClick = {
                            editingTemplate = MessageTemplateEntity(
                                id = "",
                                code = "",
                                title = "",
                                channel = MessageChannel.WHATSAPP,
                                body = "",
                                active = true,
                                version = 1,
                                updatedByUserId = null,
                                updatedAt = 0L,
                                step = null
                            )
                        },
                        color = MaterialTheme.colorScheme.primary,
                        shape = RepairAppShapes.button,
                        modifier = Modifier
                            .height(56.dp)
                            .width(56.dp),
                        tonalElevation = 0.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = stringResource(R.string.config_template_new),
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
            }
        }

        if (isAdmin) {
            item {
                SectionTitle(
                    text = stringResource(R.string.config_template_association_title),
                    tooltip = stringResource(R.string.config_template_association_tooltip)
                )
            }

            items(TemplateAnchor.entries) { anchor ->
                val assignedCode = anchorMappings[anchor]
                val assignedTemplate = templates.find { it.code == assignedCode }
                
                StandardCard(
                    onClick = { selectingAnchorForTemplate = anchor },
                    highlight = assignedTemplate?.active == true,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(RepairAppSpacing.cardPadding),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            AnchorStepHeader(anchor)
                            Spacer(modifier = Modifier.height(RepairAppSpacing.compactGap))
                            Text(anchor.label, style = MaterialTheme.typography.titleSmall)
                            Text(anchor.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                            Spacer(modifier = Modifier.height(RepairAppSpacing.compactGap))
                            Text(
                                text = stringResource(
                                    R.string.config_template_assigned_template,
                                    assignedTemplate?.title ?: stringResource(R.string.config_template_default_label, anchor.defaultCode),
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = RepairAppSpacing.screenHorizontal))
            }

            item {
                AppButton(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(R.string.config_template_reset_defaults),
                    variant = AppButtonVariant.Secondary,
                    onClick = {
                        onRequestConfirm(resetTitle, resetText) {
                            viewModel.resetTemplatesToDefaults()
                        }
                    }
                )
            }
        }

        items(filteredTemplates) { template ->
            val isUsed = template.code in usedTemplateCodes
            StandardCard(
                onClick = { editingTemplate = template },
                highlight = template.active && isUsed,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(RepairAppSpacing.cardPadding),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(RepairAppSpacing.compactGap)) {
                        Column(verticalArrangement = Arrangement.spacedBy(RepairAppSpacing.compactGap)) {
                            AppBadge(
                                text = template.step ?: stringResource(R.string.config_template_general),
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                            AppBadge(
                                text = if (template.active) {
                                    stringResource(R.string.config_template_active)
                                } else {
                                    stringResource(R.string.config_template_archived)
                                },
                                containerColor = if (template.active) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                },
                                contentColor = if (template.active) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                            if (!isUsed) {
                                AppBadge(
                                    text = stringResource(R.string.config_template_unassigned_label),
                                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                                )
                            }
                        }
                        Text(template.title, style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = template.body,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = stringResource(R.string.content_description_edit),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = RepairAppSpacing.compactGap)
                    )
                }
            }
        }

        if (templates.isEmpty()) {
            item {
                Text(stringResource(R.string.config_template_empty), style = MaterialTheme.typography.bodyMedium)
            }
        }

        item {
            AppButton(
                modifier = Modifier.fillMaxWidth().padding(top = RepairAppSpacing.screenHorizontal),
                text = stringResource(R.string.config_template_close),
                variant = AppButtonVariant.Secondary,
                size = AppButtonSize.Compact,
                onClick = onBack
            )
        }
    }

    editingTemplate?.let { template ->
        val assignedAnchors = TemplateAnchor.entries.filter { anchorMappings[it] == template.code }
        val associatedWarning = if (assignedAnchors.isNotEmpty()) {
            stringResource(R.string.config_template_associated_warning, assignedAnchors.joinToString { it.label })
        } else ""

        TemplateEditDialog(
            template = template,
            onDismiss = { 
                editingTemplate = null
                viewModel.setFormDirty(false)
            },
            onSave = { code, title, body, active, step ->
                val riskyTemplateChange = isRiskyTemplateChange(template.code, code, active) || (assignedAnchors.isNotEmpty() && !active)
                
                val warningText = if (assignedAnchors.isNotEmpty() && !active) {
                    associatedWarning
                } else if (riskyTemplateChange) {
                    confirmRisky
                } else {
                    confirmNewVersion
                }

                onRequestConfirm(
                    confirmTitle,
                    warningText,
                ) {
                    viewModel.saveTemplate(code, title, body, active, step)
                    editingTemplate = null
                }
            },
            onSetDirty = { viewModel.setFormDirty(it) }
        )
    }

    selectingAnchorForTemplate?.let { anchor ->
        AnchorSelectionDialog(
            anchor = anchor,
            templates = templates.filter { it.active },
            currentMapping = anchorMappings[anchor],
            onDismiss = { selectingAnchorForTemplate = null },
            onSelect = { templateCode ->
                viewModel.setTemplateForAnchor(anchor, templateCode)
                selectingAnchorForTemplate = null
            }
        )
    }
}

@Composable
private fun AnchorStepHeader(anchor: TemplateAnchor) {
    val stepLabel = if (anchor.stepNumber == 0) {
        stringResource(R.string.config_template_legal_step)
    } else {
        stringResource(R.string.config_template_step_prefix, anchor.stepNumber)
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(RepairAppSpacing.compactGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppBadge(
            text = stepLabel,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        Text(
            text = anchor.stepName,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun AnchorSelectionDialog(
    anchor: TemplateAnchor,
    templates: List<MessageTemplateEntity>,
    currentMapping: String?,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(RepairAppSpacing.screenHorizontal)
                    .fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.config_template_associate_with, anchor.label),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(RepairAppSpacing.cardGap))
                
                LazyColumn(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(RepairAppSpacing.compactGap)
                ) {
                    item {
                        StandardCard(
                            onClick = { onSelect(anchor.defaultCode) },
                            highlight = currentMapping == null || currentMapping == anchor.defaultCode
                        ) {
                            Text(
                                text = stringResource(R.string.config_template_default_label, anchor.defaultCode),
                                modifier = Modifier.padding(RepairAppSpacing.cardPadding),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    
                    items(templates) { template ->
                        StandardCard(
                            onClick = { onSelect(template.code) },
                            highlight = currentMapping == template.code
                        ) {
                            Column(modifier = Modifier.padding(RepairAppSpacing.cardPadding)) {
                                Text(template.title, style = MaterialTheme.typography.bodyMedium)
                                Text(template.code, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(RepairAppSpacing.cardGap))
                AppButton(
                    text = stringResource(R.string.customer_cancel),
                    onClick = onDismiss,
                    variant = AppButtonVariant.Secondary,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun TemplateEditDialog(
    template: MessageTemplateEntity,
    onDismiss: () -> Unit,
    onSave: (String, String, String, Boolean, String?) -> Unit,
    onSetDirty: (Boolean) -> Unit = {},
) {
    val isNew = template.id.isEmpty()
    var code by rememberSaveable { mutableStateOf(template.code) }
    var title by rememberSaveable { mutableStateOf(template.title) }
    var body by rememberSaveable { mutableStateOf(template.body) }
    var active by rememberSaveable { mutableStateOf(template.active) }
    var step by rememberSaveable { mutableStateOf(template.step ?: "") }

    Dialog(
        onDismissRequest = {
            onDismiss()
            onSetDirty(false)
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.95f)
                .widthIn(max = RepairAppSpacing.maxContentWidthMedium),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(RepairAppSpacing.screenHorizontal),
            ) {
                Text(
                    text = stringResource(R.string.config_template_edit_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                if (isNew) {
                    AppTextField(
                        value = code,
                        onValueChange = { 
                            code = it
                            onSetDirty(true)
                        },
                        label = stringResource(R.string.config_template_code_label),
                        testTag = RepairAppTestTags.ConfigTemplateCodeField
                    )
                } else {
                    Text(
                        text = stringResource(R.string.config_template_code_value, template.code),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = RepairAppSpacing.compactGap))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(RepairAppSpacing.fieldGap)
                ) {
                    AppTextField(
                        value = title,
                        onValueChange = { 
                            title = it
                            onSetDirty(true)
                        },
                        label = stringResource(R.string.config_template_title_label),
                        testTag = RepairAppTestTags.ConfigTemplateTitleField
                    )

                    AppTextField(
                        value = step,
                        onValueChange = { 
                            step = it
                            onSetDirty(true)
                        },
                        label = stringResource(R.string.config_template_step_label),
                    )

                    AppTextField(
                        value = body,
                        onValueChange = { 
                            body = it
                            onSetDirty(true)
                        },
                        label = stringResource(R.string.config_template_body_label),
                        testTag = RepairAppTestTags.ConfigTemplateBodyField,
                        singleLine = false
                    )

                    SectionTitle(
                        text = stringResource(R.string.config_template_variables),
                        tooltip = stringResource(R.string.config_template_variables_tooltip)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(if (active) stringResource(R.string.config_template_active) else stringResource(R.string.config_template_archived))
                        Switch(
                            checked = active,
                            onCheckedChange = { 
                                active = it
                                onSetDirty(true)
                            },
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(RepairAppSpacing.compactGap))
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = RepairAppSpacing.compactGap))

                ActionRow {
                    AppButton(
                        text = stringResource(R.string.customer_cancel),
                        onClick = {
                            onDismiss()
                            onSetDirty(false)
                        },
                        variant = AppButtonVariant.Secondary,
                        size = AppButtonSize.Compact
                    )
                    AppButton(
                        text = stringResource(R.string.config_template_save),
                        enabled = title.isNotBlank() && body.isNotBlank() && (!isNew || code.isNotBlank()),
                        onClick = {
                            onSave(if (isNew) code else template.code, title, body, active, step.ifBlank { null })
                        },
                        variant = AppButtonVariant.Primary,
                        size = AppButtonSize.Compact,
                        testTag = RepairAppTestTags.ConfigTemplateSaveButton
                    )
                }
            }
        }
    }
}

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
    "whatsapp_stato_avanzamento_pdf",
)

private fun isRiskyTemplateChange(loadedCode: String?, currentCode: String, active: Boolean): Boolean =
    (loadedCode != null && loadedCode != currentCode) || (currentCode in systemTemplateCodes && !active)


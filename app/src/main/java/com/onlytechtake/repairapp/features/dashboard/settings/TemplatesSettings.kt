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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import com.onlytechtake.repairapp.core.design.RepairAppShapes
import com.onlytechtake.repairapp.core.design.RepairAppSpacing
import com.onlytechtake.repairapp.core.design.RepairAppTestTags
import com.onlytechtake.repairapp.core.design.components.AppBadge
import com.onlytechtake.repairapp.core.design.components.AppButton
import com.onlytechtake.repairapp.core.design.components.AppButtonSize
import com.onlytechtake.repairapp.core.design.components.AppButtonVariant
import com.onlytechtake.repairapp.core.design.components.AppIconButton
import com.onlytechtake.repairapp.core.design.components.AppTextField
import com.onlytechtake.repairapp.core.design.components.StandardCard
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
    var searchQuery by rememberSaveable { mutableStateOf("") }

    val confirmTitle = stringResource(R.string.config_template_confirm_save_title)
    val confirmRisky = stringResource(R.string.config_template_confirm_save_risky)
    val confirmNewVersion = stringResource(R.string.config_template_confirm_save_new_version)
    val confirmResetTitle = stringResource(R.string.config_template_reset_confirm_title)
    val confirmResetText = stringResource(R.string.config_template_reset_confirm_text)
    val usedTemplateCodes = remember(anchorMappings) { anchorMappings.values.toSet() }

    val filteredTemplates =
        remember(templates, searchQuery) {
            if (searchQuery.isBlank()) {
                templates
            } else {
                templates.filter {
                    it.title.contains(searchQuery, ignoreCase = true) ||
                        it.code.contains(searchQuery, ignoreCase = true)
                }
            }
        }

    LazyColumn(
        modifier =
            Modifier
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
                tooltip = stringResource(R.string.config_template_tooltip),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(RepairAppSpacing.compactGap),
            ) {
                AppTextField(
                    modifier = Modifier.weight(1f),
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = stringResource(R.string.config_category_search_label), // Reuse search label
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon =
                        if (searchQuery.isNotEmpty()) {
                            {
                                AppIconButton(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = stringResource(R.string.content_description_clear),
                                    onClick = { searchQuery = "" },
                                )
                            }
                        } else {
                            null
                        },
                )

                if (isAdmin) {
                    Surface(
                        onClick = {
                            editingTemplate =
                                MessageTemplateEntity(
                                    id = "",
                                    code = "",
                                    title = "",
                                    channel = MessageChannel.WHATSAPP,
                                    body = "",
                                    active = true,
                                    version = 1,
                                    updatedByUserId = null,
                                    updatedAt = 0L,
                                    step = null,
                                )
                        },
                        color = MaterialTheme.colorScheme.primary,
                        shape = RepairAppShapes.button,
                        modifier =
                            Modifier
                                .height(56.dp)
                                .width(56.dp)
                                .testTag(RepairAppTestTags.ConfigTemplateNewButton),
                        tonalElevation = 0.dp,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = stringResource(R.string.config_template_new),
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(32.dp),
                            )
                        }
                    }
                }
            }
        }

        items(filteredTemplates, key = { it.id }) { template ->
            val isUsed = template.code in usedTemplateCodes
            StandardCard(
                onClick = { editingTemplate = template },
                highlight = template.active && isUsed,
            ) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(RepairAppSpacing.cardPadding),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(RepairAppSpacing.compactGap),
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(RepairAppSpacing.compactGap)) {
                            AppBadge(
                                text =
                                    template.step
                                        ?: stringResource(R.string.config_template_general),
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (template.active) Icons.Default.Check else Icons.Default.Clear,
                                    contentDescription = null,
                                    tint = if (template.active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(modifier = Modifier.width(RepairAppSpacing.compactGap))
                                AppBadge(
                                    text =
                                        if (template.active) {
                                            stringResource(R.string.config_template_active)
                                        } else {
                                            stringResource(R.string.config_template_archived)
                                        },
                                    containerColor =
                                        if (template.active) {
                                            MaterialTheme.colorScheme.primaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.surfaceVariant
                                        },
                                    contentColor =
                                        if (template.active) {
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (isUsed) Icons.Default.Link else Icons.Default.RadioButtonUnchecked,
                                    contentDescription = null,
                                    tint = if (isUsed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(modifier = Modifier.width(RepairAppSpacing.compactGap))
                                AppBadge(
                                    text = if (isUsed) stringResource(R.string.config_template_status_associated) else stringResource(
                                        R.string.config_template_status_free
                                    ),
                                    containerColor = if (isUsed) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.tertiaryContainer,
                                    contentColor = if (isUsed) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onTertiaryContainer,
                                )
                            }
                        }
                        Text(template.title, style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = template.body,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = stringResource(R.string.content_description_edit),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = RepairAppSpacing.compactGap),
                    )
                }
            }
        }

        if (templates.isEmpty()) {
            item {
                Text(
                    stringResource(R.string.config_template_empty),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(RepairAppSpacing.compactGap),
            ) {
                if (isAdmin) {
                    AppButton(
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(R.string.config_template_reset_defaults),
                        variant = AppButtonVariant.Danger,
                        size = AppButtonSize.Compact,
                        onClick = {
                            onRequestConfirm(
                                confirmResetTitle,
                                confirmResetText,
                            ) {
                                viewModel.resetTemplatesToDefaults()
                            }
                        },
                    )
                }

                AppButton(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(R.string.config_template_close),
                    variant = AppButtonVariant.Secondary,
                    size = AppButtonSize.Compact,
                    onClick = onBack,
                )
            }
        }
    }

    editingTemplate?.let { template ->
        val assignedAnchors = TemplateAnchor.entries.filter { anchorMappings[it] == template.code }
        val associatedWarning =
            if (assignedAnchors.isNotEmpty()) {
                val anchorLabels =
                    buildString {
                        assignedAnchors.forEachIndexed { index, anchor ->
                            append(stringResource(anchor.labelRes))
                            if (index < (assignedAnchors.size - 1)) append(", ")
                        }
                    }
                stringResource(
                    R.string.config_template_associated_warning,
                    anchorLabels,
                )
            } else {
                ""
            }

        TemplateEditDialog(
            template = template,
            onDismiss = {
                editingTemplate = null
                viewModel.setFormDirty(dirty = false)
            },
            onSave = { code, title, body, active, step ->
                val riskyTemplateChange =
                    isRiskyTemplateChange(
                        template.code,
                        code,
                        active,
                    ) ||
                        (assignedAnchors.isNotEmpty() && !active)

                val warningText =
                    if (assignedAnchors.isNotEmpty() && !active) {
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
        ) { viewModel.setFormDirty(it) }
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
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier =
                Modifier
                    .fillMaxWidth(0.95f)
                    .fillMaxHeight(0.95f)
                    .widthIn(max = RepairAppSpacing.maxContentWidthMedium),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(RepairAppSpacing.screenHorizontal),
            ) {
                Text(
                    text = stringResource(R.string.config_template_edit_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )

                if (isNew) {
                    AppTextField(
                        value = code,
                        onValueChange = {
                            code = it
                            onSetDirty(true)
                        },
                        label = stringResource(R.string.config_template_code_label),
                        testTag = RepairAppTestTags.ConfigTemplateCodeField,
                    )
                } else {
                    Text(
                        text = stringResource(R.string.config_template_code_value, template.code),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = RepairAppSpacing.compactGap))

                Column(
                    modifier =
                        Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(RepairAppSpacing.fieldGap),
                ) {
                    AppTextField(
                        value = title,
                        onValueChange = {
                            title = it
                            onSetDirty(true)
                        },
                        label = stringResource(R.string.config_template_title_label),
                        testTag = RepairAppTestTags.ConfigTemplateTitleField,
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
                        singleLine = false,
                    )

                    SectionTitle(
                        text = stringResource(R.string.config_template_variables),
                        tooltip = stringResource(R.string.config_template_variables_tooltip),
                        tooltipTestTag = RepairAppTestTags.ConfigTemplateVariablesTooltip,
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            if (active) {
                                stringResource(R.string.config_template_active)
                            } else {
                                stringResource(
                                    R.string.config_template_archived,
                                )
                            },
                        )
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
                        size = AppButtonSize.Compact,
                    )
                    AppButton(
                        text = stringResource(R.string.config_template_save),
                        enabled = title.isNotBlank() && body.isNotBlank() && (!isNew || code.isNotBlank()),
                        onClick = {
                            onSave(
                                if (isNew) code else template.code,
                                title,
                                body,
                                active,
                                step.ifBlank { null },
                            )
                        },
                        variant = AppButtonVariant.Primary,
                        size = AppButtonSize.Compact,
                        testTag = RepairAppTestTags.ConfigTemplateSaveButton,
                    )
                }
            }
        }
    }
}

private val systemTemplateCodes =
    setOf(
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

private fun isRiskyTemplateChange(
    loadedCode: String?,
    currentCode: String,
    active: Boolean,
): Boolean =
    ((loadedCode != null) && (loadedCode != currentCode)) || ((currentCode in systemTemplateCodes) && !active)

package com.onlytechtake.repairapp.features.dashboard.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.core.design.RepairAppTestTags
import com.onlytechtake.repairapp.data.local.entities.MessageTemplateEntity
import com.onlytechtake.repairapp.features.dashboard.common.ActionRow
import com.onlytechtake.repairapp.features.dashboard.common.DashboardSpacing
import com.onlytechtake.repairapp.features.dashboard.common.Field
import com.onlytechtake.repairapp.features.dashboard.common.OperatorText
import com.onlytechtake.repairapp.features.dashboard.common.SectionTitle
import com.onlytechtake.repairapp.features.dashboard.common.StandardCard
import com.onlytechtake.repairapp.features.dashboard.common.TouchButton
import com.onlytechtake.repairapp.features.workspace.ButtonTone

@Composable
fun TemplatesSettings(
    viewModel: SettingsViewModel,
    isAdmin: Boolean,
    onRequestConfirm: (String, String, () -> Unit) -> Unit,
) {
    val templates by viewModel.templates.collectAsState()
    var editingTemplate by remember { mutableStateOf<MessageTemplateEntity?>(null) }

    val confirmTitle = stringResource(R.string.config_template_confirm_save_title)
    val confirmRisky = stringResource(R.string.config_template_confirm_save_risky)
    val confirmNewVersion = stringResource(R.string.config_template_confirm_save_new_version)

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = DashboardSpacing.maxContentWidth),
        contentPadding = PaddingValues(DashboardSpacing.screenPadding),
        verticalArrangement = Arrangement.spacedBy(DashboardSpacing.listGap),
    ) {
        item {
            SectionTitle(stringResource(R.string.config_hub_templates_title))
        }

        items(templates) { template ->
            StandardCard(
                onClick = { editingTemplate = template },
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(DashboardSpacing.cardPadding),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(DashboardSpacing.compactGap)) {
                        Text(template.title, style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = (template.step ?: "Generale") + " • " + if (template.active) stringResource(R.string.config_template_active) else stringResource(R.string.config_template_archived),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (template.active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                    }
                    Text(
                        text = "v${template.version}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }

        if (templates.isEmpty()) {
            item {
                Text(stringResource(R.string.config_template_empty), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }

    editingTemplate?.let { template ->
        TemplateEditDialog(
            template = template,
            onDismiss = { editingTemplate = null },
            onSave = { code, title, body, active, step ->
                val riskyTemplateChange = isRiskyTemplateChange(template.code, code, active)
                
                onRequestConfirm(
                    confirmTitle,
                    if (riskyTemplateChange) confirmRisky else confirmNewVersion,
                ) {
                    viewModel.saveTemplate(code, title, body, active, step)
                    editingTemplate = null
                }
            }
        )
    }
}

@Composable
fun TemplateEditDialog(
    template: MessageTemplateEntity,
    onDismiss: () -> Unit,
    onSave: (String, String, String, Boolean, String?) -> Unit,
) {
    var title by rememberSaveable { mutableStateOf(template.title) }
    var body by rememberSaveable { mutableStateOf(template.body) }
    var active by rememberSaveable { mutableStateOf(template.active) }
    var step by rememberSaveable { mutableStateOf(template.step ?: "") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .widthIn(max = DashboardSpacing.maxContentWidth),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = DashboardSpacing.dialogElevation
        ) {
            Column(
                modifier = Modifier
                    .padding(DashboardSpacing.screenPadding),
                verticalArrangement = Arrangement.spacedBy(DashboardSpacing.formGap)
            ) {
                Text(
                    text = "Modifica Template",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = "Codice: ${template.code}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = DashboardSpacing.compactGap))

                Field(
                    value = title,
                    onValueChange = { title = it },
                    label = stringResource(R.string.config_template_title_label),
                    testTag = RepairAppTestTags.ConfigTemplateTitleField
                )

                Field(
                    value = step,
                    onValueChange = { step = it },
                    label = "Step associato",
                )

                Field(
                    value = body,
                    onValueChange = { body = it },
                    label = stringResource(R.string.config_template_body_label),
                    testTag = RepairAppTestTags.ConfigTemplateBodyField,
                    singleLine = false
                )

                Column(verticalArrangement = Arrangement.spacedBy(DashboardSpacing.compactGap)) {
                    Text(stringResource(R.string.config_template_variables), style = MaterialTheme.typography.titleSmall)
                    OperatorText("{{numero_commessa}}  {{stato_commessa}}  {{categoria_percorso}}  {{cliente}}")
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(if (active) stringResource(R.string.config_template_active) else stringResource(R.string.config_template_archived))
                    Switch(
                        checked = active,
                        onCheckedChange = { active = it },
                    )
                }

                ActionRow {
                    TouchButton(
                        text = "Annulla",
                        tone = ButtonTone.NEUTRAL,
                        onClick = onDismiss
                    )
                    TouchButton(
                        text = stringResource(R.string.config_template_save),
                        testTag = RepairAppTestTags.ConfigTemplateSaveButton,
                        onClick = {
                            onSave(template.code, title, body, active, step.ifBlank { null })
                        },
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
)

private fun isRiskyTemplateChange(loadedCode: String?, currentCode: String, active: Boolean): Boolean =
    (loadedCode != null && loadedCode != currentCode) || (currentCode in systemTemplateCodes && !active)

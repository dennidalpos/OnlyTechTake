package com.onlytechtake.repairapp.features.dashboard.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.core.design.RepairAppTestTags
import com.onlytechtake.repairapp.data.local.entities.MessageTemplateEntity
import com.onlytechtake.repairapp.features.dashboard.common.ActionRow
import com.onlytechtake.repairapp.features.dashboard.common.DashboardSpacing
import com.onlytechtake.repairapp.features.dashboard.common.Field
import com.onlytechtake.repairapp.features.dashboard.common.SectionTitle
import com.onlytechtake.repairapp.features.dashboard.common.StandardCard
import com.onlytechtake.repairapp.features.dashboard.common.TouchButton
import com.onlytechtake.repairapp.features.workspace.ButtonTone

@Composable
fun TemplatesSettings(
    viewModel: SettingsViewModel,
    isAdmin: Boolean,
    onRequestConfirm: (String, String, () -> Unit) -> Unit,
    onBack: () -> Unit,
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
            SectionTitle(
                text = stringResource(R.string.config_hub_templates_title),
                tooltip = "Personalizza i messaggi predefiniti per ogni fase della lavorazione e le informative legali."
            )
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
                        contentDescription = "Modifica",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 8.dp)
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
            TouchButton(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                text = "Chiudi Setup Template",
                tone = ButtonTone.NEUTRAL,
                onClick = onBack
            )
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
                .fillMaxHeight(0.95f)
                .widthIn(max = DashboardSpacing.maxContentWidth),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = DashboardSpacing.dialogElevation
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(DashboardSpacing.screenPadding),
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

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(DashboardSpacing.formGap)
                ) {
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

                    SectionTitle(
                        text = stringResource(R.string.config_template_variables),
                        tooltip = "{{numero_commessa}}, {{stato_commessa}}, {{categoria_percorso}}, {{cliente}}, {{esito_diagnosi}}, {{importo_preventivo}}"
                    )

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
                    
                    Spacer(modifier = Modifier.height(8.dp))
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = DashboardSpacing.compactGap))

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

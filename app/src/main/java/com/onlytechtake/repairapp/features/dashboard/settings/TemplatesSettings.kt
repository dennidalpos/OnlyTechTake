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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.core.design.RepairAppTestTags
import com.onlytechtake.repairapp.features.dashboard.common.ActionRow
import com.onlytechtake.repairapp.features.dashboard.common.DashboardSpacing
import com.onlytechtake.repairapp.features.dashboard.common.Field
import com.onlytechtake.repairapp.features.dashboard.common.OperatorText
import com.onlytechtake.repairapp.features.dashboard.common.SectionTitle
import com.onlytechtake.repairapp.features.dashboard.common.StandardCard
import com.onlytechtake.repairapp.features.dashboard.common.TouchButton
import com.onlytechtake.repairapp.features.workspace.WorkspaceViewModel

@Composable
fun TemplatesSettings(
    viewModel: WorkspaceViewModel,
    isAdmin: Boolean,
    onRequestConfirm: (String, String, () -> Unit) -> Unit,
) {
    val templates by viewModel.templates.collectAsState()

    var templateCode by rememberSaveable { mutableStateOf("whatsapp_aggiornamento") }
    var loadedTemplateCode by rememberSaveable { mutableStateOf<String?>(null) }
    var templateTitle by rememberSaveable { mutableStateOf("WhatsApp aggiornamento") }
    var templateBody by rememberSaveable { mutableStateOf("Aggiornamento {{numero_commessa}}: {{stato_commessa}}") }
    var templateActive by rememberSaveable { mutableStateOf(value = true) }

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = DashboardSpacing.maxContentWidth),
        contentPadding = PaddingValues(DashboardSpacing.screenPadding),
        verticalArrangement = Arrangement.spacedBy(DashboardSpacing.listGap),
    ) {
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
                        val confirmTitle = stringResource(R.string.config_template_confirm_save_title)
                        val confirmRisky = stringResource(R.string.config_template_confirm_save_risky)
                        val confirmNewVersion = stringResource(R.string.config_template_confirm_save_new_version)
                        
                        TouchButton(
                            text = stringResource(R.string.config_template_save),
                            testTag = RepairAppTestTags.ConfigTemplateSaveButton,
                            onClick = {
                                val riskyTemplateChange = isRiskyTemplateChange(loadedTemplateCode, templateCode, templateActive)
                                onRequestConfirm(
                                    confirmTitle,
                                    if (riskyTemplateChange) confirmRisky else confirmNewVersion,
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

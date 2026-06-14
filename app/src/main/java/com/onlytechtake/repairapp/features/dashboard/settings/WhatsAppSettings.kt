package com.onlytechtake.repairapp.features.dashboard.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.data.local.entities.MessageMode
import com.onlytechtake.repairapp.features.dashboard.common.ActionRow
import com.onlytechtake.repairapp.features.dashboard.common.DashboardSpacing
import com.onlytechtake.repairapp.features.dashboard.common.SectionTitle
import com.onlytechtake.repairapp.features.dashboard.common.StandardCard
import com.onlytechtake.repairapp.features.dashboard.common.TouchButton

@Composable
fun WhatsAppSettings(
    viewModel: SettingsViewModel,
    onRequestConfirm: (String, String, () -> Unit) -> Unit,
    onBack: () -> Unit,
) {
    val messageMode by viewModel.messageMode.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = DashboardSpacing.maxContentWidth),
        contentPadding = PaddingValues(DashboardSpacing.screenPadding),
        verticalArrangement = Arrangement.spacedBy(DashboardSpacing.listGap),
    ) {
        item {
            StandardCard(highlight = true) {
                Column(modifier = Modifier.padding(DashboardSpacing.cardPadding), verticalArrangement = Arrangement.spacedBy(DashboardSpacing.formGap)) {
                    SectionTitle(
                        text = stringResource(R.string.config_hub_whatsapp_title),
                        tooltip = "Configura la modalità di invio dei messaggi ai clienti. Attualmente è supportato l'invio manuale tramite WhatsApp."
                    )
                    Text(stringResource(R.string.config_whatsapp_mode, "Manuale"))
                    ActionRow {
                        val confirmTitle = stringResource(R.string.config_whatsapp_confirm_manual_title)
                        val confirmText = stringResource(R.string.config_whatsapp_confirm_manual_text)
                        TouchButton(
                            text = stringResource(R.string.config_whatsapp_manual),
                            onClick = {
                                onRequestConfirm(confirmTitle, confirmText) {
                                    viewModel.setMessageMode(MessageMode.MANUAL_WHATSAPP)
                                }
                            },
                        )
                    }
                }
            }
        }

        item {
            TouchButton(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                text = "Chiudi Setup WhatsApp",
                tone = com.onlytechtake.repairapp.features.workspace.ButtonTone.NEUTRAL,
                onClick = onBack
            )
        }
    }
}

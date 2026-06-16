package com.onlytechtake.repairapp.features.dashboard.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import com.onlytechtake.repairapp.core.design.RepairAppSpacing
import com.onlytechtake.repairapp.core.design.components.StandardCard
import com.onlytechtake.repairapp.core.design.components.AppButton
import com.onlytechtake.repairapp.core.design.components.AppButtonVariant
import com.onlytechtake.repairapp.features.dashboard.common.ActionRow
import com.onlytechtake.repairapp.features.dashboard.common.SectionTitle

@Composable
fun ColumnScope.WhatsAppSettings(
    viewModel: SettingsViewModel,
    onRequestConfirm: (String, String, () -> Unit) -> Unit,
    onBack: () -> Unit,
) {
    val messageMode by viewModel.messageMode.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .widthIn(max = RepairAppSpacing.maxContentWidthMedium),
        contentPadding = PaddingValues(RepairAppSpacing.screenHorizontal),
        verticalArrangement = Arrangement.spacedBy(RepairAppSpacing.cardGap),
    ) {
        item {
            StandardCard(highlight = true) {
                Column(modifier = Modifier.padding(RepairAppSpacing.cardPadding), verticalArrangement = Arrangement.spacedBy(RepairAppSpacing.fieldGap)) {
                    SectionTitle(
                        text = stringResource(R.string.config_hub_whatsapp_title),
                        tooltip = stringResource(R.string.config_whatsapp_tooltip),
                    )
                    Text(stringResource(R.string.config_whatsapp_mode, stringResource(R.string.config_whatsapp_manual)))
                    ActionRow {
                        val confirmTitle = stringResource(R.string.config_whatsapp_confirm_manual_title)
                        val confirmText = stringResource(R.string.config_whatsapp_confirm_manual_text)
                        AppButton(
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
            AppButton(
                modifier = Modifier.fillMaxWidth().padding(top = RepairAppSpacing.screenHorizontal),
                text = stringResource(R.string.config_whatsapp_close),
                variant = AppButtonVariant.Secondary,
                onClick = onBack
            )
        }
    }
}



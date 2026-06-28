package com.onlytechtake.repairapp.features.dashboard.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.core.design.RepairAppSpacing
import com.onlytechtake.repairapp.core.design.components.AppButton
import com.onlytechtake.repairapp.core.design.components.AppButtonSize
import com.onlytechtake.repairapp.core.design.components.AppButtonVariant
import com.onlytechtake.repairapp.core.design.components.AppScreenContainer
import com.onlytechtake.repairapp.core.design.components.StandardCard
import com.onlytechtake.repairapp.features.dashboard.common.SectionTitle

@Composable
fun UiSettings(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
) {
    val uiSettings by viewModel.uiSettings.collectAsState()

    AppScreenContainer {
        SectionTitle(
            text = stringResource(R.string.config_hub_ui_title),
            tooltip = stringResource(R.string.config_ui_tooltip),
        )

        StandardCard {
            Column(
                modifier = Modifier.padding(RepairAppSpacing.cardPadding),
                verticalArrangement = Arrangement.spacedBy(RepairAppSpacing.fieldGap),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.config_ui_dark_theme),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Switch(
                        checked = uiSettings.darkTheme,
                        onCheckedChange = { viewModel.setDarkTheme(it) },
                    )
                }

                Spacer(Modifier.height(RepairAppSpacing.cardGap))
            }
        }

        Spacer(Modifier.height(RepairAppSpacing.sectionGap))

        AppButton(
            text = stringResource(R.string.config_ui_close),
            variant = AppButtonVariant.Ghost,
            size = AppButtonSize.Default,
            modifier = Modifier.fillMaxWidth(),
            onClick = onBack,
        )
    }
}

@Preview(showBackground = true, name = "Light Mode")
@Composable
private fun UiSettingsPreview() {
    MaterialTheme {
        UiSettings(
            viewModel =
                androidx.lifecycle.viewmodel.compose
                    .viewModel(),
        ) {}
    }
}

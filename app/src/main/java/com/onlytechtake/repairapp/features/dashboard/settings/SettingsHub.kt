package com.onlytechtake.repairapp.features.dashboard.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.core.design.RepairAppTestTags
import com.onlytechtake.repairapp.features.dashboard.common.DashboardSpacing
import com.onlytechtake.repairapp.features.dashboard.common.SectionTitle
import com.onlytechtake.repairapp.features.dashboard.common.StandardCard
import com.onlytechtake.repairapp.features.workspace.WorkspaceDestination
import com.onlytechtake.repairapp.features.workspace.WorkspaceViewModel

@Composable
fun SettingsHub(
    viewModel: WorkspaceViewModel,
    isAdmin: Boolean,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = DashboardSpacing.maxContentWidth),
        contentPadding = PaddingValues(DashboardSpacing.screenPadding),
        verticalArrangement = Arrangement.spacedBy(DashboardSpacing.listGap),
    ) {
        item {
            SectionTitle(stringResource(R.string.dashboard_config))
        }

        item {
            SettingsHubCard(
                title = stringResource(R.string.config_hub_categories_title),
                subtitle = stringResource(R.string.config_hub_categories_subtitle),
                testTag = RepairAppTestTags.ConfigHubCategoriesButton,
            ) {
                viewModel.navigateTo(WorkspaceDestination.SETTINGS_CATEGORIES)
            }
        }
        item {
            SettingsHubCard(
                title = stringResource(R.string.config_hub_templates_title),
                subtitle = if (isAdmin) stringResource(R.string.config_hub_templates_subtitle_admin) else stringResource(R.string.config_hub_templates_subtitle_user),
                enabled = isAdmin,
                testTag = RepairAppTestTags.ConfigHubTemplatesButton,
            ) {
                viewModel.navigateTo(WorkspaceDestination.SETTINGS_TEMPLATES)
            }
        }
        item {
            SettingsHubCard(
                stringResource(R.string.config_hub_whatsapp_title),
                stringResource(R.string.config_hub_whatsapp_subtitle)
            ) {
                viewModel.navigateTo(WorkspaceDestination.SETTINGS_WHATSAPP)
            }
        }
        item {
            SettingsHubCard(
                "Archiviazione",
                "Spazio disco e monitoraggio"
            ) {
                viewModel.navigateTo(WorkspaceDestination.SETTINGS_STORAGE)
            }
        }
        item {
            SettingsHubCard(
                stringResource(R.string.config_hub_admin_title),
                stringResource(R.string.config_hub_admin_subtitle)
            ) {
                viewModel.navigateTo(WorkspaceDestination.ADMIN)
            }
        }
        if (isAdmin) {
            item {
                SettingsHubCard(
                    stringResource(R.string.config_hub_backup_title),
                    stringResource(R.string.config_hub_backup_subtitle),
                    testTag = RepairAppTestTags.ConfigHubBackupButton,
                ) {
                    viewModel.navigateTo(WorkspaceDestination.SETTINGS_BACKUP)
                }
            }
        }
    }
}

@Composable
internal fun SettingsHubCard(
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

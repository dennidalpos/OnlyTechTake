package com.onlytechtake.repairapp.features.dashboard.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.core.design.RepairAppTestTags
import com.onlytechtake.repairapp.features.dashboard.common.DashboardSpacing
import com.onlytechtake.repairapp.features.dashboard.common.SectionTitle
import com.onlytechtake.repairapp.features.dashboard.common.StandardCard
import com.onlytechtake.repairapp.features.dashboard.DashboardRoutes

@Composable
fun SettingsHub(
    isAdmin: Boolean,
    onNavigate: (String) -> Unit,
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
                icon = Icons.AutoMirrored.Filled.List,
                testTag = RepairAppTestTags.ConfigHubCategoriesButton,
            ) {
                onNavigate(DashboardRoutes.settings("CATEGORIES"))
            }
        }
        item {
            SettingsHubCard(
                title = stringResource(R.string.config_hub_templates_title),
                subtitle = if (isAdmin) stringResource(R.string.config_hub_templates_subtitle_admin) else stringResource(R.string.config_hub_templates_subtitle_user),
                icon = Icons.Default.Description,
                enabled = isAdmin,
                testTag = RepairAppTestTags.ConfigHubTemplatesButton,
            ) {
                onNavigate(DashboardRoutes.settings("TEMPLATES"))
            }
        }
        item {
            SettingsHubCard(
                title = stringResource(R.string.config_hub_whatsapp_title),
                subtitle = stringResource(R.string.config_hub_whatsapp_subtitle),
                icon = Icons.AutoMirrored.Filled.Send,
            ) {
                onNavigate(DashboardRoutes.settings("WHATSAPP"))
            }
        }
        item {
            SettingsHubCard(
                title = stringResource(R.string.storage_title),
                subtitle = "Spazio disco e monitoraggio",
                icon = Icons.Default.Storage,
            ) {
                onNavigate(DashboardRoutes.settings("STORAGE"))
            }
        }
        item {
            SettingsHubCard(
                title = stringResource(R.string.config_hub_admin_title),
                subtitle = stringResource(R.string.config_hub_admin_subtitle),
                icon = Icons.Default.Lock,
            ) {
                onNavigate(DashboardRoutes.ADMIN)
            }
        }
        if (isAdmin) {
            item {
                SettingsHubCard(
                    title = stringResource(R.string.config_hub_backup_title),
                    subtitle = stringResource(R.string.config_hub_backup_subtitle),
                    icon = Icons.Default.Backup,
                    testTag = RepairAppTestTags.ConfigHubBackupButton,
                ) {
                    onNavigate(DashboardRoutes.settings("EXPORT"))
                }
            }
        }
        item {
            SettingsHubCard(
                title = "Informazioni",
                subtitle = "Versione e contatti dell'app",
                icon = Icons.Default.Info,
            ) {
                onNavigate(DashboardRoutes.settings("ABOUT"))
            }
        }
    }
}

@Composable
internal fun SettingsHubCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    enabled: Boolean = true,
    testTag: String? = null,
    onClick: () -> Unit,
) {
    StandardCard(
        modifier = testTag?.let { Modifier.testTag(it) } ?: Modifier,
        onClick = if (enabled) onClick else null,
    ) {
        Row(
            modifier = Modifier.padding(DashboardSpacing.cardPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

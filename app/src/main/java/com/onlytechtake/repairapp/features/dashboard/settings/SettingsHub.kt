package com.onlytechtake.repairapp.features.dashboard.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.core.design.RepairAppSpacing
import com.onlytechtake.repairapp.core.design.RepairAppTestTags
import com.onlytechtake.repairapp.core.design.components.AppScreenContainer
import com.onlytechtake.repairapp.core.design.components.StandardCard
import com.onlytechtake.repairapp.features.dashboard.DashboardRoutes
import com.onlytechtake.repairapp.features.dashboard.common.SectionTitle

@Composable
fun SettingsHub(
    isAdmin: Boolean,
    onNavigate: (String) -> Unit,
) {
    AppScreenContainer {
        SectionTitle(stringResource(R.string.dashboard_config))

        Spacer(Modifier.height(RepairAppSpacing.cardGap))

        Column(verticalArrangement = Arrangement.spacedBy(RepairAppSpacing.cardGap)) {
            SettingsHubCard(
                title = stringResource(R.string.config_hub_categories_title),
                subtitle = stringResource(R.string.config_hub_categories_subtitle),
                icon = Icons.AutoMirrored.Filled.List,
                testTag = RepairAppTestTags.ConfigHubCategoriesButton,
            ) {
                onNavigate(DashboardRoutes.settings("CATEGORIES"))
            }

            SettingsHubCard(
                title = stringResource(R.string.config_hub_templates_title),
                subtitle =
                    if (isAdmin) {
                        stringResource(R.string.config_hub_templates_subtitle_admin)
                    } else {
                        stringResource(
                            R.string.config_hub_templates_subtitle_user,
                        )
                    },
                icon = Icons.Default.Description,
                enabled = isAdmin,
                testTag = RepairAppTestTags.ConfigHubTemplatesButton,
            ) {
                onNavigate(DashboardRoutes.settings("TEMPLATES"))
            }

            SettingsHubCard(
                title = stringResource(R.string.config_hub_template_mapping_title),
                subtitle = stringResource(R.string.config_hub_template_mapping_subtitle),
                icon = Icons.Default.Lock, // Need a better icon
                enabled = isAdmin,
                testTag = RepairAppTestTags.ConfigHubTemplateMappingButton,
            ) {
                onNavigate(DashboardRoutes.settings("TEMPLATE_MAPPING"))
            }

            SettingsHubCard(
                title = stringResource(R.string.config_hub_whatsapp_title),
                subtitle = stringResource(R.string.config_hub_whatsapp_subtitle),
                icon = Icons.AutoMirrored.Filled.Send,
                testTag = RepairAppTestTags.ConfigHubWhatsappButton,
            ) {
                onNavigate(DashboardRoutes.settings("WHATSAPP"))
            }

            SettingsHubCard(
                title = stringResource(R.string.storage_title),
                subtitle = stringResource(R.string.config_hub_storage_subtitle),
                icon = Icons.Default.Storage,
                testTag = RepairAppTestTags.ConfigHubStorageButton,
            ) {
                onNavigate(DashboardRoutes.settings("STORAGE"))
            }

            SettingsHubCard(
                title = stringResource(R.string.config_hub_admin_title),
                subtitle = stringResource(R.string.config_hub_admin_subtitle),
                icon = Icons.Default.Lock,
                testTag = RepairAppTestTags.ConfigHubAdminButton,
            ) {
                onNavigate(DashboardRoutes.ADMIN)
            }

            if (isAdmin) {
                SettingsHubCard(
                    title = stringResource(R.string.config_hub_backup_title),
                    subtitle = stringResource(R.string.config_hub_backup_subtitle),
                    icon = Icons.Default.Backup,
                    testTag = RepairAppTestTags.ConfigHubBackupButton,
                ) {
                    onNavigate(DashboardRoutes.settings("EXPORT"))
                }
            }

            SettingsHubCard(
                title = stringResource(R.string.config_hub_about_title),
                subtitle = stringResource(R.string.config_hub_about_subtitle),
                icon = Icons.Default.Info,
                testTag = RepairAppTestTags.ConfigHubAboutButton,
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
            modifier = Modifier.padding(RepairAppSpacing.cardPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(RepairAppSpacing.fieldGap),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(24.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.outline,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
            }
        }
    }
}

package com.onlytechtake.repairapp.features.dashboard.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.core.utils.formatFileSize
import com.onlytechtake.repairapp.core.design.RepairAppSpacing
import com.onlytechtake.repairapp.core.design.components.*
import com.onlytechtake.repairapp.features.dashboard.common.SectionTitle

@Composable
fun StorageSettings(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
) {
    val statsState = viewModel.storageStats.collectAsState()
    val stats = statsState.value

    AppScreenContainer {
        SectionTitle(
            text = stringResource(R.string.storage_title),
            tooltip = stringResource(R.string.storage_tooltip),
        )

        if (stats == null) {
            Text(stringResource(R.string.storage_loading), style = MaterialTheme.typography.bodyMedium)
        } else {
            StandardCard {
                Column(
                    modifier = Modifier.padding(RepairAppSpacing.cardPadding),
                    verticalArrangement = Arrangement.spacedBy(RepairAppSpacing.compactGap),
                ) {
                    Text(stringResource(R.string.storage_free_device), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    
                    val freePercentage = ((stats.freeDeviceBytes.toDouble() / stats.totalDeviceBytes.toDouble()) * 100).toInt()
                    val usedProgress = (stats.totalDeviceBytes - stats.freeDeviceBytes).toDouble() / stats.totalDeviceBytes.toDouble()
                    
                    LinearProgressIndicator(
                        progress = { usedProgress.toFloat() },
                        modifier = Modifier.fillMaxWidth().height(8.dp),
                        color = when {
                            freePercentage < 5 -> MaterialTheme.colorScheme.error
                            freePercentage < 15 -> MaterialTheme.colorScheme.tertiary
                            else -> MaterialTheme.colorScheme.primary
                        },
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.storage_free_percentage, freePercentage, formatFileSize(stats.freeDeviceBytes)),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        AppStatusChip(
                            text = stringResource(
                                when {
                                    freePercentage < 5 -> R.string.storage_status_critical
                                    freePercentage < 15 -> R.string.storage_status_warning
                                    else -> R.string.storage_status_ok
                                },
                            ),
                            containerColor = when {
                                freePercentage < 5 -> MaterialTheme.colorScheme.errorContainer
                                freePercentage < 15 -> MaterialTheme.colorScheme.tertiaryContainer
                                else -> MaterialTheme.colorScheme.primaryContainer
                            },
                            contentColor = when {
                                freePercentage < 5 -> MaterialTheme.colorScheme.onErrorContainer
                                freePercentage < 15 -> MaterialTheme.colorScheme.onTertiaryContainer
                                else -> MaterialTheme.colorScheme.onPrimaryContainer
                            }
                        )
                    }
                    
                    Spacer(Modifier.height(4.dp))
                    AppKeyValueRow(label = stringResource(R.string.storage_total_device), value = formatFileSize(stats.totalDeviceBytes))
                    AppKeyValueRow(label = stringResource(R.string.storage_app_used), value = formatFileSize(stats.appUsedBytes))
                }
            }

            Spacer(Modifier.height(RepairAppSpacing.cardGap))

            StandardCard {
                Column(
                    modifier = Modifier.padding(RepairAppSpacing.cardPadding),
                    verticalArrangement = Arrangement.spacedBy(RepairAppSpacing.compactGap),
                ) {
                    Text(stringResource(R.string.storage_app_detail_title), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    AppKeyValueRow(label = stringResource(R.string.storage_db_size), value = formatFileSize(stats.databaseBytes))
                    AppKeyValueRow(label = stringResource(R.string.storage_media_size), value = formatFileSize(stats.mediaBytes))
                    AppKeyValueRow(label = stringResource(R.string.storage_docs_size), value = formatFileSize(stats.documentsBytes))
                    AppKeyValueRow(label = stringResource(R.string.storage_cache_size), value = formatFileSize(stats.cacheBytes))
                }
            }

            Spacer(Modifier.height(RepairAppSpacing.cardGap))

            StandardCard {
                Column(
                    modifier = Modifier.padding(RepairAppSpacing.cardPadding),
                    verticalArrangement = Arrangement.spacedBy(RepairAppSpacing.compactGap),
                ) {
                    Text(stringResource(R.string.storage_deep_maintenance), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(
                        text = stringResource(R.string.storage_cleanup_orphans_tooltip),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    AppButton(
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(R.string.storage_cleanup_orphans),
                        variant = AppButtonVariant.Secondary,
                        size = AppButtonSize.Compact,
                        onClick = viewModel::cleanupOrphanedFiles
                    )
                }
            }

            Spacer(Modifier.height(RepairAppSpacing.sectionGap))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(RepairAppSpacing.actionGap)
            ) {
                AppButton(
                    modifier = Modifier.weight(1f),
                    text = stringResource(R.string.storage_cleanup_cache),
                    variant = AppButtonVariant.Secondary,
                    size = AppButtonSize.Compact,
                    onClick = viewModel::cleanupCache
                )
                AppButton(
                    modifier = Modifier.weight(1f),
                    text = stringResource(R.string.storage_refresh),
                    variant = AppButtonVariant.Secondary,
                    size = AppButtonSize.Compact,
                    onClick = viewModel::refreshStorageStats
                )
            }
        }

        Spacer(Modifier.height(RepairAppSpacing.sectionGap))

        AppButton(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(R.string.storage_close),
            variant = AppButtonVariant.Ghost,
            onClick = onBack
        )
    }
}

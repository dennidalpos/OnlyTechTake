package com.onlytechtake.repairapp.features.dashboard.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.core.utils.formatFileSize
import com.onlytechtake.repairapp.features.dashboard.common.ActionRow
import com.onlytechtake.repairapp.features.dashboard.common.DashboardSpacing
import com.onlytechtake.repairapp.features.dashboard.common.SectionTitle
import com.onlytechtake.repairapp.features.dashboard.common.StandardCard
import com.onlytechtake.repairapp.features.dashboard.common.TouchButton
import com.onlytechtake.repairapp.features.workspace.ButtonTone
import com.onlytechtake.repairapp.features.workspace.WorkspaceViewModel

@Composable
fun StorageSettings(
    viewModel: WorkspaceViewModel,
) {
    val uiState by viewModel.uiState.collectAsState()
    val stats = uiState.storageStats

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = DashboardSpacing.maxContentWidth),
        contentPadding = PaddingValues(DashboardSpacing.screenPadding),
        verticalArrangement = Arrangement.spacedBy(DashboardSpacing.listGap),
    ) {
        item {
            SectionTitle(stringResource(R.string.storage_title))
        }

        if (stats == null) {
            item {
                Text("Caricamento statistiche...", style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            item {
                StandardCard(highlight = true) {
                    Column(
                        modifier = Modifier.padding(DashboardSpacing.cardPadding),
                        verticalArrangement = Arrangement.spacedBy(DashboardSpacing.compactGap),
                    ) {
                        StorageInfoRow(stringResource(R.string.storage_free_device), formatFileSize(stats.freeDeviceBytes))
                        StorageInfoRow(stringResource(R.string.storage_app_used), formatFileSize(stats.appUsedBytes))
                        
                        val freePercentage = (stats.freeDeviceBytes.toDouble() / stats.totalDeviceBytes.toDouble() * 100).toInt()
                        Text(
                            text = "Spazio libero: $freePercentage%",
                            style = MaterialTheme.typography.labelSmall,
                            color = when {
                                freePercentage < 5 -> MaterialTheme.colorScheme.error
                                freePercentage < 15 -> MaterialTheme.colorScheme.error // Warning color would be better
                                else -> MaterialTheme.colorScheme.primary
                            }
                        )
                    }
                }
            }

            item {
                StandardCard {
                    Column(
                        modifier = Modifier.padding(DashboardSpacing.cardPadding),
                        verticalArrangement = Arrangement.spacedBy(DashboardSpacing.compactGap),
                    ) {
                        Text("Dettaglio uso App", style = MaterialTheme.typography.titleSmall)
                        StorageInfoRow(stringResource(R.string.storage_db_size), formatFileSize(stats.databaseBytes))
                        StorageInfoRow(stringResource(R.string.storage_media_size), formatFileSize(stats.mediaBytes))
                        StorageInfoRow(stringResource(R.string.storage_docs_size), formatFileSize(stats.documentsBytes))
                        StorageInfoRow(stringResource(R.string.storage_cache_size), formatFileSize(stats.cacheBytes))
                    }
                }
            }

            item {
                ActionRow {
                    TouchButton(
                        text = stringResource(R.string.storage_refresh),
                        tone = ButtonTone.NEUTRAL,
                        onClick = viewModel::refreshStorageStats
                    )
                }
            }
        }
    }
}

@Composable
private fun StorageInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
    }
}

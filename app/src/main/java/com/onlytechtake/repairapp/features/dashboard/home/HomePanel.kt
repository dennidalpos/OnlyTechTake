package com.onlytechtake.repairapp.features.dashboard.home

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.onlytechtake.repairapp.BuildConfig
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.core.demo.DemoMode
import com.onlytechtake.repairapp.domain.backup.BackupReminderPolicy
import com.onlytechtake.repairapp.domain.backup.BackupReminderSettings
import com.onlytechtake.repairapp.domain.backup.BackupReminderStatus
import com.onlytechtake.repairapp.domain.models.AppSummary
import com.onlytechtake.repairapp.features.dashboard.common.DashboardSpacing
import com.onlytechtake.repairapp.features.dashboard.common.SectionTitle
import com.onlytechtake.repairapp.features.dashboard.common.StandardCard
import com.onlytechtake.repairapp.features.dashboard.common.TouchButton
import com.onlytechtake.repairapp.features.workspace.WorkspaceDestination
import com.onlytechtake.repairapp.features.workspace.WorkspaceViewModel
import com.onlytechtake.repairapp.features.workspace.ButtonTone
import com.onlytechtake.repairapp.features.workspace.queueFilter

@Composable
fun HomePanel(summary: AppSummary, viewModel: WorkspaceViewModel, isAdmin: Boolean) {
    val queues by viewModel.operationalQueues.collectAsState()
    val backupReminderSettings by viewModel.backupReminderSettings.collectAsState()
    val exportZipLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        uri?.let(viewModel::exportCompleteBackup)
    }
    val showBackupReminder = isAdmin && viewModel.isBackupReminderDue(backupReminderSettings)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(DashboardSpacing.screenPadding),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(DashboardSpacing.listGap),
        ) {
            val showBanner = BuildConfig.IS_DEMO_VERSION || DemoMode.isEnabled
            if (showBanner) {
                val bannerText = if (DemoMode.isEnabled) {
                    stringResource(R.string.demo_mode_banner)
                } else {
                    stringResource(R.string.demo_version_banner)
                }
                DemoBanner(bannerText)
            }
            SectionTitle(stringResource(R.string.home_title))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(DashboardSpacing.compactGap),
            ) {
                MetricCard(stringResource(R.string.home_metric_users), summary.userCount.toString(), Modifier.weight(1f))
                MetricCard(stringResource(R.string.home_metric_customers), summary.customerCount.toString(), Modifier.weight(1f))
                MetricCard(stringResource(R.string.home_metric_active), summary.activeOrderCount.toString(), Modifier.weight(1f))
            }
            TouchButton(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.home_new_order),
                onClick = viewModel::openNewOrder,
            )
            if (showBackupReminder) {
                BackupReminderCard(
                    settings = backupReminderSettings,
                    onRunBackup = { exportZipLauncher.launch("onlytechtake-backup.zip") },
                ) {
                    viewModel.navigateTo(WorkspaceDestination.CONFIG_EXPORT)
                }
            }
            queues.forEach { queue ->
                StandardCard {
                    Column(
                        modifier = Modifier.padding(DashboardSpacing.cardPadding),
                        verticalArrangement = Arrangement.spacedBy(DashboardSpacing.formGap),
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(queue.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary)
                            androidx.compose.material3.Badge(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            ) {
                                Text(queue.orders.size.toString(), modifier = Modifier.padding(4.dp))
                            }
                        }
                        if (queue.orders.isEmpty()) {
                            Text(stringResource(R.string.home_empty_queue), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            queue.orders.take(3).forEach { order ->
                                TouchButton(
                                    modifier = Modifier.fillMaxWidth(),
                                    text = "${order.orderNumber} - ${order.title}",
                                    tone = ButtonTone.NEUTRAL,
                                    onClick = { viewModel.selectOrder(order.id) },
                                )
                            }
                        }
                        TouchButton(
                            modifier = Modifier.fillMaxWidth(),
                            text = stringResource(R.string.home_open_queue),
                            onClick = {
                                viewModel.setFilter(queueFilter(queue))
                                viewModel.navigateTo(WorkspaceDestination.ORDERS)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DemoBanner(text: String) {
    StandardCard(highlight = true) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun BackupReminderCard(
    settings: BackupReminderSettings,
    onRunBackup: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    StandardCard(highlight = true) {
        Column(
            modifier = Modifier.padding(DashboardSpacing.cardPadding),
            verticalArrangement = Arrangement.spacedBy(DashboardSpacing.formGap),
        ) {
            Text(
                text = stringResource(R.string.home_backup_reminder_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = backupStatusLabel(settings),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(DashboardSpacing.actionGap),
            ) {
                TouchButton(
                    modifier = Modifier.weight(1f),
                    text = stringResource(R.string.backup_run_full_zip),
                    onClick = onRunBackup,
                )
                TouchButton(
                    modifier = Modifier.weight(1f),
                    text = stringResource(R.string.home_backup_reminder_settings),
                    tone = ButtonTone.NEUTRAL,
                    onClick = onOpenSettings,
                )
            }
        }
    }
}

@Composable
private fun backupStatusLabel(settings: BackupReminderSettings): String =
    when {
        settings.lastBackupStatus == BackupReminderStatus.FAILED ->
            stringResource(R.string.backup_status_failed)
        settings.lastSuccessfulBackupAtMillis == null ->
            stringResource(R.string.backup_status_never)
        else -> {
            val days = BackupReminderPolicy.daysSinceLastSuccessfulBackup(settings, System.currentTimeMillis()) ?: 0L
            stringResource(R.string.backup_status_days_ago, days)
        }
    }

@Composable
private fun MetricCard(label: String, value: String, modifier: Modifier = Modifier) {
    StandardCard(
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                value,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

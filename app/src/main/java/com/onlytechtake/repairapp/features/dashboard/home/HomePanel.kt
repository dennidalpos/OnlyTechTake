package com.onlytechtake.repairapp.features.dashboard.home

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.onlytechtake.repairapp.BuildConfig
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.core.demo.DemoMode
import com.onlytechtake.repairapp.domain.models.AppSummary
import com.onlytechtake.repairapp.domain.workspace.OrderFilter
import com.onlytechtake.repairapp.core.design.RepairAppSpacing
import com.onlytechtake.repairapp.core.design.RepairAppTestTags
import com.onlytechtake.repairapp.core.design.components.*
import com.onlytechtake.repairapp.features.dashboard.common.BackupReminderCard
import com.onlytechtake.repairapp.features.dashboard.common.SectionTitle
import com.onlytechtake.repairapp.features.workspace.queueFilter

@Composable
fun HomePanel(
    summary: AppSummary,
    viewModel: HomeViewModel,
    isAdmin: Boolean,
    onNewOrder: () -> Unit,
    onSelectOrder: (String) -> Unit,
    onOpenQueue: (OrderFilter) -> Unit,
    onOpenBackupSettings: () -> Unit,
) {
    val queues by viewModel.operationalQueues.collectAsState()
    val backupReminderSettings by viewModel.backupReminderSettings.collectAsState()
    val showBackupReminder by viewModel.isBackupReminderDue.collectAsState()
    val exportZipLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        uri?.let(viewModel::exportCompleteBackup)
    }

    AppScreenContainer {
        val showBanner = BuildConfig.IS_DEMO_VERSION || DemoMode.isEnabled
        if (showBanner) {
            val bannerText = if (DemoMode.isEnabled) {
                stringResource(R.string.demo_mode_banner)
            } else {
                stringResource(R.string.demo_version_banner)
            }
            AppMessageBanner(
                message = bannerText,
                type = BannerType.Info,
                modifier = Modifier.padding(bottom = RepairAppSpacing.cardGap)
            )
        }

        SectionTitle(stringResource(R.string.home_title))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(RepairAppSpacing.cardGap),
        ) {
            AppMetricCard(
                label = stringResource(R.string.home_metric_users),
                value = summary.userCount.toString(),
                modifier = Modifier.weight(1f)
            )
            AppMetricCard(
                label = stringResource(R.string.home_metric_customers),
                value = summary.customerCount.toString(),
                modifier = Modifier.weight(1f)
            )
            AppMetricCard(
                label = stringResource(R.string.home_metric_active),
                value = summary.activeOrderCount.toString(),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(RepairAppSpacing.cardGap))

        AppButton(
            text = stringResource(R.string.home_new_order),
            onClick = onNewOrder,
            variant = AppButtonVariant.Primary,
            size = AppButtonSize.FullWidth,
            leadingIcon = Icons.Default.Add,
            testTag = RepairAppTestTags.HomeNewOrderButton
        )

        if (showBackupReminder) {
            Spacer(Modifier.height(RepairAppSpacing.cardGap))
            BackupReminderCard(
                settings = backupReminderSettings,
                onRunBackup = { exportZipLauncher.launch("onlytechtake-backup.zip") },
                onOpenSettings = onOpenBackupSettings
            )
        }

        Spacer(Modifier.height(RepairAppSpacing.sectionGap))
        SectionTitle(stringResource(R.string.home_queues_title)) // Added a title for queues section if it makes sense

        queues.forEach { queue ->
            AppQueueCard(
                title = queue.title,
                count = queue.orders.size,
                lastElementTitle = queue.orders.firstOrNull()?.let { "${it.orderNumber} - ${it.title}" },
                onOpenQueue = { onOpenQueue(queueFilter(queue)) },
                modifier = Modifier.padding(bottom = RepairAppSpacing.cardGap)
            )
        }
    }
}

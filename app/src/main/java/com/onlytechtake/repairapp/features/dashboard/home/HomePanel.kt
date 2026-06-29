package com.onlytechtake.repairapp.features.dashboard.home

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.onlytechtake.repairapp.BuildConfig
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.core.demo.DemoMode
import com.onlytechtake.repairapp.core.design.RepairAppSpacing
import com.onlytechtake.repairapp.core.design.RepairAppTestTags
import com.onlytechtake.repairapp.core.design.RepairAppTheme
import com.onlytechtake.repairapp.core.design.components.AppBadge
import com.onlytechtake.repairapp.core.design.components.AppButton
import com.onlytechtake.repairapp.core.design.components.AppButtonSize
import com.onlytechtake.repairapp.core.design.components.AppButtonVariant
import com.onlytechtake.repairapp.core.design.components.AppMetricCard
import com.onlytechtake.repairapp.core.design.components.AppQueueCard
import com.onlytechtake.repairapp.core.design.components.AppScreenContainer
import com.onlytechtake.repairapp.core.design.components.StandardCard
import com.onlytechtake.repairapp.core.design.repairColors
import com.onlytechtake.repairapp.domain.backup.BackupReminderSettings
import com.onlytechtake.repairapp.domain.models.AppSummary
import com.onlytechtake.repairapp.domain.workspace.OrderFilter
import com.onlytechtake.repairapp.features.dashboard.common.AppEmptyState
import com.onlytechtake.repairapp.features.dashboard.common.BackupReminderCard
import com.onlytechtake.repairapp.features.dashboard.common.SectionTitle
import com.onlytechtake.repairapp.features.workspace.OperationalQueue
import com.onlytechtake.repairapp.features.workspace.queueFilter
import com.onlytechtake.repairapp.navigation.LocalWindowSizeClass

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomePanel(
    summary: AppSummary?,
    viewModel: HomeViewModel,
    onNewOrder: () -> Unit,
    onOpenQueue: (OrderFilter) -> Unit,
    onOpenBackupSettings: () -> Unit,
) {
    val queues by viewModel.operationalQueues.collectAsState()
    val backupReminderSettings by viewModel.backupReminderSettings.collectAsState()
    val showBackupReminder by viewModel.isBackupReminderDue.collectAsState()
    val exportZipLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
            uri?.let(viewModel::exportCompleteBackup)
        }

    HomePanelContent(
        summary = summary,
        queues = queues,
        backupReminderSettings = backupReminderSettings,
        showBackupReminder = showBackupReminder,
        onNewOrder = onNewOrder,
        onOpenQueue = onOpenQueue,
        onOpenBackupSettings = onOpenBackupSettings,
        onRunBackup = { exportZipLauncher.launch("onlytechtake-backup.zip") },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomePanelContent(
    summary: AppSummary?,
    queues: List<OperationalQueue>?,
    backupReminderSettings: BackupReminderSettings,
    showBackupReminder: Boolean,
    onNewOrder: () -> Unit,
    onOpenQueue: (OrderFilter) -> Unit,
    onOpenBackupSettings: () -> Unit,
    onRunBackup: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val windowSizeClass = LocalWindowSizeClass.current
    val isWide = windowSizeClass.widthSizeClass >= WindowWidthSizeClass.Medium

    AppScreenContainer(modifier = modifier) {
        HomeHeader(onNewOrder)

        Spacer(Modifier.height(RepairAppSpacing.cardGap))

        if ((summary == null) || (queues == null)) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier.fillMaxWidth().height(200.dp),
                contentAlignment = Alignment.Center,
            ) {
                com.onlytechtake.repairapp.core.design.LoadingBrandIndicator(
                    text = stringResource(R.string.order_loading_detail),
                )
            }
        } else {
            HomeMetrics(summary)

            if (showBackupReminder && isProFlavor()) {
                Spacer(Modifier.height(RepairAppSpacing.cardGap))
                BackupReminderCard(
                    settings = backupReminderSettings,
                    onRunBackup = onRunBackup,
                    onOpenSettings = onOpenBackupSettings,
                )
            }

            if (BuildConfig.IS_DEMO_VERSION) {
                Spacer(Modifier.height(RepairAppSpacing.cardGap))
                DemoInfoCard()
            }

            Spacer(Modifier.height(RepairAppSpacing.sectionGap))
            SectionTitle(stringResource(R.string.home_queues_title))

            if (queues.isEmpty() || queues.all { it.orders.isEmpty() }) {
                AppEmptyState(
                    title = stringResource(R.string.home_empty_queues_title),
                    body = stringResource(R.string.home_empty_queues_body),
                    actionLabel = stringResource(R.string.home_new_order),
                    onAction = onNewOrder,
                )
            } else {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(RepairAppSpacing.cardGap),
                    verticalArrangement = Arrangement.spacedBy(RepairAppSpacing.cardGap),
                    maxItemsInEachRow = if (isWide) 2 else 1,
                ) {
                    queues.forEach { queue ->
                        val itemModifier =
                            if (isWide) Modifier.weight(1f) else Modifier.fillMaxWidth()
                        AppQueueCard(
                            title = stringResource(queue.titleRes),
                            count = queue.orders.size,
                            lastElementTitle =
                                queue.orders
                                    .firstOrNull()
                                    ?.let { "${it.orderNumber} - ${it.title}" },
                            onOpenQueue = { onOpenQueue(queueFilter(queue)) },
                            modifier = itemModifier,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeHeader(onNewOrder: () -> Unit) {
    val isDemo = isAnyDemo()
    val isShowcase = BuildConfig.IS_SHOWCASE_MODE_ENABLED

    Column(verticalArrangement = Arrangement.spacedBy(RepairAppSpacing.compactGap)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SectionTitle(stringResource(R.string.home_title))
            val context = LocalContext.current
            when {
                isShowcase -> {
                    AppBadge(
                        text = DemoMode.getShowcaseScenarioLabel(context),
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                }

                isDemo -> {
                    AppBadge(
                        text = DemoMode.getVersionBanner(context),
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }

                else -> {
                    AppBadge(
                        text = stringResource(R.string.home_pro_status_operational),
                        containerColor = MaterialTheme.repairColors.successContainer,
                        contentColor = MaterialTheme.repairColors.onSuccessContainer,
                    )
                }
            }
        }

        val context = LocalContext.current
        AppButton(
            text =
                when {
                    isDemo -> DemoMode.getDemoTryCta(context)
                    isShowcase -> DemoMode.getShowcaseWalkthroughLabel(context)
                    else -> stringResource(R.string.home_pro_cta_new)
                },
            onClick = onNewOrder,
            variant = AppButtonVariant.Primary,
            size = AppButtonSize.FullWidth,
            leadingIcon = if (isShowcase) Icons.Default.PlayArrow else Icons.Default.Add,
            testTag = RepairAppTestTags.HomeNewOrderButton,
        )
    }
}

@Composable
private fun HomeMetrics(summary: AppSummary) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(RepairAppSpacing.cardGap),
    ) {
        AppMetricCard(
            label = stringResource(R.string.home_metric_operators),
            value = summary.operatorCount.toString(),
            modifier = Modifier.weight(1f),
        )
        AppMetricCard(
            label = stringResource(R.string.label_customers),
            value = summary.customerCount.toString(),
            modifier = Modifier.weight(1f),
        )
        AppMetricCard(
            label = stringResource(R.string.home_metric_active),
            value = summary.activeOrderCount.toString(),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun DemoInfoCard() {
    StandardCard {
        Row(
            modifier = Modifier.padding(RepairAppSpacing.cardPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.Star,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(end = RepairAppSpacing.compactGap),
            )
            val context = LocalContext.current
            Column(verticalArrangement = Arrangement.spacedBy(RepairAppSpacing.compactGap)) {
                Text(
                    text = DemoMode.getDemoProFeaturesLabel(context),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = DemoMode.getDemoProFeaturesDesc(context),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Preview(showBackground = true, name = "Light Mode")
@Preview(
    showBackground = true,
    name = "Dark Mode",
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun HomePanelPreview() {
    CompositionLocalProvider(
        LocalWindowSizeClass provides
            WindowSizeClass.calculateFromSize(
                DpSize(400.dp, 800.dp),
            ),
    ) {
        RepairAppTheme {
            HomePanelContent(
                summary =
                    AppSummary(
                        title = "OnlyTechTake",
                        operatorCount = 3,
                        customerCount = 120,
                        activeOrderCount = 45,
                    ),
                queues = emptyList(),
                backupReminderSettings = BackupReminderSettings(),
                showBackupReminder = true,
                onNewOrder = {},
                onOpenQueue = {},
                onOpenBackupSettings = {},
                onRunBackup = {},
            )
        }
    }
}

@Suppress("SameReturnValue")
private fun isProFlavor(): Boolean = BuildConfig.IS_PRO_VERSION

@Suppress("SameReturnValue")
private fun isDemoFlavor(): Boolean = BuildConfig.IS_DEMO_VERSION

@Suppress("SameReturnValue")
private fun isAnyDemo(): Boolean = isDemoFlavor()

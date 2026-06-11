package com.onlytechtake.repairapp.features.dashboard

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
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
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.domain.models.AppSummary
import com.onlytechtake.repairapp.features.workspace.WorkspaceDestination
import com.onlytechtake.repairapp.features.workspace.WorkspaceViewModel
import com.onlytechtake.repairapp.features.workspace.ButtonTone
import com.onlytechtake.repairapp.features.workspace.queueFilter

@Composable
fun HomePanel(summary: AppSummary, viewModel: WorkspaceViewModel) {
    val queues by viewModel.operationalQueues.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(DashboardSpacing.screenPadding),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(DashboardSpacing.listGap),
        ) {
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
            queues.forEach { queue ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(DashboardSpacing.cardPadding),
                        verticalArrangement = Arrangement.spacedBy(DashboardSpacing.formGap),
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(queue.title, style = MaterialTheme.typography.titleMedium)
                            Text(queue.orders.size.toString(), style = MaterialTheme.typography.titleMedium)
                        }
                        if (queue.orders.isEmpty()) {
                            Text(stringResource(R.string.home_empty_queue), style = MaterialTheme.typography.bodyMedium)
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
private fun MetricCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
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
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

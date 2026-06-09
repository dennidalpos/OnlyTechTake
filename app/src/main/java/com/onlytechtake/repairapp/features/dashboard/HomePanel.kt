package com.onlytechtake.repairapp.features.dashboard

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.onlytechtake.repairapp.domain.models.AppSummary
import com.onlytechtake.repairapp.features.workspace.WorkspaceDestination
import com.onlytechtake.repairapp.features.workspace.WorkspaceViewModel
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
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(DashboardSpacing.compactGap),
            ) {
                SectionTitle("Banco riparazioni")
                Text(
                    text = "Segui ogni commessa dal ritiro al banco, alla diagnosi, alla riparazione e alla consegna. Gli aggiornamenti WhatsApp vengono preparati qui e inviati manualmente al cliente.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(DashboardSpacing.compactGap),
            ) {
                MetricCard("Utenti", summary.userCount.toString(), Modifier.weight(1f))
                MetricCard("Clienti", summary.customerCount.toString(), Modifier.weight(1f))
                MetricCard("Attive", summary.activeOrderCount.toString(), Modifier.weight(1f))
            }
            TouchButton(
                modifier = Modifier.fillMaxWidth(),
                text = "Nuova commessa",
                onClick = viewModel::openNewOrder,
            )
            queues.forEach { queue ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(DashboardSpacing.cardPadding),
                        verticalArrangement = Arrangement.spacedBy(DashboardSpacing.formGap),
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(DashboardSpacing.compactGap)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(queue.title, style = MaterialTheme.typography.titleMedium)
                                Text(queue.orders.size.toString(), style = MaterialTheme.typography.titleMedium)
                            }
                            Text(
                                text = queueDescription(queue.title),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (queue.orders.isEmpty()) {
                            Text("Nessuna commessa in questa coda.", style = MaterialTheme.typography.bodyMedium)
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
                            text = "Apri coda",
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
    Card(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(value, style = MaterialTheme.typography.headlineSmall)
            Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

private fun queueDescription(title: String): String =
    when {
        title.contains("aper", ignoreCase = true) -> "Presa in carico e prime verifiche al banco."
        title.contains("diagnosi", ignoreCase = true) -> "Controlli tecnici, diagnosi e raccolta esito."
        title.contains("ripar", ignoreCase = true) -> "Preventivo, approvazione cliente e lavorazione."
        title.contains("pront", ignoreCase = true) -> "Avviso al cliente e consegna finale."
        else -> "Coda operativa della giornata."
    }

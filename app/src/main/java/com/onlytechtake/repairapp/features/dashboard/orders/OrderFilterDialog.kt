package com.onlytechtake.repairapp.features.dashboard.orders

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.core.design.RepairAppSpacing
import com.onlytechtake.repairapp.core.design.RepairAppTheme
import com.onlytechtake.repairapp.core.design.components.AppButton
import com.onlytechtake.repairapp.core.design.components.AppButtonSize
import com.onlytechtake.repairapp.core.design.components.AppButtonVariant
import com.onlytechtake.repairapp.core.design.components.AppDialog
import com.onlytechtake.repairapp.core.design.components.AppQueueCard
import com.onlytechtake.repairapp.domain.models.OrderFilter
import com.onlytechtake.repairapp.domain.models.OrderStatus
import com.onlytechtake.repairapp.features.workspace.OperationalQueue
import com.onlytechtake.repairapp.features.workspace.queueFilter
import com.onlytechtake.repairapp.features.workspace.statusColors

@Composable
fun OrderFilterDialog(
    currentFilter: OrderFilter,
    onDismissRequest: () -> Unit,
    onFilterSelected: (OrderFilter) -> Unit,
) {
    val queues =
        listOf(
            OperationalQueue(R.string.queue_open, setOf(OrderStatus.OPEN), emptyList()),
            OperationalQueue(R.string.queue_processing, setOf(OrderStatus.IN_PROGRESS), emptyList()),
            OperationalQueue(R.string.queue_diagnosis, setOf(OrderStatus.DIAGNOSIS), emptyList()),
            OperationalQueue(R.string.queue_result, setOf(OrderStatus.RESULT), emptyList()),
            OperationalQueue(R.string.queue_repair, setOf(OrderStatus.REPAIRING), emptyList()),
            OperationalQueue(R.string.queue_ready, setOf(OrderStatus.READY_FOR_PICKUP), emptyList()),
            OperationalQueue(R.string.queue_closed, setOf(OrderStatus.CLOSED), emptyList()),
        )

    var selectedFilter by remember { mutableStateOf(currentFilter) }

    AppDialog(
        onDismissRequest = onDismissRequest,
        title = stringResource(R.string.orders_filter_title),
        content = {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(RepairAppSpacing.compactGap),
                verticalArrangement = Arrangement.spacedBy(RepairAppSpacing.compactGap),
                modifier = Modifier.heightIn(max = 400.dp),
            ) {
                items(queues) { queue ->
                    val (containerColor, contentColor) = getQueueColors(queue.titleRes)
                    val filter = queueFilter(queue)
                    val isSelected = selectedFilter.statuses == filter.statuses

                    AppQueueCard(
                        title = stringResource(queue.titleRes),
                        count = 0,
                        onOpenQueue = {
                            selectedFilter = filter
                        },
                        badgeContainerColor = containerColor,
                        badgeContentColor = contentColor,
                        highlight = isSelected,
                    )
                }
            }
        },
        confirmButton = {
            AppButton(
                text = stringResource(R.string.action_confirm),
                onClick = {
                    onFilterSelected(selectedFilter)
                    onDismissRequest()
                },
                variant = AppButtonVariant.Primary,
                size = AppButtonSize.Compact,
            )
        },
    )
}

@Composable
private fun getQueueColors(titleRes: Int): Pair<Color, Color> {
    val status =
        when (titleRes) {
            R.string.queue_open -> OrderStatus.OPEN
            R.string.queue_processing -> OrderStatus.IN_PROGRESS
            R.string.queue_diagnosis -> OrderStatus.DIAGNOSIS
            R.string.queue_result -> OrderStatus.RESULT
            R.string.queue_repair -> OrderStatus.REPAIRING
            R.string.queue_ready -> OrderStatus.READY_FOR_PICKUP
            else -> OrderStatus.CLOSED
        }
    return statusColors(status)
}

@Preview(showBackground = true)
@Composable
private fun OrderFilterDialogPreviewLight() {
    RepairAppTheme(darkTheme = false) {
        OrderFilterDialog(
            currentFilter = OrderFilter(statuses = setOf(OrderStatus.OPEN)),
            onDismissRequest = {},
        ) { }
    }
}

@Preview(showBackground = true)
@Composable
private fun OrderFilterDialogPreviewDark() {
    RepairAppTheme(darkTheme = true) {
        OrderFilterDialog(
            currentFilter = OrderFilter(statuses = setOf(OrderStatus.OPEN)),
            onDismissRequest = {},
        ) { }
    }
}

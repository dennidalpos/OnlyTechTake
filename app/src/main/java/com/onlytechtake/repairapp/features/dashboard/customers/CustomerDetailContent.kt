package com.onlytechtake.repairapp.features.dashboard.customers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.core.design.RepairAppSpacing
import com.onlytechtake.repairapp.core.design.components.AppIconButton
import com.onlytechtake.repairapp.core.design.components.AppKeyValueRow
import com.onlytechtake.repairapp.core.design.components.AppStatusBadge
import com.onlytechtake.repairapp.core.design.components.StandardCard
import com.onlytechtake.repairapp.core.utils.displayName
import com.onlytechtake.repairapp.core.utils.labelRes
import com.onlytechtake.repairapp.domain.models.Customer
import com.onlytechtake.repairapp.domain.models.OrderStatus
import com.onlytechtake.repairapp.features.dashboard.common.OperatorText
import com.onlytechtake.repairapp.features.dashboard.common.SectionTitle
import com.onlytechtake.repairapp.features.workspace.OrderSummaryRow

@Composable
fun CustomerDetailContent(
    customer: Customer,
    viewModel: CustomersViewModel,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onOrderClick: (String) -> Unit,
) {
    val openOrders by viewModel
        .observeOrdersForCustomer(customer.id, closed = false)
        .collectAsState(emptyList())
    val allOrders by viewModel
        .observeOrdersForCustomer(customer.id, closed = true)
        .collectAsState(emptyList())
    val closedOrders =
        allOrders.filter { it.order.status == OrderStatus.CLOSED }
    val context = LocalContext.current

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(RepairAppSpacing.cardGap),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = customer.displayName(context),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                AppStatusBadge(
                    text = stringResource(customer.type.labelRes),
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
            Row {
                AppIconButton(
                    imageVector = Icons.Default.Edit,
                    contentDescription = stringResource(R.string.customer_detail_edit),
                    onClick = onEdit,
                    tint = MaterialTheme.colorScheme.primary,
                )
                AppIconButton(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.content_description_delete),
                    onClick = onDelete,
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }

        StandardCard {
            Column(
                modifier = Modifier.padding(RepairAppSpacing.cardPadding),
                verticalArrangement = Arrangement.spacedBy(RepairAppSpacing.compactGap),
            ) {
                customer.phone?.let {
                    AppKeyValueRow(
                        label = stringResource(R.string.customer_field_phone),
                        value = it,
                        labelStyle = MaterialTheme.typography.labelMedium,
                        valueStyle = MaterialTheme.typography.bodyMedium,
                    )
                }
                customer.email?.let {
                    AppKeyValueRow(
                        label = stringResource(R.string.customer_field_email),
                        value = it,
                        labelStyle = MaterialTheme.typography.labelMedium,
                        valueStyle = MaterialTheme.typography.bodyMedium,
                    )
                }
                customer.address?.let {
                    AppKeyValueRow(
                        label = stringResource(R.string.customer_field_address),
                        value = it,
                        labelStyle = MaterialTheme.typography.labelMedium,
                        valueStyle = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }

        if (!customer.notes.isNullOrBlank()) {
            SectionTitle(stringResource(R.string.customer_field_notes))
            OperatorText(customer.notes)
        }

        SectionTitle(stringResource(R.string.customer_open_orders, openOrders.size))
        if (openOrders.isEmpty()) {
            Text(
                stringResource(R.string.customer_no_open_orders),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline,
            )
        } else {
            openOrders.forEach { item ->
                OrderSummaryRow(
                    item,
                    onOrderClick,
                )
            }
        }

        SectionTitle(stringResource(R.string.customer_closed_orders, closedOrders.size))
        if (closedOrders.isEmpty()) {
            Text(
                stringResource(R.string.customer_no_closed_orders),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline,
            )
        } else {
            closedOrders.forEach { item ->
                OrderSummaryRow(
                    item,
                    onOrderClick,
                )
            }
        }
    }
}

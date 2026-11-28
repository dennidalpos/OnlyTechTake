package com.onlytechtake.repairapp.features.dashboard.customers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.core.design.RepairAppSpacing
import com.onlytechtake.repairapp.core.design.components.StandardCard
import com.onlytechtake.repairapp.core.utils.displayName
import com.onlytechtake.repairapp.core.utils.labelRes
import com.onlytechtake.repairapp.domain.models.Customer
import com.onlytechtake.repairapp.features.dashboard.common.AppEmptyState

@Composable
fun CustomerCard(
    customer: Customer,
    onClick: () -> Unit,
) {
    StandardCard(
        modifier = Modifier.testTag("customer_card_${customer.id}"),
        onClick = onClick,
    ) {
        Row(
            modifier =
                Modifier
                    .padding(RepairAppSpacing.cardPadding)
                    .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                val context = LocalContext.current
                Text(
                    customer.displayName(context),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text =
                        listOfNotNull(
                            stringResource(customer.type.labelRes),
                            customer.phone,
                        ).joinToString(" – "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
            )
        }
    }
}

@Composable
fun CustomerEmptyState(hasQuery: Boolean) {
    AppEmptyState(
        title =
            if (hasQuery) {
                stringResource(R.string.customer_no_results_title)
            } else {
                stringResource(
                    R.string.customer_search_empty_title,
                )
            },
        body = if (hasQuery) stringResource(R.string.customer_no_results) else stringResource(R.string.customer_search_hint),
    )
}

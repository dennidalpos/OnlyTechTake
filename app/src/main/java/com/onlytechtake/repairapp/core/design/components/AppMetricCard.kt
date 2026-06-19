package com.onlytechtake.repairapp.core.design.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.onlytechtake.repairapp.core.design.RepairAppSizes
import com.onlytechtake.repairapp.core.design.RepairAppSpacing

@Composable
fun AppMetricCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    StandardCard(
        modifier = modifier.height(RepairAppSizes.metricCardHeight),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(RepairAppSpacing.cardPadding),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

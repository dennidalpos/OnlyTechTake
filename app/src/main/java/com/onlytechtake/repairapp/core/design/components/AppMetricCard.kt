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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.onlytechtake.repairapp.core.design.RepairAppSizes
import com.onlytechtake.repairapp.core.design.RepairAppSpacing

@Composable
fun AppMetricCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    isCompact: Boolean = false,
) {
    val height = if (isCompact) 64.dp else RepairAppSizes.metricCardHeight
    val padding = if (isCompact) RepairAppSpacing.compactGap else RepairAppSpacing.cardPadding
    val valueStyle =
        if (isCompact) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineMedium
    val labelStyle =
        if (isCompact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium

    StandardCard(
        modifier = modifier.height(height),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = value,
                style = valueStyle,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = label,
                style = labelStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

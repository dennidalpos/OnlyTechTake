package com.onlytechtake.repairapp.core.design.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.core.design.RepairAppSpacing

@Composable
fun AppQueueCard(
    title: String,
    count: Int,
    lastElementTitle: String? = null,
    onOpenQueue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    StandardCard(
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(RepairAppSpacing.cardPadding),
            verticalArrangement = Arrangement.spacedBy(RepairAppSpacing.compactGap)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                AppBadge(
                    text = count.toString(),
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            
            if (count > 0 && lastElementTitle != null) {
                Text(
                    text = lastElementTitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            } else {
                Text(
                    text = stringResource(R.string.home_empty_queue),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            AppButton(
                text = stringResource(R.string.home_open_queue),
                onClick = onOpenQueue,
                variant = AppButtonVariant.Secondary,
                size = AppButtonSize.Compact,
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}

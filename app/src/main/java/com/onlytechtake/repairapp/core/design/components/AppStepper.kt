package com.onlytechtake.repairapp.core.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.onlytechtake.repairapp.core.design.RepairAppSizes
import com.onlytechtake.repairapp.core.design.repairColors

@Composable
fun AppStepper(
    currentStep: Int,
    totalSteps: Int,
    completedStep: Int,
    onStepClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    stepTestTag: ((Int) -> String)? = null,
) {
    val semanticColors = MaterialTheme.repairColors
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        for (i in 1..totalSteps) {
            val isCompleted = i <= completedStep
            val isCurrent = i == currentStep
            val isSelectable = i <= completedStep + 1

            StepItem(
                step = i,
                isCurrent = isCurrent,
                isCompleted = isCompleted,
                isSelectable = isSelectable,
                onClick = { if (isSelectable) onStepClick(i) },
                modifier = stepTestTag?.let { Modifier.testTag(it(i)) } ?: Modifier,
            )

            if (i < totalSteps) {
                Box(
                    modifier =
                        Modifier
                            .width(24.dp)
                            .height(2.dp)
                            .background(
                                if (i <= completedStep) {
                                    semanticColors.stepCompleted
                                } else {
                                    MaterialTheme.colorScheme.outlineVariant
                                },
                            ),
                )
            }
        }
    }
}

@Composable
private fun StepItem(
    step: Int,
    isCurrent: Boolean,
    isCompleted: Boolean,
    isSelectable: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val semanticColors = MaterialTheme.repairColors

    Box(
        modifier =
            modifier
                .size(RepairAppSizes.touchTarget)
                .semantics { role = Role.Button }
                .then(if (isSelectable) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isCurrent -> MaterialTheme.colorScheme.primary
                            isCompleted -> semanticColors.stepCompleted
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        },
                    ),
            contentAlignment = Alignment.Center,
        ) {
            if (isCompleted && !isCurrent) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = semanticColors.onSuccess,
                )
            } else {
                Text(
                    text = step.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color =
                        if (isCurrent) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                )
            }
        }
    }
}

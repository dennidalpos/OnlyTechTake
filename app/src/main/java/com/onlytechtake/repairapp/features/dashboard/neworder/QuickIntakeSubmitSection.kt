package com.onlytechtake.repairapp.features.dashboard.neworder

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.onlytechtake.repairapp.features.dashboard.common.DashboardSpacing
import com.onlytechtake.repairapp.features.dashboard.common.StandardCard
import com.onlytechtake.repairapp.features.dashboard.common.StepSectionTitle
import com.onlytechtake.repairapp.features.dashboard.common.TouchButton

@Composable
fun QuickIntakeSubmitSection(
    stepNumber: Int,
    missingRequirements: List<String>,
    enabled: Boolean,
    onSubmit: () -> Unit,
) {
    StandardCard {
        Column(
            modifier = Modifier.padding(DashboardSpacing.cardPadding),
            verticalArrangement = Arrangement.spacedBy(DashboardSpacing.formGap),
        ) {
            StepSectionTitle(stepNumber, "Conferma")
            if (missingRequirements.isNotEmpty()) {
                Text(
                    text = "Mancano: ${missingRequirements.joinToString(", ")}",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            TouchButton(
                modifier = Modifier.fillMaxWidth(),
                text = "Crea commessa in carico",
                enabled = enabled,
                onClick = onSubmit,
            )
        }
    }
}

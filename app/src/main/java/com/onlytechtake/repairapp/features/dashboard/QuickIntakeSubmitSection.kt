package com.onlytechtake.repairapp.features.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun QuickIntakeSubmitSection(
    stepNumber: Int,
    missingRequirements: List<String>,
    enabled: Boolean,
    onSubmit: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            StepSectionTitle(stepNumber, "Conferma")
            if (missingRequirements.isNotEmpty()) {
                Text(
                    text = "Mancano: ${missingRequirements.joinToString(", ")}",
                    color = MaterialTheme.colorScheme.error,
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

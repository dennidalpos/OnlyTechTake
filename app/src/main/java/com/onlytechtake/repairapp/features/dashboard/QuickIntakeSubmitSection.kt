package com.onlytechtake.repairapp.features.dashboard

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun QuickIntakeSubmitSection(
    missingRequirements: List<String>,
    enabled: Boolean,
    onSubmit: () -> Unit,
) {
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

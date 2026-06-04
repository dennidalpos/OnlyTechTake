package com.onlytechtake.repairapp.features.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

@Composable
fun QuickIntakeProblemSection(
    stepNumber: Int,
    problemSummary: String,
    onProblemSummaryChange: (String) -> Unit,
    problemDetails: String,
    onProblemDetailsChange: (String) -> Unit,
    nextAction: KeyboardActions,
    doneAction: KeyboardActions,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            StepSectionTitle(stepNumber, "Problema")
            Field(
                value = problemSummary,
                onValueChange = onProblemSummaryChange,
                label = "Sintesi problema",
                required = true,
                showError = problemSummary.isBlank(),
                singleLine = true,
                imeAction = ImeAction.Next,
                keyboardActions = nextAction,
            )
            Field(
                value = problemDetails,
                onValueChange = onProblemDetailsChange,
                label = "Dettagli problema",
                imeAction = ImeAction.Done,
                keyboardActions = doneAction,
            )
        }
    }
}

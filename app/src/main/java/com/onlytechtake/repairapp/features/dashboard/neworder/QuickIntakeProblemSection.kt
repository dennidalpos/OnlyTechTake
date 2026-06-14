package com.onlytechtake.repairapp.features.dashboard.neworder

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import com.onlytechtake.repairapp.core.design.RepairAppTestTags
import com.onlytechtake.repairapp.features.dashboard.common.DashboardSpacing
import com.onlytechtake.repairapp.features.dashboard.common.Field
import com.onlytechtake.repairapp.features.dashboard.common.StandardCard
import com.onlytechtake.repairapp.features.dashboard.common.StepSectionTitle

@Composable
fun QuickIntakeProblemSection(
    stepNumber: Int,
    problemSummary: String,
    onProblemSummaryChange: (String) -> Unit,
    problemDetails: String,
    onProblemDetailsChange: (String) -> Unit,
    nextAction: KeyboardActions,
    doneAction: KeyboardActions,
    highlight: Boolean = false,
) {
    StandardCard(highlight = highlight) {
        Column(
            modifier = Modifier.padding(DashboardSpacing.cardPadding),
            verticalArrangement = Arrangement.spacedBy(DashboardSpacing.formGap),
        ) {
            StepSectionTitle(stepNumber, "Dettagli commessa")
            Field(
                value = problemSummary,
                onValueChange = onProblemSummaryChange,
                label = "Titolo lavorazione",
                required = true,
                showError = problemSummary.isBlank(),
                singleLine = true,
                imeAction = ImeAction.Next,
                keyboardActions = nextAction,
                testTag = RepairAppTestTags.NewOrderProblemSummaryField,
            )
            Field(
                value = problemDetails,
                onValueChange = onProblemDetailsChange,
                label = "Descrizione",
                showOptional = true,
                imeAction = ImeAction.Done,
                keyboardActions = doneAction,
                testTag = RepairAppTestTags.NewOrderProblemDetailsField,
            )
        }
    }
}

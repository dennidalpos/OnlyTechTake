package com.onlytechtake.repairapp.features.dashboard.neworder

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.core.design.RepairAppSpacing
import com.onlytechtake.repairapp.core.design.RepairAppTestTags
import com.onlytechtake.repairapp.core.design.components.AppTextField
import com.onlytechtake.repairapp.core.design.components.StandardCard

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
    StandardCard {
        Column(
            modifier = Modifier.padding(RepairAppSpacing.cardPadding),
            verticalArrangement = Arrangement.spacedBy(RepairAppSpacing.fieldGap),
        ) {
            Text(
                text = stringResource(R.string.intake_step_problem),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            AppTextField(
                value = problemSummary,
                onValueChange = onProblemSummaryChange,
                label = stringResource(R.string.intake_problem_title),
                required = true,
                showError = problemSummary.isBlank(),
                singleLine = true,
                imeAction = ImeAction.Next,
                keyboardActions = nextAction,
                testTag = RepairAppTestTags.NewOrderProblemSummaryField,
            )
            AppTextField(
                value = problemDetails,
                onValueChange = onProblemDetailsChange,
                label = stringResource(R.string.intake_problem_description),
                showOptional = true,
                imeAction = ImeAction.Done,
                keyboardActions = doneAction,
                testTag = RepairAppTestTags.NewOrderProblemDetailsField,
            )
        }
    }
}

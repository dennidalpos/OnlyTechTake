package com.onlytechtake.repairapp.features.dashboard.neworder

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.core.design.RepairAppTestTags
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
            StepSectionTitle(stepNumber, stringResource(R.string.intake_submit_title))
            if (missingRequirements.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.intake_submit_missing, missingRequirements.joinToString(", ")),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            TouchButton(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.intake_submit_create),
                enabled = enabled,
                fullWidth = true,
                testTag = RepairAppTestTags.IntakeSubmitButton,
                onClick = onSubmit,
            )
        }
    }
}

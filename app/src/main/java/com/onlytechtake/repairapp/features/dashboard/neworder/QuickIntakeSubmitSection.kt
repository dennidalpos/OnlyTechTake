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
import androidx.compose.ui.text.font.FontWeight
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.core.design.RepairAppTestTags
import com.onlytechtake.repairapp.core.design.RepairAppSpacing
import com.onlytechtake.repairapp.core.design.components.*

@Composable
fun QuickIntakeSubmitSection(
    stepNumber: Int,
    missingRequirements: List<String>,
    enabled: Boolean,
    onSubmit: () -> Unit,
) {
    StandardCard {
        Column(
            modifier = Modifier.padding(RepairAppSpacing.cardPadding),
            verticalArrangement = Arrangement.spacedBy(RepairAppSpacing.fieldGap),
        ) {
            Text(
                text = stringResource(R.string.intake_submit_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            if (missingRequirements.isNotEmpty()) {
                AppMessageBanner(
                    message = stringResource(R.string.intake_submit_missing, missingRequirements.joinToString(", ")),
                    type = BannerType.Warning
                )
            }
            
            AppButton(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.intake_submit_create),
                enabled = enabled,
                variant = AppButtonVariant.Primary,
                size = AppButtonSize.Default,
                testTag = RepairAppTestTags.IntakeSubmitButton,
                onClick = onSubmit,
            )
        }
    }
}

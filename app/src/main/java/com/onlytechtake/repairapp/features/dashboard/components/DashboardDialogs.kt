package com.onlytechtake.repairapp.features.dashboard.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.core.design.RepairAppTestTags
import com.onlytechtake.repairapp.core.design.components.AppButton
import com.onlytechtake.repairapp.core.design.components.AppButtonSize
import com.onlytechtake.repairapp.core.design.components.AppButtonVariant
import com.onlytechtake.repairapp.core.design.components.AppDialog

@Composable
fun UnsavedChangesDialog(
    onDismiss: () -> Unit,
    onDiscard: () -> Unit,
) {
    AppDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.dashboard_unsaved_title),
        text = stringResource(R.string.dashboard_unsaved_text),
        confirmButton = {
            AppButton(
                text = stringResource(R.string.dashboard_unsaved_leave),
                onClick = onDiscard,
                variant = AppButtonVariant.Danger,
                size = AppButtonSize.Compact,
                testTag = RepairAppTestTags.DashboardDiscardChangesButton,
            )
        },
        dismissButton = {
            AppButton(
                text = stringResource(R.string.dashboard_unsaved_continue),
                onClick = onDismiss,
                variant = AppButtonVariant.Secondary,
                size = AppButtonSize.Compact,
            )
        },
    )
}

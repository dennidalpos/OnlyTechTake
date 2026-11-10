package com.onlytechtake.repairapp.core.design.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.core.design.RepairAppShapes

@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    required: Boolean = false,
    showOptional: Boolean = false,
    showError: Boolean = false,
    singleLine: Boolean = true,
    imeAction: ImeAction = ImeAction.Default,
    keyboardType: KeyboardType = KeyboardType.Text,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.Sentences,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardActions: KeyboardActions? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    testTag: String? = null,
    fullWidth: Boolean = true,
    errorText: String? = null,
) {
    val focusManager = LocalFocusManager.current
    val effectiveImeAction = if (imeAction == ImeAction.Default) ImeAction.Next else imeAction
    val effectiveKeyboardActions =
        keyboardActions ?: KeyboardActions(
            onNext = { focusManager.moveFocus(FocusDirection.Down) },
            onDone = { focusManager.clearFocus() },
        )

    val labelRequiredSuffix = stringResource(R.string.label_required_suffix)
    val labelOptionalFormat = stringResource(R.string.label_optional_format)
    val labelOptional = stringResource(R.string.label_optional)

    val labelText =
        remember(
            label,
            required,
            showOptional,
            labelRequiredSuffix,
            labelOptionalFormat,
            labelOptional,
        ) {
            when {
                required -> {
                    label + labelRequiredSuffix
                }

                showOptional -> {
                    labelOptionalFormat.format(
                        label,
                        labelOptional,
                    )
                }

                else -> {
                    label
                }
            }
        }

    androidx.compose.foundation.layout.Column(
        modifier = modifier.then(if (fullWidth) Modifier.fillMaxWidth() else Modifier),
    ) {
        OutlinedTextField(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .then(testTag?.let { Modifier.testTag(it) } ?: Modifier),
            value = value,
            onValueChange = onValueChange,
            label = {
                Text(
                    text = labelText,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
            },
            singleLine = singleLine,
            maxLines = if (singleLine) 1 else Int.MAX_VALUE,
            isError = showError,
            shape = RepairAppShapes.field,
            colors =
                OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                ),
            keyboardOptions =
                KeyboardOptions(
                    keyboardType = keyboardType,
                    imeAction = effectiveImeAction,
                    capitalization = capitalization,
                ),
            visualTransformation = visualTransformation,
            keyboardActions = effectiveKeyboardActions,
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            textStyle = MaterialTheme.typography.bodyLarge,
        )
        if (showError && (errorText != null)) {
            Text(
                text = errorText,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp),
            )
        }
    }
}

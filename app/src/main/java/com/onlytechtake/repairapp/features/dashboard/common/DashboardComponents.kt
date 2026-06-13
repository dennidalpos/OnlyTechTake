package com.onlytechtake.repairapp.features.dashboard.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.core.design.RepairAppTestTags
import com.onlytechtake.repairapp.features.workspace.ButtonTone

object DashboardSpacing {
    val screenPadding = 16.dp
    val listGap = 12.dp
    val cardPadding = 12.dp
    val formGap = 12.dp
    val compactGap = 8.dp
    val actionGap = 8.dp
}

@Composable
fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(bottom = 4.dp),
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
fun StandardCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    highlight: Boolean = false,
    border: BorderStroke? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val effectiveBorder = border ?: if (highlight) {
        BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
    } else {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))
    }

    val cardModifier = modifier.fillMaxWidth()
    if (onClick != null) {
        Card(
            modifier = cardModifier.clickable(onClick = onClick),
            border = effectiveBorder,
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = containerColor),
            content = content
        )
    } else {
        Card(
            modifier = cardModifier,
            border = effectiveBorder,
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = containerColor),
            content = content
        )
    }
}

@Composable
fun StepSectionTitle(stepNumber: Int, text: String) {
    Text(
        text = "$stepNumber. $text",
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.secondary
    )
}

@Composable
fun Field(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    required: Boolean = false,
    showOptional: Boolean = false,
    showError: Boolean = false,
    singleLine: Boolean = false,
    imeAction: ImeAction = ImeAction.Default,
    keyboardType: KeyboardType = KeyboardType.Text,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.Sentences,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardActions: KeyboardActions? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    testTag: String? = null,
) {
    val focusManager = LocalFocusManager.current
    val effectiveImeAction = if (imeAction == ImeAction.Default) ImeAction.Next else imeAction
    val effectiveKeyboardActions = keyboardActions ?: KeyboardActions(
        onNext = { focusManager.moveFocus(FocusDirection.Down) },
        onDone = { focusManager.clearFocus() },
    )

    val labelText = when {
        required -> "$label *"
        showOptional -> "$label (${stringResource(R.string.label_optional)})"
        else -> label
    }

    OutlinedTextField(
        modifier = modifier
            .fillMaxWidth()
            .then(testTag?.let { Modifier.testTag(it) } ?: Modifier),
        value = value,
        onValueChange = onValueChange,
        label = { Text(labelText) },
        singleLine = singleLine,
        isError = showError,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
        ),
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = effectiveImeAction,
            capitalization = capitalization
        ),
        visualTransformation = visualTransformation,
        keyboardActions = effectiveKeyboardActions,
        trailingIcon = trailingIcon,
        textStyle = MaterialTheme.typography.bodyLarge
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ActionRow(content: @Composable () -> Unit) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(DashboardSpacing.actionGap),
        verticalArrangement = Arrangement.spacedBy(DashboardSpacing.actionGap),
    ) {
        content()
    }
}

@Composable
fun WizardNavigationBar(
    onBack: (() -> Unit)? = null,
    nextLabel: String = stringResource(R.string.nav_next),
    nextEnabled: Boolean = true,
    nextTestTag: String? = null,
    onNext: () -> Unit,
) {
    StandardCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(DashboardSpacing.compactGap),
            horizontalArrangement = Arrangement.spacedBy(DashboardSpacing.actionGap),
        ) {
            onBack?.let {
                TouchButton(
                    modifier = Modifier.weight(1f),
                    text = stringResource(R.string.nav_back),
                    tone = ButtonTone.NEUTRAL,
                    onClick = it,
                )
            }
            TouchButton(
                modifier = Modifier.weight(1f),
                text = nextLabel,
                enabled = nextEnabled,
                testTag = nextTestTag,
                onClick = onNext,
            )
        }
    }
}

@Composable
fun OperatorText(text: String, modifier: Modifier = Modifier) {
    Text(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(8.dp))
            .padding(10.dp),
        text = text,
        color = MaterialTheme.colorScheme.onSecondaryContainer,
    )
}

@Composable
fun CompactExpandableRow(
    title: String,
    subtitle: String,
    detail: String,
    modifier: Modifier = Modifier,
) {
    var expanded by rememberSaveable { mutableStateOf(value = false) }
    val expandedState = if (expanded) stringResource(R.string.state_expanded) else stringResource(R.string.state_collapsed)

    StandardCard(
        modifier = modifier
            .semantics {
                stateDescription = expandedState
            },
        onClick = { expanded = !expanded },
    ) {
        Column(
            modifier = Modifier.padding(DashboardSpacing.compactGap),
            verticalArrangement = Arrangement.spacedBy(DashboardSpacing.compactGap),
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (detail.isNotBlank() && expanded) {
                OperatorText(detail)
            }
        }
    }
}

@Composable
fun TouchButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tone: ButtonTone = ButtonTone.PRIMARY,
    selected: Boolean? = null,
    stateDescription: String? = null,
    testTag: String? = null,
) {
    Button(
        modifier = modifier
            .defaultMinSize(minWidth = 60.dp, minHeight = 36.dp)
            .then(testTag?.let { Modifier.testTag(it) } ?: Modifier)
            .semantics {
                selected?.let { this.selected = it }
                stateDescription?.let { this.stateDescription = it }
            },
        enabled = enabled,
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = tone.containerColor(),
            contentColor = tone.contentColor(),
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f),
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
        ),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        onClick = onClick,
    ) {
        Text(
            text = text,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

@Composable
fun TouchButtonWithIcon(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tone: ButtonTone = ButtonTone.PRIMARY,
    testTag: String? = null,
) {
    Button(
        modifier = modifier
            .defaultMinSize(minWidth = 60.dp, minHeight = 36.dp)
            .then(testTag?.let { Modifier.testTag(it) } ?: Modifier),
        enabled = enabled,
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = tone.containerColor(),
            contentColor = tone.contentColor(),
        ),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
        onClick = onClick,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.padding(end = 4.dp).size(16.dp)
        )
        Text(
            text = text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

@Composable
private fun ButtonTone.containerColor(): Color =
    when (this) {
        ButtonTone.PRIMARY -> MaterialTheme.colorScheme.primary
        ButtonTone.NEUTRAL -> MaterialTheme.colorScheme.secondaryContainer
        ButtonTone.SUCCESS -> Color(0xFF237A3B)
        ButtonTone.INFO -> MaterialTheme.colorScheme.tertiary
        ButtonTone.WARNING -> Color(0xFFE8A600)
        ButtonTone.DANGER -> MaterialTheme.colorScheme.error
    }

@Composable
private fun ButtonTone.contentColor(): Color =
    when (this) {
        ButtonTone.PRIMARY -> MaterialTheme.colorScheme.onPrimary
        ButtonTone.NEUTRAL -> MaterialTheme.colorScheme.onSecondaryContainer
        ButtonTone.INFO -> MaterialTheme.colorScheme.onTertiary
        ButtonTone.DANGER -> MaterialTheme.colorScheme.onError
        ButtonTone.WARNING -> Color(0xFF2A2100)
        ButtonTone.SUCCESS -> Color.White
    }

@Composable
fun CustomerTypeToggle(
    isBusiness: Boolean,
    onBusinessChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(DashboardSpacing.actionGap)) {
        TypeToggleButton(
            text = stringResource(R.string.customer_type_private),
            selected = !isBusiness,
            selectedTone = ButtonTone.SUCCESS,
            onClick = { onBusinessChange(false) },
            modifier = Modifier.weight(1f),
            testTag = RepairAppTestTags.CustomerPrivateToggle,
        )
        TypeToggleButton(
            text = stringResource(R.string.customer_type_business),
            selected = isBusiness,
            selectedTone = ButtonTone.INFO,
            onClick = { onBusinessChange(true) },
            modifier = Modifier.weight(1f),
            testTag = RepairAppTestTags.CustomerBusinessToggle,
        )
    }
}

@Composable
private fun TypeToggleButton(
    text: String,
    selected: Boolean,
    selectedTone: ButtonTone,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String? = null,
) {
    val selectedState = if (selected) stringResource(R.string.state_selected) else stringResource(R.string.state_not_selected)
    
    TextButton(
        modifier = modifier
            .defaultMinSize(minHeight = 40.dp)
            .then(testTag?.let { Modifier.testTag(it) } ?: Modifier)
            .semantics {
                this.selected = selected
                stateDescription = selectedState
            },
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.textButtonColors(
            containerColor = if (selected) selectedTone.containerColor() else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (selected) selectedTone.contentColor() else MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        onClick = onClick,
    ) {
        Text(text)
    }
}

@Composable
fun NoteFieldWithDialog(
    note: String,
    onNoteChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = stringResource(R.string.customer_field_notes),
    testTag: String? = null,
) {
    var showDialog by rememberSaveable { mutableStateOf(value = false) }
    var tempNote by rememberSaveable { mutableStateOf(note) }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(DashboardSpacing.compactGap)) {
        if (note.isNotBlank()) {
            OperatorText(note)
        }
        TouchButton(
            modifier = Modifier.fillMaxWidth(),
            text = if (note.isBlank()) label else "$label (${stringResource(R.string.customer_edit).lowercase()})",
            tone = ButtonTone.NEUTRAL,
            testTag = testTag,
            onClick = {
                tempNote = note
                showDialog = true
            },
        )
    }

    if (showDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(label) },
            text = {
                OutlinedTextField(
                    value = tempNote,
                    onValueChange = { tempNote = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(label) },
                    minLines = 3,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onNoteChange(tempNote)
                        showDialog = false
                    }
                ) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(stringResource(R.string.customer_cancel))
                }
            },
        )
    }
}

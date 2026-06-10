package com.onlytechtake.repairapp.features.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.onlytechtake.repairapp.core.design.RepairAppTestTags
import com.onlytechtake.repairapp.data.local.entities.CustomerEntity
import com.onlytechtake.repairapp.data.local.entities.CustomerType

object DashboardSpacing {
    val screenPadding = 20.dp
    val listGap = 16.dp
    val cardPadding = 16.dp
    val formGap = 14.dp
    val compactGap = 10.dp
    val actionGap = 10.dp
}

@Composable
fun SectionTitle(text: String) {
    Text(text = text, style = MaterialTheme.typography.titleLarge)
}

@Composable
fun StepSectionTitle(stepNumber: Int, text: String) {
    Text(text = "$stepNumber. $text", style = MaterialTheme.typography.titleMedium)
}

@Composable
fun Field(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    required: Boolean = false,
    showError: Boolean = false,
    singleLine: Boolean = false,
    imeAction: ImeAction = ImeAction.Default,
    keyboardType: KeyboardType = KeyboardType.Text,
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

    OutlinedTextField(
        modifier = modifier
            .fillMaxWidth()
            .then(testTag?.let { Modifier.testTag(it) } ?: Modifier),
        value = value,
        onValueChange = onValueChange,
        label = { Text(if (required) "$label *" else label) },
        singleLine = singleLine,
        isError = showError,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = effectiveImeAction),
        visualTransformation = visualTransformation,
        keyboardActions = effectiveKeyboardActions,
        trailingIcon = trailingIcon,
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
    nextLabel: String = "Avanti",
    nextEnabled: Boolean = true,
    nextTestTag: String? = null,
    onNext: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(DashboardSpacing.compactGap),
            horizontalArrangement = Arrangement.spacedBy(DashboardSpacing.actionGap),
        ) {
            onBack?.let {
                TouchButton(
                    modifier = Modifier.weight(1f),
                    text = "Indietro",
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
    var expanded by rememberSaveable { mutableStateOf(false) }
    Card(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                stateDescription = if (expanded) "Espanso" else "Compresso"
            }
            .clickable(
                onClickLabel = if (expanded) "Comprimi dettaglio" else "Espandi dettaglio",
                role = Role.Button,
                onClick = { expanded = !expanded },
            ),
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(
            modifier = Modifier.padding(DashboardSpacing.compactGap),
            verticalArrangement = Arrangement.spacedBy(DashboardSpacing.compactGap),
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (detail.isNotBlank()) {
                if (expanded) {
                    OperatorText(detail)
                } else {
                    Text(detail, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
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
    androidx.compose.material3.Button(
        modifier = modifier
            .defaultMinSize(minWidth = 96.dp, minHeight = 56.dp)
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
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f),
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.62f),
        ),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
        onClick = onClick,
    ) {
        Text(
            text = text,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

enum class ButtonTone {
    PRIMARY,
    NEUTRAL,
    SUCCESS,
    INFO,
    WARNING,
    DANGER,
}

@Composable
private fun ButtonTone.containerColor(): Color =
    when (this) {
        ButtonTone.PRIMARY -> MaterialTheme.colorScheme.primary
        ButtonTone.NEUTRAL -> MaterialTheme.colorScheme.surfaceVariant
        ButtonTone.SUCCESS -> Color(0xFF237A3B)
        ButtonTone.INFO -> MaterialTheme.colorScheme.tertiary
        ButtonTone.WARNING -> Color(0xFFE8A600)
        ButtonTone.DANGER -> MaterialTheme.colorScheme.error
    }

@Composable
private fun ButtonTone.contentColor(): Color =
    when (this) {
        ButtonTone.PRIMARY -> MaterialTheme.colorScheme.onPrimary
        ButtonTone.NEUTRAL -> MaterialTheme.colorScheme.onSurfaceVariant
        ButtonTone.INFO -> MaterialTheme.colorScheme.onTertiary
        ButtonTone.DANGER -> MaterialTheme.colorScheme.onError
        ButtonTone.WARNING -> Color(0xFF2A2100)
        else -> Color.White
    }

@Composable
fun CustomerTypeToggle(
    isBusiness: Boolean,
    onBusinessChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(DashboardSpacing.actionGap)) {
        TypeToggleButton(
            text = "Privato",
            selected = !isBusiness,
            selectedTone = ButtonTone.SUCCESS,
            onClick = { onBusinessChange(false) },
            modifier = Modifier.weight(1f),
            testTag = RepairAppTestTags.CustomerPrivateToggle,
        )
        TypeToggleButton(
            text = "Azienda",
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
    TextButton(
        modifier = modifier
            .defaultMinSize(minHeight = 56.dp)
            .then(testTag?.let { Modifier.testTag(it) } ?: Modifier)
            .semantics {
                this.selected = selected
                stateDescription = if (selected) "Selezionato" else "Non selezionato"
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

fun CustomerEntity.displayName(): String =
    when (type) {
        CustomerType.PRIVATE -> listOfNotNull(firstName, lastName).joinToString(" ").ifBlank { "Privato senza nome" }
        CustomerType.BUSINESS -> companyName.orEmpty().ifBlank { "Azienda senza nome" }
    }

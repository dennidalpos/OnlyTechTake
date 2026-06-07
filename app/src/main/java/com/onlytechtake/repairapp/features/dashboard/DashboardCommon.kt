package com.onlytechtake.repairapp.features.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.onlytechtake.repairapp.data.local.entities.CustomerEntity
import com.onlytechtake.repairapp.data.local.entities.CustomerType

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
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    trailingIcon: (@Composable () -> Unit)? = null,
) {
    OutlinedTextField(
        modifier = modifier.fillMaxWidth(),
        value = value,
        onValueChange = onValueChange,
        label = { Text(if (required) "$label *" else label) },
        singleLine = singleLine,
        isError = showError,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        visualTransformation = visualTransformation,
        keyboardActions = keyboardActions,
        trailingIcon = trailingIcon,
    )
}

@Composable
fun ActionRow(content: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        content()
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
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
) {
    androidx.compose.material3.Button(
        modifier = modifier
            .defaultMinSize(minHeight = 56.dp)
            .semantics {
                selected?.let { this.selected = it }
                stateDescription?.let { this.stateDescription = it }
            },
        enabled = enabled,
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = tone.containerColor(),
            contentColor = tone.contentColor(),
        ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        onClick = onClick,
    ) {
        Text(text)
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
        ButtonTone.NEUTRAL -> MaterialTheme.colorScheme.secondary
        ButtonTone.SUCCESS -> Color(0xFF2E7D32)
        ButtonTone.INFO -> Color(0xFF1565C0)
        ButtonTone.WARNING -> Color(0xFFF9A825)
        ButtonTone.DANGER -> MaterialTheme.colorScheme.error
    }

@Composable
private fun ButtonTone.contentColor(): Color =
    when (this) {
        ButtonTone.WARNING -> Color(0xFF1F1F1F)
        else -> Color.White
    }

@Composable
fun CustomerTypeToggle(
    isBusiness: Boolean,
    onBusinessChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TypeToggleButton(
            text = "Privato",
            selected = !isBusiness,
            selectedContainer = Color(0xFF2E7D32),
            onClick = { onBusinessChange(false) },
            modifier = Modifier.weight(1f),
        )
        TypeToggleButton(
            text = "Azienda",
            selected = isBusiness,
            selectedContainer = Color(0xFFF9A825),
            onClick = { onBusinessChange(true) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun TypeToggleButton(
    text: String,
    selected: Boolean,
    selectedContainer: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TextButton(
        modifier = modifier
            .defaultMinSize(minHeight = 56.dp)
            .semantics {
                this.selected = selected
                stateDescription = if (selected) "Selezionato" else "Non selezionato"
            },
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.textButtonColors(
            containerColor = if (selected) selectedContainer else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
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

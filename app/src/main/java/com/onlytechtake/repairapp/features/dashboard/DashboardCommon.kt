package com.onlytechtake.repairapp.features.dashboard

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
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
fun TouchButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tone: ButtonTone = ButtonTone.PRIMARY,
) {
    androidx.compose.material3.Button(
        modifier = modifier.defaultMinSize(minHeight = 56.dp),
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
            .semantics { this.selected = selected },
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

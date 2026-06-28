package com.onlytechtake.repairapp.core.design.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.core.design.RepairAppShapes
import com.onlytechtake.repairapp.core.design.RepairAppTestTags
import com.onlytechtake.repairapp.core.design.repairColors
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

enum class BannerType {
    Info,
    Success,
    Warning,
    Error,
}

@Composable
fun AppMessageBanner(
    message: String,
    type: BannerType,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    val colors =
        when (type) {
            BannerType.Info -> {
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }

            BannerType.Success -> {
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.repairColors.successContainer,
                    contentColor = MaterialTheme.repairColors.onSuccessContainer,
                )
            }

            BannerType.Warning -> {
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.repairColors.warningContainer,
                    contentColor = MaterialTheme.repairColors.onWarningContainer,
                )
            }

            BannerType.Error -> {
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        }

    val icon: ImageVector =
        when (type) {
            BannerType.Info -> Icons.Default.Info
            BannerType.Success -> Icons.Default.CheckCircle
            BannerType.Warning -> Icons.Default.Warning
            BannerType.Error -> Icons.Default.Error
        }

    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .semantics { liveRegion = LiveRegionMode.Polite },
        shape = RepairAppShapes.card,
        colors = colors,
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            action?.invoke()
        }
    }
}

@Composable
fun DismissibleAppMessageBanner(
    message: String,
    type: BannerType,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    autoDismissMillis: Long = if (type == BannerType.Error) 8_000L else 4_000L,
) {
    LaunchedEffect(message, type) {
        if (autoDismissMillis > 0L) {
            delay(autoDismissMillis.milliseconds)
            onDismiss()
        }
    }

    AppMessageBanner(
        message = message,
        type = type,
        modifier = modifier,
        action = {
            AppIconButton(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(R.string.action_close),
                onClick = onDismiss,
                tint = LocalContentColor.current,
                modifier = Modifier.testTag(RepairAppTestTags.MessageBannerCloseButton),
            )
        },
    )
}

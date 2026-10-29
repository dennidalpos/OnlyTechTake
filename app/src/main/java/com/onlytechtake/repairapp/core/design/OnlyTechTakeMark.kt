package com.onlytechtake.repairapp.core.design

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.onlytechtake.repairapp.R
import kotlin.math.roundToInt

@Composable
fun OnlyTechTakeMark(
    modifier: Modifier = Modifier,
) {
    Image(
        painter = painterResource(id = R.drawable.ic_brand_badge),
        contentDescription = null,
        modifier = modifier,
    )
}

@Composable
fun LoadingBrandIndicator(
    modifier: Modifier = Modifier,
    text: String = stringResource(R.string.auth_system_ready),
    contentDescription: String = text,
    size: Dp = 140.dp,
) {
    var targetProgress by remember { mutableFloatStateOf(0f) }
    val progress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = 1500, easing = LinearEasing),
        label = "onlytechtake-loading-progress",
    )

    LaunchedEffect(Unit) {
        targetProgress = 1f
    }

    val percentage = (progress * 100).roundToInt()

    Column(
        modifier =
            modifier.semantics {
                this.contentDescription = "$contentDescription $percentage%"
                liveRegion = LiveRegionMode.Polite
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = { progress },
                modifier = Modifier.size(size),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round,
                strokeWidth = 6.dp,
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                OnlyTechTakeMark(modifier = Modifier.size(size * 0.4f))
                Text(
                    text = "$percentage%",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        Spacer(modifier = Modifier.size(24.dp))
        Text(
            text =
                when {
                    progress < 0.3f -> stringResource(R.string.loading_init_db)
                    progress < 0.6f -> stringResource(R.string.loading_verify_session)
                    progress < 0.9f -> stringResource(R.string.loading_load_workspace)
                    else -> stringResource(R.string.loading_system_ready)
                },
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

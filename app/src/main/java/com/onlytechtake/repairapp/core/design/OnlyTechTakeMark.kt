package com.onlytechtake.repairapp.core.design

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun OnlyTechTakeMark(
    modifier: Modifier = Modifier,
    paperColor: Color = MaterialTheme.colorScheme.surface,
    primaryColor: Color = MaterialTheme.colorScheme.primary,
    textColor: Color = MaterialTheme.colorScheme.secondary,
    accentColor: Color = MaterialTheme.colorScheme.tertiary,
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val documentLeft = w * 0.24f
        val documentTop = h * 0.12f
        val documentWidth = w * 0.52f
        val documentHeight = h * 0.72f
        val radius = w * 0.08f

        drawRoundRect(
            color = paperColor,
            topLeft = Offset(documentLeft, documentTop),
            size = Size(documentWidth, documentHeight),
            cornerRadius = CornerRadius(radius, radius),
        )
        drawRoundRect(
            color = textColor.copy(alpha = 0.14f),
            topLeft = Offset(documentLeft, documentTop),
            size = Size(documentWidth, documentHeight),
            cornerRadius = CornerRadius(radius, radius),
            style = Stroke(width = w * 0.025f),
        )
        drawRoundRect(
            color = primaryColor,
            topLeft = Offset(w * 0.32f, h * 0.30f),
            size = Size(w * 0.36f, h * 0.065f),
            cornerRadius = CornerRadius(w * 0.012f, w * 0.012f),
        )
        drawRoundRect(
            color = textColor,
            topLeft = Offset(w * 0.33f, h * 0.49f),
            size = Size(w * 0.24f, h * 0.055f),
            cornerRadius = CornerRadius(w * 0.01f, w * 0.01f),
        )
        drawRoundRect(
            color = textColor,
            topLeft = Offset(w * 0.33f, h * 0.61f),
            size = Size(w * 0.17f, h * 0.055f),
            cornerRadius = CornerRadius(w * 0.01f, w * 0.01f),
        )

        val check = Path().apply {
            moveTo(w * 0.61f, h * 0.76f)
            lineTo(w * 0.72f, h * 0.86f)
            lineTo(w * 0.96f, h * 0.55f)
        }
        drawPath(
            path = check,
            color = accentColor,
            style = Stroke(width = w * 0.09f, cap = StrokeCap.Square),
        )
    }
}

@Composable
fun LoadingBrandIndicator(
    modifier: Modifier = Modifier,
    text: String = "Caricamento app",
    contentDescription: String = text,
    size: Dp = 96.dp,
) {
    val transition = rememberInfiniteTransition(label = "onlytechtake-loading")
    val progress by transition.animateFloat(
        initialValue = 0.18f,
        targetValue = 0.86f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "onlytechtake-loading-progress",
    )

    Column(
        modifier = modifier.semantics {
            this.contentDescription = contentDescription
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
            )
            OnlyTechTakeMark(modifier = Modifier.size(size * 0.62f))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

package com.onlytechtake.repairapp.features.dashboard

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path as ComposePath
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.graphics.createBitmap
import com.onlytechtake.repairapp.data.local.entities.SignatureEntity

data class SignatureCaptureResult(
    val signerName: String,
    val privacyAccepted: Boolean,
    val termsAccepted: Boolean,
    val bitmap: Bitmap,
)

@Composable
fun SignatureCaptureForm(
    privacyText: String,
    termsText: String,
    modifier: Modifier = Modifier,
    title: String = "Privacy e firma",
    existingSignatures: List<SignatureEntity> = emptyList(),
    submitText: String? = null,
    onSubmit: (SignatureCaptureResult) -> Unit = {},
    onCaptureChanged: (SignatureCaptureResult?) -> Unit = {},
) {
    var signerName by rememberSaveable { mutableStateOf("") }
    var privacyAccepted by rememberSaveable { mutableStateOf(false) }
    var termsAccepted by rememberSaveable { mutableStateOf(false) }
    var strokes by remember { mutableStateOf<List<SignatureStroke>>(emptyList()) }
    var currentStroke by remember { mutableStateOf<SignatureStroke?>(null) }
    val hasSignature = strokes.any { it.points.size > 1 }
    val capture = if (signerName.isNotBlank() && privacyAccepted && termsAccepted && hasSignature) {
        SignatureCaptureResult(
            signerName = signerName,
            privacyAccepted = privacyAccepted,
            termsAccepted = termsAccepted,
            bitmap = renderSignatureBitmap(strokes),
        )
    } else {
        null
    }

    LaunchedEffect(signerName, privacyAccepted, termsAccepted, strokes) {
        onCaptureChanged(capture)
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle(title)
        existingSignatures.forEach { signature ->
            Text("${signature.signerName} - ${signature.fileName} - v privacy ${signature.privacyTemplateVersion}")
        }
        if (existingSignatures.isEmpty() && submitText != null) {
            Text("Nessuna firma acquisita.")
        }

        Text(privacyText, style = MaterialTheme.typography.bodySmall)
        Text(termsText, style = MaterialTheme.typography.bodySmall)
        Field(signerName, { signerName = it }, "Nome firmatario")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(
                    value = privacyAccepted,
                    role = Role.Checkbox,
                    onValueChange = { privacyAccepted = it },
                ),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = privacyAccepted, onCheckedChange = null)
            Text("Accetta informativa privacy")
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(
                    value = termsAccepted,
                    role = Role.Checkbox,
                    onValueChange = { termsAccepted = it },
                ),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = termsAccepted, onCheckedChange = null)
            Text("Accetta condizioni di presa in carico")
        }

        SignatureCanvas(
            strokes = strokes,
            currentStroke = currentStroke,
            onStart = { point -> currentStroke = SignatureStroke(listOf(point)) },
            onDrag = { point ->
                currentStroke = currentStroke?.let { it.copy(points = it.points + point) }
            },
            onEnd = {
                currentStroke?.takeIf { it.points.size > 1 }?.let { strokes = strokes + it }
                currentStroke = null
            },
        )

        ActionRow {
            OutlinedButton(
                onClick = {
                    strokes = emptyList()
                    currentStroke = null
                },
            ) {
                Text("Pulisci firma")
            }
            if (submitText != null) {
                Button(
                    enabled = capture != null,
                    onClick = { capture?.let(onSubmit) },
                ) {
                    Text(submitText)
                }
            }
        }
    }
}

@Composable
private fun SignatureCanvas(
    strokes: List<SignatureStroke>,
    currentStroke: SignatureStroke?,
    onStart: (SignaturePoint) -> Unit,
    onDrag: (SignaturePoint) -> Unit,
    onEnd: () -> Unit,
) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .background(androidx.compose.ui.graphics.Color.White)
            .border(1.dp, MaterialTheme.colorScheme.outline)
            .semantics {
                contentDescription = "Area firma cliente. Tracciare la firma con dito o pennino."
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        onStart(offset.toSignaturePoint(size.width, size.height))
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        onDrag(change.position.toSignaturePoint(size.width, size.height))
                    },
                    onDragEnd = onEnd,
                    onDragCancel = onEnd,
                )
            },
    ) {
        (strokes + listOfNotNull(currentStroke)).forEach { stroke ->
            drawPath(
                path = stroke.toComposePath(size.width, size.height),
                color = androidx.compose.ui.graphics.Color.Black,
                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }
    }
}

private fun renderSignatureBitmap(strokes: List<SignatureStroke>) =
    createBitmap(BITMAP_WIDTH, BITMAP_HEIGHT).also { bitmap ->
        val canvas = AndroidCanvas(bitmap)
        canvas.drawColor(Color.WHITE)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            strokeWidth = BITMAP_STROKE_WIDTH
        }
        strokes.forEach { stroke ->
            canvas.drawPath(stroke.toAndroidPath(BITMAP_WIDTH.toFloat(), BITMAP_HEIGHT.toFloat()), paint)
        }
    }

private data class SignatureStroke(
    val points: List<SignaturePoint>,
)

private data class SignaturePoint(
    val x: Float,
    val y: Float,
)

private fun Offset.toSignaturePoint(width: Int, height: Int): SignaturePoint =
    SignaturePoint(
        x = (x / width.coerceAtLeast(1)).coerceIn(0f, 1f),
        y = (y / height.coerceAtLeast(1)).coerceIn(0f, 1f),
    )

private fun SignatureStroke.toComposePath(width: Float, height: Float): ComposePath =
    ComposePath().also { path ->
        points.firstOrNull()?.let { first ->
            path.moveTo(first.x * width, first.y * height)
            points.drop(1).forEach { point -> path.lineTo(point.x * width, point.y * height) }
        }
    }

private fun SignatureStroke.toAndroidPath(width: Float, height: Float): Path =
    Path().also { path ->
        points.firstOrNull()?.let { first ->
            path.moveTo(first.x * width, first.y * height)
            points.drop(1).forEach { point -> path.lineTo(point.x * width, point.y * height) }
        }
    }

private const val BITMAP_WIDTH = 1200
private const val BITMAP_HEIGHT = 400
private const val BITMAP_STROKE_WIDTH = 8f

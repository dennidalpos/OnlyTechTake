package com.onlytechtake.repairapp.features.dashboard.common

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.graphics.createBitmap
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.core.design.RepairAppSpacing
import com.onlytechtake.repairapp.core.design.RepairAppTestTags
import com.onlytechtake.repairapp.core.design.components.AppButton
import com.onlytechtake.repairapp.core.design.components.AppButtonSize
import com.onlytechtake.repairapp.core.design.components.AppButtonVariant
import com.onlytechtake.repairapp.core.design.components.AppDialog
import com.onlytechtake.repairapp.core.design.components.AppTextField
import com.onlytechtake.repairapp.core.design.components.StandardCard
import com.onlytechtake.repairapp.core.utils.formatFileSize
import com.onlytechtake.repairapp.core.utils.formatUiDateTime
import com.onlytechtake.repairapp.data.local.entities.SignatureEntity
import android.graphics.Canvas as AndroidCanvas
import androidx.compose.ui.graphics.Path as ComposePath

data class SignatureCaptureResult(
    val signerName: String,
    val businessSignerName: String? = null,
    val privacyAccepted: Boolean,
    val termsAccepted: Boolean,
    val bitmap: Bitmap,
)

@Composable
fun SignatureCaptureForm(
    privacyText: String,
    termsText: String,
    modifier: Modifier = Modifier,
    title: String = stringResource(R.string.signature_form_title),
    isBusiness: Boolean = false,
    existingSignatures: List<SignatureEntity> = emptyList(),
    submitText: String? = null,
    onSubmit: (SignatureCaptureResult) -> Unit = {},
    onCaptureChanged: (SignatureCaptureResult?) -> Unit = {},
) {
    var businessSignerName by rememberSaveable { mutableStateOf("") }
    var privacyAccepted by rememberSaveable { mutableStateOf(value = false) }
    var termsAccepted by rememberSaveable { mutableStateOf(value = false) }
    var textDialog by rememberSaveable { mutableStateOf<SignatureTextDialog?>(null) }
    var selectedSignaturePath by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedSignatureTitle by rememberSaveable { mutableStateOf("") }
    var strokes by remember { mutableStateOf<List<SignatureStroke>>(emptyList()) }
    var currentStroke by remember { mutableStateOf<SignatureStroke?>(null) }
    val hasSignature = strokes.any { it.points.size > 1 }
    val defaultSignerName = stringResource(R.string.signature_default_signer)
    val capture =
        if (privacyAccepted && termsAccepted && hasSignature) {
            SignatureCaptureResult(
                signerName = defaultSignerName,
                businessSignerName = businessSignerName.takeIf { isBusiness && it.isNotBlank() },
                privacyAccepted = privacyAccepted,
                termsAccepted = termsAccepted,
                bitmap = renderSignatureBitmap(strokes),
            )
        } else {
            null
        }

    LaunchedEffect(privacyAccepted, termsAccepted, strokes, businessSignerName) {
        onCaptureChanged(capture)
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(RepairAppSpacing.compactGap),
    ) {
        SectionTitle(title)

        if (isBusiness) {
            AppTextField(
                value = businessSignerName,
                onValueChange = { businessSignerName = it },
                label = stringResource(R.string.signature_business_signer_label),
                singleLine = true,
            )
        }
        existingSignatures.forEach { signature ->
            val desc = stringResource(R.string.signature_signer_label, signature.signerName)
            StandardCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(RepairAppSpacing.compactGap),
                    verticalArrangement = Arrangement.spacedBy(RepairAppSpacing.compactGap),
                ) {
                    LocalImagePreview(
                        path = signature.localPath,
                        description = desc,
                    ) {
                        selectedSignaturePath = signature.localPath
                        selectedSignatureTitle = desc
                    }
                    Text(
                        stringResource(
                            R.string.signature_info_main,
                            signature.signerName,
                            formatUiDateTime(signature.signedAt),
                        ),
                    )
                    Text(
                        stringResource(
                            R.string.signature_info_meta,
                            signature.fileName,
                            formatFileSize(signature.sizeBytes),
                            signature.privacyTemplateVersion,
                        ),
                    )
                }
            }
        }
        if ((existingSignatures.isEmpty() && submitText != null)) {
            Text(stringResource(R.string.signature_empty))
        }

        Text(
            modifier =
                Modifier.clickable(
                    onClickLabel = stringResource(R.string.signature_read_privacy),
                    role = Role.Button,
                    onClick = { textDialog = SignatureTextDialog.PRIVACY },
                ),
            text = stringResource(R.string.signature_read_privacy),
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.bodySmall,
        )
        Text(
            modifier =
                Modifier.clickable(
                    onClickLabel = stringResource(R.string.signature_read_terms),
                    role = Role.Button,
                    onClick = { textDialog = SignatureTextDialog.TERMS },
                ),
            text = stringResource(R.string.signature_read_terms),
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.bodySmall,
        )
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = privacyAccepted,
                        role = Role.Checkbox,
                        onValueChange = { privacyAccepted = it },
                    ),
            horizontalArrangement = Arrangement.spacedBy(RepairAppSpacing.compactGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = privacyAccepted, onCheckedChange = null)
            Text(stringResource(R.string.signature_accept_privacy))
        }
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = termsAccepted,
                        role = Role.Checkbox,
                        onValueChange = { termsAccepted = it },
                    ),
            horizontalArrangement = Arrangement.spacedBy(RepairAppSpacing.compactGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = termsAccepted, onCheckedChange = null)
            Text(stringResource(R.string.signature_accept_terms))
        }

        SignatureCanvas(
            strokes = strokes,
            currentStroke = currentStroke,
            onStart = { point -> currentStroke = SignatureStroke(listOf(point)) },
            onTap = { point ->
                strokes +=
                    SignatureStroke(
                        listOf(
                            point,
                            point.copy(x = (point.x + SIGNATURE_DOT_DELTA).coerceAtMost(1f)),
                        ),
                    )
            },
            onDrag = { point ->
                currentStroke = currentStroke?.let { it.copy(points = it.points + point) }
            },
            onEnd = {
                currentStroke?.takeIf { it.points.size > 1 }?.let { strokes += it }
                currentStroke = null
            },
        )

        ActionRow {
            AppButton(
                text = stringResource(R.string.signature_clear),
                onClick = {
                    strokes = emptyList()
                    currentStroke = null
                },
                variant = AppButtonVariant.Secondary,
                size = AppButtonSize.Compact,
            )
            submitText?.let {
                AppButton(
                    text = it,
                    enabled = capture != null,
                    onClick = { capture?.let(onSubmit) },
                    variant = AppButtonVariant.Primary,
                    size = AppButtonSize.Compact,
                )
            }
        }
    }

    selectedSignaturePath?.let { path ->
        LocalImageViewer(
            path = path,
            title = selectedSignatureTitle,
            onDismiss = { selectedSignaturePath = null },
        )
    }

    textDialog?.let { dialog ->
        AppDialog(
            onDismissRequest = { textDialog = null },
            title =
                when (dialog) {
                    SignatureTextDialog.PRIVACY -> stringResource(R.string.signature_privacy_title)
                    SignatureTextDialog.TERMS -> stringResource(R.string.signature_terms_title)
                },
            text =
                when (dialog) {
                    SignatureTextDialog.PRIVACY -> privacyText
                    SignatureTextDialog.TERMS -> termsText
                },
            confirmButton = {
                AppButton(
                    text = stringResource(R.string.action_close),
                    onClick = { textDialog = null },
                    variant = AppButtonVariant.Primary,
                    size = AppButtonSize.Compact,
                )
            },
        )
    }
}

@Composable
private fun SignatureCanvas(
    strokes: List<SignatureStroke>,
    currentStroke: SignatureStroke?,
    onStart: (SignaturePoint) -> Unit,
    onTap: (SignaturePoint) -> Unit,
    onDrag: (SignaturePoint) -> Unit,
    onEnd: () -> Unit,
) {
    val canvasDescription = stringResource(R.string.signature_canvas_description)
    Canvas(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(180.dp)
                .testTag(RepairAppTestTags.SignatureCanvas)
                .background(androidx.compose.ui.graphics.Color.White)
                .border(1.dp, MaterialTheme.colorScheme.outline)
                .semantics {
                    contentDescription = canvasDescription
                }
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        onTap(offset.toSignaturePoint(size.width, size.height))
                    }
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
        val paint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
                strokeWidth = BITMAP_STROKE_WIDTH
            }
        strokes.forEach { stroke ->
            canvas.drawPath(
                stroke.toAndroidPath(BITMAP_WIDTH.toFloat(), BITMAP_HEIGHT.toFloat()),
                paint,
            )
        }
    }

private data class SignatureStroke(
    val points: List<SignaturePoint>,
)

private data class SignaturePoint(
    val x: Float,
    val y: Float,
)

private enum class SignatureTextDialog {
    PRIVACY,
    TERMS,
}

private fun Offset.toSignaturePoint(
    width: Int,
    height: Int,
): SignaturePoint =
    SignaturePoint(
        x = (x / width.coerceAtLeast(1)).coerceIn(0f, 1f),
        y = (y / height.coerceAtLeast(1)).coerceIn(0f, 1f),
    )

private fun SignatureStroke.toComposePath(
    width: Float,
    height: Float,
): ComposePath =
    ComposePath().also { path ->
        points.firstOrNull()?.let { first ->
            path.moveTo(first.x * width, first.y * height)
            points.drop(1).forEach { point -> path.lineTo(point.x * width, point.y * height) }
        }
    }

private fun SignatureStroke.toAndroidPath(
    width: Float,
    height: Float,
): Path =
    Path().also { path ->
        points.firstOrNull()?.let { first ->
            path.moveTo(first.x * width, first.y * height)
            points.drop(1).forEach { point -> path.lineTo(point.x * width, point.y * height) }
        }
    }

private const val BITMAP_WIDTH = 1200
private const val BITMAP_HEIGHT = 400
private const val BITMAP_STROKE_WIDTH = 8f
private const val SIGNATURE_DOT_DELTA = 0.001f

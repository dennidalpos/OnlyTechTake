package com.onlytechtake.repairapp.features.dashboard.common

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.LayersClear
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.core.design.components.AppButton
import com.onlytechtake.repairapp.core.design.components.AppButtonSize
import com.onlytechtake.repairapp.core.design.components.AppButtonVariant
import com.onlytechtake.repairapp.core.design.components.AppDialog
import com.onlytechtake.repairapp.core.design.components.AppIconButton
import com.onlytechtake.repairapp.core.media.ARROW_HEAD_ANGLE
import com.onlytechtake.repairapp.core.media.ImageEditArrow
import com.onlytechtake.repairapp.core.media.ImageEditMode
import com.onlytechtake.repairapp.core.media.MIN_DRAG_DISTANCE
import com.onlytechtake.repairapp.core.media.buildEditedBitmap
import com.onlytechtake.repairapp.core.media.decodeOrientedBitmap
import com.onlytechtake.repairapp.core.media.imagePlacement
import com.onlytechtake.repairapp.core.media.normalizeBitmapRect
import com.onlytechtake.repairapp.core.media.rotateRight
import com.onlytechtake.repairapp.core.media.toBitmapOffset
import com.onlytechtake.repairapp.core.media.toCanvasOffset
import com.onlytechtake.repairapp.core.media.toCanvasSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun LocalImagePreview(
    path: String,
    description: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val bitmap by rememberDecodedLocalBitmap(path, PREVIEW_MAX_DIMENSION)
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(160.dp)
                .then(
                    if (onClick != null) {
                        Modifier.clickable(role = Role.Button, onClick = onClick)
                    } else {
                        Modifier
                    },
                ),
        contentAlignment = Alignment.Center,
    ) {
        val previewBitmap = bitmap
        if (previewBitmap == null) {
            Text(
                stringResource(R.string.image_preview_unavailable),
                style = MaterialTheme.typography.bodySmall,
            )
        } else {
            Image(
                bitmap = previewBitmap.asImageBitmap(),
                contentDescription = description,
                modifier = Modifier.fillMaxWidth(),
                contentScale = ContentScale.Fit,
            )
        }
    }
}

@Composable
fun LocalImageViewer(
    path: String,
    title: String,
    onDismiss: () -> Unit,
    onSaveEdited: ((Bitmap) -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
) {
    var buildEditedBitmap by remember(path) { mutableStateOf<(() -> Bitmap?)?>(null) }
    var editedBitmapToConfirm by remember(path) { mutableStateOf<Bitmap?>(null) }
    AppDialog(
        onDismissRequest = onDismiss,
        title = title,
        content = {
            if (onSaveEdited == null) {
                ZoomableLocalImage(
                    path = path,
                    description = title,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(360.dp)
                            .padding(top = 8.dp),
                )
            } else {
                EditableLocalImage(
                    path = path,
                    description = title,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(420.dp)
                            .padding(top = 8.dp),
                ) { buildEditedBitmap = it }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                onDelete?.let { delete ->
                    AppButton(
                        text = stringResource(R.string.content_description_delete),
                        onClick = {
                            delete()
                            onDismiss()
                        },
                        variant = AppButtonVariant.Danger,
                        size = AppButtonSize.Compact,
                    )
                }
                if (onSaveEdited != null) {
                    AppButton(
                        text = stringResource(R.string.customer_save),
                        onClick = {
                            editedBitmapToConfirm = buildEditedBitmap?.invoke()
                        },
                        variant = AppButtonVariant.Primary,
                        size = AppButtonSize.Compact,
                    )
                }
                AppButton(
                    text = stringResource(R.string.action_close),
                    onClick = onDismiss,
                    variant = AppButtonVariant.Secondary,
                    size = AppButtonSize.Compact,
                )
            }
        },
    )

    editedBitmapToConfirm?.let { bitmap ->
        AppDialog(
            onDismissRequest = { editedBitmapToConfirm = null },
            title = stringResource(R.string.image_replace_confirm_title),
            text = stringResource(R.string.image_replace_confirm_text),
            confirmButton = {
                AppButton(
                    text = stringResource(R.string.image_replace_confirm_action),
                    onClick = {
                        onSaveEdited?.invoke(bitmap)
                        editedBitmapToConfirm = null
                        onDismiss()
                    },
                    variant = AppButtonVariant.Primary,
                    size = AppButtonSize.Compact,
                )
            },
            dismissButton = {
                AppButton(
                    text = stringResource(R.string.customer_cancel),
                    onClick = { editedBitmapToConfirm = null },
                    variant = AppButtonVariant.Secondary,
                    size = AppButtonSize.Compact,
                )
            },
        )
    }
}

@Composable
private fun EditableLocalImage(
    path: String,
    description: String,
    modifier: Modifier = Modifier,
    onBuildEditedBitmapChange: (() -> Bitmap?) -> Unit,
) {
    val decodedBitmap by rememberDecodedLocalBitmap(path, EDIT_MAX_DIMENSION)
    var bitmap by remember(path) { mutableStateOf<Bitmap?>(null) }
    var editMode by remember(path) { mutableStateOf(ImageEditMode.ARROW) }
    var arrows by remember(path) { mutableStateOf(emptyList<ImageEditArrow>()) }
    var cropRect by remember(path) { mutableStateOf<Rect?>(null) }
    var dragStart by remember(path) { mutableStateOf<Offset?>(null) }
    var dragCurrent by remember(path) { mutableStateOf<Offset?>(null) }
    var canvasSize by remember(path) { mutableStateOf(IntSize.Zero) }

    LaunchedEffect(decodedBitmap) {
        bitmap = decodedBitmap?.copy(Bitmap.Config.ARGB_8888, true)
    }

    SideEffect {
        onBuildEditedBitmapChange {
            bitmap?.let { source -> buildEditedBitmap(source, arrows, cropRect) }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ActionRow {
            AppIconButton(
                imageVector = Icons.Default.Refresh,
                contentDescription = stringResource(R.string.image_edit_rotate),
                onClick = {
                    bitmap = bitmap?.rotateRight()
                    arrows = emptyList()
                    cropRect = null
                },
                tint = MaterialTheme.colorScheme.primary,
            )
            AppIconButton(
                imageVector = Icons.Default.Crop,
                contentDescription = stringResource(R.string.image_edit_crop),
                onClick = { editMode = ImageEditMode.CROP },
                tint = if (editMode == ImageEditMode.CROP) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            AppIconButton(
                imageVector = Icons.AutoMirrored.Filled.TrendingFlat,
                contentDescription = stringResource(R.string.image_edit_arrow),
                onClick = { editMode = ImageEditMode.ARROW },
                tint = if (editMode == ImageEditMode.ARROW) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            AppIconButton(
                imageVector = Icons.AutoMirrored.Filled.Undo,
                contentDescription = stringResource(R.string.image_edit_undo_arrow),
                enabled = arrows.isNotEmpty(),
                onClick = { arrows = arrows.dropLast(1) },
                tint = if (arrows.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
            )
            AppIconButton(
                imageVector = Icons.Default.LayersClear,
                contentDescription = stringResource(R.string.image_edit_clear_crop),
                enabled = cropRect != null,
                onClick = { cropRect = null },
                tint = if (cropRect != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
            )
        }
        ImageEditCanvas(
            bitmap = bitmap,
            description = description,
            arrows = arrows,
            cropRect = cropRect,
            dragStart = dragStart,
            dragCurrent = dragCurrent,
            editMode = editMode,
            canvasSize = canvasSize,
            onCanvasSizeChange = { canvasSize = it },
            onDragStart = { offset ->
                val source = bitmap ?: return@ImageEditCanvas
                val mapped = offset.toBitmapOffset(canvasSize, source) ?: return@ImageEditCanvas
                dragStart = mapped
                dragCurrent = mapped
            },
            onDrag = { offset ->
                val source = bitmap ?: return@ImageEditCanvas
                dragCurrent = offset.toBitmapOffset(canvasSize, source)
            },
            onDragEnd = {
                val start = dragStart
                val end = dragCurrent
                if ((start != null && end != null)) {
                    when (editMode) {
                        ImageEditMode.ARROW -> {
                            if ((start - end).getDistance() >= MIN_DRAG_DISTANCE) {
                                arrows += ImageEditArrow(start, end)
                            }
                        }

                        ImageEditMode.CROP -> {
                            normalizeBitmapRect(start, end, bitmap)?.let { cropRect = it }
                        }
                    }
                }
                dragStart = null
                dragCurrent = null
            },
            modifier = modifier,
        )
    }
}

@Composable
private fun ImageEditCanvas(
    bitmap: Bitmap?,
    description: String,
    arrows: List<ImageEditArrow>,
    cropRect: Rect?,
    dragStart: Offset?,
    dragCurrent: Offset?,
    editMode: ImageEditMode,
    canvasSize: IntSize,
    onCanvasSizeChange: (IntSize) -> Unit,
    onDragStart: (Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.clipToBounds(), contentAlignment = Alignment.Center) {
        if (bitmap == null) {
            Text(
                stringResource(R.string.image_preview_unavailable),
                style = MaterialTheme.typography.bodySmall,
            )
        } else {
            val imageBitmap = remember(bitmap) { bitmap.asImageBitmap() }
            Canvas(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(420.dp)
                        .onSizeChanged(onCanvasSizeChange)
                        .pointerInput(editMode, bitmap, canvasSize) {
                            detectDragGestures(
                                onDragStart = onDragStart,
                                onDrag = { change, _ -> onDrag(change.position) },
                                onDragEnd = onDragEnd,
                                onDragCancel = onDragEnd,
                            )
                        },
            ) {
                val placement =
                    imagePlacement(Size(bitmap.width.toFloat(), bitmap.height.toFloat()), size)
                drawImage(
                    image = imageBitmap,
                    dstOffset = IntOffset(placement.left.roundToInt(), placement.top.roundToInt()),
                    dstSize = IntSize(placement.width.roundToInt(), placement.height.roundToInt()),
                )
                arrows.forEach { arrow ->
                    drawBitmapArrow(
                        arrow.start.toCanvasOffset(placement, bitmap),
                        arrow.end.toCanvasOffset(placement, bitmap),
                    )
                }
                cropRect?.let { rect ->
                    drawRect(
                        color = Color.White,
                        topLeft = rect.topLeft.toCanvasOffset(placement, bitmap),
                        size = rect.size.toCanvasSize(placement, bitmap),
                        style = Stroke(width = 3.dp.toPx()),
                    )
                }
                if (dragStart != null && dragCurrent != null) {
                    when (editMode) {
                        ImageEditMode.ARROW -> {
                            drawBitmapArrow(
                                dragStart.toCanvasOffset(placement, bitmap),
                                dragCurrent.toCanvasOffset(placement, bitmap),
                                color = Color.Yellow,
                            )
                        }

                        ImageEditMode.CROP -> {
                            normalizeBitmapRect(dragStart, dragCurrent, bitmap)?.let { rect ->
                                drawRect(
                                    color = Color.Yellow,
                                    topLeft = rect.topLeft.toCanvasOffset(placement, bitmap),
                                    size = rect.size.toCanvasSize(placement, bitmap),
                                    style = Stroke(width = 3.dp.toPx()),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ZoomableLocalImage(
    path: String,
    description: String,
    modifier: Modifier = Modifier,
) {
    val bitmap by rememberDecodedLocalBitmap(path, VIEWER_MAX_DIMENSION)
    var scale by remember(path) { mutableFloatStateOf(1f) }
    var offset by remember(path) { mutableStateOf(Offset.Zero) }
    val transformState =
        rememberTransformableState { _, zoomChange, panChange, _ ->
            val nextScale = (scale * zoomChange).coerceIn(1f, 5f)
            scale = nextScale
            offset = if (nextScale == 1f) Offset.Zero else offset + panChange
        }

    Box(
        modifier =
            modifier
                .clipToBounds(),
        contentAlignment = Alignment.Center,
    ) {
        val viewerBitmap = bitmap
        if (viewerBitmap == null) {
            Text(
                stringResource(R.string.image_preview_unavailable),
                style = MaterialTheme.typography.bodySmall,
            )
        } else {
            Image(
                bitmap = viewerBitmap.asImageBitmap(),
                contentDescription = description,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(
                            viewerBitmap.width.toFloat() /
                                viewerBitmap.height
                                    .coerceAtLeast(
                                        1,
                                    ).toFloat(),
                        ).graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = offset.x
                            translationY = offset.y
                        }.transformable(transformState),
                contentScale = ContentScale.Fit,
            )
        }
    }
}

@Composable
private fun rememberDecodedLocalBitmap(
    path: String,
    maxDimension: Int,
) = produceState<Bitmap?>(initialValue = null, path, maxDimension, fileVersion(path)) {
    value =
        withContext(Dispatchers.IO) {
            decodeOrientedBitmap(path, maxDimension)
        }
}

private fun fileVersion(path: String): Pair<Long, Long> = File(path).let { file -> file.lastModified() to file.length() }

private fun DrawScope.drawBitmapArrow(
    start: Offset,
    end: Offset,
    color: Color = Color.Red,
) {
    val strokeWidth = 5.dp.toPx()
    drawLine(color = color, start = start, end = end, strokeWidth = strokeWidth)
    val angle = atan2(end.y - start.y, end.x - start.x)
    val headLength = 22.dp.toPx()
    val left =
        Offset(
            x = end.x - headLength * cos(angle - ARROW_HEAD_ANGLE),
            y = end.y - headLength * sin(angle - ARROW_HEAD_ANGLE),
        )
    val right =
        Offset(
            x = end.x - headLength * cos(angle + ARROW_HEAD_ANGLE),
            y = end.y - headLength * sin(angle + ARROW_HEAD_ANGLE),
        )
    drawLine(color = color, start = end, end = left, strokeWidth = strokeWidth)
    drawLine(color = color, start = end, end = right, strokeWidth = strokeWidth)
}

private const val PREVIEW_MAX_DIMENSION = 640
private const val VIEWER_MAX_DIMENSION = 1920
private const val EDIT_MAX_DIMENSION = 1920

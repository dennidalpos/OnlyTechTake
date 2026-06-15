package com.onlytechtake.repairapp.features.dashboard.neworder

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.core.design.RepairAppTestTags
import com.onlytechtake.repairapp.core.design.components.AppButtonSize
import com.onlytechtake.repairapp.data.repositories.StagedOrderMedia
import com.onlytechtake.repairapp.features.dashboard.common.CameraCapturePreview
import com.onlytechtake.repairapp.features.dashboard.common.LocalImagePreview
import com.onlytechtake.repairapp.features.dashboard.common.LocalImageViewer
import com.onlytechtake.repairapp.features.dashboard.common.ActionRow
import com.onlytechtake.repairapp.features.dashboard.common.DashboardSpacing
import com.onlytechtake.repairapp.features.dashboard.common.StandardCard
import com.onlytechtake.repairapp.features.dashboard.common.StepSectionTitle
import com.onlytechtake.repairapp.features.dashboard.common.TouchButton
import com.onlytechtake.repairapp.features.workspace.ButtonTone
import com.onlytechtake.repairapp.features.workspace.isImageAttachment
import java.io.File

@Composable
fun QuickIntakeMediaSection(
    stepNumber: Int,
    intakeSessionId: String,
    stagedMedia: List<StagedOrderMedia>,
    showCamera: Boolean,
    onShowCameraChange: (Boolean) -> Unit,
    onRemoveMedia: (String) -> Unit,
    onReplaceMedia: (StagedOrderMedia, Bitmap) -> Unit,
    onLaunchAttachmentPicker: () -> Unit,
    createPhotoFile: (String, String) -> Result<File>,
    onPhotoCaptured: (File) -> Unit,
    onCameraError: (String) -> Unit,
) {
    var selectedImagePath by rememberSaveable { mutableStateOf<String?>(null) }

    StandardCard {
        Column(
            modifier = Modifier.padding(DashboardSpacing.cardPadding),
            verticalArrangement = Arrangement.spacedBy(DashboardSpacing.formGap),
        ) {
            StepSectionTitle(stepNumber, stringResource(R.string.intake_step_media))
            if (stagedMedia.isEmpty()) {
                Text(stringResource(R.string.intake_media_empty), style = MaterialTheme.typography.bodyMedium)
            } else {
                stagedMedia.forEach { media ->
                    if (media.isImageAttachment()) {
                        StandardCard(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                        ) {
                            Column(
                                modifier = Modifier.padding(DashboardSpacing.compactGap),
                                verticalArrangement = Arrangement.spacedBy(DashboardSpacing.compactGap),
                            ) {
                                LocalImagePreview(
                                    path = media.localPath,
                                    description = media.fileName,
                                    onClick = { selectedImagePath = media.localPath },
                                )
                                StagedMediaSummary(media = media, onRemoveMedia = onRemoveMedia)
                            }
                        }
                    } else {
                        StandardCard(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                        ) {
                            Column(
                                modifier = Modifier.padding(DashboardSpacing.compactGap),
                                verticalArrangement = Arrangement.spacedBy(DashboardSpacing.compactGap),
                            ) {
                                StagedMediaSummary(media = media, onRemoveMedia = onRemoveMedia)
                            }
                        }
                    }
                }
            }
            ActionRow {
                TouchButton(
                    text = stringResource(R.string.intake_media_take_photo),
                    size = AppButtonSize.Compact,
                    testTag = RepairAppTestTags.IntakeMediaTakePhotoButton,
                    onClick = { onShowCameraChange(true) },
                )
                TouchButton(
                    text = stringResource(R.string.intake_media_attach_file),
                    size = AppButtonSize.Compact,
                    testTag = RepairAppTestTags.IntakeMediaAttachFileButton,
                    onClick = onLaunchAttachmentPicker,
                )
                if (showCamera) {
                    TouchButton(
                        text = stringResource(R.string.intake_media_close_camera),
                        tone = ButtonTone.NEUTRAL,
                        size = AppButtonSize.Compact,
                        testTag = RepairAppTestTags.IntakeMediaCloseCameraButton,
                        onClick = { onShowCameraChange(false) },
                    )
                }
            }
            if (showCamera) {
                androidx.compose.material3.AlertDialog(
                    onDismissRequest = { onShowCameraChange(false) },
                    title = { Text(stringResource(R.string.intake_media_take_photo)) },
                    text = {
                        CameraCapturePreview(
                            orderId = intakeSessionId,
                            orderNumber = "presa_in_carico",
                            createPhotoFile = createPhotoFile,
                            onPhotoCaptured = {
                                onPhotoCaptured(it)
                                onShowCameraChange(false)
                            },
                            onError = onCameraError,
                        )
                    },
                    confirmButton = {},
                    dismissButton = {
                        androidx.compose.material3.TextButton(onClick = { onShowCameraChange(false) }) {
                            Text(stringResource(R.string.customer_detail_close))
                        }
                    },
                )
            }
        }
    }

    selectedImagePath?.let { path ->
        val media = stagedMedia.firstOrNull { it.localPath == path }
        if (media == null) {
            selectedImagePath = null
        } else {
            LocalImageViewer(
                path = media.localPath,
                title = media.fileName,
                onDismiss = { selectedImagePath = null },
                onSaveEdited = { bitmap -> onReplaceMedia(media, bitmap) },
            )
        }
    }
}

@Composable
private fun StagedMediaSummary(
    media: StagedOrderMedia,
    onRemoveMedia: (String) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(DashboardSpacing.compactGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(DashboardSpacing.compactGap),
        ) {
            Text(
                text = media.fileName,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(R.string.intake_media_summary, media.mediaType, media.sizeBytes / 1024),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        TouchButton(
            text = stringResource(R.string.intake_media_remove),
            tone = ButtonTone.DANGER,
            size = AppButtonSize.Compact,
            onClick = { onRemoveMedia(media.localPath) },
        )
    }
}

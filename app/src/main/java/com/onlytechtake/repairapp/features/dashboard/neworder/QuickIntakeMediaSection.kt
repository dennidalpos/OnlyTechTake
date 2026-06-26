package com.onlytechtake.repairapp.features.dashboard.neworder

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.core.design.RepairAppSpacing
import com.onlytechtake.repairapp.core.design.RepairAppTestTags
import com.onlytechtake.repairapp.core.design.components.AppButton
import com.onlytechtake.repairapp.core.design.components.AppButtonSize
import com.onlytechtake.repairapp.core.design.components.AppButtonVariant
import com.onlytechtake.repairapp.core.design.components.AppDialog
import com.onlytechtake.repairapp.core.design.components.AppIconButton
import com.onlytechtake.repairapp.core.design.components.StandardCard
import com.onlytechtake.repairapp.data.repositories.StagedOrderMedia
import com.onlytechtake.repairapp.features.dashboard.common.CameraCapturePreview
import com.onlytechtake.repairapp.features.dashboard.common.LocalImagePreview
import com.onlytechtake.repairapp.features.dashboard.common.LocalImageViewer
import com.onlytechtake.repairapp.features.workspace.isImageAttachment
import java.io.File

@Composable
fun QuickIntakeMediaSection(
    @Suppress("UNUSED_PARAMETER") stepNumber: Int,
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
    var selectedImagePath by remember { mutableStateOf<String?>(null) }

    StandardCard {
        Column(
            modifier = Modifier.padding(RepairAppSpacing.cardPadding),
            verticalArrangement = Arrangement.spacedBy(RepairAppSpacing.fieldGap),
        ) {
            Text(
                text = stringResource(R.string.intake_step_media),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            if (stagedMedia.isEmpty()) {
                Text(
                    stringResource(R.string.intake_media_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline,
                )
            } else {
                stagedMedia.forEach { media ->
                    StandardCard(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f),
                    ) {
                        Column(
                            modifier = Modifier.padding(RepairAppSpacing.compactGap),
                            verticalArrangement = Arrangement.spacedBy(RepairAppSpacing.compactGap),
                        ) {
                            if (media.isImageAttachment()) {
                                LocalImagePreview(
                                    path = media.localPath,
                                    description = media.fileName,
                                ) { selectedImagePath = media.localPath }
                            }
                            StagedMediaSummary(media = media, onRemoveMedia = onRemoveMedia)
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(RepairAppSpacing.actionGap),
            ) {
                AppButton(
                    modifier = Modifier.weight(1f),
                    text = stringResource(R.string.intake_media_take_photo),
                    size = AppButtonSize.Compact,
                    testTag = RepairAppTestTags.IntakeMediaTakePhotoButton,
                    onClick = { onShowCameraChange(true) },
                )
                AppButton(
                    modifier = Modifier.weight(1f),
                    text = stringResource(R.string.intake_media_attach_file),
                    size = AppButtonSize.Compact,
                    testTag = RepairAppTestTags.IntakeMediaAttachFileButton,
                    onClick = onLaunchAttachmentPicker,
                )
            }

            if (showCamera) {
                AppDialog(
                    onDismissRequest = { onShowCameraChange(false) },
                    title = stringResource(R.string.intake_media_take_photo),
                    content = {
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
                        AppButton(
                            text = stringResource(R.string.action_close),
                            onClick = { onShowCameraChange(false) },
                            variant = AppButtonVariant.Secondary,
                            size = AppButtonSize.Compact,
                        )
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
        horizontalArrangement = Arrangement.spacedBy(RepairAppSpacing.compactGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = media.fileName,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text =
                    stringResource(
                        R.string.intake_media_summary,
                        media.mediaType,
                        media.sizeBytes / 1024,
                    ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        AppIconButton(
            imageVector = Icons.Default.Delete,
            contentDescription = stringResource(R.string.intake_media_remove),
            onClick = { onRemoveMedia(media.localPath) },
            tint = MaterialTheme.colorScheme.error,
        )
    }
}

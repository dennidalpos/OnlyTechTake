package com.onlytechtake.repairapp.features.dashboard.detail

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.core.design.RepairAppSpacing
import com.onlytechtake.repairapp.core.design.RepairAppTestTags
import com.onlytechtake.repairapp.core.design.components.AppButton
import com.onlytechtake.repairapp.core.design.components.AppButtonSize
import com.onlytechtake.repairapp.core.design.components.AppButtonVariant
import com.onlytechtake.repairapp.core.design.components.AppDialog
import com.onlytechtake.repairapp.core.design.components.AppIconButton
import com.onlytechtake.repairapp.core.design.components.StandardCard
import com.onlytechtake.repairapp.data.local.entities.OrderMediaEntity
import com.onlytechtake.repairapp.domain.workspace.OrderDetail
import com.onlytechtake.repairapp.features.dashboard.common.CameraCaptureMode
import com.onlytechtake.repairapp.features.dashboard.common.CameraCapturePreview
import com.onlytechtake.repairapp.features.dashboard.common.LocalImagePreview
import com.onlytechtake.repairapp.features.dashboard.common.LocalImageViewer
import com.onlytechtake.repairapp.features.dashboard.common.SectionTitle
import com.onlytechtake.repairapp.features.workspace.isFinalPhoto
import com.onlytechtake.repairapp.features.workspace.isImageAttachment
import com.onlytechtake.repairapp.features.workspace.isVideoAttachment

@Composable
fun OrderPhotoSection(
    detail: OrderDetail,
    viewModel: OrderDetailViewModel,
) {
    var showCameraMode by rememberSaveable { mutableStateOf<CameraCaptureMode?>(null) }
    var selectedImageId by rememberSaveable { mutableStateOf<String?>(null) }
    var deleteMedia by remember { mutableStateOf<OrderMediaEntity?>(null) }
    val attachmentLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let { viewModel.importAttachment(it) }
        }
    val images = detail.media.filter { it.isImageAttachment() && !it.isFinalPhoto() }
    val videos = detail.media.filter { it.isVideoAttachment() }
    val documents = detail.media.filter { !it.isImageAttachment() && !it.isVideoAttachment() }

    Column(verticalArrangement = Arrangement.spacedBy(RepairAppSpacing.compactGap)) {
        SectionTitle(stringResource(R.string.intake_step_media))

        if (images.isEmpty() && videos.isEmpty() && documents.isEmpty()) {
            Text(
                stringResource(R.string.intake_media_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline,
            )
        } else {
            if (images.isNotEmpty()) {
                Text(
                    stringResource(R.string.order_images_title),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                )
                images.forEach { media ->
                    StandardCard {
                        Column(
                            modifier = Modifier.padding(RepairAppSpacing.compactGap),
                            verticalArrangement = Arrangement.spacedBy(RepairAppSpacing.compactGap),
                        ) {
                            LocalImagePreview(
                                path = media.localPath,
                                description = media.fileName,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                            ) { selectedImageId = media.id }
                            MediaMetadataRow(media) { deleteMedia = media }
                        }
                    }
                }
            }

            if (videos.isNotEmpty()) {
                Text(
                    stringResource(R.string.label_media_video),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = RepairAppSpacing.compactGap),
                )
                videos.forEach { media ->
                    StandardCard {
                        Column(
                            modifier = Modifier.padding(RepairAppSpacing.compactGap),
                            verticalArrangement = Arrangement.spacedBy(RepairAppSpacing.compactGap),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(RepairAppSpacing.compactGap),
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                            ) {
                                androidx.compose.material3.Icon(
                                    imageVector = Icons.Default.Videocam,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                                Text(
                                    text = media.fileName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            MediaMetadataRow(media) { deleteMedia = media }
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(RepairAppSpacing.actionGap),
        ) {
            AppButton(
                modifier =
                    Modifier
                        .weight(1f)
                        .testTag(RepairAppTestTags.IntakeMediaTakePhotoButton),
                text = null,
                variant = AppButtonVariant.Secondary,
                size = AppButtonSize.Compact,
                leadingIcon = Icons.Default.PhotoCamera,
                onClick = { showCameraMode = CameraCaptureMode.PHOTO },
            )

            AppButton(
                modifier =
                    Modifier
                        .weight(1f)
                        .testTag(RepairAppTestTags.IntakeMediaTakeVideoButton),
                text = null,
                variant = AppButtonVariant.Secondary,
                size = AppButtonSize.Compact,
                leadingIcon = Icons.Default.Videocam,
                enabled =
                    detail.assignedUser?.permissions?.contains(
                        com.onlytechtake.repairapp.data.local.entities.UserPermission.RECORD_VIDEO,
                    ) == true,
                onClick = { showCameraMode = CameraCaptureMode.VIDEO },
            )

            AppButton(
                modifier = Modifier.weight(1f),
                text = null,
                variant = AppButtonVariant.Secondary,
                size = AppButtonSize.Compact,
                leadingIcon = Icons.Default.AttachFile,
                onClick = { attachmentLauncher.launch("*/*") },
            )
        }
    }

    showCameraMode?.let { mode ->
        AppDialog(
            onDismissRequest = { showCameraMode = null },
            title =
                stringResource(
                    if (mode == CameraCaptureMode.PHOTO) R.string.intake_media_take_photo else R.string.intake_media_take_video,
                ),
            content = {
                CameraCapturePreview(
                    orderId = detail.order.id,
                    orderNumber = detail.order.orderNumber,
                    initialMode = mode,
                    canRecordVideo =
                        detail.assignedUser?.permissions?.contains(
                            com.onlytechtake.repairapp.data.local.entities.UserPermission.RECORD_VIDEO,
                        ) == true,
                    createPhotoFile = { _, number -> viewModel.createPhotoFile(number) },
                    createVideoFile = { number -> viewModel.createVideoFile(number) },
                    onMediaCaptured = { file, capturedMode ->
                        if (capturedMode == CameraCaptureMode.PHOTO) {
                            viewModel.registerCapturedPhoto(file)
                        } else {
                            viewModel.registerCapturedVideo(file)
                        }
                        showCameraMode = null
                    },
                    onError = viewModel::reportCameraError,
                )
            },
            confirmButton = {},
            dismissButton = {
                AppButton(
                    text = stringResource(R.string.action_close),
                    onClick = { showCameraMode = null },
                    variant = AppButtonVariant.Secondary,
                    size = AppButtonSize.Compact,
                )
            },
        )
    }

    selectedImageId?.let { id ->
        val media = detail.media.firstOrNull { it.id == id }
        if (media == null) {
            selectedImageId = null
        } else {
            LocalImageViewer(
                path = media.localPath,
                title = media.fileName,
                onDismiss = { selectedImageId = null },
                onSaveEdited = { bitmap -> viewModel.replaceImage(media.id, bitmap) },
            )
        }
    }

    deleteMedia?.let { media ->
        AppDialog(
            onDismissRequest = { deleteMedia = null },
            title =
                stringResource(
                    R.string.action_delete_confirm_title,
                    stringResource(R.string.label_media),
                ),
            text = stringResource(R.string.action_delete_confirm_text, media.fileName),
            confirmButton = {
                AppButton(
                    text = stringResource(R.string.action_confirm),
                    onClick = {
                        viewModel.deleteMedia(media.id)
                        deleteMedia = null
                    },
                    variant = AppButtonVariant.Danger,
                    size = AppButtonSize.Compact,
                )
            },
            dismissButton = {
                AppButton(
                    text = stringResource(R.string.customer_cancel),
                    onClick = { deleteMedia = null },
                    variant = AppButtonVariant.Secondary,
                    size = AppButtonSize.Compact,
                )
            },
        )
    }
}

@Composable
private fun MediaMetadataRow(
    media: OrderMediaEntity,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        Text(
            com.onlytechtake.repairapp.core.utils
                .formatUiDateTime(media.capturedAt),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.weight(1f),
        )
        AppIconButton(
            imageVector = Icons.Default.Delete,
            contentDescription = stringResource(R.string.content_description_delete),
            onClick = onDelete,
            tint = MaterialTheme.colorScheme.error,
        )
    }
}

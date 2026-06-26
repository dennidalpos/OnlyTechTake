package com.onlytechtake.repairapp.features.dashboard.detail

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.core.design.RepairAppSpacing
import com.onlytechtake.repairapp.core.design.components.AppButton
import com.onlytechtake.repairapp.core.design.components.AppButtonSize
import com.onlytechtake.repairapp.core.design.components.AppButtonVariant
import com.onlytechtake.repairapp.core.design.components.AppDialog
import com.onlytechtake.repairapp.core.design.components.AppIconButton
import com.onlytechtake.repairapp.core.design.components.StandardCard
import com.onlytechtake.repairapp.data.local.entities.OrderMediaEntity
import com.onlytechtake.repairapp.domain.workspace.OrderDetail
import com.onlytechtake.repairapp.features.dashboard.common.CameraCapturePreview
import com.onlytechtake.repairapp.features.dashboard.common.LocalImagePreview
import com.onlytechtake.repairapp.features.dashboard.common.LocalImageViewer
import com.onlytechtake.repairapp.features.dashboard.common.SectionTitle
import com.onlytechtake.repairapp.features.workspace.isFinalPhoto
import com.onlytechtake.repairapp.features.workspace.isImageAttachment

@Composable
fun OrderPhotoSection(
    detail: OrderDetail,
    viewModel: OrderDetailViewModel,
) {
    var showCamera by rememberSaveable { mutableStateOf(false) }
    var selectedImageId by rememberSaveable { mutableStateOf<String?>(null) }
    var deleteMedia by remember { mutableStateOf<OrderMediaEntity?>(null) }
    val attachmentLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let { viewModel.importAttachment(detail.order.id, it) }
        }
    val images = detail.media.filter { it.isImageAttachment() && !it.isFinalPhoto() }
    val documents = detail.media.filterNot { it.isImageAttachment() }

    Column(verticalArrangement = Arrangement.spacedBy(RepairAppSpacing.compactGap)) {
        SectionTitle(stringResource(R.string.intake_step_media))

        if (images.isEmpty() && documents.isEmpty()) {
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
                                onClick = { selectedImageId = media.id },
                            )
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
                                    onClick = { deleteMedia = media },
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            }
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
                modifier = Modifier.weight(1f),
                text = stringResource(R.string.intake_media_take_photo),
                variant = AppButtonVariant.Secondary,
                size = AppButtonSize.Compact,
                leadingIcon = Icons.Default.PhotoCamera,
                onClick = { showCamera = true },
            )
            AppButton(
                modifier = Modifier.weight(1f),
                text = stringResource(R.string.intake_media_attach_file),
                variant = AppButtonVariant.Ghost,
                size = AppButtonSize.Compact,
                leadingIcon = Icons.Default.AttachFile,
                onClick = { attachmentLauncher.launch("*/*") },
            )
        }
    }

    if (showCamera) {
        AppDialog(
            onDismissRequest = { showCamera = false },
            title = stringResource(R.string.intake_media_take_photo),
            content = {
                CameraCapturePreview(
                    orderId = detail.order.id,
                    orderNumber = detail.order.orderNumber,
                    createPhotoFile = viewModel::createPhotoFile,
                    onPhotoCaptured = {
                        viewModel.registerCapturedPhoto(detail.order.id, it)
                        showCamera = false
                    },
                    onError = viewModel::reportCameraError,
                )
            },
            confirmButton = {},
            dismissButton = {
                AppButton(
                    text = stringResource(R.string.action_close),
                    onClick = { showCamera = false },
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
            title = stringResource(R.string.action_delete_confirm_title, stringResource(R.string.label_media)),
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

package com.onlytechtake.repairapp.features.dashboard.detail

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.onlytechtake.repairapp.data.local.entities.OrderMediaEntity
import com.onlytechtake.repairapp.data.local.entities.OrderStatus
import com.onlytechtake.repairapp.domain.workspace.OrderDetail
import com.onlytechtake.repairapp.features.dashboard.common.LocalImagePreview
import com.onlytechtake.repairapp.features.dashboard.common.LocalImageViewer
import com.onlytechtake.repairapp.features.dashboard.common.ActionRow
import com.onlytechtake.repairapp.features.dashboard.common.CameraCapturePreview
import com.onlytechtake.repairapp.features.dashboard.common.CompactExpandableRow
import com.onlytechtake.repairapp.features.dashboard.common.DashboardSpacing
import com.onlytechtake.repairapp.features.dashboard.common.SectionTitle
import com.onlytechtake.repairapp.features.dashboard.common.StandardCard
import com.onlytechtake.repairapp.features.dashboard.common.TouchButton
import com.onlytechtake.repairapp.features.workspace.ButtonTone
import com.onlytechtake.repairapp.features.workspace.WorkspaceViewModel
import com.onlytechtake.repairapp.features.workspace.isFinalPhoto
import com.onlytechtake.repairapp.features.workspace.isImageAttachment

@Composable
fun OrderPhotoSection(
    detail: OrderDetail,
    viewModel: WorkspaceViewModel,
) {
    var showCamera by rememberSaveable { mutableStateOf(false) }
    var showFinalCamera by rememberSaveable { mutableStateOf(false) }
    var selectedImageId by rememberSaveable { mutableStateOf<String?>(null) }
    var deleteMedia by remember { mutableStateOf<OrderMediaEntity?>(null) }
    val attachmentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.importAttachment(detail.order.id, it) }
    }
    val finalPhotos = detail.media.filter { it.isFinalPhoto() }
    val images = detail.media.filter { it.isImageAttachment() && !it.isFinalPhoto() }
    val documents = detail.media.filterNot { it.isImageAttachment() }

    Column(verticalArrangement = Arrangement.spacedBy(DashboardSpacing.compactGap)) {
        SectionTitle("Foto e allegati")
        if (images.isEmpty() && documents.isEmpty()) {
            Text("Nessuna foto o allegato.")
        } else {
            if (images.isNotEmpty()) {
                Text("Immagini", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.secondary)
                images.forEach { media ->
                    StandardCard {
                        Column(
                            modifier = Modifier.padding(DashboardSpacing.compactGap),
                            verticalArrangement = Arrangement.spacedBy(DashboardSpacing.compactGap),
                        ) {
                            LocalImagePreview(
                                path = media.localPath,
                                description = "Foto commessa",
                                onClick = { selectedImageId = media.id },
                            )
                        }
                    }
                }
            }
            if (documents.isNotEmpty()) {
                Text("Documenti", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.secondary)
                documents.forEach { media ->
                    CompactExpandableRow(
                        title = "Allegato",
                        subtitle = "Documento commessa",
                        detail = media.fileName,
                    )
                }
            }
            if (finalPhotos.isNotEmpty()) {
                Text("Lavoro finale", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.tertiary)
                finalPhotos.forEach { media ->
                    StandardCard {
                        Column(
                            modifier = Modifier.padding(DashboardSpacing.compactGap),
                            verticalArrangement = Arrangement.spacedBy(DashboardSpacing.compactGap),
                        ) {
                            LocalImagePreview(
                                path = media.localPath,
                                description = "Foto finale",
                                onClick = { selectedImageId = media.id },
                            )
                        }
                    }
                }
            }
        }

        val canCaptureFinal = detail.order.status == OrderStatus.READY_FOR_PICKUP || 
                           detail.order.status == OrderStatus.CLOSED

        ActionRow {
            TouchButton(text = "Scatta foto", onClick = {
                showFinalCamera = false
                showCamera = true
            })
            if (canCaptureFinal) {
                TouchButton(text = "Foto finale", onClick = {
                    showCamera = false
                    showFinalCamera = true
                })
            }
            TouchButton(text = "Allega file", onClick = { attachmentLauncher.launch("*/*") })
            if (showCamera) {
                TouchButton(text = "Chiudi camera", tone = ButtonTone.NEUTRAL, onClick = { showCamera = false })
            }
            if (showFinalCamera) {
                TouchButton(text = "Chiudi foto finale", tone = ButtonTone.NEUTRAL, onClick = { showFinalCamera = false })
            }
        }

        if (showCamera) {
            AlertDialog(
                onDismissRequest = { showCamera = false },
                title = { Text("Acquisisci foto") },
                text = {
                    CameraCapturePreview(
                        orderId = detail.order.id,
                        orderNumber = detail.order.orderNumber,
                        createPhotoFile = viewModel::createPhotoFile,
                        onPhotoCaptured = { file ->
                            viewModel.registerCapturedPhoto(detail.order.id, file)
                            showCamera = false
                        },
                        onError = viewModel::reportCameraError,
                    )
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showCamera = false }) {
                        Text("Annulla")
                    }
                },
            )
        }
        if (showFinalCamera) {
            AlertDialog(
                onDismissRequest = { showFinalCamera = false },
                title = { Text("Acquisisci foto finale") },
                text = {
                    CameraCapturePreview(
                        orderId = detail.order.id,
                        orderNumber = detail.order.orderNumber,
                        createPhotoFile = viewModel::createPhotoFile,
                        onPhotoCaptured = { file ->
                            viewModel.registerCapturedFinalPhoto(detail.order.id, file)
                            showFinalCamera = false
                        },
                        onError = viewModel::reportCameraError,
                    )
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showFinalCamera = false }) {
                        Text("Annulla")
                    }
                },
            )
        }
    }

    selectedImageId?.let { mediaId ->
        val media = detail.media.firstOrNull { it.id == mediaId }
        if (media == null) {
            selectedImageId = null
        } else {
            LocalImageViewer(
                path = media.localPath,
                title = media.fileName,
                onDismiss = { selectedImageId = null },
                onSaveEdited = { bitmap: Bitmap ->
                    viewModel.replaceImage(media.id, bitmap)
                },
                onDelete = {
                    deleteMedia = media
                },
            )
        }
    }

    deleteMedia?.let { media ->
        AlertDialog(
            onDismissRequest = { deleteMedia = null },
            title = { Text("Eliminare foto?") },
            text = { Text("La foto ${media.fileName} verra' rimossa dalla commessa.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteMedia(media.id)
                        deleteMedia = null
                    },
                ) {
                    Text("Elimina")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteMedia = null }) {
                    Text("Annulla")
                }
            },
        )
    }
}

package com.onlytechtake.repairapp.features.dashboard

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.onlytechtake.repairapp.data.repositories.StagedOrderMedia
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

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(DashboardSpacing.cardPadding),
            verticalArrangement = Arrangement.spacedBy(DashboardSpacing.formGap),
        ) {
            StepSectionTitle(stepNumber, "Foto e allegati")
            if (stagedMedia.isEmpty()) {
                Text("Aggiungi almeno una foto o un allegato.")
            } else {
                stagedMedia.forEach { media ->
                    if (media.isImageAttachment()) {
                        Card(modifier = Modifier.fillMaxWidth()) {
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
                        Card(modifier = Modifier.fillMaxWidth()) {
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
                TouchButton(text = "Scatta foto", onClick = { onShowCameraChange(true) })
                TouchButton(text = "Allega file", onClick = onLaunchAttachmentPicker)
                if (showCamera) {
                    TouchButton(text = "Chiudi camera", tone = ButtonTone.NEUTRAL, onClick = { onShowCameraChange(false) })
                }
            }
            if (showCamera) {
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
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(DashboardSpacing.compactGap),
    ) {
        Text(
            text = media.fileName,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = "${media.mediaType} - ${media.sizeBytes / 1024} KB",
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        TouchButton(
            modifier = Modifier.fillMaxWidth(),
            text = "Rimuovi",
            tone = ButtonTone.DANGER,
            onClick = { onRemoveMedia(media.localPath) },
        )
    }
}

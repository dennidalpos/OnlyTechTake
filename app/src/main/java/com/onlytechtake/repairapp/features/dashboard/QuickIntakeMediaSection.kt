package com.onlytechtake.repairapp.features.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.onlytechtake.repairapp.data.repositories.StagedOrderMedia
import java.io.File

@Composable
fun QuickIntakeMediaSection(
    stepNumber: Int,
    intakeSessionId: String,
    stagedMedia: List<StagedOrderMedia>,
    showCamera: Boolean,
    onShowCameraChange: (Boolean) -> Unit,
    onRemoveMedia: (String) -> Unit,
    onLaunchAttachmentPicker: () -> Unit,
    createPhotoFile: (String, String) -> Result<File>,
    onPhotoCaptured: (File) -> Unit,
    onCameraError: (String) -> Unit,
) {
    var selectedImage by rememberSaveable { mutableStateOf<Pair<String, String>?>(null) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            StepSectionTitle(stepNumber, "Foto e allegati")
            if (stagedMedia.isEmpty()) {
                Text("Aggiungi almeno una foto o un allegato.")
            } else {
                stagedMedia.forEach { media ->
                    if (media.isImage()) {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                LocalImagePreview(
                                    path = media.localPath,
                                    description = media.fileName,
                                    onClick = { selectedImage = media.localPath to media.fileName },
                                )
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        modifier = Modifier.weight(1f),
                                        text = "${media.mediaType}: ${media.fileName} - ${media.sizeBytes / 1024} KB",
                                    )
                                    OutlinedButton(onClick = { onRemoveMedia(media.localPath) }) {
                                        Text("Rimuovi")
                                    }
                                }
                            }
                        }
                    } else {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                modifier = Modifier.weight(1f),
                                text = "${media.mediaType}: ${media.fileName} - ${media.sizeBytes / 1024} KB",
                            )
                            OutlinedButton(onClick = { onRemoveMedia(media.localPath) }) {
                                Text("Rimuovi")
                            }
                        }
                    }
                }
            }
            ActionRow {
                TouchButton(text = "Scatta foto", onClick = { onShowCameraChange(true) })
                TouchButton(text = "Allega file", onClick = onLaunchAttachmentPicker)
                if (showCamera) {
                    OutlinedButton(onClick = { onShowCameraChange(false) }) {
                        Text("Chiudi camera")
                    }
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

    selectedImage?.let { (path, title) ->
        LocalImageViewer(path = path, title = title, onDismiss = { selectedImage = null })
    }
}

private fun StagedOrderMedia.isImage(): Boolean =
    mediaType.equals("PHOTO", ignoreCase = true) ||
        mediaType.equals("IMAGE", ignoreCase = true) ||
        mimeType.startsWith("image/", ignoreCase = true)

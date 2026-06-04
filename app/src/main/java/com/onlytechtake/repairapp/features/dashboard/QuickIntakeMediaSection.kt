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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.onlytechtake.repairapp.data.repositories.StagedOrderMedia
import java.io.File

@Composable
fun QuickIntakeMediaSection(
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
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Foto e allegati", style = MaterialTheme.typography.titleMedium)
            if (stagedMedia.isEmpty()) {
                Text("Aggiungi almeno una foto o un allegato.")
            } else {
                stagedMedia.forEach { media ->
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
}

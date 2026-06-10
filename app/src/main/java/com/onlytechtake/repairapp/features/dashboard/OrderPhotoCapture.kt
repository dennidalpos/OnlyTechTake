package com.onlytechtake.repairapp.features.dashboard

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.onlytechtake.repairapp.data.local.entities.OrderMediaEntity
import com.onlytechtake.repairapp.core.media.normalizeCameraTargetRotation
import com.onlytechtake.repairapp.domain.workspace.OrderDetail
import com.onlytechtake.repairapp.features.workspace.isFinalPhoto
import com.onlytechtake.repairapp.features.workspace.isImageAttachment
import com.onlytechtake.repairapp.features.workspace.WorkspaceViewModel
import java.io.File

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
                Text("Immagini")
                images.forEach { media ->
                    Card(modifier = Modifier.fillMaxWidth()) {
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
                Text("Documenti")
                documents.forEach { media ->
                    CompactExpandableRow(
                        title = "Allegato",
                        subtitle = "Documento commessa",
                        detail = media.fileName,
                    )
                }
            }
            if (finalPhotos.isNotEmpty()) {
                Text("Lavoro finito")
                finalPhotos.forEach { media ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(DashboardSpacing.compactGap),
                            verticalArrangement = Arrangement.spacedBy(DashboardSpacing.compactGap),
                        ) {
                            LocalImagePreview(
                                path = media.localPath,
                                description = "Foto lavoro finito",
                                onClick = { selectedImageId = media.id },
                            )
                        }
                    }
                }
            }
        }

        ActionRow {
            TouchButton(text = "Scatta foto", onClick = {
                showFinalCamera = false
                showCamera = true
            })
            TouchButton(text = "Foto finita", onClick = {
                showCamera = false
                showFinalCamera = true
            })
            TouchButton(text = "Allega file", onClick = { attachmentLauncher.launch("*/*") })
            if (showCamera) {
                TouchButton(text = "Chiudi camera", tone = ButtonTone.NEUTRAL, onClick = { showCamera = false })
            }
            if (showFinalCamera) {
                TouchButton(text = "Chiudi foto finita", tone = ButtonTone.NEUTRAL, onClick = { showFinalCamera = false })
            }
        }

        if (showCamera) {
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
        }
        if (showFinalCamera) {
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

@Composable
fun CameraCapturePreview(
    orderId: String,
    orderNumber: String,
    createPhotoFile: (String, String) -> Result<File>,
    onPhotoCaptured: (File) -> Unit,
    onError: (String) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember(context) { ContextCompat.getMainExecutor(context) }
    val previewView = remember(context) {
        PreviewView(context).apply {
            contentDescription = "Anteprima camera per acquisire una foto della commessa"
        }
    }
    val imageCapture = remember { ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build() }
    var hasCameraPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasCameraPermission = granted
        if (!granted) onError("Permesso camera negato")
    }

    if (!hasCameraPermission) {
        TouchButton(text = "Concedi permesso camera", onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) })
        return
    }

    DisposableEffect(context, lifecycleOwner, previewView, imageCapture) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener(
            {
                runCatching {
                    val cameraProvider = cameraProviderFuture.get()
                    val selector = when {
                        cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) -> CameraSelector.DEFAULT_BACK_CAMERA
                        cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) -> CameraSelector.DEFAULT_FRONT_CAMERA
                        else -> error("Camera non disponibile")
                    }
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    imageCapture.targetRotation = cameraTargetRotation(context, previewView)
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview, imageCapture)
                }.onFailure {
                    onError(it.message ?: "Camera non disponibile")
                }
            },
            executor,
        )

        onDispose {
            runCatching {
                if (cameraProviderFuture.isDone) {
                    cameraProviderFuture.get().unbindAll()
                }
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(DashboardSpacing.compactGap)) {
        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp),
            factory = { previewView },
        )
        TouchButton(
            modifier = Modifier.fillMaxWidth(),
            text = "Acquisisci foto",
            onClick = {
                imageCapture.targetRotation = cameraTargetRotation(context, previewView)
                createPhotoFile(orderId, orderNumber).fold(
                    onSuccess = { file ->
                        val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()
                        imageCapture.takePicture(
                            outputOptions,
                            executor,
                            object : ImageCapture.OnImageSavedCallback {
                                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                    onPhotoCaptured(file)
                                }

                                override fun onError(exception: ImageCaptureException) {
                                    file.delete()
                                    onError(exception.message ?: "Scatto non riuscito")
                                }
                            },
                        )
                    },
                    onFailure = { onError(it.message ?: "File foto non disponibile") },
                )
            },
        )
    }
}

@Suppress("DEPRECATION")
private fun cameraTargetRotation(context: android.content.Context, previewView: PreviewView): Int {
    val windowRotation = (context.getSystemService(android.content.Context.WINDOW_SERVICE) as? WindowManager)
        ?.defaultDisplay
        ?.rotation
    return normalizeCameraTargetRotation(previewView.display?.rotation ?: windowRotation)
}

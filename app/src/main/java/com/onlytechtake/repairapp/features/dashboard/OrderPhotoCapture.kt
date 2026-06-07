package com.onlytechtake.repairapp.features.dashboard

import android.Manifest
import android.content.pm.PackageManager
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
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
    var selectedImage by rememberSaveable { mutableStateOf<Pair<String, String>?>(null) }
    val attachmentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.importAttachment(detail.order.id, it) }
    }
    val finalPhotos = detail.media.filter { it.isFinalPhoto() }
    val images = detail.media.filter { it.isImageAttachment() && !it.isFinalPhoto() }
    val documents = detail.media.filterNot { it.isImageAttachment() }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle("Foto e allegati")
        if (images.isEmpty() && documents.isEmpty()) {
            Text("Nessuna foto o allegato.")
        } else {
            if (images.isNotEmpty()) {
                Text("Immagini")
                images.forEach { media ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            LocalImagePreview(
                                path = media.localPath,
                                description = "Foto commessa",
                                onClick = { selectedImage = media.localPath to media.fileName },
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
                        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            LocalImagePreview(
                                path = media.localPath,
                                description = "Foto lavoro finito",
                                onClick = { selectedImage = media.localPath to media.fileName },
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
                OutlinedButton(onClick = { showCamera = false }) {
                    Text("Chiudi camera")
                }
            }
            if (showFinalCamera) {
                OutlinedButton(onClick = { showFinalCamera = false }) {
                    Text("Chiudi foto finita")
                }
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

    selectedImage?.let { (path, title) ->
        LocalImageViewer(path = path, title = title, onDismiss = { selectedImage = null })
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
        Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
            Text("Concedi permesso camera")
        }
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

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp),
            factory = { previewView },
        )
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
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
        ) {
            Text("Acquisisci foto")
        }
    }
}

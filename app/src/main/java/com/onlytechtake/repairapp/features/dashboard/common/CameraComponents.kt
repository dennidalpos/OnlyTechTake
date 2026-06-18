package com.onlytechtake.repairapp.features.dashboard.common

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.core.design.RepairAppSpacing
import com.onlytechtake.repairapp.core.design.components.AppButton
import com.onlytechtake.repairapp.core.media.normalizeCameraTargetRotation
import java.io.File

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
    val previewDescription = stringResource(R.string.camera_preview_description)
    val executor = remember(context) { ContextCompat.getMainExecutor(context) }
    val previewView =
        remember(context, previewDescription) {
            PreviewView(context).apply {
                contentDescription = previewDescription
            }
        }
    val imageCapture = remember { ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build() }
    var hasCameraPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            hasCameraPermission = granted
            if (!granted) onError("Permesso camera negato")
        }

    if (!hasCameraPermission) {
        AppButton(text = "Concedi permesso camera", onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) })
        return
    }

    DisposableEffect(context, lifecycleOwner, previewView, imageCapture) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener(
            {
                runCatching {
                    val cameraProvider = cameraProviderFuture.get()
                    val selector =
                        when {
                            cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) -> CameraSelector.DEFAULT_BACK_CAMERA
                            cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) -> CameraSelector.DEFAULT_FRONT_CAMERA
                            else -> error("Camera non disponibile")
                        }
                    val preview =
                        Preview.Builder().build().also {
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

    Column(verticalArrangement = Arrangement.spacedBy(RepairAppSpacing.compactGap)) {
        AndroidView(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(280.dp),
            factory = { previewView },
        )
        AppButton(
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
private fun cameraTargetRotation(
    context: Context,
    previewView: PreviewView,
): Int {
    val windowRotation =
        (context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager)
            ?.defaultDisplay
            ?.rotation
    return normalizeCameraTargetRotation(previewView.display?.rotation ?: windowRotation)
}

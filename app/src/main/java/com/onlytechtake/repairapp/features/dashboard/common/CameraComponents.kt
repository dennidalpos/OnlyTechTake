package com.onlytechtake.repairapp.features.dashboard.common

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.core.design.RepairAppShapes
import com.onlytechtake.repairapp.core.design.RepairAppSpacing
import com.onlytechtake.repairapp.core.design.RepairAppTestTags
import com.onlytechtake.repairapp.core.design.components.AppButton
import com.onlytechtake.repairapp.core.design.components.AppButtonVariant
import com.onlytechtake.repairapp.core.media.normalizeCameraTargetRotation
import java.io.File

enum class CameraCaptureMode { PHOTO, VIDEO }

@Composable
fun CameraCapturePreview(
    orderId: String,
    orderNumber: String,
    initialMode: CameraCaptureMode = CameraCaptureMode.PHOTO,
    createPhotoFile: (String, String) -> Result<File>,
    createVideoFile: (String) -> Result<File>,
    onMediaCaptured: (File, CameraCaptureMode) -> Unit,
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

    var captureMode by remember { mutableStateOf(initialMode) }
    var recording: Recording? by remember { mutableStateOf(null) }
    var isRecording by remember { mutableStateOf(value = false) }

    val imageCapture =
        remember {
            ImageCapture
                .Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()
        }

    val videoCapture =
        remember {
            val recorder =
                Recorder
                    .Builder()
                    .setQualitySelector(QualitySelector.from(Quality.SD))
                    .build()
            androidx.camera.video.VideoCapture
                .withOutput(recorder)
        }

    val permissions =
        remember {
            mutableListOf(Manifest.permission.CAMERA)
                .apply {
                    add(Manifest.permission.RECORD_AUDIO)
                }.toTypedArray()
        }

    var hasPermissions by remember {
        mutableStateOf(
            permissions.all {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            },
        )
    }

    val cameraPermissionDeniedMsg = stringResource(R.string.camera_permission_denied)
    val cameraUnavailableMsg = stringResource(R.string.camera_unavailable)
    val cameraCaptureFailedMsg = stringResource(R.string.camera_capture_failed)
    val cameraFileErrorMsg = stringResource(R.string.camera_file_error)

    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            hasPermissions = result.values.all { it }
            if (!hasPermissions) onError(cameraPermissionDeniedMsg)
        }

    if (!hasPermissions) {
        AppButton(
            text = stringResource(R.string.camera_grant_permission),
            onClick = { permissionLauncher.launch(permissions) },
        )
        return
    }

    DisposableEffect(
        context,
        lifecycleOwner,
        previewView,
        imageCapture,
        videoCapture,
        captureMode,
    ) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener(
            {
                runCatching {
                    val cameraProvider = cameraProviderFuture.get()
                    val selector =
                        when {
                            cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) -> CameraSelector.DEFAULT_BACK_CAMERA
                            cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) -> CameraSelector.DEFAULT_FRONT_CAMERA
                            else -> error(cameraUnavailableMsg)
                        }
                    val preview =
                        Preview.Builder().build().also {
                            it.surfaceProvider = previewView.surfaceProvider
                        }
                    cameraProvider.unbindAll()

                    if (captureMode == CameraCaptureMode.PHOTO) {
                        imageCapture.targetRotation = cameraTargetRotation(context, previewView)
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            selector,
                            preview,
                            imageCapture,
                        )
                    } else {
                        videoCapture.targetRotation = cameraTargetRotation(context, previewView)
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            selector,
                            preview,
                            videoCapture,
                        )
                    }
                }.onFailure {
                    onError(it.message ?: cameraUnavailableMsg)
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

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(RepairAppSpacing.cardGap),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RepairAppShapes.card),
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { previewView },
            )
        }

        if (!isRecording) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(RepairAppSpacing.actionGap),
            ) {
                AppButton(
                    modifier = Modifier.weight(1f),
                    text = stringResource(R.string.label_media_photo),
                    variant = if (captureMode == CameraCaptureMode.PHOTO) AppButtonVariant.Primary else AppButtonVariant.Ghost,
                    onClick = { captureMode = CameraCaptureMode.PHOTO },
                )
                AppButton(
                    modifier = Modifier.weight(1f),
                    text = stringResource(R.string.label_media_video),
                    variant = if (captureMode == CameraCaptureMode.VIDEO) AppButtonVariant.Primary else AppButtonVariant.Ghost,
                    onClick = { captureMode = CameraCaptureMode.VIDEO },
                )
            }
        }

        if (captureMode == CameraCaptureMode.PHOTO) {
            AppButton(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .testTag(RepairAppTestTags.IntakeMediaCapturePhotoButton),
                text = stringResource(R.string.camera_capture_photo),
                leadingIcon = Icons.Default.PhotoCamera,
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
                                        onMediaCaptured(file, CameraCaptureMode.PHOTO)
                                    }

                                    override fun onError(exception: ImageCaptureException) {
                                        file.delete()
                                        onError(exception.message ?: cameraCaptureFailedMsg)
                                    }
                                },
                            )
                        },
                        onFailure = { onError(it.message ?: cameraFileErrorMsg) },
                    )
                },
            )
        } else {
            AppButton(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .testTag(if (isRecording) RepairAppTestTags.IntakeMediaStopVideoButton else RepairAppTestTags.IntakeMediaCaptureVideoButton),
                text =
                    if (isRecording) {
                        stringResource(R.string.camera_stop_video)
                    } else {
                        stringResource(
                            R.string.camera_capture_video,
                        )
                    },
                variant = if (isRecording) AppButtonVariant.Danger else AppButtonVariant.Primary,
                leadingIcon = if (isRecording) Icons.Default.Stop else Icons.Default.Videocam,
                onClick = {
                    if (isRecording) {
                        recording?.stop()
                        recording = null
                        isRecording = false
                    } else {
                        createVideoFile(orderNumber).fold(
                            onSuccess = { file ->
                                val outputOptions = FileOutputOptions.Builder(file).build()
                                recording =
                                    videoCapture.output
                                        .prepareRecording(context, outputOptions)
                                        .withAudioEnabled()
                                        .start(executor) { event ->
                                            if (event is VideoRecordEvent.Finalize) {
                                                if (event.hasError()) {
                                                    file.delete()
                                                    onError(
                                                        event.cause?.message
                                                            ?: cameraCaptureFailedMsg,
                                                    )
                                                } else {
                                                    onMediaCaptured(file, CameraCaptureMode.VIDEO)
                                                }
                                            }
                                        }
                                isRecording = true
                            },
                        ) { onError(it.message ?: cameraFileErrorMsg) }
                    }
                },
            )
        }
    }
}

private fun cameraTargetRotation(
    context: Context,
    previewView: PreviewView,
): Int {
    val windowRotation =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.display?.rotation
        } else {
            @Suppress("DEPRECATION")
            (context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager)
                ?.defaultDisplay
                ?.rotation
        }
    return normalizeCameraTargetRotation(previewView.display?.rotation ?: windowRotation)
}

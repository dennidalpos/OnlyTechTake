package com.onlytechtake.repairapp.features.workspace

import android.graphics.Bitmap
import android.net.Uri
import com.onlytechtake.repairapp.data.repositories.OrderMediaRepository
import com.onlytechtake.repairapp.data.repositories.BackupRepository
import com.onlytechtake.repairapp.data.repositories.SignatureRepository
import com.onlytechtake.repairapp.data.repositories.StagedOrderMedia
import com.onlytechtake.repairapp.domain.auth.AuthRepository
import com.onlytechtake.repairapp.domain.auth.AuthenticatedUser
import com.onlytechtake.repairapp.domain.workspace.CreateIntakeOrderUseCase
import com.onlytechtake.repairapp.domain.workspace.IntakeOrderInput
import com.onlytechtake.repairapp.domain.workspace.OrderDetail
import com.onlytechtake.repairapp.domain.workspace.validationErrors
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class WorkspaceMediaActions(
    private val authRepository: AuthRepository,
    private val currentUser: StateFlow<AuthenticatedUser?>,
    private val selectedOrder: StateFlow<OrderDetail?>,
    private val mutableUiState: MutableStateFlow<WorkspaceUiState>,
    private val scope: CoroutineScope,
    private val orderMediaRepository: OrderMediaRepository,
    private val signatureRepository: SignatureRepository,
    private val createIntakeOrderUseCase: CreateIntakeOrderUseCase,
    private val showSuccess: (String) -> Unit,
    private val showError: (Throwable) -> Unit,
    private val showMessage: (String, Boolean) -> Unit,
) {
    fun createPhotoFile(orderId: String, orderNumber: String): Result<File> =
        runCatching { orderMediaRepository.createPhotoFile(orderId, orderNumber) }

    fun createIntakePhotoFile(sessionId: String): Result<File> =
        runCatching { orderMediaRepository.createIntakePhotoFile(sessionId) }

    fun stageCapturedIntakePhoto(file: File, onStaged: (StagedOrderMedia) -> Unit) {
        orderMediaRepository.stageCapturedIntakePhoto(file).fold(
            onSuccess = {
                onStaged(it)
                showSuccess("Foto pronta per la presa in carico")
            },
            onFailure = showError,
        )
    }

    fun deleteStagedMedia(path: String) {
        orderMediaRepository.deleteStagedMedia(path).fold(
            onSuccess = { showSuccess("Media rimosso") },
            onFailure = showError,
        )
    }

    fun replaceStagedImage(staged: StagedOrderMedia, bitmap: Bitmap, onReplaced: (StagedOrderMedia) -> Unit) {
        orderMediaRepository.replaceStagedImage(staged, bitmap).fold(
            onSuccess = {
                onReplaced(it)
                showSuccess("Foto modificata")
            },
            onFailure = showError,
        )
    }

    fun importIntakeAttachment(sessionId: String, uri: Uri, onStaged: (StagedOrderMedia) -> Unit) {
        scope.launch {
            orderMediaRepository.importIntakeAttachment(sessionId, uri).fold(
                onSuccess = {
                    onStaged(it)
                    showSuccess("Allegato pronto per la presa in carico")
                },
                onFailure = showError,
            )
        }
    }

    fun createIntakeOrder(
        input: IntakeOrderInput,
        stagedMedia: List<StagedOrderMedia>,
        signerName: String,
        privacyAccepted: Boolean,
        termsAccepted: Boolean,
        signatureBitmap: Bitmap,
        onCreated: () -> Unit,
    ) {
        scope.launch {
            val actor = currentUser.value ?: return@launch showMessage("Sessione non valida", true)
            authRepository.touchSession()
            val errors = input.copy(
                stagedMediaCount = stagedMedia.size,
                hasPrivacySignature = privacyAccepted && termsAccepted,
            ).validationErrors()
            if (errors.isNotEmpty()) return@launch showMessage(errors.joinToString("\n"), true)

            createIntakeOrderUseCase.execute(
                actor = actor,
                input = input,
                stagedMedia = stagedMedia,
                signerName = signerName,
                privacyAccepted = privacyAccepted,
                termsAccepted = termsAccepted,
                signatureBitmap = signatureBitmap,
            ).fold(
                onSuccess = { result ->
                    mutableUiState.update {
                        it.copy(
                            destination = WorkspaceDestination.ORDER_DETAIL,
                            selectedOrderId = result.order.id,
                            customerSearchQuery = "",
                            preparedMessage = null,
                            preparedMessageBody = "",
                        )
                    }
                    onCreated()
                    showSuccess("Commessa presa in carico")
                },
                onFailure = showError,
            )
        }
    }

    fun registerCapturedPhoto(orderId: String, file: File) {
        scope.launch {
            val actor = currentUser.value ?: return@launch showMessage("Sessione non valida", true)
            authRepository.touchSession()
            orderMediaRepository.registerCapturedPhoto(actor, orderId, file).fold(
                onSuccess = { showSuccess("Foto salvata") },
                onFailure = showError,
            )
        }
    }

    fun registerCapturedFinalPhoto(orderId: String, file: File) {
        scope.launch {
            val actor = currentUser.value ?: return@launch showMessage("Sessione non valida", true)
            authRepository.touchSession()
            orderMediaRepository.registerCapturedFinalPhoto(actor, orderId, file).fold(
                onSuccess = { showSuccess("Foto lavoro finito salvata") },
                onFailure = showError,
            )
        }
    }

    fun importAttachment(orderId: String, uri: Uri) {
        scope.launch {
            val actor = currentUser.value ?: return@launch showMessage("Sessione non valida", true)
            authRepository.touchSession()
            orderMediaRepository.importAttachment(actor, orderId, uri).fold(
                onSuccess = { showSuccess("Allegato salvato") },
                onFailure = showError,
            )
        }
    }

    fun deleteMedia(mediaId: String) {
        scope.launch {
            val actor = currentUser.value ?: return@launch showMessage("Sessione non valida", true)
            authRepository.touchSession()
            orderMediaRepository.deleteMedia(actor, mediaId).fold(
                onSuccess = { showSuccess("Media rimosso") },
                onFailure = showError,
            )
        }
    }

    fun replaceImage(mediaId: String, bitmap: Bitmap) {
        scope.launch {
            val actor = currentUser.value ?: return@launch showMessage("Sessione non valida", true)
            authRepository.touchSession()
            orderMediaRepository.replaceImage(actor, mediaId, bitmap).fold(
                onSuccess = { showSuccess("Foto modificata") },
                onFailure = showError,
            )
        }
    }

    fun saveSignature(
        orderId: String,
        signerName: String,
        privacyAccepted: Boolean,
        termsAccepted: Boolean,
        bitmap: Bitmap,
    ) {
        scope.launch {
            val actor = currentUser.value ?: return@launch showMessage("Sessione non valida", true)
            authRepository.touchSession()
            signatureRepository.saveSignature(
                actor = actor,
                orderId = orderId,
                signerName = signerName,
                privacyAccepted = privacyAccepted,
                termsAccepted = termsAccepted,
                bitmap = bitmap,
            ).fold(
                onSuccess = { showSuccess("Firma salvata") },
                onFailure = showError,
            )
        }
    }

    fun exportSelectedOrderPdf(backupRepository: BackupRepository, uri: Uri) {
        scope.launch {
            val actor = currentUser.value ?: return@launch showMessage("Sessione non valida", true)
            authRepository.touchSession()
            val detail = selectedOrder.value ?: return@launch showMessage("Seleziona prima una commessa", true)
            backupRepository.exportOrderSummaryPdf(actor, detail, uri).fold(
                onSuccess = { showSuccess("PDF commessa esportato") },
                onFailure = showError,
            )
        }
    }
}

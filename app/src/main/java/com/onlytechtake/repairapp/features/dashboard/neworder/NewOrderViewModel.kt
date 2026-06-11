package com.onlytechtake.repairapp.features.dashboard.neworder

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.onlytechtake.repairapp.core.error.ErrorHandler
import com.onlytechtake.repairapp.data.repositories.CustomerRepository
import com.onlytechtake.repairapp.data.repositories.OrderMediaRepository
import com.onlytechtake.repairapp.data.repositories.StagedOrderMedia
import com.onlytechtake.repairapp.domain.auth.AuthRepository
import com.onlytechtake.repairapp.domain.workspace.IntakeOrderInput
import com.onlytechtake.repairapp.domain.workspace.CreateIntakeOrderUseCase
import com.onlytechtake.repairapp.features.workspace.AuthenticatedActionRunner
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

class NewOrderViewModel(
    private val createIntakeOrderUseCase: CreateIntakeOrderUseCase,
    private val orderMediaRepository: OrderMediaRepository,
    private val customerRepository: CustomerRepository,
    private val authRepository: AuthRepository,
    private val errorHandler: ErrorHandler,
) : ViewModel() {

    private val actionRunner = AuthenticatedActionRunner(
        authRepository = authRepository,
        currentUser = authRepository.currentUser,
        scope = viewModelScope,
        showSuccess = { /* Handle success */ },
        showError = { errorHandler.handleError(it) },
        showMessage = { _, _ -> /* Handle message */ }
    )

    fun createIntakePhotoFile(sessionId: String): Result<File> =
        runCatching { orderMediaRepository.createIntakePhotoFile(sessionId) }

    fun stageCapturedPhoto(file: File, onStaged: (StagedOrderMedia) -> Unit) {
        viewModelScope.launch {
            orderMediaRepository.stageCapturedIntakePhoto(file)
                .onSuccess(onStaged)
                .onFailure { errorHandler.handleError(it) }
        }
    }

    fun deleteStagedMedia(path: String) {
        orderMediaRepository.deleteStagedMedia(path)
    }

    fun replaceStagedImage(staged: StagedOrderMedia, bitmap: Bitmap, onReplaced: (StagedOrderMedia) -> Unit) {
        viewModelScope.launch {
            orderMediaRepository.replaceStagedImage(staged, bitmap)
                .onSuccess(onReplaced)
                .onFailure { errorHandler.handleError(it) }
        }
    }

    fun importAttachment(sessionId: String, uri: Uri, onStaged: (StagedOrderMedia) -> Unit) {
        viewModelScope.launch {
            orderMediaRepository.importIntakeAttachment(sessionId, uri)
                .onSuccess(onStaged)
                .onFailure { errorHandler.handleError(it) }
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
        actionRunner.run(onSuccess = onCreated) { actor ->
            createIntakeOrderUseCase.execute(
                actor = actor,
                input = input,
                stagedMedia = stagedMedia,
                signerName = signerName,
                privacyAccepted = privacyAccepted,
                termsAccepted = termsAccepted,
                signatureBitmap = signatureBitmap
            )
        }
    }

    class Factory(
        private val createIntakeOrderUseCase: CreateIntakeOrderUseCase,
        private val orderMediaRepository: OrderMediaRepository,
        private val customerRepository: CustomerRepository,
        private val authRepository: AuthRepository,
        private val errorHandler: ErrorHandler,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return NewOrderViewModel(
                createIntakeOrderUseCase,
                orderMediaRepository,
                customerRepository,
                authRepository,
                errorHandler
            ) as T
        }
    }
}

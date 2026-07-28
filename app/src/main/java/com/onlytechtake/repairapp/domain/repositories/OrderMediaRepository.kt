package com.onlytechtake.repairapp.domain.repositories

import android.graphics.Bitmap
import android.net.Uri
import com.onlytechtake.repairapp.domain.auth.AuthenticatedUser
import com.onlytechtake.repairapp.domain.models.OrderMedia
import com.onlytechtake.repairapp.domain.models.StagedOrderMedia
import java.io.File

interface OrderMediaRepository {
    fun createPhotoFile(
        orderId: String,
        orderNumber: String,
    ): File

    fun createVideoFile(
        orderId: String,
        orderNumber: String,
    ): File

    fun createIntakePhotoFile(sessionId: String): File

    fun createIntakeVideoFile(sessionId: String): File

    suspend fun stageCapturedIntakePhoto(file: File): Result<StagedOrderMedia>

    suspend fun stageCapturedIntakeVideo(file: File): Result<StagedOrderMedia>

    suspend fun importIntakeAttachment(
        sessionId: String,
        uri: Uri,
    ): Result<StagedOrderMedia>

    suspend fun registerCapturedPhoto(
        actor: AuthenticatedUser,
        orderId: String,
        file: File,
    ): Result<OrderMedia>

    suspend fun registerCapturedVideo(
        actor: AuthenticatedUser,
        orderId: String,
        file: File,
    ): Result<OrderMedia>

    suspend fun importAttachment(
        actor: AuthenticatedUser,
        orderId: String,
        uri: Uri,
    ): Result<OrderMedia>

    suspend fun deleteMedia(
        actor: AuthenticatedUser,
        mediaId: String,
    ): Result<Unit>

    suspend fun replaceImage(
        actor: AuthenticatedUser,
        mediaId: String,
        bitmap: Bitmap,
    ): Result<OrderMedia>

    suspend fun attachStagedMedia(
        actor: AuthenticatedUser,
        orderId: String,
        stagedMedia: List<StagedOrderMedia>,
    ): Result<List<OrderMedia>>

    suspend fun deleteMediaFiles(media: List<OrderMedia>)

    suspend fun deleteStagedMedia(path: String): Result<Unit>

    suspend fun replaceStagedImage(
        staged: StagedOrderMedia,
        bitmap: Bitmap,
    ): Result<StagedOrderMedia>
}

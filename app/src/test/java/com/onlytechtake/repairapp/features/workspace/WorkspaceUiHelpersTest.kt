package com.onlytechtake.repairapp.features.workspace

import android.content.Context
import com.onlytechtake.repairapp.core.utils.formatFileSize
import com.onlytechtake.repairapp.core.utils.formatUiDateTime
import com.onlytechtake.repairapp.data.local.entities.DiagnosisOutcome
import com.onlytechtake.repairapp.data.local.entities.OrderMediaEntity
import com.onlytechtake.repairapp.data.local.entities.OrderPriority
import com.onlytechtake.repairapp.data.local.entities.OrderStatus
import com.onlytechtake.repairapp.data.local.entities.RepairOrderEntity
import com.onlytechtake.repairapp.data.local.entities.SignatureEntity
import com.onlytechtake.repairapp.domain.workspace.OrderAttachmentCounts
import com.onlytechtake.repairapp.domain.workspace.canConfirmDiagnosisResult
import com.onlytechtake.repairapp.domain.workspace.nextActionLabel
import com.onlytechtake.repairapp.domain.workspace.shouldShowDiagnosisEstimate
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneOffset
import java.util.Locale

class WorkspaceUiHelpersTest {
    private val context =
        mockk<Context> {
            every { getString(any()) } returns "label"
            every { getString(any(), any()) } returns "label"
        }

    @Test
    fun imageRecognitionUsesMediaTypeAndMimeType() {
        assertTrue(media("PHOTO", "application/octet-stream").isImageAttachment())
        assertTrue(media("DOCUMENT", "image/png").isImageAttachment())
    }

    @Test
    fun attachmentCountsSplitImagesDocumentsAndSignatures() {
        val counts =
            attachmentCounts(
                media =
                    listOf(
                        media("PHOTO", "image/jpeg"),
                        media("DOCUMENT", "application/pdf"),
                        media("FINAL_PHOTO", "image/jpeg"),
                    ),
                signatures = listOf(signature()),
            )

        assertEquals(
            AttachmentCounts(images = 1, documents = 1, signatures = 1, finalPhotos = 1),
            counts,
        )
        assertEquals("Foto 1 - Documenti 1 - Firme 1 - Lavoro finito 1", counts.label(context))
        assertEquals("Foto 2 - Documenti 3 - Firme 1", OrderAttachmentCounts(2, 3, 1).label(context))
    }

    @Test
    fun finalPhotosAreImageHistoryButNotIntakePhotos() {
        val media = media("FINAL_PHOTO", "image/jpeg")

        assertTrue(media.isImageAttachment())
        assertTrue(media.isFinalPhoto())
    }

    @Test
    fun fileSizeAndDatesAreReadable() {
        val locale = Locale.ITALY
        assertEquals("512 B", formatFileSize(512, locale))
        assertEquals("2 KB", formatFileSize(2_048, locale))
        assertEquals("1,5 MB", formatFileSize(1_572_864, locale))
        assertEquals("01/01/1970 00:00", formatUiDateTime(0L, ZoneOffset.UTC, locale))
    }

    @Test
    fun nextActionComesFromWorkflowPolicy() {
        assertEquals("label", nextActionLabel(context, OrderStatus.OPEN))
        assertEquals("label", nextActionLabel(context, OrderStatus.DIAGNOSIS))
        assertEquals("label", nextActionLabel(context, OrderStatus.RESULT))
        assertEquals("label", nextActionLabel(context, OrderStatus.CLOSED))
        assertEquals(
            "label",
            nextActionLabel(context, order(DiagnosisOutcome.REPAIRABLE)),
        )
        assertEquals(
            "label",
            nextActionLabel(context, order(DiagnosisOutcome.NOT_REPAIRABLE)),
        )
    }

    @Test
    fun orderStatusStepNumberMapsWorkflowStates() {
        assertEquals(1, orderStatusStepNumber(OrderStatus.OPEN))
        assertEquals(2, orderStatusStepNumber(OrderStatus.IN_PROGRESS))
        assertEquals(2, orderStatusStepNumber(OrderStatus.DIAGNOSIS))
        assertEquals(3, orderStatusStepNumber(OrderStatus.RESULT))
        assertEquals(4, orderStatusStepNumber(OrderStatus.REPAIRING))
        assertEquals(5, orderStatusStepNumber(OrderStatus.READY_FOR_PICKUP))
        assertEquals(6, orderStatusStepNumber(OrderStatus.CLOSED))
    }

    @Test
    fun diagnosisUiRequiresOutcomeAndPositiveEstimateOnlyWhenRepairable() {
        assertTrue(!shouldShowDiagnosisEstimate(null))
        assertTrue(shouldShowDiagnosisEstimate(DiagnosisOutcome.REPAIRABLE))
        assertTrue(!canConfirmDiagnosisResult("Esito", null, null))
        assertTrue(!canConfirmDiagnosisResult("Esito", DiagnosisOutcome.REPAIRABLE, null))
        assertTrue(!canConfirmDiagnosisResult("Esito", DiagnosisOutcome.REPAIRABLE, 0L))
        assertTrue(canConfirmDiagnosisResult("Esito", DiagnosisOutcome.REPAIRABLE, 10_000L))
        assertTrue(canConfirmDiagnosisResult("Esito", DiagnosisOutcome.NOT_REPAIRABLE, null))
    }

    private fun media(
        mediaType: String,
        mimeType: String,
    ) = OrderMediaEntity(
        id = "media-$mediaType-$mimeType",
        orderId = "order",
        mediaType = mediaType,
        localPath = "path",
        fileName = "file",
        mimeType = mimeType,
        sizeBytes = 1L,
        width = null,
        height = null,
        createdByUserId = null,
        capturedAt = 0L,
        createdAt = 0L,
    )

    private fun signature() =
        SignatureEntity(
            id = "signature",
            orderId = "order",
            signerName = "Cliente",
            privacyAccepted = true,
            termsAccepted = true,
            privacyTextSnapshot = "privacy",
            privacyTemplateVersion = 1,
            termsTextSnapshot = "terms",
            termsTemplateVersion = 1,
            localPath = "signature.png",
            fileName = "signature.png",
            mimeType = "image/png",
            sizeBytes = 1L,
            width = 100,
            height = 40,
            createdByUserId = null,
            signedAt = 0L,
            createdAt = 0L,
        )

    private fun order(outcome: DiagnosisOutcome) =
        RepairOrderEntity(
            id = "order",
            orderNumber = "2026-0001",
            customerId = "customer",
            title = "Oggetto",
            categoryId = "category",
            status = OrderStatus.RESULT,
            priority = OrderPriority.NORMAL,
            assignedToUserId = "operator",
            createdByUserId = "operator",
            description = null,
            internalNotes = null,
            diagnosisOutcome = outcome,
            estimateAmountCents = 10_000L.takeIf { outcome == DiagnosisOutcome.REPAIRABLE },
            finalAmountCents = null,
            createdAt = 0L,
            updatedAt = 0L,
            closedAt = null,
            closureReason = null,
        )
}

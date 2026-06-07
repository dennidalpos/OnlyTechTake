package com.onlytechtake.repairapp.features.workspace

import com.onlytechtake.repairapp.data.local.entities.OrderMediaEntity
import com.onlytechtake.repairapp.data.local.entities.OrderStatus
import com.onlytechtake.repairapp.data.local.entities.SignatureEntity
import com.onlytechtake.repairapp.domain.workspace.OrderAttachmentCounts
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkspaceUiHelpersTest {
    @Test
    fun imageRecognitionUsesMediaTypeAndMimeType() {
        assertTrue(media("PHOTO", "application/octet-stream").isImageAttachment())
        assertTrue(media("DOCUMENT", "image/png").isImageAttachment())
    }

    @Test
    fun attachmentCountsSplitImagesDocumentsAndSignatures() {
        val counts = attachmentCounts(
            media = listOf(
                media("PHOTO", "image/jpeg"),
                media("DOCUMENT", "application/pdf"),
                media("FINAL_PHOTO", "image/jpeg"),
            ),
            signatures = listOf(signature()),
        )

        assertEquals(AttachmentCounts(images = 1, documents = 1, signatures = 1, finalPhotos = 1), counts)
        assertEquals("Foto 1 - Documenti 1 - Firme 1 - Lavoro finito 1", counts.label())
        assertEquals("Foto 2 - Documenti 3 - Firme 1", OrderAttachmentCounts(2, 3, 1).label())
    }

    @Test
    fun finalPhotosAreImageHistoryButNotIntakePhotos() {
        val media = media("FINAL_PHOTO", "image/jpeg")

        assertTrue(media.isImageAttachment())
        assertTrue(media.isFinalPhoto())
    }

    @Test
    fun fileSizeAndDatesAreReadable() {
        assertEquals("512 B", formatFileSize(512))
        assertEquals("2 KB", formatFileSize(2_048))
        assertEquals("1,5 MB", formatFileSize(1_572_864))
        assertEquals("01/01/1970 00:00", formatUiDateTime(0L, ZoneOffset.UTC))
    }

    @Test
    fun nextActionComesFromWorkflowPolicy() {
        assertEquals("Prendi in carico", nextActionLabel(OrderStatus.OPEN))
        assertEquals("Registra risultato", nextActionLabel(OrderStatus.DIAGNOSIS))
        assertEquals("Nessuna azione", nextActionLabel(OrderStatus.CLOSED))
    }

    private fun media(mediaType: String, mimeType: String) = OrderMediaEntity(
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

    private fun signature() = SignatureEntity(
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
}

package com.onlytechtake.repairapp.data.repositories

import com.onlytechtake.repairapp.core.media.MediaStorageProvider
import java.io.File

class OrderDocumentFileStorage(
    private val mediaStorageProvider: MediaStorageProvider
) {
    
    fun getDocumentFile(orderId: String, fileName: String): File {
        return File(mediaStorageProvider.getOrderDocumentsDir(orderId), fileName)
    }

    fun getOrderDocumentsDir(orderId: String): File {
        return mediaStorageProvider.getOrderDocumentsDir(orderId)
    }
}

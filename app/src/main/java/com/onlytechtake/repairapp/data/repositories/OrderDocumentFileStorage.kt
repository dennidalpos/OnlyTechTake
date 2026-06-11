package com.onlytechtake.repairapp.data.repositories

import android.content.Context
import java.io.File

class OrderDocumentFileStorage(private val context: Context) {
    
    fun getDocumentFile(orderId: String, fileName: String): File {
        val dir = File(context.filesDir, "orders/$orderId/documents")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return File(dir, fileName)
    }

    fun getOrderDocumentsDir(orderId: String): File {
        return File(context.filesDir, "orders/$orderId/documents")
    }
}

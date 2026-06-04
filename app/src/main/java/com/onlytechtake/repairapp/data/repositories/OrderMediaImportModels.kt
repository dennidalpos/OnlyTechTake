package com.onlytechtake.repairapp.data.repositories

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns

data class StagedOrderMedia(
    val localPath: String,
    val fileName: String,
    val mimeType: String,
    val mediaType: String,
    val sizeBytes: Long,
    val width: Int?,
    val height: Int?,
)

internal fun ContentResolver.displayName(uri: Uri): String? =
    query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
        } else {
            null
        }
    }

internal fun ContentResolver.openableSize(uri: Uri): Long? =
    query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
        val sizeColumn = cursor.getColumnIndex(OpenableColumns.SIZE)
        if (cursor.moveToFirst() && sizeColumn >= 0 && !cursor.isNull(sizeColumn)) {
            cursor.getLong(sizeColumn)
        } else {
            null
        }
    }

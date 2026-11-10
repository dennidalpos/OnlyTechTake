package com.onlytechtake.repairapp.domain.models

data class StagedOrderMedia(
    val localPath: String,
    val fileName: String,
    val mimeType: String,
    val mediaType: String,
    val sizeBytes: Long,
    val width: Int?,
    val height: Int?,
)

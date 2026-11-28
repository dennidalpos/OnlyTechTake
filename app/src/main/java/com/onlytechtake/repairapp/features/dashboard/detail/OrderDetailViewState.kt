package com.onlytechtake.repairapp.features.dashboard.detail

import com.onlytechtake.repairapp.domain.models.OrderDetail
import com.onlytechtake.repairapp.domain.models.OrderDocument
import com.onlytechtake.repairapp.domain.models.PreparedMessage

data class OrderDetailViewState(
    val detail: OrderDetail? = null,
    val documents: List<OrderDocument> = emptyList(),
    val isProcessing: Boolean = false,
    val isGeneratingStatusPdf: Boolean = false,
    val statusPdfError: String? = null,
    val showPdfSentConfirmation: Boolean = false,
    val preparedMessage: PreparedMessage? = null,
    val preparedMessageBody: String = "",
    val notesExpanded: Boolean = false,
    val photosExpanded: Boolean = false,
    val timelineExpanded: Boolean = false,
    val signaturesExpanded: Boolean = false,
)

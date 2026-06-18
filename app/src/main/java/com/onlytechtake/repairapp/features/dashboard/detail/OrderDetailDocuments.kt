package com.onlytechtake.repairapp.features.dashboard.detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.core.design.RepairAppSpacing
import com.onlytechtake.repairapp.core.design.components.*
import com.onlytechtake.repairapp.core.utils.formatUiDateTime
import com.onlytechtake.repairapp.data.local.entities.OrderDocumentEntity

@Composable
fun OrderDocumentSection(
    isGeneratingPdf: Boolean,
    pdfError: String?,
    documents: List<OrderDocumentEntity>,
    hasSignatures: Boolean,
    onGeneratePdf: (Boolean) -> Unit,
    onShareDocument: (String) -> Unit,
    onDeleteDocument: (String) -> Unit,
    requestConfirm: (String, String, () -> Unit) -> Unit,
) {
    val missingSignatureTitle = stringResource(R.string.order_pdf_missing_signature_title)
    val missingSignatureText = stringResource(R.string.order_pdf_missing_signature_text)
    val deleteDocumentConfirmTitle = stringResource(R.string.order_confirm_delete_document_title)
    val deleteDocumentConfirmText = stringResource(R.string.order_confirm_delete_document_text)

    StandardCard {
        Column(
            modifier = Modifier.padding(RepairAppSpacing.cardPadding),
            verticalArrangement = Arrangement.spacedBy(RepairAppSpacing.fieldGap),
        ) {
            Text(stringResource(R.string.order_customer_documents), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            AppButton(
                text = stringResource(R.string.order_generate_status_pdf),
                variant = AppButtonVariant.Secondary,
                enabled = !isGeneratingPdf,
                size = AppButtonSize.Compact,
                onClick = {
                    if (!hasSignatures) {
                        requestConfirm(
                            missingSignatureTitle,
                            missingSignatureText,
                        ) {
                            onGeneratePdf(false)
                        }
                    } else {
                        onGeneratePdf(true)
                    }
                },
            )

            if (isGeneratingPdf) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            pdfError?.let { error ->
                AppMessageBanner(message = "Errore: $error", type = BannerType.Error)
            }

            if (documents.isNotEmpty()) {
                Text(stringResource(R.string.order_document_history), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
                documents.forEach { doc ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f).clickable { onShareDocument(doc.id) }) {
                            Text(doc.fileName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            Text(
                                text = stringResource(R.string.order_document_generated, formatUiDateTime(doc.createdAt)) + (if (doc.sharedAt != null) " (${stringResource(R.string.order_wa_sent)})" else ""),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary,
                            )
                        }
                        Row {
                            AppIconButton(
                                imageVector = Icons.Default.Share,
                                contentDescription = stringResource(R.string.order_share),
                                onClick = { onShareDocument(doc.id) },
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            AppIconButton(
                                imageVector = Icons.Default.Delete,
                                contentDescription = stringResource(R.string.content_description_delete),
                                tint = MaterialTheme.colorScheme.error,
                                onClick = {
                                    requestConfirm(
                                        deleteDocumentConfirmTitle,
                                        deleteDocumentConfirmText,
                                    ) {
                                        onDeleteDocument(doc.id)
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

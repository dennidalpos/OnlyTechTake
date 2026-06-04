package com.onlytechtake.repairapp.features.dashboard

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.onlytechtake.repairapp.domain.workspace.OrderDetail
import com.onlytechtake.repairapp.features.workspace.WorkspaceViewModel

@Composable
fun OrderSignatureSection(
    detail: OrderDetail,
    viewModel: WorkspaceViewModel,
) {
    val templates by viewModel.templates.collectAsState()
    val privacyText = templates.firstOrNull { it.code == PRIVACY_TEMPLATE_CODE && it.active }?.body ?: DEFAULT_PRIVACY_TEXT
    val termsText = templates.firstOrNull { it.code == TERMS_TEMPLATE_CODE && it.active }?.body ?: DEFAULT_TERMS_TEXT

    SignatureCaptureForm(
        title = "Firma privacy",
        privacyText = privacyText,
        termsText = termsText,
        existingSignatures = detail.signatures,
        submitText = "Salva firma",
        onSubmit = { capture ->
            viewModel.saveSignature(
                orderId = detail.order.id,
                signerName = capture.signerName,
                privacyAccepted = capture.privacyAccepted,
                termsAccepted = capture.termsAccepted,
                bitmap = capture.bitmap,
            )
        },
    )
}

const val PRIVACY_TEMPLATE_CODE = "privacy_breve"
const val TERMS_TEMPLATE_CODE = "condizioni_presa_in_carico"
const val DEFAULT_PRIVACY_TEXT = "I dati sono trattati solo per gestione riparazione, comunicazioni e obblighi fiscali."
const val DEFAULT_TERMS_TEXT = "Il cliente autorizza la presa in carico del bene e accetta le condizioni operative comunicate."

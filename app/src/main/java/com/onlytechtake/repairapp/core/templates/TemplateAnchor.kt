package com.onlytechtake.repairapp.core.templates

import androidx.annotation.StringRes
import com.onlytechtake.repairapp.R

/**
 * Represents a logical point in the application where a message template is used.
 * Users can associate different templates to these anchors in the settings.
 */
enum class TemplateAnchor(
    val settingKey: String,
    val defaultCode: String,
    val stepNumber: Int,
    @param:StringRes val stepNameRes: Int,
    @param:StringRes val labelRes: Int,
    @param:StringRes val descriptionRes: Int,
) {
    WHATSAPP_INTAKE(
        settingKey = "anchor_wa_intake",
        defaultCode = "whatsapp_presa_in_carico",
        stepNumber = 1,
        stepNameRes = R.string.anchor_step_intake,
        labelRes = R.string.anchor_label_wa_intake,
        descriptionRes = R.string.anchor_desc_wa_intake,
    ),
    WHATSAPP_UPDATE(
        settingKey = "anchor_wa_update",
        defaultCode = "whatsapp_aggiornamento",
        stepNumber = 2,
        stepNameRes = R.string.anchor_step_update,
        labelRes = R.string.anchor_label_wa_update,
        descriptionRes = R.string.anchor_desc_wa_update,
    ),
    WHATSAPP_PROCESSING(
        settingKey = "anchor_wa_processing",
        defaultCode = "whatsapp_lavorazione",
        stepNumber = 2,
        stepNameRes = R.string.anchor_step_update,
        labelRes = R.string.anchor_label_wa_processing,
        descriptionRes = R.string.anchor_desc_wa_processing,
    ),
    WHATSAPP_DIAGNOSIS(
        settingKey = "anchor_wa_diagnosis",
        defaultCode = "whatsapp_diagnosi",
        stepNumber = 3,
        stepNameRes = R.string.anchor_step_diagnosis,
        labelRes = R.string.anchor_label_wa_diagnosis,
        descriptionRes = R.string.anchor_desc_wa_diagnosis,
    ),
    WHATSAPP_RESULT(
        settingKey = "anchor_wa_result",
        defaultCode = "whatsapp_risultato",
        stepNumber = 4,
        stepNameRes = R.string.anchor_step_outcome,
        labelRes = R.string.anchor_label_wa_result,
        descriptionRes = R.string.anchor_desc_wa_result,
    ),
    WHATSAPP_QUOTE(
        settingKey = "anchor_wa_quote",
        defaultCode = "whatsapp_preventivo_intervento",
        stepNumber = 4,
        stepNameRes = R.string.anchor_step_quote,
        labelRes = R.string.anchor_label_wa_quote,
        descriptionRes = R.string.anchor_desc_wa_quote,
    ),
    WHATSAPP_NOT_REPAIRABLE(
        settingKey = "anchor_wa_not_repairable",
        defaultCode = "whatsapp_non_riparabile",
        stepNumber = 4,
        stepNameRes = R.string.anchor_step_outcome,
        labelRes = R.string.anchor_label_wa_not_repairable,
        descriptionRes = R.string.anchor_desc_wa_not_repairable,
    ),
    WHATSAPP_REPAIRING(
        settingKey = "anchor_wa_repairing",
        defaultCode = "whatsapp_riparazione",
        stepNumber = 5,
        stepNameRes = R.string.anchor_step_repair,
        labelRes = R.string.anchor_label_wa_repairing,
        descriptionRes = R.string.anchor_desc_wa_repairing,
    ),
    WHATSAPP_COMPLETED(
        settingKey = "anchor_wa_completed",
        defaultCode = "whatsapp_completamento",
        stepNumber = 7,
        stepNameRes = R.string.anchor_step_closed,
        labelRes = R.string.anchor_label_wa_completed,
        descriptionRes = R.string.anchor_desc_wa_completed,
    ),
    WHATSAPP_READY(
        settingKey = "anchor_wa_ready",
        defaultCode = "whatsapp_pronto_ritiro",
        stepNumber = 6,
        stepNameRes = R.string.anchor_step_pickup,
        labelRes = R.string.anchor_label_wa_ready,
        descriptionRes = R.string.anchor_desc_wa_ready,
    ),
    WHATSAPP_PDF_STATUS(
        settingKey = "anchor_wa_pdf_status",
        defaultCode = "whatsapp_stato_avanzamento_pdf",
        stepNumber = 6,
        stepNameRes = R.string.anchor_step_docs,
        labelRes = R.string.anchor_label_wa_pdf,
        descriptionRes = R.string.anchor_desc_wa_pdf,
    ),
    PRIVACY_TEXT(
        settingKey = "anchor_privacy_text",
        defaultCode = "privacy_breve",
        stepNumber = 0,
        stepNameRes = R.string.anchor_step_legal,
        labelRes = R.string.anchor_label_legal_privacy,
        descriptionRes = R.string.anchor_desc_legal_privacy,
    ),
    TERMS_TEXT(
        settingKey = "anchor_terms_text",
        defaultCode = "condizioni_presa_in_carico",
        stepNumber = 0,
        stepNameRes = R.string.anchor_step_legal,
        labelRes = R.string.anchor_label_legal_terms,
        descriptionRes = R.string.anchor_desc_legal_terms,
    ),
}

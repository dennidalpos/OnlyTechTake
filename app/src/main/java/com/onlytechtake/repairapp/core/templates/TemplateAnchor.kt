package com.onlytechtake.repairapp.core.templates

/**
 * Represents a logical point in the application where a message template is used.
 * Users can associate different templates to these anchors in the settings.
 */
enum class TemplateAnchor(
    val settingKey: String,
    val defaultCode: String,
    val stepNumber: Int,
    val stepName: String,
    val label: String,
    val description: String
) {
    WHATSAPP_INTAKE(
        settingKey = "anchor_wa_intake",
        defaultCode = "whatsapp_presa_in_carico",
        stepNumber = 1,
        stepName = "Ingresso",
        label = "WA: Presa in carico",
        description = "Inviato quando una nuova commessa viene aperta."
    ),
    WHATSAPP_UPDATE(
        settingKey = "anchor_wa_update",
        defaultCode = "whatsapp_aggiornamento",
        stepNumber = 2,
        stepName = "Aggiornamento",
        label = "WA: Aggiornamento",
        description = "Inviato per comunicare cambi di stato generici."
    ),
    WHATSAPP_DIAGNOSIS(
        settingKey = "anchor_wa_diagnosis",
        defaultCode = "whatsapp_diagnosi",
        stepNumber = 3,
        stepName = "Diagnosi",
        label = "WA: Diagnosi in corso",
        description = "Inviato quando l'oggetto entra in fase di diagnosi."
    ),
    WHATSAPP_QUOTE(
        settingKey = "anchor_wa_quote",
        defaultCode = "whatsapp_preventivo_intervento",
        stepNumber = 4,
        stepName = "Preventivo",
        label = "WA: Preventivo pronto",
        description = "Inviato quando il preventivo è pronto per l'accettazione."
    ),
    WHATSAPP_NOT_REPAIRABLE(
        settingKey = "anchor_wa_not_repairable",
        defaultCode = "whatsapp_non_riparabile",
        stepNumber = 4,
        stepName = "Esito",
        label = "WA: Non riparabile",
        description = "Inviato quando l'oggetto non risulta riparabile."
    ),
    WHATSAPP_REPAIRING(
        settingKey = "anchor_wa_repairing",
        defaultCode = "whatsapp_riparazione",
        stepNumber = 5,
        stepName = "Riparazione",
        label = "WA: In riparazione",
        description = "Inviato quando inizia l'intervento tecnico."
    ),
    WHATSAPP_READY(
        settingKey = "anchor_wa_ready",
        defaultCode = "whatsapp_pronto_ritiro",
        stepNumber = 6,
        stepName = "Ritiro",
        label = "WA: Pronto ritiro",
        description = "Inviato quando il dispositivo può essere ritirato."
    ),
    WHATSAPP_PDF_STATUS(
        settingKey = "anchor_wa_pdf_status",
        defaultCode = "whatsapp_stato_avanzamento_pdf",
        stepNumber = 6,
        stepName = "Documenti",
        label = "WA: Invio PDF Stato",
        description = "Accompagna l'invio del documento PDF dello stato avanzamento."
    ),
    PRIVACY_TEXT(
        settingKey = "anchor_privacy_text",
        defaultCode = "privacy_breve",
        stepNumber = 0,
        stepName = "Legale",
        label = "Legale: Privacy",
        description = "Testo dell'informativa privacy mostrato in fase di accettazione."
    ),
    TERMS_TEXT(
        settingKey = "anchor_terms_text",
        defaultCode = "condizioni_presa_in_carico",
        stepNumber = 0,
        stepName = "Legale",
        label = "Legale: Termini e Condizioni",
        description = "Condizioni di presa in carico mostrate in fase di accettazione."
    );

    companion object {
        fun fromSettingKey(key: String): TemplateAnchor? = entries.find { it.settingKey == key }
    }
}

package com.onlytechtake.repairapp.features.dashboard.common

const val PRIVACY_TEMPLATE_CODE = "privacy_breve"
const val TERMS_TEMPLATE_CODE = "condizioni_presa_in_carico"
const val DEFAULT_PRIVACY_TEXT = "Informativa privacy sintetica GDPR: dati trattati per presa in carico, diagnosi, preventivo, riparazione, consegna, comunicazioni operative e obblighi di legge."
const val DEFAULT_TERMS_TEXT = "Il cliente autorizza presa in carico, custodia e diagnosi tecnica dell'oggetto. Ogni intervento con preventivo richiede conferma."

data class TemplatePreset(
    val code: String,
    val title: String,
    val body: String,
    val step: String? = null
)

val TEMPLATE_PRESETS = listOf(
    TemplatePreset(
        code = PRIVACY_TEMPLATE_CODE,
        title = "Informativa Privacy",
        body = DEFAULT_PRIVACY_TEXT,
        step = "Accettazione"
    ),
    TemplatePreset(
        code = TERMS_TEMPLATE_CODE,
        title = "Termini e Condizioni",
        body = DEFAULT_TERMS_TEXT,
        step = "Accettazione"
    ),
    TemplatePreset(
        code = "whatsapp_accettazione",
        title = "WA: Conferma Accettazione",
        body = "Ciao {{cliente}}, abbiamo preso in carico il tuo dispositivo (Commessa {{numero_commessa}}). Ti aggiorneremo appena terminata la diagnosi.",
        step = "Aperta"
    ),
    TemplatePreset(
        code = "whatsapp_preventivo_intervento",
        title = "WA: Preventivo Pronto",
        body = "Ciao {{cliente}}, per la commessa {{numero_commessa}} è pronto il preventivo. Diagnosi: {{esito_diagnosi}}. Totale stimato: {{importo_preventivo}}.",
        step = "Esito"
    ),
    TemplatePreset(
        code = "whatsapp_pronto_ritiro",
        title = "WA: Pronto per il Ritiro",
        body = "Ottime notizie {{cliente}}! Il tuo dispositivo ({{numero_commessa}}) è pronto per il ritiro. Ti aspettiamo in negozio.",
        step = "Pronta per il ritiro"
    ),
    TemplatePreset(
        code = "whatsapp_non_riparabile",
        title = "WA: Non Riparabile",
        body = "Gentile {{cliente}}, purtroppo il dispositivo relativo alla commessa {{numero_commessa}} non è riparabile. Puoi passare per il ritiro.",
        step = "Esito / Chiusa"
    )
)

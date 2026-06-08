package com.onlytechtake.repairapp.features.dashboard

enum class QuoteDecision {
    ACCEPTED,
    REJECTED,
}

fun toggleQuoteDecision(current: QuoteDecision?, selected: QuoteDecision): QuoteDecision? =
    if (current == selected) null else selected

fun canConfirmQuoteDecision(decision: QuoteDecision?, rejectNote: String): Boolean =
    when (decision) {
        QuoteDecision.ACCEPTED -> true
        QuoteDecision.REJECTED -> rejectNote.isNotBlank()
        null -> false
    }

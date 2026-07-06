package com.onlytechtake.repairapp.core.utils

import java.util.Locale

fun Long.formatEuroCents(): String = String.format(Locale.getDefault(), "%.2f €", this / 100.0)

fun Long?.formatEuroCents(): String = this?.formatEuroCents() ?: "0,00 €"

fun Long.formatEuroCentsForExport(): String = "EUR ${this / 100},${(this % 100).toString().padStart(2, '0')}"

fun Long?.formatEuroCentsForExport(): String = this?.formatEuroCentsForExport().orEmpty()

fun parseEuroCentsOrNull(value: String): Long? {
    val normalized = value.trim().replace(',', '.')
    if (normalized.isBlank()) return null
    val parts = normalized.split('.', limit = 2)
    val euros = parts.getOrNull(0)?.filter(Char::isDigit)?.toLongOrNull() ?: return null
    val cents =
        parts
            .getOrNull(1)
            ?.filter(Char::isDigit)
            ?.take(2)
            ?.padEnd(2, '0')
            ?.toLongOrNull()
            ?: 0L
    return (euros * 100L) + cents
}

package com.onlytechtake.repairapp.core.utils

import java.util.Locale

fun Long.formatEuroCents(): String =
    String.format(Locale.ITALY, "%.2f €", this / 100.0)

fun Long?.formatEuroCents(): String =
    this?.formatEuroCents() ?: "0,00 €"

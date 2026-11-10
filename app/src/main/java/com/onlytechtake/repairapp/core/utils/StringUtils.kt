package com.onlytechtake.repairapp.core.utils

/**
 * Normalizes a string by trimming it and capitalizing the first letter.
 */
fun String.capitalizeFirst(): String = this.trim().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

package com.onlytechtake.repairapp.core.media

fun normalizeCameraTargetRotation(displayRotation: Int?): Int = displayRotation?.takeIf { it in 0..3 } ?: 0

package com.onlytechtake.repairapp.domain.models

data class AppSummary(
    val title: String,
    val operatorCount: Int,
    val customerCount: Int,
    val activeOrderCount: Int,
)

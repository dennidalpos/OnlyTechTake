package com.onlytechtake.repairapp.domain.workspace

import com.onlytechtake.repairapp.data.local.entities.ClosureReason

val discardedCleanupReasons: List<ClosureReason> = listOf(
    ClosureReason.CUSTOMER_DECLINED,
    ClosureReason.CANCELLED,
    ClosureReason.NOT_REPAIRABLE,
)

fun ClosureReason?.isDiscardedForCleanup(): Boolean =
    this in discardedCleanupReasons

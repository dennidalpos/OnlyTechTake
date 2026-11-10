package com.onlytechtake.repairapp.domain.logic

import com.onlytechtake.repairapp.domain.models.ClosureReason

val discardedCleanupReasons: List<ClosureReason> =
    listOf(
        ClosureReason.CUSTOMER_DECLINED,
        ClosureReason.CANCELLED,
        ClosureReason.NOT_REPAIRABLE,
    )

fun ClosureReason?.isDiscardedForCleanup(): Boolean = this in discardedCleanupReasons

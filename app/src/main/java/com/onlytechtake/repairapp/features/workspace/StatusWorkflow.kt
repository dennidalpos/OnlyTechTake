package com.onlytechtake.repairapp.features.workspace

import com.onlytechtake.repairapp.domain.workspace.OrderStatusAction

fun OrderStatusAction.tone(): ButtonTone =
    when (this) {
        OrderStatusAction.CLOSE_ORDER -> ButtonTone.DANGER
        OrderStatusAction.READY_FOR_PICKUP -> ButtonTone.SUCCESS
        OrderStatusAction.RECORD_RESULT -> ButtonTone.INFO
        else -> ButtonTone.PRIMARY
    }

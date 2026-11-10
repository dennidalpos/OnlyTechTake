package com.onlytechtake.repairapp.domain.logic

import com.onlytechtake.repairapp.core.templates.TemplateAnchor
import com.onlytechtake.repairapp.domain.models.DiagnosisOutcome
import com.onlytechtake.repairapp.domain.models.Order
import com.onlytechtake.repairapp.domain.models.OrderStatus
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RepairWorkflowEngine
    @Inject
    constructor() {
        fun getPrimaryActionOverride(
            status: OrderStatus,
            diagnosisOutcome: DiagnosisOutcome?,
            quoteAccepted: Boolean?,
        ): OrderStatusAction? {
            val isResultStep = status == OrderStatus.RESULT
            val isRepairable = diagnosisOutcome == DiagnosisOutcome.REPAIRABLE
            val isNotRepairable = diagnosisOutcome == DiagnosisOutcome.NOT_REPAIRABLE

            return when {
                isResultStep && isRepairable && quoteAccepted == false -> OrderStatusAction.CLOSE_ORDER
                isResultStep && isNotRepairable -> OrderStatusAction.READY_FOR_PICKUP
                else -> null
            }
        }

        fun getWhatsAppTemplateForStatus(order: Order): TemplateAnchor =
            when (order.status) {
                OrderStatus.OPEN -> {
                    TemplateAnchor.WHATSAPP_INTAKE
                }

                OrderStatus.IN_PROGRESS -> {
                    TemplateAnchor.WHATSAPP_UPDATE
                }

                OrderStatus.DIAGNOSIS -> {
                    TemplateAnchor.WHATSAPP_DIAGNOSIS
                }

                OrderStatus.RESULT -> {
                    if (order.diagnosisOutcome == DiagnosisOutcome.REPAIRABLE) {
                        TemplateAnchor.WHATSAPP_QUOTE
                    } else {
                        TemplateAnchor.WHATSAPP_NOT_REPAIRABLE
                    }
                }

                OrderStatus.REPAIRING -> {
                    TemplateAnchor.WHATSAPP_REPAIRING
                }

                OrderStatus.READY_FOR_PICKUP -> {
                    TemplateAnchor.WHATSAPP_READY
                }

                OrderStatus.CLOSED -> {
                    TemplateAnchor.WHATSAPP_COMPLETED
                }
            }
    }

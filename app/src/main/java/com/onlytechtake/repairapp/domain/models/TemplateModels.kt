package com.onlytechtake.repairapp.domain.models

import androidx.compose.runtime.Immutable

@Immutable
data class Template(
    val id: String,
    val code: String,
    val title: String,
    val channel: MessageChannel,
    val body: String,
    val active: Boolean,
    val version: Int,
    val step: String? = null,
)

data class MessageLog(
    val id: String,
    val orderId: String,
    val customerId: String,
    val channel: MessageChannel,
    val mode: MessageMode,
    val body: String,
    val status: MessageStatus,
    val sentAt: Long?,
    val createdAt: Long,
)

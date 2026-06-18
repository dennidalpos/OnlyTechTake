package com.onlytechtake.repairapp.data.repositories

import android.net.Uri
import androidx.core.net.toUri

interface WhatsAppSender {
    suspend fun prepare(message: WhatsAppOutgoingMessage): Result<WhatsAppSendAction>
}

data class WhatsAppOutgoingMessage(
    val phone: String,
    val body: String,
)

sealed interface WhatsAppSendAction {
    data class OpenIntent(
        val uri: Uri,
    ) : WhatsAppSendAction
}

class ManualWhatsAppSender : WhatsAppSender {
    override suspend fun prepare(message: WhatsAppOutgoingMessage): Result<WhatsAppSendAction> =
        runCatching {
            require(message.phone.isNotBlank()) { "Telefono cliente mancante" }
            require(message.body.isNotBlank()) { "Messaggio vuoto" }
            WhatsAppSendAction.OpenIntent(
                "https://wa.me/${message.phone}?text=${Uri.encode(message.body)}".toUri(),
            )
        }
}

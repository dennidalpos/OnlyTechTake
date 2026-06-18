package com.onlytechtake.repairapp.data.repositories

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

class WhatsAppSendersTest {
    @Test
    fun manualSenderRejectsEmptyMessage() =
        runBlocking {
            val result =
                ManualWhatsAppSender().prepare(
                    WhatsAppOutgoingMessage(phone = "+39123456789", body = ""),
                )

            assertTrue(result.isFailure)
            assertTrue(
                result
                    .exceptionOrNull()
                    ?.message
                    .orEmpty()
                    .contains("Messaggio vuoto"),
            )
        }
}

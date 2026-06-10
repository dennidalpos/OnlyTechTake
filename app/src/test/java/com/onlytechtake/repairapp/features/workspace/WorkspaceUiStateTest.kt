package com.onlytechtake.repairapp.features.workspace

import com.onlytechtake.repairapp.domain.workspace.PreparedMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WorkspaceUiStateTest {
    @Test
    fun clearingPreviewRemovesMessageAndEditedBody() {
        val state = WorkspaceUiState(
            preparedMessage = PreparedMessage("order", "customer", "333", "Originale"),
            preparedMessageBody = "Modificato",
        ).withoutPreparedMessage()

        assertNull(state.preparedMessage)
        assertEquals("", state.preparedMessageBody)
    }
}

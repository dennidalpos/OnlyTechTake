package com.onlytechtake.repairapp.features.workspace

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkspaceManager
    @Inject
    constructor() {
        private val _message = MutableStateFlow<WorkspaceMessage?>(null)
        val message = _message.asStateFlow()

        private val _isFormDirty = MutableStateFlow(value = false)
        val isFormDirty = _isFormDirty.asStateFlow()

        private val _resetSignal = MutableStateFlow(value = 0)
        val resetSignal = _resetSignal.asStateFlow()

        private val _autoSaveSignal = MutableStateFlow(value = 0)
        val autoSaveSignal = _autoSaveSignal.asStateFlow()

        fun triggerReset() {
            _resetSignal.value += 1
        }

        fun triggerAutoSave() {
            _autoSaveSignal.value += 1
        }

        fun showMessage(
            text: String,
            isError: Boolean = false,
            autoDismissMillis: Long? = null,
        ) {
            _message.value = WorkspaceMessage(text, isError, autoDismissMillis)
        }

        fun clearMessage() {
            _message.value = null
        }

        fun setFormDirty(dirty: Boolean) {
            _isFormDirty.value = dirty
        }
    }

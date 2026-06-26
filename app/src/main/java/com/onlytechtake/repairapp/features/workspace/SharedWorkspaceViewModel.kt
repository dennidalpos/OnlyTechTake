package com.onlytechtake.repairapp.features.workspace

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class SharedWorkspaceViewModel : ViewModel() {
    private val _message = MutableStateFlow<WorkspaceMessage?>(null)
    val message = _message.asStateFlow()

    private val _isFormDirty = MutableStateFlow(value = false)
    val isFormDirty = _isFormDirty.asStateFlow()

    private val _resetSignal = MutableStateFlow(value = 0)
    val resetSignal = _resetSignal.asStateFlow()

    fun triggerReset() {
        _resetSignal.value += 1
    }

    @Suppress("unused")
    fun showMessage(
        text: String,
        isError: Boolean = false,
    ) {
        _message.value = WorkspaceMessage(text, isError)
    }

    fun clearMessage() {
        _message.value = null
    }

    fun setFormDirty(dirty: Boolean) {
        _isFormDirty.value = dirty
    }

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = SharedWorkspaceViewModel() as T
    }
}

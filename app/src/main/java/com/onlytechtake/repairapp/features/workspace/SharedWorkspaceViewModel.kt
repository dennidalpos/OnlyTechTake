package com.onlytechtake.repairapp.features.workspace

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SharedWorkspaceViewModel
    @Inject
    constructor(
        private val workspaceManager: WorkspaceManager,
    ) : ViewModel() {
        val message = workspaceManager.message
        val isFormDirty = workspaceManager.isFormDirty
        val resetSignal = workspaceManager.resetSignal
        val autoSaveSignal = workspaceManager.autoSaveSignal

        fun triggerReset() {
            workspaceManager.triggerReset()
        }

        fun triggerAutoSave() {
            workspaceManager.triggerAutoSave()
        }

        fun showMessage(
            text: String,
            isError: Boolean = false,
            autoDismissMillis: Long? = null,
        ) {
            workspaceManager.showMessage(text, isError, autoDismissMillis)
        }

        fun clearMessage() {
            workspaceManager.clearMessage()
        }

        fun setFormDirty(dirty: Boolean) {
            workspaceManager.setFormDirty(dirty)
        }
    }

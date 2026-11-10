package com.onlytechtake.repairapp.core.demo

import android.content.Context
import android.util.Log
import com.onlytechtake.repairapp.R
import com.onlytechtake.repairapp.demo.DataSeeder
import com.onlytechtake.repairapp.domain.auth.AuthRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShowcaseManager
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val dataSeeder: DataSeeder,
        private val authRepository: AuthRepository,
    ) {
        private val _isProcessing = MutableStateFlow(value = false)
        val isProcessing = _isProcessing.asStateFlow()

        private val _error = MutableStateFlow<String?>(null)
        val error = _error.asStateFlow()

        suspend fun prepareShowcase(onDone: () -> Unit) {
            if (_isProcessing.value) return
            executeProcessing {
                dataSeeder.seed(context)
                onDone()
            }
        }

        suspend fun loginAdmin() {
            executeProcessing {
                val password = dataSeeder.defaultPassword ?: return@executeProcessing
                authRepository.login("admin", password.toCharArray())
            }
        }

        suspend fun loginDemo(isConfigured: Boolean) {
            executeProcessing {
                if (!isConfigured) {
                    runCatching { dataSeeder.seed(context) }
                        .onFailure { Log.e("ShowcaseManager", "Demo seed failed", it) }
                }
                authRepository.loginDemo()
            }
        }

        private suspend fun executeProcessing(block: suspend () -> Unit) {
            _isProcessing.value = true
            _error.value = null
            try {
                block()
            } catch (e: Exception) {
                Log.e("ShowcaseManager", "Processing failed", e)
                _error.value = context.getString(R.string.error_init_failed, e.message)
            } finally {
                _isProcessing.value = false
            }
        }
    }

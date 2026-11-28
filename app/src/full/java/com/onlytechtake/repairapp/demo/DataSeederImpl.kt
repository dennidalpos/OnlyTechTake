package com.onlytechtake.repairapp.demo

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * No-op implementation for Full flavor.
 * Aligned with IO offloading pattern of other flavors.
 */
class DataSeederImpl : DataSeeder {
    override suspend fun seed(context: Context) =
        withContext(Dispatchers.IO) {
            // No-op for production
        }
}

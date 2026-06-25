package com.onlytechtake.repairapp.demo

import android.content.Context

/**
 * No-op implementation for Full flavor.
 */
class DataSeederImpl : DataSeeder {
    override suspend fun seed(context: Context) {}
}

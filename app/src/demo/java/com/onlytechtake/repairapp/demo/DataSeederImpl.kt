package com.onlytechtake.repairapp.demo

import android.content.Context

/**
 * No-op implementation for Demo flavor.
 */
class DataSeederImpl : DataSeeder {
    override suspend fun seed(context: Context) {}
}

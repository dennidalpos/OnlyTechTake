package com.onlytechtake.repairapp.demo

import android.content.Context

/**
 * A no-op implementation of [DataSeeder] used when no seeding is required (e.g., Full and Demo flavors).
 */
class NoOpDataSeeder : DataSeeder {
    override suspend fun seed(context: Context) {
        // No-op
    }
}

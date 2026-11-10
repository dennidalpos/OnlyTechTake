package com.onlytechtake.repairapp.demo

import android.content.Context

/**
 * Interface for seeding initial or demo data into the application.
 * Implementations are flavor-specific.
 */
interface DataSeeder {
    /**
     * Seeds the application data. This is typically called once during the first run.
     */
    suspend fun seed(context: Context)

    /**
     * Optional default password for seeded accounts.
     */
    @Suppress("SameReturnValue")
    val defaultPassword: String? get() = null
}

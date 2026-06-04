package com.onlytechtake.repairapp.data.local.db

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RepairAppDatabaseMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        RepairAppDatabase::class.java,
    )

    @Test
    fun currentSchemaValidatesWithDeclaredMigrations() {
        helper.createDatabase(TEST_DATABASE, RepairAppDatabase.DATABASE_VERSION).close()

        helper.runMigrationsAndValidate(
            TEST_DATABASE,
            RepairAppDatabase.DATABASE_VERSION,
            true,
            *RepairAppDatabase.MIGRATIONS,
        ).close()
    }

    private companion object {
        const val TEST_DATABASE = "migration-test"
    }
}

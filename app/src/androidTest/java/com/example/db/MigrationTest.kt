package com.example.db

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MigrationTest {
    private val TEST_DB = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java.canonicalName,
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun migrateAll() {
        // Placeholder test for DB migration
        // Create earliest version of the database.
        // helper.createDatabase(TEST_DB, 1).apply {
        //     close()
        // }
        // Open latest version of the database. Room will validate the schema.
        // val db = helper.runMigrationsAndValidate(TEST_DB, 2, true)
    }
}

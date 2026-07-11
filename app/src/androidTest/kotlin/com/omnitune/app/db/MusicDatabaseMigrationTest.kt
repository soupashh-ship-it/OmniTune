package com.omnitune.app.db

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MusicDatabaseMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        InternalDatabase::class.java,
    )

    @Test
    fun everySupportedVersionMigratesToCurrentSchema() {
        for (startVersion in 1 until 7) {
            val name = "migration-$startVersion"
            helper.createDatabase(name, startVersion).close()
            helper.runMigrationsAndValidate(
                name,
                7,
                true,
                *InternalDatabase.ALL_MIGRATIONS,
            ).close()
        }
    }
}

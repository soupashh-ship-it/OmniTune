package com.omnitune.app.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith

/** Exercises SchemaTools against physical SQLite files rather than mocks. */
@RunWith(AndroidJUnit4::class)
class SchemaToolsRepairInstrumentedTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val databaseNames = mutableListOf<String>()

    @After
    fun cleanUp() {
        databaseNames.forEach { name ->
            context.deleteDatabase(name)
            File(context.getDatabasePath(name).parentFile, "$name.repair-backups").deleteRecursively()
        }
    }

    @Test
    fun restoresMissingTableAndIndexFromCurrentSchema() {
        val name = newName("missing-table-index")
        val databaseFile = createCurrentDatabase(name)
        withRawDatabase(databaseFile) { db ->
            db.execSQL("DROP TABLE lyrics")
            db.execSQL("DROP INDEX IF EXISTS index_song_liked")
        }

        SchemaTools.repairDatabaseFile(context, name)

        withRawDatabase(databaseFile) { db ->
            assertTrue(tableExists(db, "lyrics"))
            assertTrue(indexExists(db, "index_song_liked"))
        }
        assertTrue(hasSafetyCopy(databaseFile))
    }

    @Test
    fun replacesPartialTableSchemaWithoutDroppingExistingRows() {
        val name = newName("partial-schema")
        val databaseFile = createCurrentDatabase(name)
        withRawDatabase(databaseFile) { db ->
            db.execSQL("""
                INSERT INTO song (
                    id, title, duration, thumbnailUrl, albumId, albumName, explicit, year,
                    date, dateModified, liked, likedDate, totalPlayTime, inLibrary, dateDownload, isLocal, download_state
                ) VALUES (
                    'repair_song', 'Preserved track', 180, NULL, NULL, NULL, 0, NULL,
                    NULL, NULL, 0, NULL, 0, NULL, NULL, 0, 0
                )
            """.trimIndent())
            db.execSQL("ALTER TABLE song ADD COLUMN interruptedUpgradeMarker TEXT")
        }

        SchemaTools.repairDatabaseFile(context, name)

        withRawDatabase(databaseFile) { db ->
            assertTrue(rowExists(db, "SELECT 1 FROM song WHERE id = 'repair_song' AND title = 'Preserved track'"))
            assertFalse(columnExists(db, "song", "interruptedUpgradeMarker"))
        }
        assertTrue(hasSafetyCopy(databaseFile))
    }

    @Test
    fun completesInterruptedOrUnsupportedUpgradeAtTheCurrentVersion() {
        val name = newName("unsupported-upgrade")
        val databaseFile = createCurrentDatabase(name)
        withRawDatabase(databaseFile) { db ->
            db.execSQL("ALTER TABLE queue ADD COLUMN interruptedUpgradeMarker TEXT")
            db.execSQL("PRAGMA user_version = 999")
        }

        SchemaTools.repairDatabaseFile(context, name)

        withRawDatabase(databaseFile) { db ->
            assertFalse(columnExists(db, "queue", "interruptedUpgradeMarker"))
            assertEquals(CURRENT_ROOM_DATABASE_SCHEMA_VERSION.toLong(), pragmaLong(db, "user_version"))
        }
        assertTrue(hasSafetyCopy(databaseFile))
    }

    @Test
    fun invalidForeignKeysFailClosedAfterRetainingSafetyCopy() {
        val name = newName("invalid-foreign-key")
        val databaseFile = createCurrentDatabase(name)
        withRawDatabase(databaseFile) { db ->
            db.execSQL("PRAGMA foreign_keys=OFF")
            db.execSQL("INSERT INTO related_song_map (id, songId, relatedSongId) VALUES (1, 'missing', 'also-missing')")
        }

        try {
            SchemaTools.repairDatabaseFile(context, name)
            fail("Expected foreign-key validation to reject the repaired database")
        } catch (_: IllegalStateException) {
            // Expected: the source file remains recoverable from the retained safety copy.
        }
        assertTrue(hasSafetyCopy(databaseFile))
    }

    @Test
    fun corruptFileRecoversOnlyAfterRetainingSafetyCopy() {
        val name = newName("corrupt-file")
        val databaseFile = context.getDatabasePath(name)
        databaseFile.parentFile?.mkdirs()
        databaseFile.writeBytes(byteArrayOf(0x00, 0x01, 0x02, 0x03))

        SchemaTools.repairDatabaseFile(context, name)

        withRawDatabase(databaseFile) { db ->
            assertTrue(tableExists(db, "song"))
            assertEquals(CURRENT_ROOM_DATABASE_SCHEMA_VERSION.toLong(), pragmaLong(db, "user_version"))
        }
        assertTrue(hasSafetyCopy(databaseFile))
    }

    private fun newName(prefix: String): String = "$prefix-${System.nanoTime()}.db".also(databaseNames::add)

    private fun createCurrentDatabase(name: String): File {
        context.deleteDatabase(name)
        Room.databaseBuilder(context, InternalDatabase::class.java, name)
            .addMigrations(*InternalDatabase.ALL_MIGRATIONS)
            .build()
            .also { database ->
                database.openHelper.writableDatabase
                database.close()
            }
        return context.getDatabasePath(name)
    }

    private fun hasSafetyCopy(databaseFile: File): Boolean =
        File(databaseFile.parentFile, "${databaseFile.name}.repair-backups")
            .listFiles()
            ?.any { it.name.endsWith(".bak") }
            ?: false

    private fun withRawDatabase(file: File, block: (SQLiteDatabase) -> Unit) {
        val database = SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READWRITE)
        try {
            block(database)
        } finally {
            database.close()
        }
    }

    private fun tableExists(database: SQLiteDatabase, name: String): Boolean =
        rowExists(database, "SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = '$name'")

    private fun indexExists(database: SQLiteDatabase, name: String): Boolean =
        rowExists(database, "SELECT 1 FROM sqlite_master WHERE type = 'index' AND name = '$name'")

    private fun columnExists(database: SQLiteDatabase, table: String, column: String): Boolean {
        database.rawQuery("PRAGMA table_info(`$table`)", null).use { cursor ->
            val nameIndex = cursor.getColumnIndexOrThrow("name")
            return generateSequence { if (cursor.moveToNext()) cursor.getString(nameIndex) else null }
                .any { it == column }
        }
    }

    private fun rowExists(database: SQLiteDatabase, query: String): Boolean =
        database.rawQuery(query, null).use { cursor -> cursor.moveToFirst() }

    private fun pragmaLong(database: SQLiteDatabase, pragma: String): Long =
        database.rawQuery("PRAGMA $pragma", null).use { cursor ->
            check(cursor.moveToFirst()) { "PRAGMA $pragma returned no value" }
            cursor.getLong(0)
        }
}

/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */



package com.omnitune.app.db

import android.annotation.SuppressLint
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import androidx.core.content.contentValuesOf
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.DeleteColumn
import androidx.room.DeleteTable
import androidx.room.RenameColumn
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.withTransaction
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import com.omnitune.app.db.entities.AlbumArtistMap
import com.omnitune.app.db.entities.AlbumEntity
import com.omnitune.app.db.entities.ArtistEntity
import com.omnitune.app.db.entities.Event
import com.omnitune.app.db.entities.FormatEntity
import com.omnitune.app.db.entities.LyricsEntity
import com.omnitune.app.db.entities.PlaylistEntity
import com.omnitune.app.db.entities.PlayCountEntity
import com.omnitune.app.db.entities.PlaylistSongMap
import com.omnitune.app.db.entities.PlaylistSongMapPreview
import com.omnitune.app.db.entities.RelatedSongMap
import com.omnitune.app.db.entities.SearchHistory
import com.omnitune.app.db.entities.SetVideoIdEntity
import com.omnitune.app.db.entities.SongAlbumMap
import com.omnitune.app.db.entities.SongArtistMap
import com.omnitune.app.db.entities.SongEntity
import com.omnitune.app.db.entities.SortedSongAlbumMap
import com.omnitune.app.db.entities.SortedSongArtistMap
import com.omnitune.app.db.entities.TagEntity
import com.omnitune.app.db.entities.PlaylistTagMap
import com.omnitune.app.extensions.toSQLiteQuery
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.Date
import java.util.concurrent.Executor
import kotlin.coroutines.resume

private const val TAG = "MusicDatabase"
private const val CURRENT_VERSION = 7

internal fun isRecoverableDatabaseSchemaFailure(error: Throwable): Boolean {
    val message = generateSequence(error) { it.cause }
        .mapNotNull { it.message?.lowercase() }
        .joinToString(" ")
    return listOf(
        "migration didn't properly handle",
        "room cannot verify the data integrity",
        "pre-packaged database has an invalid schema",
        "duplicate column name",
    ).any(message::contains)
}

class MusicDatabase(
    private val delegate: InternalDatabase,
) : DatabaseDao by delegate.dao {
    val openHelper: SupportSQLiteOpenHelper
        get() = delegate.openHelper

    fun query(block: MusicDatabase.() -> Unit) =
        with(delegate) {
            queryExecutor.execute {
                block(this@MusicDatabase)
            }
        }

    fun transaction(block: MusicDatabase.() -> Unit) =
        with(delegate) {
            transactionExecutor.execute {
                runInTransaction {
                    block(this@MusicDatabase)
                }
            }
        }

    suspend fun <R> withTransaction(block: suspend MusicDatabase.() -> R): R =
        delegate.withTransaction {
            block(this@MusicDatabase)
        }

    suspend fun awaitIdle(timeoutMs: Long = 5_000L) {
        withTimeout(timeoutMs) {
            awaitExecutor(delegate.queryExecutor)
            awaitExecutor(delegate.transactionExecutor)
        }
    }

    fun close() = delegate.close()

    private suspend fun awaitExecutor(executor: Executor) {
        suspendCancellableCoroutine { cont ->
            executor.execute {
                if (cont.isActive) cont.resume(Unit)
            }
        }
    }
}

@Database(
    entities = [
        SongEntity::class,
        ArtistEntity::class,
        AlbumEntity::class,
        PlaylistEntity::class,
        SongArtistMap::class,
        SongAlbumMap::class,
        AlbumArtistMap::class,
        PlaylistSongMap::class,
        SearchHistory::class,
        FormatEntity::class,
        LyricsEntity::class,
        Event::class,
        RelatedSongMap::class,
        SetVideoIdEntity::class,
        PlayCountEntity::class,
        TagEntity::class,
        PlaylistTagMap::class,
        com.omnitune.app.db.entities.SongSkipEntity::class,
        com.omnitune.app.db.entities.QueueEntity::class
    ],
    views = [
        SortedSongArtistMap::class,
        SortedSongAlbumMap::class,
        PlaylistSongMapPreview::class,
    ],
    version = CURRENT_VERSION,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class InternalDatabase : RoomDatabase() {
    abstract val dao: DatabaseDao

    companion object {
        const val DB_NAME = "song.db"

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Empty migration: The schema for version 1 in OmniTune was already the 
                // modern schema. We just bump the version to 2 to enable proper 
                // exportSchema tracking moving forward.
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE Song ADD COLUMN download_state INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `queue` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT, `mediaIdList` TEXT NOT NULL, `startIndex` INTEGER NOT NULL, `position` INTEGER NOT NULL)"
                )
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_song_inLibrary` ON `song` (`inLibrary`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_song_isLocal` ON `song` (`isLocal`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_song_liked` ON `song` (`liked`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_album_inLibrary` ON `album` (`inLibrary`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_artist_bookmarkedAt` ON `artist` (`bookmarkedAt`)")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE `queue` ADD COLUMN `playbackSourceType` TEXT")
                database.execSQL("ALTER TABLE `queue` ADD COLUMN `playbackSourceId` TEXT")
                database.execSQL("ALTER TABLE `queue` ADD COLUMN `playbackSourceTitle` TEXT")
                database.execSQL("ALTER TABLE `queue` ADD COLUMN `playbackSeedSongId` TEXT")
                database.execSQL("ALTER TABLE `queue` ADD COLUMN `playbackGenre` TEXT")
                database.execSQL("ALTER TABLE `queue` ADD COLUMN `playbackMood` TEXT")
                database.execSQL("ALTER TABLE `queue` ADD COLUMN `playbackArtist` TEXT")
                database.execSQL("ALTER TABLE `queue` ADD COLUMN `playbackAllowAutoplay` INTEGER NOT NULL DEFAULT 1")
                database.execSQL("ALTER TABLE `queue` ADD COLUMN `playbackShuffledCollection` INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                val hasDownloadedColumn = database.query("PRAGMA table_info(`playlist`)").use { cursor ->
                    val nameIndex = cursor.getColumnIndex("name")
                    generateSequence { if (cursor.moveToNext()) cursor.getString(nameIndex) else null }
                        .any { it == "isDownloaded" }
                }
                if (!hasDownloadedColumn) {
                    database.execSQL("ALTER TABLE `playlist` ADD COLUMN `isDownloaded` INTEGER NOT NULL DEFAULT 0")
                }
            }
        }

        fun newInstance(context: Context): MusicDatabase {
            fun build() = Room.databaseBuilder(context, InternalDatabase::class.java, DB_NAME)
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                .addCallback(DatabaseCallback())
                .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
                .setTransactionExecutor(java.util.concurrent.Executors.newFixedThreadPool(4))
                .setQueryExecutor(java.util.concurrent.Executors.newFixedThreadPool(4))
                .build()

            var db = build()
            try {
                db.openHelper.writableDatabase
            } catch (error: Exception) {
                if (!isRecoverableDatabaseSchemaFailure(error)) throw error

                Log.e(TAG, "Database schema upgrade failed; attempting non-destructive repair", error)
                runCatching { db.close() }
                SchemaTools.repairDatabaseFile(context, DB_NAME)
                db = build()
                db.openHelper.writableDatabase
            }

            return MusicDatabase(delegate = db)
        }
    }
}

private class DatabaseCallback : RoomDatabase.Callback() {
    override fun onOpen(db: SupportSQLiteDatabase) {
        super.onOpen(db)
        java.util.concurrent.Executors.newSingleThreadExecutor().execute {
            try {
                db.query("PRAGMA busy_timeout = 60000").close()
                db.query("PRAGMA cache_size = -16000").close()
                db.query("PRAGMA wal_autocheckpoint = 1000").close()
                db.query("PRAGMA synchronous = NORMAL").close()
                db.query("PRAGMA temp_store = MEMORY").close()
                db.query("PRAGMA mmap_size = 268435456").close()
                
                cleanupDuplicatePlaylistsOnOpen(db)
                ensurePlaylistBrowseIdIndex(db)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set PRAGMA settings", e)
            }
        }
    }
    
    private fun cleanupDuplicatePlaylistsOnOpen(db: SupportSQLiteDatabase) {
        try {
            db.execSQL("""
                DELETE FROM playlist_song_map WHERE playlistId IN (
                    SELECT p1.id FROM playlist p1
                    WHERE p1.browseId IS NOT NULL
                    AND EXISTS (
                        SELECT 1 FROM playlist p2 
                        WHERE p2.browseId = p1.browseId 
                        AND p2.id != p1.id
                        AND (
                            (SELECT COUNT(*) FROM playlist_song_map WHERE playlistId = p2.id) >
                            (SELECT COUNT(*) FROM playlist_song_map WHERE playlistId = p1.id)
                            OR (
                                (SELECT COUNT(*) FROM playlist_song_map WHERE playlistId = p2.id) =
                                (SELECT COUNT(*) FROM playlist_song_map WHERE playlistId = p1.id)
                                AND p2.rowid < p1.rowid
                            )
                        )
                    )
                )
            """)
            
            db.execSQL("""
                DELETE FROM playlist WHERE id IN (
                    SELECT p1.id FROM playlist p1
                    WHERE p1.browseId IS NOT NULL
                    AND EXISTS (
                        SELECT 1 FROM playlist p2 
                        WHERE p2.browseId = p1.browseId 
                        AND p2.id != p1.id
                        AND (
                            (SELECT COUNT(*) FROM playlist_song_map WHERE playlistId = p2.id) >
                            (SELECT COUNT(*) FROM playlist_song_map WHERE playlistId = p1.id)
                            OR (
                                (SELECT COUNT(*) FROM playlist_song_map WHERE playlistId = p2.id) =
                                (SELECT COUNT(*) FROM playlist_song_map WHERE playlistId = p1.id)
                                AND p2.rowid < p1.rowid
                            )
                        )
                    )
                )
            """)
        } catch (e: Exception) {
            Log.w(TAG, "Duplicate playlist cleanup skipped", e)
        }
    }
    
    private fun ensurePlaylistBrowseIdIndex(db: SupportSQLiteDatabase) {
        try {
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_playlist_browseId ON playlist (browseId) WHERE browseId IS NOT NULL")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to create browseId index", e)
        }
    }
}

// =============================================================================
// UNIVERSAL MIGRATION - Handles schema upgrade to current version
// =============================================================================

/**
 * Universal migration that properly handles schema changes for any source version.
 * Recreates tables with correct schema when needed to fix default value issues.
 */
private class UniversalMigration(
    private val context: Context,
    startVersion: Int,
    endVersion: Int,
) : Migration(startVersion, endVersion) {

    override fun migrate(db: SupportSQLiteDatabase) {
        val from = startVersion
        val to = endVersion
        Log.i(TAG, "Running universal migration from $from to $to")

        val expectedDb = Room.inMemoryDatabaseBuilder(context, InternalDatabase::class.java).build()
        try {
            val expected = expectedDb.openHelper.writableDatabase
            SchemaTools.reconcileDatabase(db = db, expectedDb = expected)
            Log.i(TAG, "Migration completed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Migration failed", e)
            throw e
        } finally {
            expectedDb.close()
        }
    }
}

private object SchemaTools {
    private val IGNORED_TABLES = setOf("android_metadata", "room_master_table", "sqlite_sequence")

    fun repairDatabaseFile(
        context: Context,
        name: String,
    ) {
        val expectedDb = Room.inMemoryDatabaseBuilder(context, InternalDatabase::class.java).build()
        val fileHelper =
            FrameworkSQLiteOpenHelperFactory()
                .create(
                    SupportSQLiteOpenHelper.Configuration
                        .builder(context)
                        .name(name)
                        .callback(
                            object : SupportSQLiteOpenHelper.Callback(CURRENT_VERSION) {
                                override fun onCreate(db: SupportSQLiteDatabase) = Unit

                                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

                                override fun onDowngrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                            },
                        ).build(),
                )

        try {
            val expected = expectedDb.openHelper.writableDatabase
            val identityHash = readIdentityHash(expected)
            val db = fileHelper.writableDatabase

            reconcileDatabase(db = db, expectedDb = expected)
            if (identityHash != null) {
                updateIdentityHash(db = db, identityHash = identityHash)
            }
        } finally {
            runCatching { fileHelper.close() }
            runCatching { expectedDb.close() }
        }
    }

    fun reconcileDatabase(
        db: SupportSQLiteDatabase,
        expectedDb: SupportSQLiteDatabase,
    ) {
        val expectedMaster = readMasterEntries(expectedDb)
        val expectedTables = expectedMaster.filter { it.type == "table" && it.name !in IGNORED_TABLES }
        val expectedIndices =
            expectedMaster.filter { it.type == "index" && it.sql != null && it.tblName !in IGNORED_TABLES }
        val expectedViews = expectedMaster.filter { it.type == "view" && it.sql != null }
        val expectedTriggers = expectedMaster.filter { it.type == "trigger" && it.sql != null }

        db.execSQL("PRAGMA foreign_keys=OFF")
        dropNonTableObjects(db)

        expectedTables.forEach { table ->
            ensureTableSchema(db = db, expectedDb = expectedDb, table = table, expectedIndices = expectedIndices)
        }

        expectedViews.forEach { db.execSQL(it.sql!!) }
        expectedTriggers.forEach { db.execSQL(it.sql!!) }

        db.execSQL("PRAGMA foreign_keys=ON")
    }

    private fun readIdentityHash(db: SupportSQLiteDatabase): String? =
        runCatching {
            db.query("SELECT identity_hash FROM room_master_table WHERE id = 42").use { cursor ->
                if (!cursor.moveToFirst()) return null
                cursor.getString(0)
            }
        }.getOrNull()

    private fun updateIdentityHash(
        db: SupportSQLiteDatabase,
        identityHash: String,
    ) {
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
        db.execSQL(
            "INSERT OR REPLACE INTO room_master_table (id, identity_hash) VALUES (42, ?)",
            arrayOf(identityHash),
        )
    }

    private fun ensureTableSchema(
        db: SupportSQLiteDatabase,
        expectedDb: SupportSQLiteDatabase,
        table: MasterEntry,
        expectedIndices: List<MasterEntry>,
    ) {
        val expectedColumns = readColumns(expectedDb, table.name)
        if (expectedColumns.isEmpty()) return

        val existing = tableExists(db, table.name)
        if (!existing) {
            db.execSQL(table.sql!!)
            expectedIndices.filter { it.tblName == table.name }.forEach { db.execSQL(it.sql!!) }
            return
        }

        val actualColumns = readColumns(db, table.name)
        if (!schemaMismatch(expectedColumns, actualColumns)) {
            expectedIndices.filter { it.tblName == table.name }.forEach { db.execSQL(it.sql!!) }
            return
        }

        val oldTable = "_old_${table.name}"
        db.execSQL("ALTER TABLE `${table.name}` RENAME TO `$oldTable`")
        db.execSQL(table.sql!!)

        val expectedOrdered = expectedColumns.values.sortedBy { it.cid }
        val insertColumns = expectedOrdered.joinToString(",") { "`${it.name}`" }
        val selectExpr =
            expectedOrdered.joinToString(",") { col ->
                val old = actualColumns[col.name]
                when {
                    old != null -> "`${col.name}`"
                    col.defaultValue != null -> col.defaultValue
                    col.notNull -> defaultLiteral(col.type)
                    else -> "NULL"
                }
            }

        db.execSQL("INSERT INTO `${table.name}` ($insertColumns) SELECT $selectExpr FROM `$oldTable`")
        db.execSQL("DROP TABLE `$oldTable`")
        expectedIndices.filter { it.tblName == table.name }.forEach { db.execSQL(it.sql!!) }

        if (table.sql.orEmpty().uppercase().contains("AUTOINCREMENT")) {
            val idColumn = expectedColumns.values.firstOrNull { it.name.equals("id", ignoreCase = true) }?.name ?: "id"
            runCatching {
                db.execSQL("DELETE FROM sqlite_sequence WHERE name = ?", arrayOf(table.name))
                db.execSQL(
                    "INSERT INTO sqlite_sequence(name, seq) SELECT ?, IFNULL(MAX(`$idColumn`), 0) FROM `${table.name}`",
                    arrayOf(table.name),
                )
            }
        }
    }

    private fun defaultLiteral(type: String?): String {
        val t = normalizeType(type)
        return when {
            t.contains("INT") -> "0"
            t.contains("CHAR") || t.contains("CLOB") || t.contains("TEXT") -> "''"
            t.contains("BLOB") -> "X''"
            t.contains("REAL") || t.contains("FLOA") || t.contains("DOUB") -> "0.0"
            else -> "NULL"
        }
    }

    private fun dropNonTableObjects(db: SupportSQLiteDatabase) {
        db.query("SELECT type, name FROM sqlite_master WHERE sql IS NOT NULL").use { cursor ->
            val typeIdx = cursor.getColumnIndex("type")
            val nameIdx = cursor.getColumnIndex("name")
            while (cursor.moveToNext()) {
                val type = cursor.getString(typeIdx)
                val name = cursor.getString(nameIdx)
                if (type == "view") db.execSQL("DROP VIEW IF EXISTS `$name`")
                if (type == "trigger") db.execSQL("DROP TRIGGER IF EXISTS `$name`")
                if (type == "index") db.execSQL("DROP INDEX IF EXISTS `$name`")
            }
        }
    }

    private fun schemaMismatch(
        expected: Map<String, ColumnInfo>,
        actual: Map<String, ColumnInfo>,
    ): Boolean {
        if (expected.keys != actual.keys) return true
        expected.forEach { (name, e) ->
            val a = actual[name] ?: return true
            if (normalizeType(e.type) != normalizeType(a.type)) return true
            if (e.notNull != a.notNull) return true
            val ed = e.defaultValue?.trim()
            val ad = a.defaultValue?.trim()
            if (ed != ad) return true
        }
        return false
    }

    private fun normalizeType(type: String?): String =
        (type ?: "").trim().uppercase().substringBefore(' ')

    private fun tableExists(db: SupportSQLiteDatabase, name: String): Boolean =
        db.query("SELECT 1 FROM sqlite_master WHERE type='table' AND name=?", arrayOf(name)).use { it.moveToFirst() }

    private fun readColumns(db: SupportSQLiteDatabase, table: String): Map<String, ColumnInfo> {
        val cols = linkedMapOf<String, ColumnInfo>()
        db.query("PRAGMA table_info(`$table`)").use { cursor ->
            val cidIdx = cursor.getColumnIndex("cid")
            val nameIdx = cursor.getColumnIndex("name")
            val typeIdx = cursor.getColumnIndex("type")
            val notNullIdx = cursor.getColumnIndex("notnull")
            val defaultIdx = cursor.getColumnIndex("dflt_value")
            while (cursor.moveToNext()) {
                val name = cursor.getString(nameIdx)
                cols[name] =
                    ColumnInfo(
                        cid = cursor.getInt(cidIdx),
                        name = name,
                        type = cursor.getString(typeIdx),
                        notNull = cursor.getInt(notNullIdx) == 1,
                        defaultValue = if (cursor.isNull(defaultIdx)) null else cursor.getString(defaultIdx),
                    )
            }
        }
        return cols
    }

    private fun readMasterEntries(db: SupportSQLiteDatabase): List<MasterEntry> {
        val items = mutableListOf<MasterEntry>()
        db.query("SELECT type, name, tbl_name, sql FROM sqlite_master WHERE sql IS NOT NULL").use { cursor ->
            val typeIdx = cursor.getColumnIndex("type")
            val nameIdx = cursor.getColumnIndex("name")
            val tblIdx = cursor.getColumnIndex("tbl_name")
            val sqlIdx = cursor.getColumnIndex("sql")
            while (cursor.moveToNext()) {
                items.add(
                    MasterEntry(
                        type = cursor.getString(typeIdx),
                        name = cursor.getString(nameIdx),
                        tblName = cursor.getString(tblIdx),
                        sql = cursor.getString(sqlIdx),
                    ),
                )
            }
        }
        return items
    }

    private data class MasterEntry(
        val type: String,
        val name: String,
        val tblName: String,
        val sql: String?,
    )

    private data class ColumnInfo(
        val cid: Int,
        val name: String,
        val type: String?,
        val notNull: Boolean,
        val defaultValue: String?,
    )
}


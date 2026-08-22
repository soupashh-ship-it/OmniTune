/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.backup

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.omnitune.app.db.InternalDatabase
import com.omnitune.app.db.MusicDatabase
import com.omnitune.app.db.entities.SongEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayInputStream
import java.io.File
import java.time.LocalDateTime

/**
 * These operate solely on an in-memory Room database and the Android test
 * application's files directory. They must never be pointed at user data.
 */
@RunWith(AndroidJUnit4::class)
class OmniBackupRepositoryInstrumentedTest {
    private val json = Json { encodeDefaults = true }

    @Test
    fun mergeKeepsExistingLibraryAndAddsIncomingRecords() = runBlocking {
        withRepository { database, repository, _ ->
            database.insert(SongEntity(id = "existing", title = "Existing", liked = true))

            repository.importBackup(
                ByteArrayInputStream(json.encodeToString(snapshot("incoming")).encodeToByteArray()),
                OmniRestoreMode.MERGE,
            )

            assertNotNull(database.song("existing").first())
            assertNotNull(database.song("incoming").first())
        }
    }

    @Test
    fun replaceCreatesSafetyArchiveAndRemovesOldRestorableRecords() = runBlocking {
        withRepository { database, repository, context ->
            database.insert(
                SongEntity(
                    id = "existing",
                    title = "Existing",
                    inLibrary = LocalDateTime.now(),
                ),
            )

            val result = repository.importBackup(
                ByteArrayInputStream(json.encodeToString(snapshot("incoming")).encodeToByteArray()),
                OmniRestoreMode.REPLACE,
            )

            assertNull(database.song("existing").first())
            assertNotNull(database.song("incoming").first())
            assertNotNull(result.safetyBackup)
            assertTrue(requireNotNull(result.safetyBackup).file.isFile)
            assertEquals(1, result.counts.songs)
            File(context.filesDir, "restore_safety_backups").deleteRecursively()
        }
    }

    private suspend fun withRepository(
        block: suspend (MusicDatabase, OmniBackupRepository, Context) -> Unit,
    ) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val internal = Room.inMemoryDatabaseBuilder(context, InternalDatabase::class.java).build()
        val database = MusicDatabase(internal)
        try {
            block(database, OmniBackupRepository(database, context), context)
        } finally {
            internal.close()
            File(context.filesDir, "restore_safety_backups").deleteRecursively()
        }
    }

    private fun snapshot(songId: String) = OmniBackupSnapshot(
        createdAtEpochMillis = 1_725_000_000_000L,
        library = BackupLibrarySection(
            exportedSongCount = 1,
            exportedLikedSongCount = 1,
        ),
        songs = listOf(BackupSong(id = songId, title = "Incoming", liked = true)),
    )
}

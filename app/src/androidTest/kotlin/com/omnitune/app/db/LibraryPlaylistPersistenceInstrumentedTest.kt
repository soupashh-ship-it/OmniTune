package com.omnitune.app.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.omnitune.app.db.entities.PlaylistEntity
import com.omnitune.app.db.entities.SongEntity
import com.omnitune.app.db.entities.TagEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LibraryPlaylistPersistenceInstrumentedTest {

    @Test
    fun playlistLifecyclePreservesSongsPreventsDuplicatesAndRemovesFolderLinks() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val internal = Room.inMemoryDatabaseBuilder(context, InternalDatabase::class.java).build()
        val database = MusicDatabase(internal)
        val playlist = PlaylistEntity(id = "fixture-playlist", name = "Fixture playlist", isLocal = true)
        val tag = TagEntity(id = "fixture-folder", name = "Fixture folder")
        val songs = listOf(
            SongEntity(id = "fixture-song-1", title = "One", liked = true),
            SongEntity(id = "fixture-song-2", title = "Two"),
            SongEntity(id = "fixture-song-3", title = "Three"),
        )

        try {
            songs.forEach(database::insert)
            database.insert(playlist)
            val savedPlaylist = requireNotNull(database.getPlaylistByIdBlocking(playlist.id))
            database.addSongToPlaylist(savedPlaylist, songs.map(SongEntity::id))
            database.addSongToPlaylist(savedPlaylist, listOf(songs.first().id))
            database.insert(tag)
            database.addTagToPlaylist(playlist.id, tag.id)

            assertEquals(songs.map(SongEntity::id), database.playlistSongs(playlist.id).first().map { it.song.id })
            assertEquals(listOf(playlist.id), database.playlistIdsByTags(listOf(tag.id)).first())

            database.move(playlist.id, fromPosition = 0, toPosition = 2)
            assertEquals(
                listOf("fixture-song-2", "fixture-song-3", "fixture-song-1"),
                database.playlistSongs(playlist.id).first().map { it.song.id },
            )

            database.removeSongFromPlaylist(playlist.id, "fixture-song-2")
            assertEquals(
                listOf("fixture-song-3", "fixture-song-1"),
                database.playlistSongs(playlist.id).first().map { it.song.id },
            )

            database.update(playlist.copy(name = "Renamed fixture playlist"))
            assertEquals("Renamed fixture playlist", database.getPlaylistById(playlist.id)?.playlist?.name)

            database.delete(playlist.copy(name = "Renamed fixture playlist"))
            assertNull(database.getPlaylistById(playlist.id))
            assertTrue(database.playlistSongs(playlist.id).first().isEmpty())
            assertTrue(database.playlistIdsByTags(listOf(tag.id)).first().isEmpty())
            assertEquals("One", database.getSongById(songs.first().id)?.song?.title)
        } finally {
            database.close()
        }
    }

    @Test
    fun failedPlaylistTransactionDoesNotLeavePartialLibraryState() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val internal = Room.inMemoryDatabaseBuilder(context, InternalDatabase::class.java).build()
        val database = MusicDatabase(internal)
        val playlist = PlaylistEntity(id = "rollback-playlist", name = "Rollback fixture", isLocal = true)
        val song = SongEntity(id = "rollback-song", title = "Rollback song")

        try {
            try {
                database.withTransaction {
                    insert(song)
                    insert(playlist)
                    val savedPlaylist = requireNotNull(getPlaylistByIdBlocking(playlist.id))
                    addSongToPlaylist(savedPlaylist, listOf(song.id))
                    throw IllegalStateException("intentional transaction failure")
                }
                fail("The transaction must propagate the fixture failure")
            } catch (_: IllegalStateException) {
                // Expected: Room must roll back every write in the transaction.
            }

            assertNull(database.getSongById(song.id))
            assertNull(database.getPlaylistById(playlist.id))
            assertTrue(database.playlistSongs(playlist.id).first().isEmpty())
        } finally {
            database.close()
        }
    }
}

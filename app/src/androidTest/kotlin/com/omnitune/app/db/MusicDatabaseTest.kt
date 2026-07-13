package com.omnitune.app.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.omnitune.app.db.entities.SongEntity
import com.omnitune.app.models.MediaMetadata
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDateTime

@RunWith(AndroidJUnit4::class)
class MusicDatabaseTest {

    @Test
    fun insertingMetadataCreatesUsableLibraryRelations() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val internal = Room.inMemoryDatabaseBuilder(context, InternalDatabase::class.java).build()
        val database = MusicDatabase(internal)
        val metadata = MediaMetadata(
            id = "song-with-relations",
            title = "Library Song",
            artists = listOf(MediaMetadata.Artist("UC_artist", "Library Artist", "https://artist/image.jpg")),
            duration = 180,
            thumbnailUrl = "https://album/image.jpg",
            album = MediaMetadata.Album("album-id", "Library Album"),
            inLibrary = LocalDateTime.now(),
        )

        database.insert(metadata)

        assertEquals("https://album/image.jpg", database.songsByNameAsc().first().single().thumbnailUrl)
        assertEquals("https://artist/image.jpg", database.artistsByNameAsc().first().single().thumbnailUrl)
        assertEquals("Library Album", database.albumsByNameAsc().first().single().title)
        assertEquals("Library Song", database.albumSongs("album-id").first().single().title)
        internal.close()
    }

    @Test
    fun databaseIsCreated() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val internal = Room.inMemoryDatabaseBuilder(context, InternalDatabase::class.java)
            .build()
        val database = MusicDatabase(internal)
        assertNotNull(database)

        val song = SongEntity(
            id = "test123",
            title = "Test Song",
            duration = 240,
            thumbnailUrl = "https://example.com/thumb.jpg"
        )
        database.insert(song)

        val retrieved = database.song("test123").first()
        assertNotNull(retrieved)
        assert(retrieved?.title == "Test Song")

        internal.close()

    }
    @Test
    fun addSameSongTwiceDoesNotDuplicate() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val internal = Room.inMemoryDatabaseBuilder(context, InternalDatabase::class.java).build()
        val database = MusicDatabase(internal)

        val song = SongEntity(id = "song1", title = "Song", duration = 100, thumbnailUrl = "")
        database.insert(song)
        val playlist = com.omnitune.app.db.entities.PlaylistEntity(name = "My Playlist")
        database.insert(playlist)

        val dbPlaylist = database.getPlaylistByIdBlocking(playlist.id)!!
        database.addSongToPlaylist(dbPlaylist, listOf("song1"))
        database.addSongToPlaylist(dbPlaylist, listOf("song1")) // Duplicate
        
        val songsInPlaylist = database.playlistSongs(playlist.id).first()
        assert(songsInPlaylist.size == 1)
        
        internal.close()
    }

    @Test
    fun deletePlaylistRemovesCrossrefsButNotSongs() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val internal = Room.inMemoryDatabaseBuilder(context, InternalDatabase::class.java).build()
        val database = MusicDatabase(internal)

        val song = SongEntity(id = "song2", title = "Song", duration = 100, thumbnailUrl = "")
        database.insert(song)
        val playlist = com.omnitune.app.db.entities.PlaylistEntity(name = "Delete Me")
        database.insert(playlist)

        val dbPlaylist = database.getPlaylistByIdBlocking(playlist.id)!!
        database.addSongToPlaylist(dbPlaylist, listOf("song2"))
        
        database.delete(dbPlaylist.playlist)
        
        val songsInPlaylist = database.playlistSongs(playlist.id).first()
        assert(songsInPlaylist.isEmpty())
        assertNotNull(database.getSongById("song2")) // Song still exists
        
        internal.close()
    }

    @Test
    fun removeTrackRemovesOnlyCrossref() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val internal = Room.inMemoryDatabaseBuilder(context, InternalDatabase::class.java).build()
        val database = MusicDatabase(internal)

        val song = SongEntity(id = "song3", title = "Song", duration = 100, thumbnailUrl = "")
        database.insert(song)
        val playlist = com.omnitune.app.db.entities.PlaylistEntity(name = "My Playlist")
        database.insert(playlist)

        val dbPlaylist = database.getPlaylistByIdBlocking(playlist.id)!!
        database.addSongToPlaylist(dbPlaylist, listOf("song3"))
        
        database.removeSongFromPlaylist(playlist.id, "song3")
        
        val songsInPlaylist = database.playlistSongs(playlist.id).first()
        assert(songsInPlaylist.isEmpty())
        assertNotNull(database.getSongById("song3")) // Song still exists
        
        internal.close()
    }
}

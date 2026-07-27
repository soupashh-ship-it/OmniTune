package com.omnitune.app.data

import com.omnitune.app.db.DatabaseDao
import com.omnitune.app.db.entities.LyricsEntity
import com.omnitune.app.lyrics.LyricsHelper
import com.omnitune.app.models.AppResult
import com.omnitune.app.models.MediaMetadata
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito

class LyricsRepositoryImplTest {
    @Test
    fun `verified provider lyrics replace a potentially stale cache`() = runBlocking {
        val helper = Mockito.mock(LyricsHelper::class.java)
        val database = Mockito.mock(DatabaseDao::class.java)
        Mockito.`when`(database.lyrics("song-id")).thenReturn(
            flowOf(LyricsEntity(id = "song-id", lyrics = "[00:01.00]Cached line")),
        )
        val metadata = MediaMetadata(
            id = "song-id",
            title = "Song",
            artists = listOf(MediaMetadata.Artist(id = "", name = "Artist")),
            duration = 180,
        )
        Mockito.`when`(helper.getLyrics(metadata)).thenReturn("[00:02.00]Verified line")
        val repository = LyricsRepositoryImpl(helper, database)

        val result = repository.loadLyrics("song-id", "Song", "Artist", 180)

        assertTrue(result is AppResult.Success)
        assertEquals("Verified line", (result as AppResult.Success).data.single().text)
        Mockito.verify(database).upsert(LyricsEntity(id = "song-id", lyrics = "[00:02.00]Verified line"))
    }

    @Test
    fun `plain cached lyrics are refreshed when synced lyrics are available`() = runBlocking {
        val helper = Mockito.mock(LyricsHelper::class.java)
        val database = Mockito.mock(DatabaseDao::class.java)
        Mockito.`when`(database.lyrics("song-id")).thenReturn(
            flowOf(LyricsEntity(id = "song-id", lyrics = "Plain cached line")),
        )
        val metadata = MediaMetadata(
            id = "song-id",
            title = "Song",
            artists = listOf(MediaMetadata.Artist(id = "", name = "Artist")),
            duration = 180,
        )
        Mockito.`when`(helper.getLyrics(metadata)).thenReturn("[00:02.00]Synced provider line")
        val repository = LyricsRepositoryImpl(helper, database)

        val result = repository.loadLyrics("song-id", "Song", "Artist", 180)

        assertTrue(result is AppResult.Success)
        val lines = (result as AppResult.Success).data
        assertEquals("Synced provider line", lines.single().text)
        assertEquals(2_000L, lines.single().timestamp)
        Mockito.verify(database).upsert(LyricsEntity(id = "song-id", lyrics = "[00:02.00]Synced provider line"))
    }

    @Test
    fun `lyrics load can be called from main dispatcher`() = runTest {
        val helper = Mockito.mock(LyricsHelper::class.java)
        val database = Mockito.mock(DatabaseDao::class.java)
        Mockito.`when`(database.lyrics("song-id")).thenReturn(
            flowOf(LyricsEntity(id = "song-id", lyrics = "[00:04.00]Main-safe line")),
        )
        val metadata = MediaMetadata(
            id = "song-id",
            title = "Song",
            artists = listOf(MediaMetadata.Artist(id = "", name = "Artist")),
            duration = 180,
        )
        Mockito.`when`(helper.getLyrics(metadata)).thenReturn("[00:04.00]Main-safe verified line")
        val repository = LyricsRepositoryImpl(helper, database)

        val result = repository.loadLyrics("song-id", "Song", "Artist", 180)

        assertTrue(result is AppResult.Success)
        val lines = (result as AppResult.Success).data
        assertEquals("Main-safe verified line", lines.single().text)
        assertEquals(4_000L, lines.single().timestamp)
    }

    @Test
    fun `unverified cached lyrics are not displayed when providers cannot confirm the track`() = runBlocking {
        val helper = Mockito.mock(LyricsHelper::class.java)
        val database = Mockito.mock(DatabaseDao::class.java)
        Mockito.`when`(database.lyrics("song-id")).thenReturn(
            flowOf(LyricsEntity(id = "song-id", lyrics = "[00:01.00]Potentially wrong cached line")),
        )
        val metadata = MediaMetadata(
            id = "song-id",
            title = "Song",
            artists = listOf(MediaMetadata.Artist(id = "", name = "Artist")),
            duration = 180,
        )
        Mockito.`when`(helper.getLyrics(metadata)).thenReturn(LyricsEntity.LYRICS_NOT_FOUND)
        val repository = LyricsRepositoryImpl(helper, database)

        val result = repository.loadLyrics("song-id", "Song", "Artist", 180)

        assertTrue(result is AppResult.Error)
    }

    @Test
    fun `plain lyrics are returned as displayable unsynced lines`() {
        val helper = Mockito.mock(LyricsHelper::class.java)
        val database = Mockito.mock(DatabaseDao::class.java)
        val repository = LyricsRepositoryImpl(helper, database)

        val lines = repository.parseLrc(
            """
            First plain line

            Second plain line
            """.trimIndent(),
        )

        assertEquals(listOf("First plain line", "Second plain line"), lines.map { it.text })
        assertEquals(listOf(-1L, -1L), lines.map { it.timestamp })
    }

    @Test
    fun `synced lyrics keep timestamps for auto scroll`() {
        val helper = Mockito.mock(LyricsHelper::class.java)
        val database = Mockito.mock(DatabaseDao::class.java)
        val repository = LyricsRepositoryImpl(helper, database)

        val lines = repository.parseLrc(
            """
            [00:01.00]First synced line
            [00:03.50]Second synced line
            """.trimIndent(),
        )

        assertEquals(listOf("First synced line", "Second synced line"), lines.map { it.text })
        assertEquals(listOf(1_000L, 3_500L), lines.map { it.timestamp })
    }
}

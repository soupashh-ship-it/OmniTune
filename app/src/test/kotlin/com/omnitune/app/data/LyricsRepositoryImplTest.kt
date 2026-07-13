package com.omnitune.app.data

import com.omnitune.app.db.DatabaseDao
import com.omnitune.app.db.entities.LyricsEntity
import com.omnitune.app.lyrics.LyricsHelper
import com.omnitune.app.models.AppResult
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito

class LyricsRepositoryImplTest {
    @Test
    fun `cached lyrics are returned without another provider request`() = runBlocking {
        val helper = Mockito.mock(LyricsHelper::class.java)
        val database = Mockito.mock(DatabaseDao::class.java)
        Mockito.`when`(database.lyrics("song-id")).thenReturn(
            flowOf(LyricsEntity(id = "song-id", lyrics = "[00:01.00]Cached line")),
        )
        val repository = LyricsRepositoryImpl(helper, database)

        val result = repository.loadLyrics("song-id", "Song", "Artist", 180)

        assertTrue(result is AppResult.Success)
        assertEquals("Cached line", (result as AppResult.Success).data.single().text)
        Mockito.verifyNoInteractions(helper)
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

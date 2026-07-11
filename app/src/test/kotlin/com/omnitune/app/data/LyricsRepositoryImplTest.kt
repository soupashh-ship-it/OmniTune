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
}

package com.omnitune.shared

import com.omnitune.shared.data.lyrics.LyricsService
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class LyricsServiceTest {

    @Test
    fun testLyricsServiceReturnsSyncedLyrics() = runBlocking {
        val service = LyricsService()
        val result = service.getLyrics(
            videoId = "test_vid_1",
            title = "Viva La Vida",
            artist = "Coldplay",
            durationSec = 242
        )

        assertTrue(result.isSuccess, "Lyrics retrieval should succeed")
        val lyrics = result.getOrNull()
        assertNotNull(lyrics)
        assertEquals("test_vid_1", lyrics.videoId)
        assertTrue(lyrics.isSynced, "Should return synchronized lyrics")
        assertTrue(lyrics.syncedLyrics.isNotEmpty(), "Synced lyrics should not be empty")
        assertEquals(lyrics.syncedLyrics, lyrics.lines)

        // Subsequent call returns cached lyrics
        val cached = service.getLyrics(
            videoId = "test_vid_1",
            title = "Viva La Vida",
            artist = "Coldplay",
            durationSec = 242
        ).getOrNull()
        assertEquals(lyrics, cached)
    }

    @Test
    fun testLyricsServiceSynthesizesWhenEmpty() = runBlocking {
        val service = LyricsService()
        val result = service.getLyrics(
            videoId = "unknown_track_99",
            title = "Unknown Song",
            artist = "Unknown Artist",
            durationSec = 180
        )

        assertTrue(result.isSuccess)
        val lyrics = result.getOrNull()
        assertNotNull(lyrics)
        assertTrue(lyrics.isSynced)
        assertTrue(lyrics.syncedLyrics.size >= 4)
    }
}

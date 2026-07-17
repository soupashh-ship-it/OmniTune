package com.omnitune.app.lyrics

import com.omnitune.app.models.MediaMetadata
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricsQualityTest {
    private val englishSong = MediaMetadata(
        id = "song",
        title = "Midnight City",
        artists = listOf(MediaMetadata.Artist(id = "artist", name = "M83")),
        duration = 244,
    )

    @Test
    fun `rejects likely wrong script lyrics for song metadata`() {
        val score = LyricsQuality.score(
            providerName = "LrcLib",
            lyrics = "ここにいるよ\n君の声を探してる\n夜の中で",
            mediaMetadata = englishSong,
            isSynced = true,
        )

        assertNull(score)
    }

    @Test
    fun `scores synced lyrics from trusted providers above plain text`() {
        val syncedScore = LyricsQuality.score(
            providerName = "YouTube",
            lyrics = "[00:01.00] Midnight city waits for us\n[00:05.00] The lights are calling",
            mediaMetadata = englishSong,
            isSynced = true,
        )
        val plainScore = LyricsQuality.score(
            providerName = "KuGou",
            lyrics = "Midnight city waits for us\nThe lights are calling",
            mediaMetadata = englishSong,
            isSynced = false,
        )

        assertNotNull(syncedScore)
        assertNotNull(plainScore)
        assertTrue(syncedScore!! > plainScore!!)
    }
}

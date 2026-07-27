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
    fun `accepts exact-video lyrics without title or artist words`() {
        val exactVideoScore = LyricsQuality.score(
            providerName = "YouTube",
            lyrics = "[00:01.00] The neon waits for us\n[00:05.00] The lights are calling",
            mediaMetadata = englishSong,
            isSynced = true,
            isTrackBound = true,
        )
        val unverifiedSearchScore = LyricsQuality.score(
            providerName = "KuGou",
            lyrics = "The neon waits for us\nThe lights are calling",
            mediaMetadata = englishSong,
            isSynced = false,
        )

        assertNotNull(exactVideoScore)
        assertNull(unverifiedSearchScore)
    }

    @Test
    fun `accepts search lyrics only with strong title and artist evidence`() {
        val metadata = englishSong.copy(
            artists = listOf(MediaMetadata.Artist(id = "artist", name = "Example Artist")),
        )

        val score = LyricsQuality.score(
            providerName = "LrcLib",
            lyrics = "Midnight City\nPerformed by Example Artist\nThe lights are calling",
            mediaMetadata = metadata,
            isSynced = false,
        )

        assertNotNull(score)
    }

    @Test
    fun `prefers synchronized metadata match over exact-video plain lyrics`() {
        val exactVideoPlain = LyricsQuality.score(
            providerName = "YouTube Music",
            lyrics = "The neon waits for us\nThe lights are calling",
            mediaMetadata = englishSong,
            isSynced = false,
            isTrackBound = true,
        )
        val metadataMatchedSynced = LyricsQuality.score(
            providerName = "LrcLib",
            lyrics = "[00:01.00] The neon waits for us\n[00:05.00] The lights are calling",
            mediaMetadata = englishSong,
            isSynced = true,
            isMetadataBound = true,
        )

        assertNotNull(exactVideoPlain)
        assertNotNull(metadataMatchedSynced)
        assertTrue(metadataMatchedSynced!! > exactVideoPlain!!)
    }
}

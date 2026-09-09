package com.omnitune.app.playback

import androidx.media3.common.C
import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerProgressMapperTest {
    @Test
    fun usesPlayerDurationWhenAvailable() {
        val snapshot = PlayerProgressMapper.snapshot(
            playerPositionMs = 2_000L,
            playerDurationMs = 10_000L,
            metadataDurationSeconds = 20,
        )

        assertEquals(2_000L, snapshot.positionMs)
        assertEquals(10_000L, snapshot.durationMs)
        assertEquals(0.2f, snapshot.progress, 0.0001f)
    }

    @Test
    fun fallsBackToMetadataDurationWhenPlayerDurationIsUnset() {
        val snapshot = PlayerProgressMapper.snapshot(
            playerPositionMs = 3_000L,
            playerDurationMs = C.TIME_UNSET,
            metadataDurationSeconds = 10,
        )

        assertEquals(3_000L, snapshot.positionMs)
        assertEquals(10_000L, snapshot.durationMs)
        assertEquals(0.3f, snapshot.progress, 0.0001f)
    }

    @Test
    fun clampsInvalidValues() {
        val snapshot = PlayerProgressMapper.snapshot(
            playerPositionMs = 12_000L,
            playerDurationMs = 10_000L,
            metadataDurationSeconds = null,
        )

        assertEquals(10_000L, snapshot.positionMs)
        assertEquals(10_000L, snapshot.durationMs)
        assertEquals(1f, snapshot.progress, 0.0001f)
    }
}

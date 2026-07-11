package com.omnitune.app.playback

import com.omnitune.app.playback.recovery.PlaybackErrorType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackRecoveryCoordinatorTest {
    @Test
    fun `only track-specific errors auto-skip`() {
        assertFalse(PlaybackErrorType.NetworkError.shouldAutoSkipTrack())
        assertFalse(PlaybackErrorType.Timeout.shouldAutoSkipTrack())
        assertFalse(PlaybackErrorType.Unknown.shouldAutoSkipTrack())
        assertTrue(PlaybackErrorType.Forbidden403.shouldAutoSkipTrack())
        assertTrue(PlaybackErrorType.NotFound404.shouldAutoSkipTrack())
        assertTrue(PlaybackErrorType.BotCheck.shouldAutoSkipTrack())
    }
}

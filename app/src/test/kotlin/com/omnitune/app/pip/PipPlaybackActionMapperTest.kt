package com.omnitune.app.pip

import com.omnitune.app.playback.PlaybackActions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PipPlaybackActionMapperTest {
    @Test
    fun pipActionsMapToPlaybackServiceNamespace() {
        assertEquals(
            PlaybackActions.ACTION_PLAY_PAUSE,
            PipPlaybackActionMapper.toPlaybackAction(PlaybackActions.ACTION_PIP_PLAY_PAUSE),
        )
        assertEquals(
            PlaybackActions.ACTION_NEXT,
            PipPlaybackActionMapper.toPlaybackAction(PlaybackActions.ACTION_PIP_NEXT),
        )
        assertEquals(
            PlaybackActions.ACTION_PREVIOUS,
            PipPlaybackActionMapper.toPlaybackAction(PlaybackActions.ACTION_PIP_PREVIOUS),
        )
    }

    @Test
    fun unknownActionIsIgnored() {
        assertNull(PipPlaybackActionMapper.toPlaybackAction("com.example.unsupported"))
        assertNull(PipPlaybackActionMapper.toPlaybackAction(null))
    }
}

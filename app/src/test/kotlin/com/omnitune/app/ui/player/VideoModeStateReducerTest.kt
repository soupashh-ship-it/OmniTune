package com.omnitune.app.ui.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoModeStateReducerTest {
    @Test
    fun `enabling video mode binds it to the active media item`() {
        val session = VideoModeStateReducer.setEnabled(VideoModeSession(), enabled = true, mediaId = "video-1")

        assertTrue(session.enabled)
        assertEquals("video-1", session.mediaId)
    }

    @Test
    fun `disabling video mode clears bound media item`() {
        val session = VideoModeStateReducer.setEnabled(
            VideoModeSession(enabled = true, mediaId = "video-1"),
            enabled = false,
            mediaId = "video-1",
        )

        assertFalse(session.enabled)
        assertEquals(null, session.mediaId)
    }

    @Test
    fun `switching media while video mode is active exits video mode`() {
        val session = VideoModeStateReducer.onMediaItemChanged(
            VideoModeSession(enabled = true, mediaId = "video-1"),
            mediaId = "audio-1",
        )

        assertFalse(session.enabled)
        assertEquals(null, session.mediaId)
    }

    @Test
    fun `same media keeps active video mode`() {
        val session = VideoModeStateReducer.onMediaItemChanged(
            VideoModeSession(enabled = true, mediaId = "video-1"),
            mediaId = "video-1",
        )

        assertTrue(session.enabled)
        assertEquals("video-1", session.mediaId)
    }

    @Test
    fun `legacy active video mode without bound media captures current item`() {
        val session = VideoModeStateReducer.onMediaItemChanged(
            VideoModeSession(enabled = true, mediaId = null),
            mediaId = "video-1",
        )

        assertTrue(session.enabled)
        assertEquals("video-1", session.mediaId)
    }
}

package com.omnitune.app.playback

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StreamResolutionTargetTest {
    @Test
    fun `only the originally requested queue item can receive a resolved stream`() {
        val target = StreamResolutionTarget(
            mediaId = "video-id",
            mediaItemIndex = 3,
            resumePositionMs = 45_000L,
        )

        assertTrue(target.isCurrent(currentMediaId = "video-id", currentMediaItemIndex = 3))
        assertFalse(target.isCurrent(currentMediaId = "next-video", currentMediaItemIndex = 3))
        assertFalse(target.isCurrent(currentMediaId = "video-id", currentMediaItemIndex = 4))
        assertTrue(target.resumePositionMs == 45_000L)
    }
}

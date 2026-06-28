package com.omnitune.app.playback

import com.omnitune.app.models.PlaybackQualityMode
import com.omnitune.app.models.StreamQuality
import org.junit.Assert.assertEquals
import org.junit.Test

class StreamUrlResolverTest {

    @Test
    fun `mapQuality maps correctly`() {
        assertEquals(StreamQuality.LOW, StreamUrlResolver.mapQuality(PlaybackQualityMode.DATA_SAVER))
        assertEquals(StreamQuality.MEDIUM, StreamUrlResolver.mapQuality(PlaybackQualityMode.BALANCED))
        assertEquals(StreamQuality.BEST, StreamUrlResolver.mapQuality(PlaybackQualityMode.HIGH))
        assertEquals(StreamQuality.HIGH, StreamUrlResolver.mapQuality(PlaybackQualityMode.AUTO))
    }
}

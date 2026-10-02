package com.omnitune.shared

import com.omnitune.shared.data.innertube.AudioFormatCandidate
import com.omnitune.shared.data.innertube.PoTokenGenerator
import com.omnitune.shared.data.innertube.StreamResolver
import com.omnitune.shared.data.network.currentTimeMillis
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class StreamResolverTest {

    @Test
    fun testSelectBestAudioFormatPrioritizesItag140() {
        val candidates = listOf(
            AudioFormatCandidate(itag = 251, container = "webm", codec = "opus", bitrate = 160000, mimeType = "audio/webm"),
            AudioFormatCandidate(itag = 140, container = "m4a", codec = "mp4a.40.2", bitrate = 128000, mimeType = "audio/mp4"),
            AudioFormatCandidate(itag = 139, container = "m4a", codec = "mp4a.40.2", bitrate = 48000, mimeType = "audio/mp4")
        )

        val selected = StreamResolver.selectBestAudioFormat(candidates)
        assertNotNull(selected)
        assertEquals(140, selected.itag, "Best format must prioritize AAC itag 140 for iOS compatibility")
        assertEquals("audio/mp4", selected.mimeType)
    }

    @Test
    fun testSelectBestAudioFormatFallbackTo139() {
        val candidates = listOf(
            AudioFormatCandidate(itag = 251, container = "webm", codec = "opus", bitrate = 160000, mimeType = "audio/webm"),
            AudioFormatCandidate(itag = 139, container = "m4a", codec = "mp4a.40.2", bitrate = 48000, mimeType = "audio/mp4")
        )

        val selected = StreamResolver.selectBestAudioFormat(candidates)
        assertNotNull(selected)
        assertEquals(139, selected.itag, "When 140 is missing, should fall back to AAC itag 139")
    }

    @Test
    fun testResolveStreamDirectAac() {
        val videoId = "song_abc_123"
        val streamInfo = StreamResolver.resolveStream(videoId = videoId, clientProfileName = "ANDROID_VR_NO_AUTH")

        assertEquals(videoId, streamInfo.videoId)
        assertEquals(140, streamInfo.itag)
        assertEquals("audio/mp4", streamInfo.mimeType)
        assertTrue(streamInfo.bitrate > 0)
        assertTrue(streamInfo.streamUrl.contains("id=$videoId"))
        assertTrue(streamInfo.streamUrl.contains("itag=140"))
        assertTrue(streamInfo.streamUrl.contains("&pot="))

        val potMatch = Regex("""[?&]pot=([^&]+)""").find(streamInfo.streamUrl)
        assertNotNull(potMatch, "Stream URL must include &pot=")
        val potToken = potMatch.groupValues[1]
        assertTrue(potToken.length > 20)

        // Verify the generated PoToken in the URL
        val verified = PoTokenGenerator.verifyPoToken(potToken)
        assertTrue(verified.valid, "PoToken embedded in stream URL must be valid")
    }

    @Test
    fun testStreamUrlExpirationCheck() {
        val nowSec = currentTimeMillis() / 1000
        val futureUrl = "https://example.com/videoplayback?expire=${nowSec + 3600}&id=test"
        assertFalse(StreamResolver.isExpired(futureUrl, nowSec), "Future stream URL should not be expired")

        val pastUrl = "https://example.com/videoplayback?expire=${nowSec - 10}&id=test"
        assertTrue(StreamResolver.isExpired(pastUrl, nowSec), "Past stream URL should be expired")
    }

    @Test
    fun testIpadOsClientProfileResolution() {
        val videoId = "track_ipados_999"
        val streamInfo = StreamResolver.resolveStream(videoId = videoId, clientProfileName = "IPADOS")

        assertTrue(streamInfo.streamUrl.contains("c=IPADOS"))
        assertEquals(140, streamInfo.itag)
        assertFalse(streamInfo.isExpired(currentTimeMillis()))
    }
}

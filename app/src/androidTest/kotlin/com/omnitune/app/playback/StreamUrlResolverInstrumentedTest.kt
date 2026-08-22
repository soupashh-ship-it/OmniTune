package com.omnitune.app.playback

import androidx.media3.common.MediaItem
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.omnitune.app.models.PlaybackQualityMode
import com.omnitune.app.models.StreamQuality
import com.omnitune.app.models.StreamResult
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Exercises the real MediaItem stream replacement and resolver-memory-cache wiring. */
@RunWith(AndroidJUnit4::class)
class StreamUrlResolverInstrumentedTest {

    @Before
    fun clearCacheBeforeTest() {
        StreamUrlResolver.clearMemoryCache("resolver fixture setup")
    }

    @After
    fun clearCache() {
        StreamUrlResolver.clearMemoryCache("resolver fixture cleanup")
    }

    @Test
    fun successfulLookupReplacesBareVideoIdAndCachesTheStream() = runBlocking {
        val videoId = "fixtureId01"
        val item = MediaItem.Builder().setMediaId(videoId).setUri(videoId).build()
        var lookupCalls = 0
        var requestedQuality: StreamQuality? = null

        val resolved = StreamUrlResolver.resolveMediaItem(
            mediaItem = item,
            streamLookup = { requestedId, quality ->
                lookupCalls++
                assertEquals(videoId, requestedId)
                requestedQuality = quality
                StreamResult("https://stream.fixture.test/audio", "audio/mpeg")
            },
            qualityMode = PlaybackQualityMode.HIGH,
        )

        requireNotNull(resolved)
        assertEquals("https://stream.fixture.test/audio", resolved.localConfiguration?.uri.toString())
        assertEquals("audio/mpeg", resolved.localConfiguration?.mimeType)
        assertEquals(videoId, resolved.localConfiguration?.customCacheKey)
        assertEquals(StreamQuality.BEST, requestedQuality)
        assertEquals(1, lookupCalls)

        val cached = StreamUrlResolver.resolveMediaItem(
            mediaItem = item,
            streamLookup = { _, _ -> error("a fresh lookup must not run while the stream is cached") },
        )
        assertEquals("https://stream.fixture.test/audio", cached?.localConfiguration?.uri.toString())
        assertEquals(1, lookupCalls)

        StreamUrlResolver.invalidate(videoId)
        val refreshed = StreamUrlResolver.resolveMediaItem(
            mediaItem = item,
            streamLookup = { _, _ ->
                lookupCalls++
                StreamResult("https://stream.fixture.test/refreshed", "audio/mpeg")
            },
        )
        assertEquals("https://stream.fixture.test/refreshed", refreshed?.localConfiguration?.uri.toString())
        assertEquals(2, lookupCalls)
    }

    @Test
    fun unavailableOrNonYoutubeItemsDoNotProduceBrokenMediaItems() = runBlocking {
        val unavailable = MediaItem.Builder().setMediaId("missingTrk1").setUri("missingTrk1").build()
        assertNull(
            StreamUrlResolver.resolveMediaItem(
                mediaItem = unavailable,
                streamLookup = { _, _ -> null },
            ),
        )

        var lookupCalled = false
        val httpItem = MediaItem.Builder()
            .setMediaId("already-resolved")
            .setUri("https://example.invalid/already-resolved")
            .build()
        assertNull(
            StreamUrlResolver.resolveMediaItem(
                mediaItem = httpItem,
                streamLookup = { _, _ ->
                    lookupCalled = true
                    null
                },
            ),
        )
        assertTrue(!lookupCalled)
    }

    @Test
    fun timedOutLookupReturnsNoItemAndDoesNotPoisonTheResolverCache() = runBlocking {
        val videoId = "fixtureId01"
        val item = MediaItem.Builder().setMediaId(videoId).setUri(videoId).build()
        var lookupCalls = 0

        assertNull(
            StreamUrlResolver.resolveMediaItem(
                mediaItem = item,
                streamLookup = { _, _ ->
                    lookupCalls++
                    error("a zero-timeout lookup must not begin")
                },
                lookupTimeoutMillis = 0L,
            ),
        )
        assertEquals(0, lookupCalls)

        val recovered = StreamUrlResolver.resolveMediaItem(
            mediaItem = item,
            streamLookup = { _, _ ->
                lookupCalls++
                StreamResult("https://stream.fixture.test/recovered", "audio/mpeg")
            },
        )
        assertEquals("https://stream.fixture.test/recovered", recovered?.localConfiguration?.uri.toString())
        assertEquals(1, lookupCalls)
    }
}

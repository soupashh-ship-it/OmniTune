package com.omnitune.app.playback

import androidx.media3.common.C
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflinePlaybackCacheRoutingTest {

    @Test
    fun completedByteCompleteDownloadUsesPersistentDownloadCache() {
        assertTrue(
            OfflinePlaybackCacheRouting.isFullyCached(
                isCompleted = true,
                expectedContentLength = 4_096,
                cachedPrefixLength = 4_096,
            ),
        )
        assertEquals(
            PlaybackCacheRoute.DOWNLOAD_CACHE,
            OfflinePlaybackCacheRouting.routeFor(isCompletedDownloadPlayable = true),
        )
    }

    @Test
    fun partialOrUnknownCompletedEntryFallsBackToStreamRouting() {
        assertFalse(
            OfflinePlaybackCacheRouting.isFullyCached(
                isCompleted = true,
                expectedContentLength = 4_096,
                cachedPrefixLength = 4_095,
            ),
        )
        assertFalse(
            OfflinePlaybackCacheRouting.isFullyCached(
                isCompleted = true,
                expectedContentLength = C.LENGTH_UNSET.toLong(),
                cachedPrefixLength = Long.MAX_VALUE,
            ),
        )
        assertEquals(
            PlaybackCacheRoute.STREAM_CACHE,
            OfflinePlaybackCacheRouting.routeFor(isCompletedDownloadPlayable = false),
        )
    }

    @Test
    fun incompleteDownloadIsNeverRoutedAsOfflineEvenWhenItHasCachedBytes() {
        assertFalse(
            OfflinePlaybackCacheRouting.isFullyCached(
                isCompleted = false,
                expectedContentLength = 4_096,
                cachedPrefixLength = 4_096,
            ),
        )
    }

    @Test
    fun customCacheKeyIsPreservedAndLegacyRequestsFallBackToDownloadId() {
        assertEquals(
            "cache-song-42",
            OfflineDownloadIdentity.cacheKey("song-42", "cache-song-42"),
        )
        assertEquals(
            "song-42",
            OfflineDownloadIdentity.cacheKey("song-42", null),
        )
    }
}

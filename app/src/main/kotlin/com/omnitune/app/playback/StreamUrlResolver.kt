package com.omnitune.app.playback

import android.net.Uri
import android.util.LruCache
import androidx.media3.common.MediaItem
import com.omnitune.app.data.StreamExtractor
import com.omnitune.app.models.StreamResult
import com.omnitune.app.models.StreamQuality
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import timber.log.Timber

/**
 * Resolves YouTube video IDs to playable stream URLs.
 *
 * When a MediaItem has a URI that is a bare YouTube video ID (not an http URL),
 * this extracts the actual stream URL via [StreamExtractor] and replaces the URI
 * so ExoPlayer can play it.
 */
object StreamUrlResolver {

    data class CachedStream(val streamResult: StreamResult, val fetchedAtMs: Long)

    /** In-memory cache for resolved stream results (videoId -> CachedStream). */
    private val streamCache = LruCache<String, CachedStream>(50)

    fun invalidate(videoId: String) {
        streamCache.remove(videoId)
        Timber.d("StreamUrlResolver: invalidated cache for $videoId")
    }

    /**
     * Returns true if the given URI is a bare YouTube video ID (not a standard URL).
     */
    fun isYouTubeVideoId(uri: Uri?): Boolean {
        val raw = uri?.toString()?.trim() ?: return false
        if (raw.startsWith("http://") || raw.startsWith("https://")) return false
        // YouTube video IDs are 11 characters: alphanumeric + hyphens + underscores
        return raw.matches(Regex("^[a-zA-Z0-9_-]{11}$"))
    }

    /**
     * Resolves a MediaItem with a YouTube video ID to one with a playable stream URL.
     * Returns null if resolution fails. Uses an in-memory cache to avoid re-extracting.
     */
    suspend fun resolveMediaItem(
        mediaItem: MediaItem,
        streamExtractor: StreamExtractor,
        downloadUtil: DownloadUtil? = null
    ): MediaItem? {
        val videoId = mediaItem.localConfiguration?.uri?.toString()?.trim() ?: return null
        if (!isYouTubeVideoId(mediaItem.localConfiguration?.uri)) return null

        // Check if the item is fully downloaded
        if (downloadUtil != null) {
            try {
                val download = downloadUtil.downloadManager.downloadIndex.getDownload(videoId)
                if (download != null && download.state == androidx.media3.exoplayer.offline.Download.STATE_COMPLETED) {
                    Timber.d("StreamUrlResolver: offline cache hit for $videoId")
                    return mediaItem.buildUpon()
                        .setUri(Uri.parse("file:///offline/$videoId"))
                        .setCustomCacheKey(videoId)
                        .build()
                }
            } catch (e: Exception) {
                Timber.w(e, "Error checking download index for $videoId")
            }
        }

        val cached = streamCache.get(videoId)
        if (cached != null) {
            val isExpired = (System.currentTimeMillis() - cached.fetchedAtMs) > 4 * 60 * 60 * 1000L
            if (!isExpired) {
                Timber.d("StreamUrlResolver: cache hit for $videoId")
                return mediaItem.buildUpon()
                    .setUri(Uri.parse(cached.streamResult.url))
                    .setMimeType(cached.streamResult.contentType)
                    .setCustomCacheKey(videoId)
                    .build()
            } else {
                streamCache.remove(videoId)
            }
        }

        Timber.d("StreamUrlResolver: resolving $videoId")
        val streamResult = streamExtractor.extract(videoId, StreamQuality.HIGH)
        if (streamResult == null) {
            Timber.w("StreamUrlResolver: no stream found for $videoId")
            return null
        }

        streamCache.put(videoId, CachedStream(streamResult, System.currentTimeMillis()))
        Timber.d("StreamUrlResolver: resolved $videoId (${streamResult.contentType})")

        return mediaItem.buildUpon()
            .setUri(Uri.parse(streamResult.url))
            .setMimeType(streamResult.contentType)
            .setCustomCacheKey(videoId)
            .build()
    }

    /**
     * Resolves a list of MediaItems in parallel, replacing YouTube video IDs with playable stream URLs.
     * Items that fail resolution are filtered out.
     */
    suspend fun resolveMediaItems(
        items: List<MediaItem>,
        streamExtractor: StreamExtractor,
        downloadUtil: DownloadUtil? = null
    ): List<MediaItem> = coroutineScope {
        val semaphore = Semaphore(3)
        items.map { item ->
            async {
                semaphore.withPermit {
                    if (isYouTubeVideoId(item.localConfiguration?.uri)) {
                        resolveMediaItem(item, streamExtractor, downloadUtil)
                    } else {
                        item
                    }
                }
            }
        }.mapNotNull { it.await() }
    }
}

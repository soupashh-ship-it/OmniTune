package com.omnitune.app.playback

import android.net.Uri
import android.util.LruCache
import androidx.media3.common.MediaItem
import com.omnitune.app.data.StreamExtractor
import com.omnitune.app.models.StreamResult
import com.omnitune.app.models.StreamQuality
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.async
import timber.log.Timber

/**
 * Resolves YouTube video IDs to playable stream URLs.
 *
 * When a MediaItem has a URI that is a bare YouTube video ID (not an http URL),
 * this extracts the actual stream URL via [StreamExtractor] and replaces the URI
 * so ExoPlayer can play it.
 */
object StreamUrlResolver {

    /** In-memory cache for resolved stream results (videoId -> StreamResult). */
    private val streamCache = LruCache<String, StreamResult>(50)

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
    ): MediaItem? {
        val videoId = mediaItem.localConfiguration?.uri?.toString()?.trim() ?: return null
        if (!isYouTubeVideoId(mediaItem.localConfiguration?.uri)) return null

        val cached = streamCache.get(videoId)
        if (cached != null) {
            Timber.d("StreamUrlResolver: cache hit for $videoId")
            return mediaItem.buildUpon()
                .setUri(Uri.parse(cached.url))
                .setMimeType(cached.contentType)
                .build()
        }

        Timber.d("StreamUrlResolver: resolving $videoId")
        val streamResult = streamExtractor.extract(videoId, StreamQuality.HIGH)
        if (streamResult == null) {
            Timber.w("StreamUrlResolver: no stream found for $videoId")
            return null
        }

        streamCache.put(videoId, streamResult)
        Timber.d("StreamUrlResolver: resolved $videoId (${streamResult.contentType})")

        return mediaItem.buildUpon()
            .setUri(Uri.parse(streamResult.url))
            .setMimeType(streamResult.contentType)
            .build()
    }

    /**
     * Resolves a list of MediaItems in parallel, replacing YouTube video IDs with playable stream URLs.
     * Items that fail resolution are filtered out.
     */
    suspend fun resolveMediaItems(
        items: List<MediaItem>,
        streamExtractor: StreamExtractor,
    ): List<MediaItem> = coroutineScope {
        items.map { item ->
            async {
                if (isYouTubeVideoId(item.localConfiguration?.uri)) {
                    resolveMediaItem(item, streamExtractor)
                } else {
                    item
                }
            }
        }.mapNotNull { it.await() }
    }
}

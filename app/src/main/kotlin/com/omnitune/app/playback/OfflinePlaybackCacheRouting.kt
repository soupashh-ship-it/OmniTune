package com.omnitune.app.playback

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.TransferListener
import java.io.IOException

/**
 * Keeps the Media3 download identity stable from enqueue through playback.
 *
 * Download IDs are the database and queue identity. The custom cache key is the on-disk media
 * identity and must be used by every player data source rather than the expiring stream URL.
 */
internal object OfflineDownloadIdentity {
    fun cacheKey(downloadId: String, customCacheKey: String?): String =
        customCacheKey?.takeIf { it.isNotBlank() } ?: downloadId
}

/** A completed download is never allowed to fall through to the network cache. */
internal enum class PlaybackCacheRoute {
    DOWNLOAD_CACHE,
    STREAM_CACHE,
}

internal object OfflinePlaybackCacheRouting {
    fun routeFor(isCompletedDownloadPlayable: Boolean): PlaybackCacheRoute =
        if (isCompletedDownloadPlayable) PlaybackCacheRoute.DOWNLOAD_CACHE
        else PlaybackCacheRoute.STREAM_CACHE

    /**
     * A DownloadManager terminal state alone is not sufficient after process death or storage
     * cleanup. Require a known resource length and a contiguous cached range from byte zero.
     */
    fun isFullyCached(
        isCompleted: Boolean,
        expectedContentLength: Long,
        cachedPrefixLength: Long,
    ): Boolean =
        isCompleted &&
            expectedContentLength != C.LENGTH_UNSET.toLong() &&
            expectedContentLength > 0 &&
            cachedPrefixLength >= expectedContentLength
}

/**
 * Defers cache selection until Media3 opens a [DataSpec], when its custom cache key is available.
 * The persistent download source has no upstream, so a completed download can never silently
 * become a network request if its cache changes while it is being played.
 */
@UnstableApi
internal class OfflineCacheRoutingDataSourceFactory(
    private val completedDownloadSourceFactory: DataSource.Factory,
    private val streamSourceFactory: DataSource.Factory,
    private val isCompletedDownloadCacheKey: (String) -> Boolean,
) : DataSource.Factory {

    override fun createDataSource(): DataSource = OfflineCacheRoutingDataSource(
        completedDownloadSourceFactory = completedDownloadSourceFactory,
        streamSourceFactory = streamSourceFactory,
        isCompletedDownloadCacheKey = isCompletedDownloadCacheKey,
    )
}

@UnstableApi
private class OfflineCacheRoutingDataSource(
    private val completedDownloadSourceFactory: DataSource.Factory,
    private val streamSourceFactory: DataSource.Factory,
    private val isCompletedDownloadCacheKey: (String) -> Boolean,
) : DataSource {

    private val transferListeners = mutableListOf<TransferListener>()
    private var delegate: DataSource? = null

    override fun addTransferListener(transferListener: TransferListener) {
        transferListeners += transferListener
        delegate?.addTransferListener(transferListener)
    }

    override fun open(dataSpec: DataSpec): Long {
        check(delegate == null) { "Data source must be closed before it is reopened" }
        val route = OfflinePlaybackCacheRouting.routeFor(
            dataSpec.key?.let(isCompletedDownloadCacheKey) == true,
        )
        val source = when (route) {
            PlaybackCacheRoute.DOWNLOAD_CACHE -> completedDownloadSourceFactory.createDataSource()
            PlaybackCacheRoute.STREAM_CACHE -> streamSourceFactory.createDataSource()
        }
        transferListeners.forEach(source::addTransferListener)
        delegate = source
        return try {
            source.open(dataSpec)
        } catch (error: IOException) {
            try {
                source.close()
            } catch (closeError: IOException) {
                error.addSuppressed(closeError)
            }
            delegate = null
            throw error
        }
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int =
        requireNotNull(delegate) { "Data source is not open" }.read(buffer, offset, length)

    override fun getUri(): Uri? = delegate?.uri

    override fun getResponseHeaders(): Map<String, List<String>> =
        delegate?.responseHeaders ?: emptyMap()

    override fun close() {
        val source = delegate ?: return
        delegate = null
        source.close()
    }
}

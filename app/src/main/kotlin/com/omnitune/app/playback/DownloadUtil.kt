/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.playback

import android.content.Context
import android.net.Uri
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.StatFs
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.cache.ContentMetadata
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import com.omnitune.app.backup.OfflineDownloadArchive
import com.omnitune.app.constants.DownloadWifiOnlyKey
import com.omnitune.app.data.StreamExtractor
import com.omnitune.app.db.MusicDatabase
import com.omnitune.app.utils.PreferenceStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages the ExoPlayer download cache directory and provides download helper.
 * Injected as singleton via Hilt.
 */
@Singleton
@UnstableApi
class DownloadUtil @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: okhttp3.OkHttpClient,
    private val streamExtractor: StreamExtractor,
    private val database: MusicDatabase,
) {
    private val downloadScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val resolveSlots = Semaphore(4)
    private val downloadStateMutex = Mutex()
    private val resolvingIds = ConcurrentHashMap.newKeySet<String>()
    private val staleRepairIds = ConcurrentHashMap.newKeySet<String>()

    val databaseProvider by lazy {
        StandaloneDatabaseProvider(context)
    }

    val downloadCache: SimpleCache by lazy {
        val downloadDir = OfflineDownloadArchive.downloadDirectory(context)
        if (!downloadDir.exists()) downloadDir.mkdirs()
        SimpleCache(downloadDir, NoOpCacheEvictor(), databaseProvider)
    }

    val playbackCache: SimpleCache by lazy {
        val cacheDir = context.cacheDir.resolve("stream-cache")
        if (!cacheDir.exists()) cacheDir.mkdirs()
        SimpleCache(cacheDir, LeastRecentlyUsedCacheEvictor(512L * 1024L * 1024L), databaseProvider)
    }

    val downloadManager: androidx.media3.exoplayer.offline.DownloadManager by lazy {
        val dataSourceFactory = androidx.media3.datasource.okhttp.OkHttpDataSource.Factory(okHttpClient)
            .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36 OmniTune")
        val executor = java.util.concurrent.Executors.newFixedThreadPool(8)
        androidx.media3.exoplayer.offline.DownloadManager(
            context,
            databaseProvider,
            downloadCache,
            dataSourceFactory,
            executor
        ).apply {
            maxParallelDownloads = preferredDownloadParallelism()
            minRetryCount = 5
            addListener(object : androidx.media3.exoplayer.offline.DownloadManager.Listener {
                override fun onDownloadChanged(
                    downloadManager: androidx.media3.exoplayer.offline.DownloadManager,
                    download: Download,
                    finalException: Exception?,
                ) = updateDatabaseDownloadState(download)

                override fun onDownloadRemoved(
                    downloadManager: androidx.media3.exoplayer.offline.DownloadManager,
                    download: Download,
                ) = updateDatabaseDownloadState(download, removed = true)
            })
            downloadScope.launch {
                val cursor = downloadIndex.getDownloads()
                try {
                    while (cursor.moveToNext()) updateDatabaseDownloadState(cursor.download)
                } finally {
                    cursor.close()
                }
            }
        }
    }

    private fun updateDatabaseDownloadState(download: Download, removed: Boolean = false) {
        downloadScope.launch {
            downloadStateMutex.withLock {
                val current = if (removed) null else downloadManager.downloadIndex.getDownload(download.request.id)
                val hasCompleteCache = current?.let(::hasCompleteCache) == true
                val state = DownloadLifecyclePolicy.persistedState(
                    downloadState = current?.state,
                    removed = removed,
                    hasCompleteCache = hasCompleteCache,
                )
                database.withTransaction {
                    updateDownloadState(
                        songId = download.request.id,
                        state = state,
                        downloadedAt = if (state == 2) java.time.LocalDateTime.now() else null,
                    )
                    refreshDownloadedPlaylists(download.request.id)
                }
                if (current?.state == Download.STATE_COMPLETED && !hasCompleteCache) {
                    repairStaleCompletedDownload(current, "completed entry has incomplete cache data")
                }
            }
        }
    }

    fun release() {
        downloadScope.cancel()
        try {
            downloadCache.release()
        } catch (e: Exception) {
            Timber.w(e, "Error releasing download cache")
        }
        try {
            playbackCache.release()
        } catch (e: Exception) {
            Timber.w(e, "Error releasing playback cache")
        }
    }

    fun clearPlaybackCache() {
        try {
            playbackCache.keys.toList().forEach { playbackCache.removeResource(it) }
        } catch (e: Exception) {
            Timber.w(e, "Error clearing playback cache")
        }
    }

    fun enqueue(
        videoId: String,
        title: String,
        resolvedStreamUrl: String? = null,
        onResult: (success: Boolean, message: String) -> Unit = { _, _ -> },
    ) {
        val admission = DownloadLifecyclePolicy.preflight(
            videoId = videoId,
            wifiOnly = PreferenceStore.get(DownloadWifiOnlyKey) ?: true,
            connectedToWifi = isConnectedToWifi(),
            availableBytes = availableDownloadStorageBytes(),
        )
        if (admission is DownloadAdmission.Rejected) {
            onResult(false, admission.message)
            return
        }
        if (!resolvingIds.add(videoId)) {
            onResult(true, "Download already starting")
            return
        }

        downloadScope.launch {
            try {
                downloadManager.maxParallelDownloads = preferredDownloadParallelism()
                val existing = downloadManager.downloadIndex.getDownload(videoId)
                if (existing?.state in setOf(
                        Download.STATE_QUEUED,
                        Download.STATE_DOWNLOADING,
                        Download.STATE_COMPLETED,
                    )
                ) {
                    withContext(Dispatchers.Main) { onResult(true, "Download already queued") }
                    return@launch
                }

                val streamUrl = resolvedStreamUrl ?: resolveSlots.withPermit {
                    streamExtractor.extractWithFallback(videoId, preferredDownloadStreamQuality())?.url
                }
                if (streamUrl == null) {
                    withContext(Dispatchers.Main) { onResult(false, "Download failed: stream unavailable") }
                    return@launch
                }

                val request = DownloadRequest.Builder(videoId, Uri.parse(streamUrl))
                    .setCustomCacheKey(OfflineDownloadIdentity.cacheKey(videoId, null))
                    .setData(title.toByteArray(Charsets.UTF_8))
                    .build()
                DownloadService.sendAddDownload(context, ExoDownloadService::class.java, request, false)
                DownloadService.sendResumeDownloads(context, ExoDownloadService::class.java, false)
                withContext(Dispatchers.Main) { onResult(true, "Download queued") }
            } catch (e: Exception) {
                Timber.w(e, "Failed to queue download for %s", videoId)
                withContext(Dispatchers.Main) { onResult(false, "Download failed: stream unavailable") }
            } finally {
                resolvingIds.remove(videoId)
            }
        }
    }

    private fun isConnectedToWifi(): Boolean {
        val manager = context.getSystemService(ConnectivityManager::class.java) ?: return false
        val network = manager.activeNetwork ?: return false
        return manager.getNetworkCapabilities(network)
            ?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
    }

    private fun availableDownloadStorageBytes(): Long = runCatching {
        StatFs(context.filesDir.absolutePath).availableBytes.coerceAtLeast(0L)
    }.getOrDefault(0L)

    /** Returns a completed download only when its persistent cache is byte-complete. */
    fun findPlayableDownload(downloadId: String): Download? {
        val download = try {
            downloadManager.downloadIndex.getDownload(downloadId)
        } catch (error: Exception) {
            Timber.w(error, "Unable to read download index for %s", downloadId)
            null
        }
        return download?.takeIf(::isPlayable)
    }

    /**
     * Used by the player data source after Media3 has supplied the custom cache key. Legacy
     * requests with a distinct custom key are supported by the fallback index scan.
     */
    fun isPlayableCacheKey(cacheKey: String): Boolean {
        val direct = try {
            downloadManager.downloadIndex.getDownload(cacheKey)
        } catch (error: Exception) {
            Timber.w(error, "Unable to read download index for cache key %s", cacheKey)
            null
        }
        if (direct != null && OfflineDownloadIdentity.cacheKey(direct.request.id, direct.request.customCacheKey) == cacheKey) {
            return isPlayable(direct)
        }

        val cursor = try {
            downloadManager.downloadIndex.getDownloads(Download.STATE_COMPLETED)
        } catch (error: Exception) {
            Timber.w(error, "Unable to scan completed downloads for cache key %s", cacheKey)
            return false
        }
        return try {
            while (cursor.moveToNext()) {
                val download = cursor.download
                if (OfflineDownloadIdentity.cacheKey(download.request.id, download.request.customCacheKey) == cacheKey) {
                    return isPlayable(download)
                }
            }
            false
        } finally {
            cursor.close()
        }
    }

    /** Reusable playability gate for the downloads UI, queueing, and resolver. */
    fun isPlayable(download: Download): Boolean {
        val playable = hasCompleteCache(download)
        if (!playable && download.state == Download.STATE_COMPLETED) {
            repairStaleCompletedDownload(download, "completed entry failed byte-completeness check")
        }
        return playable
    }

    private fun hasCompleteCache(download: Download): Boolean {
        if (download.state != Download.STATE_COMPLETED) return false
        val cacheKey = OfflineDownloadIdentity.cacheKey(download.request.id, download.request.customCacheKey)
        return try {
            val metadataLength = ContentMetadata.getContentLength(downloadCache.getContentMetadata(cacheKey))
            val expectedLength = listOf(download.contentLength, metadataLength)
                .filter { it > 0 }
                .maxOrNull()
                ?: return false
            val cachedPrefixLength = downloadCache.getCachedLength(cacheKey, 0, expectedLength)
            OfflinePlaybackCacheRouting.isFullyCached(
                isCompleted = true,
                expectedContentLength = expectedLength,
                cachedPrefixLength = cachedPrefixLength,
            )
        } catch (error: Exception) {
            Timber.w(error, "Unable to validate download cache for %s", download.request.id)
            false
        }
    }

    /**
     * A terminal index record without all of its bytes is stale. Remove its cache resource and
     * index entry, then mark the Room row unavailable so it cannot keep being offered offline.
     */
    private fun repairStaleCompletedDownload(download: Download, reason: String) {
        if (!staleRepairIds.add(download.request.id)) return
        downloadScope.launch {
            try {
                val cacheKey = OfflineDownloadIdentity.cacheKey(download.request.id, download.request.customCacheKey)
                Timber.w("Repairing stale completed download %s: %s", download.request.id, reason)
                try {
                    downloadCache.removeResource(cacheKey)
                } catch (error: Exception) {
                    Timber.w(error, "Unable to clear stale cache for %s", download.request.id)
                }
                try {
                    database.withTransaction {
                        updateDownloadState(download.request.id, state = 0, downloadedAt = null)
                        refreshDownloadedPlaylists(download.request.id)
                    }
                } catch (error: Exception) {
                    Timber.w(error, "Unable to mark stale download unavailable for %s", download.request.id)
                }
                try {
                    DownloadService.sendRemoveDownload(context, ExoDownloadService::class.java, download.request.id, false)
                } catch (error: Exception) {
                    Timber.w(error, "Unable to remove stale download index entry for %s", download.request.id)
                }
            } finally {
                staleRepairIds.remove(download.request.id)
            }
        }
    }
}

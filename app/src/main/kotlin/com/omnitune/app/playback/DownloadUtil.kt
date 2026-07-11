/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.playback

import android.content.Context
import android.net.Uri
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import com.omnitune.app.backup.OfflineDownloadArchive
import com.omnitune.app.data.StreamExtractor
import com.omnitune.app.db.MusicDatabase
import com.omnitune.app.models.StreamQuality
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

    val databaseProvider by lazy {
        StandaloneDatabaseProvider(context)
    }

    val downloadCache: SimpleCache by lazy {
        val downloadDir = OfflineDownloadArchive.downloadDirectory(context)
        if (!downloadDir.exists()) downloadDir.mkdirs()
        SimpleCache(downloadDir, NoOpCacheEvictor(), databaseProvider)
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
            maxParallelDownloads = 8
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
                val state = when {
                    removed || current == null || current.state == Download.STATE_REMOVING -> 0
                    current.state == Download.STATE_COMPLETED -> 2
                    current.state == Download.STATE_FAILED -> 3
                    else -> 1
                }
                database.withTransaction {
                    updateDownloadState(
                        songId = download.request.id,
                        state = state,
                        downloadedAt = if (state == 2) java.time.LocalDateTime.now() else null,
                    )
                    refreshDownloadedPlaylists(download.request.id)
                }
            }
        }
    }

    fun release() {
        downloadScope.cancel()
        try {
            downloadCache.release()
        } catch (e: Exception) {
            Timber.w(e, "Error releasing caches")
        }
    }

    fun enqueue(
        videoId: String,
        title: String,
        resolvedStreamUrl: String? = null,
        onResult: (success: Boolean, message: String) -> Unit = { _, _ -> },
    ) {
        if (videoId.isBlank()) {
            onResult(false, "Download failed: invalid song")
            return
        }
        if (!resolvingIds.add(videoId)) {
            onResult(true, "Download already starting")
            return
        }

        downloadScope.launch {
            try {
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
                    streamExtractor.extractWithFallback(videoId, StreamQuality.BEST)?.url
                }
                if (streamUrl == null) {
                    withContext(Dispatchers.Main) { onResult(false, "Download failed: stream unavailable") }
                    return@launch
                }

                val request = DownloadRequest.Builder(videoId, Uri.parse(streamUrl))
                    .setCustomCacheKey(videoId)
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

    /**
     * Reusable playability gate that checks state and safely verifies cache presence.
     */
    fun isPlayable(download: androidx.media3.exoplayer.offline.Download): Boolean {
        Timber.i("Diagnostics: Playback request for download ${download.request.id}, state: ${download.state}")
        
        if (download.state == androidx.media3.exoplayer.offline.Download.STATE_REMOVING) {
            Timber.w("Diagnostics: Playback rejected (Removing)")
            return false
        }
        if (download.state != androidx.media3.exoplayer.offline.Download.STATE_COMPLETED) {
            Timber.w("Diagnostics: Playback rejected (Not Completed: ${download.state})")
            return false
        }
        
        // Safe cache detection without brittle file-path assumptions
        val cachedSpans = downloadCache.getCachedSpans(download.request.id)
        if (cachedSpans.isEmpty()) {
            Timber.e("Diagnostics: Playback failed (Missing Cache)")
            return false
        }
        
        Timber.i("Diagnostics: Playback approved")
        return true
    }
}

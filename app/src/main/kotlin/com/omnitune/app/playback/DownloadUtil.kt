/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.playback

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.io.File
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
) {
    val databaseProvider by lazy {
        StandaloneDatabaseProvider(context)
    }

    val downloadCache: SimpleCache by lazy {
        val downloadDir = File(context.filesDir, "downloads")
        if (!downloadDir.exists()) downloadDir.mkdirs()
        SimpleCache(downloadDir, NoOpCacheEvictor(), databaseProvider)
    }

    // Cache for streaming (separate from download cache)
    val playerCache: SimpleCache by lazy {
        val cacheDir = File(context.cacheDir, "player_cache")
        if (!cacheDir.exists()) cacheDir.mkdirs()
        SimpleCache(
            cacheDir,
            LeastRecentlyUsedCacheEvictor(512L * 1024 * 1024), // 512MB LRU
            databaseProvider
        )
    }

    val downloadManager: androidx.media3.exoplayer.offline.DownloadManager by lazy {
        val dataSourceFactory = androidx.media3.datasource.okhttp.OkHttpDataSource.Factory(okHttpClient)
            .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36 OmniTune")
        val executor = java.util.concurrent.Executors.newFixedThreadPool(4)
        androidx.media3.exoplayer.offline.DownloadManager(
            context,
            databaseProvider,
            downloadCache,
            dataSourceFactory,
            executor
        ).apply {
            maxParallelDownloads = 3
        }
    }

    fun release() {
        try {
            downloadCache.release()
            playerCache.release()
        } catch (e: Exception) {
            Timber.w(e, "Error releasing caches")
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

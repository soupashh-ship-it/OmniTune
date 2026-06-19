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
) {
    private val databaseProvider by lazy {
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

    fun release() {
        try {
            downloadCache.release()
            playerCache.release()
        } catch (e: Exception) {
            Timber.w(e, "Error releasing caches")
        }
    }
}

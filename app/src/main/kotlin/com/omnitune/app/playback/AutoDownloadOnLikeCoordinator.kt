/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.playback

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.Download
import com.omnitune.app.constants.AutoDownloadOnLikeKey
import com.omnitune.app.db.MusicDatabase
import com.omnitune.app.utils.dataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Watches the liked-songs table and enqueues downloads for newly liked songs while the
 * auto-download-on-like preference is enabled.
 *
 * Extracted from MusicService as part of the playback coordinator decomposition; the service
 * only starts/stops this observer instead of owning its state machine. See
 * docs/architecture/music-service-decomposition-plan.md for the remaining extraction steps.
 */
@UnstableApi
class AutoDownloadOnLikeCoordinator(
    private val preferences: Flow<Preferences>,
    private val database: MusicDatabase,
    private val downloadUtil: DownloadUtil,
    private val scope: kotlinx.coroutines.CoroutineScope,
) {
    constructor(
        context: Context,
        database: MusicDatabase,
        downloadUtil: DownloadUtil,
        scope: kotlinx.coroutines.CoroutineScope,
    ) : this(
        preferences = context.applicationContext.dataStore.data,
        database = database,
        downloadUtil = downloadUtil,
        scope = scope,
    )

    private var job: Job? = null

    fun start() {
        stop()
        job = scope.launch(Dispatchers.IO) {
            var knownLikedIds = emptySet<String>()
            var enabledLastEmission = false

            combine(
                preferences.map { it[AutoDownloadOnLikeKey] ?: false }.distinctUntilChanged(),
                database.likedSongsByRowIdAsc(),
            ) { enabled, songs -> enabled to songs }
                .collect { (enabled, songs) ->
                    val likedIds = songs.map { it.song.id }.toSet()
                    if (!enabled) {
                        knownLikedIds = likedIds
                        enabledLastEmission = false
                        return@collect
                    }
                    if (!enabledLastEmission) {
                        // First emission after enabling is the baseline, not a batch of new likes.
                        knownLikedIds = likedIds
                        enabledLastEmission = true
                        return@collect
                    }

                    songs
                        .filter { it.song.id !in knownLikedIds }
                        .forEach { song -> queueAutoDownload(song.song.id, song.song.title) }
                    knownLikedIds = likedIds
                }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }

    private fun queueAutoDownload(mediaId: String, title: String) {
        try {
            val existing = downloadUtil.downloadManager.downloadIndex.getDownload(mediaId)
            if (existing != null && existing.state != Download.STATE_FAILED) return

            downloadUtil.enqueue(mediaId, title)
        } catch (e: Exception) {
            Timber.tag("MusicService").w(e, "Failed to auto-download liked song $mediaId")
        }
    }
}

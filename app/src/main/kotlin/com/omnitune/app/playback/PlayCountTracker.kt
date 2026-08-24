/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.playback

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.media3.common.Player
import androidx.media3.common.C
import com.omnitune.app.constants.ScrobbleDelayPercentKey
import com.omnitune.app.constants.ScrobbleDelaySecondsKey
import com.omnitune.app.constants.ScrobbleMinSongDurationKey
import com.omnitune.app.db.MusicDatabase
import com.omnitune.app.models.MediaMetadata
import com.omnitune.app.utils.dataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Watches playback position and, once a track crosses the user's scrobble/play-count threshold,
 * records exactly one play-count increment and hands off to [ScrobblingManager].
 *
 * Extracted from MusicService as part of the playback coordinator decomposition. All Player
 * access is marshalled to the main thread exactly as before the extraction.
 */
class PlayCountTracker(
    private val player: Player,
    private val database: MusicDatabase,
    private val scrobblingManager: ScrobblingManager,
    private val currentMetadataProvider: () -> MediaMetadata?,
    private val preferences: Flow<Preferences>,
    private val scope: CoroutineScope,
) {
    constructor(
        context: Context,
        player: Player,
        database: MusicDatabase,
        scrobblingManager: ScrobblingManager,
        currentMetadataProvider: () -> MediaMetadata?,
        scope: CoroutineScope,
    ) : this(
        player = player,
        database = database,
        scrobblingManager = scrobblingManager,
        currentMetadataProvider = currentMetadataProvider,
        preferences = context.applicationContext.dataStore.data,
        scope = scope,
    )

    private var job: Job? = null
    private var lastRecordedMediaId: String? = null

    fun startFor(mediaItem: androidx.media3.common.MediaItem?) {
        job?.cancel()
        val mediaId = mediaItem?.mediaId ?: return
        if (mediaId == lastRecordedMediaId) return

        job = scope.launch(Dispatchers.IO) {
            var durationMs = withContext(Dispatchers.Main) { player.duration }
            while (durationMs == C.TIME_UNSET || durationMs <= 0L) {
                delay(1000)
                if (!isActive) return@launch
                durationMs = withContext(Dispatchers.Main) { player.duration }
            }

            val minimumDurationMs = (preferences.map { it[ScrobbleMinSongDurationKey] ?: 30 }.first() * 1000L)
            if (durationMs < minimumDurationMs) return@launch // Skip very short tracks
            val delayPercent = preferences.map { it[ScrobbleDelayPercentKey] ?: 50f }.first()
            val delaySeconds = preferences.map { it[ScrobbleDelaySecondsKey] ?: 30 }.first()

            // Threshold is the minimum of (delayPercent %) or (delaySeconds), clamped to 10s min
            val thresholdMs = minOf((durationMs * delayPercent / 100f).toLong(), delaySeconds * 1000L).coerceAtLeast(10_000L)

            while (isActive) {
                val (currentPos, currentId) = withContext(Dispatchers.Main) {
                    Pair(player.currentPosition, player.currentMediaItem?.mediaId)
                }

                if (currentId == mediaId) {
                    if (currentPos >= thresholdMs) {
                        Timber.tag("MusicService").d("Recording play count for $mediaId")
                        database.incrementPlayCount(mediaId)
                        // Submit scrobble
                        val scrobbleMeta = currentMetadataProvider()
                        if (scrobbleMeta != null) {
                            scrobblingManager.onScrobbleThreshold(
                                title = scrobbleMeta.title,
                                artist = scrobbleMeta.artists.firstOrNull()?.name ?: "",
                                album = scrobbleMeta.album?.title,
                                durationMs = durationMs,
                            )
                        }
                        lastRecordedMediaId = mediaId
                        break
                    }
                } else {
                    break
                }

                delay(1000L)
            }
        }
    }

    fun cancel() {
        job?.cancel()
        job = null
    }
}

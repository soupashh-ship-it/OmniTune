/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.playback

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.media3.common.MediaItem
import com.omnitune.app.constants.HistoryDuration
import com.omnitune.app.db.MusicDatabase
import com.omnitune.app.db.entities.SongSkipEntity
import com.omnitune.app.extensions.metadata
import com.omnitune.app.models.MediaMetadata
import com.omnitune.app.playback.continuation.PlaybackSourceType
import com.omnitune.app.playback.continuation.TasteSignal
import com.omnitune.app.playback.continuation.TasteSignalClassifier
import com.omnitune.app.utils.dataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Outcome of closing a taste window: the classified signal plus the metadata of the track the
 * window belonged to, so callers can apply feature-specific side effects (autoplay seeding,
 * skip counters) without re-deriving them.
 */
data class TasteWindowResult(
    val signal: TasteSignal,
    val windowMetadata: MediaMetadata?,
)

/**
 * Owns the "listening window" state machine: one window per track transition, closed when the
 * track is skipped or completes. Classifies each window into a [TasteSignal] and persists a
 * listening event for meaningful listens.
 *
 * Extracted from MusicService as part of the playback coordinator decomposition (see
 * docs/architecture/music-service-decomposition-plan.md). The service passes main-thread
 * Player snapshots in; this class never touches the player itself.
 */
class TasteSignalRecorder(
    private val database: MusicDatabase,
    private val preferences: Flow<Preferences>,
    private val scope: CoroutineScope,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    constructor(
        context: Context,
        database: MusicDatabase,
        scope: CoroutineScope,
        clock: () -> Long = System::currentTimeMillis,
    ) : this(
        preferences = context.applicationContext.dataStore.data,
        database = database,
        scope = scope,
        clock = clock,
    )

    private var windowMediaId: String? = null
    private var windowMetadata: MediaMetadata? = null
    private var windowStartedAtMs: Long = 0L
    private var windowSourceType: PlaybackSourceType = PlaybackSourceType.UNKNOWN
    private var windowDurationMs: Long? = null

    fun hasOpenWindow(): Boolean = windowMediaId != null

    /**
     * Opens a new listening window for [mediaItem]. Call on every media item transition.
     * [playerDurationMs] should be `player.duration` when known, or null if unset.
     */
    fun beginWindow(
        mediaItem: MediaItem?,
        sourceType: PlaybackSourceType,
        playerDurationMs: Long?,
    ) {
        val meta = mediaItem?.metadata
        windowMediaId = mediaItem?.mediaId
        windowMetadata = meta
        windowStartedAtMs = clock()
        windowSourceType = sourceType
        windowDurationMs = meta?.duration
            ?.takeIf { it > 0 }
            ?.let { it * 1000L }
            ?: playerDurationMs.takeIf { it != null && it > 0L }
    }

    /**
     * Closes the open window, records the listening event when warranted, and returns the
     * classified outcome. Returns null when no window was open.
     *
     * [currentPlayerMediaId]/[playerPositionMs] carry a main-thread Player snapshot; the
     * position is only trusted while the player still sits on the same item as the open
     * window, otherwise wall-clock elapsed time is used.
     */
    fun endWindow(
        completed: Boolean,
        currentPlayerMediaId: String?,
        playerPositionMs: Long?,
    ): TasteWindowResult? {
        val mediaId = windowMediaId ?: return null
        val sourceType = windowSourceType
        val wallClockListenedMs = clock() - windowStartedAtMs
        val listenedMs = playerPositionMs
            ?.takeIf { currentPlayerMediaId == mediaId && it > 0L }
            ?: wallClockListenedMs
        val durationMs = windowDurationMs
        val actualListenedMs = durationMs
            ?.takeIf { it > 0L }
            ?.let { listenedMs.coerceAtMost(it) }
            ?: listenedMs
        val positive = TasteSignalClassifier.isPositiveListen(listenedMs, durationMs)
        val skippedQuickly = TasteSignalClassifier.isQuickSkip(listenedMs, completed)
        val signal = TasteSignal(
            songId = mediaId,
            sourceType = sourceType,
            listenedMillis = actualListenedMs,
            durationMillis = durationMs,
            completed = completed,
            skippedQuickly = skippedQuickly,
            positive = positive,
        )

        recordListeningEventIfNeeded(
            mediaId = mediaId,
            metadata = windowMetadata,
            listenedMs = actualListenedMs,
            positive = positive,
            completed = completed,
        )

        val windowMeta = windowMetadata
        clearWindow()
        return TasteWindowResult(signal = signal, windowMetadata = windowMeta)
    }

    private fun clearWindow() {
        windowMediaId = null
        windowMetadata = null
        windowStartedAtMs = 0L
        windowDurationMs = null
    }

    private fun recordListeningEventIfNeeded(
        mediaId: String,
        metadata: MediaMetadata?,
        listenedMs: Long,
        positive: Boolean,
        completed: Boolean,
    ) {
        if (listenedMs < MIN_LISTEN_HISTORY_MS) return
        if (!positive && !completed) return

        // Snapshot the metadata before any suspension point clears the window state.
        val eventMetadata = metadata
        scope.launch(Dispatchers.IO) {
            try {
                eventMetadata?.let { database.insert(it) }
                database.insertRecentEvent(mediaId, listenedMs)
                val historyDays = preferences.first()[HistoryDuration] ?: 30f
                if (historyDays > 0f) {
                    val cutoff = clock() - historyDays.toLong().coerceAtLeast(1L) * 86_400_000L
                    database.deleteEventsBefore(cutoff)
                }
                database.incrementTotalPlayTime(mediaId, listenedMs)
                Timber.tag("OmniTuneRecent").d("Recorded listening event: mediaId=$mediaId listenedMs=$listenedMs")
            } catch (e: Exception) {
                Timber.tag("OmniTuneRecent").w(e, "Failed to record listening event for $mediaId")
            }
        }
    }

    companion object {
        const val MIN_LISTEN_HISTORY_MS = 10_000L
    }
}

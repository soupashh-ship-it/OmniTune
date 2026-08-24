/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.playback.continuation

import androidx.datastore.preferences.core.Preferences
import androidx.media3.common.MediaItem
import com.omnitune.app.constants.AutoplaySimilarSongsKey
import com.omnitune.app.constants.RestrictExplicitContentKey
import com.omnitune.app.db.MusicDatabase
import com.omnitune.app.db.entities.SongSkipEntity
import com.omnitune.app.extensions.metadata
import com.omnitune.app.models.MediaMetadata
import com.omnitune.app.playback.TasteWindowResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Owns the autoplay continuation state (recently-played ring buffer, failed-candidate set,
 * positive-autoplay seed) and drives the candidate selection loop.
 *
 * Extracted from MusicService as part of the playback coordinator decomposition (see
 * docs/architecture/music-service-decomposition-plan.md). Stream resolution and the actual
 * queue hand-off are injected so this class stays testable and free of Player coupling.
 * Liked-songs looping remains in the service because it is pure player mutation.
 */
class AutoplayContinuationManager(
    private val database: MusicDatabase,
    private val preferences: Flow<Preferences>,
    private val autoplayResolver: AutoplayRecommendationResolver,
    private val scope: CoroutineScope,
) {
    private val recentlyAutoplayedIds = ArrayDeque<String>()
    private val failedAutoplayCandidateIds = linkedSetOf<String>()
    private var lastPositiveAutoplaySeed: MediaMetadata? = null

    /** Clears transient state when a brand-new queue replaces the current one. */
    fun clearTransientState() {
        failedAutoplayCandidateIds.clear()
        recentlyAutoplayedIds.clear()
    }

    /**
     * Forgets failed candidates only (new queue), keeping the recently-played ring buffer so
     * already-heard tracks are still not re-suggested.
     */
    fun clearFailedCandidates() {
        failedAutoplayCandidateIds.clear()
    }

    /**
     * Applies feature side effects of a closed taste window: positive autoplay results become
     * the next radio seed; quick skips mark the candidate failed and bump its skip counter.
     */
    fun onTasteWindowResult(result: TasteWindowResult) {
        val signal = result.signal
        if (signal.sourceType != PlaybackSourceType.AUTOPLAY_RADIO) return

        if (signal.positive && result.windowMetadata != null) {
            lastPositiveAutoplaySeed = result.windowMetadata
            Timber.tag("OmniTuneContinuation").i("Positive autoplay signal for ${signal.songId} after ${signal.listenedMillis}ms")
        }
        if (signal.skippedQuickly) {
            failedAutoplayCandidateIds.add(signal.songId)
            recordSkip(signal.songId)
            Timber.tag("OmniTuneContinuation").i("Quick skip signal for autoplay candidate ${signal.songId}")
        }
    }

    fun remember(mediaId: String) {
        if (mediaId.isBlank()) return
        recentlyAutoplayedIds.addLast(mediaId)
        while (recentlyAutoplayedIds.size > MAX_RECENT_AUTOPLAY_IDS) {
            recentlyAutoplayedIds.removeFirst()
        }
    }

    /**
     * Selects and starts the next autoplay track. Returns false immediately when autoplay is
     * disabled or not allowed for [playbackContext].
     *
     * [resolve] performs stream resolution for a candidate (returns null when unresolvable);
     * [startRadio] hands the resolved item to the service queue pipeline.
     */
    suspend fun continueWithAutoplay(
        playbackContext: PlaybackContext,
        hasNextItem: Boolean,
        seedFallbacks: List<MediaMetadata?>,
        resolve: suspend (AutoplayCandidate) -> MediaItem?,
        startRadio: (seedTrack: MediaMetadata, resolved: MediaItem) -> Unit,
    ) {
        val prefs = preferences.first()
        val autoplayEnabled = prefs[AutoplaySimilarSongsKey] ?: true
        if (!PlaybackContinuationPolicy.shouldRunAutoplay(
                autoplayEnabled = autoplayEnabled,
                playbackContext = playbackContext,
                hasNextItem = hasNextItem,
            )
        ) {
            Timber.tag("OmniTuneContinuation").i("Autoplay skipped: enabled=$autoplayEnabled context=${playbackContext.sourceType}")
            return
        }

        val currentTrack = lastPositiveAutoplaySeed
            ?: seedFallbacks.firstOrNull { it != null }
            ?: run {
                Timber.tag("OmniTuneContinuation").i("Autoplay stopped: no seed metadata")
                return
            }
        val recentlyPlayedIds = recentlyAutoplayedIds.toSet()

        repeat(AutoplayRetryPolicy.MAX_STREAM_RESOLUTION_ATTEMPTS) {
            val candidate = withContext(Dispatchers.IO) {
                autoplayResolver.getNextAutoplayCandidate(
                    currentTrack = currentTrack,
                    playbackContext = playbackContext,
                    recentlyPlayedIds = recentlyPlayedIds,
                    failedCandidateIds = failedAutoplayCandidateIds,
                )
            } ?: run {
                Timber.tag("OmniTuneContinuation").i("Autoplay stopped: no valid candidate")
                return
            }
            if ((prefs[RestrictExplicitContentKey] ?: false) &&
                candidate.mediaItem.metadata?.explicit == true
            ) {
                failedAutoplayCandidateIds.add(candidate.mediaItem.mediaId)
                Timber.tag("OmniTuneContinuation").i(
                    "Autoplay skipped explicit candidate ${candidate.mediaItem.mediaId}",
                )
                return@repeat
            }

            val resolved = resolve(candidate)
            if (resolved != null) {
                remember(resolved.mediaId)
                startRadio(currentTrack, resolved)
                Timber.tag("OmniTuneContinuation").i(
                    "Autoplay selected ${resolved.mediaId} via ${candidate.source}: ${candidate.reason}",
                )
                return
            }

            failedAutoplayCandidateIds.add(candidate.mediaItem.mediaId)
            Timber.tag("OmniTuneContinuation").w(
                "Autoplay candidate failed stream resolution: ${candidate.mediaItem.mediaId} via ${candidate.source}",
            )
        }

        Timber.tag("OmniTuneContinuation").w(
            "Autoplay stopped after ${AutoplayRetryPolicy.MAX_STREAM_RESOLUTION_ATTEMPTS} failed candidates",
        )
    }

    private fun recordSkip(mediaId: String) {
        scope.launch(Dispatchers.IO) {
            try {
                val existing = database.getSkip(mediaId)
                database.upsertSkip(
                    existing?.copy(
                        skipCount = existing.skipCount + 1,
                        lastSkippedAt = System.currentTimeMillis(),
                    ) ?: SongSkipEntity(songId = mediaId, skipCount = 1),
                )
            } catch (e: Exception) {
                Timber.tag("OmniTuneContinuation").w(e, "Failed to record skip for $mediaId")
            }
        }
    }

    companion object {
        const val MAX_RECENT_AUTOPLAY_IDS = 40
    }
}

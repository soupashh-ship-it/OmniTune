/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.playback

import androidx.datastore.preferences.core.Preferences
import androidx.media3.common.Player
import com.omnitune.app.constants.SponsorBlockEnabledKey
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

internal class SponsorBlockPlaybackCoordinator(
    private val player: Player,
    private val preferences: Flow<Preferences>,
    private val client: SponsorBlockApiClient,
    private val scope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : Player.Listener {
    private var preferenceJob: Job? = null
    private var fetchJob: Job? = null
    private var monitorJob: Job? = null
    private var enabled = false
    private var currentVideoId: String? = null
    private var segments: List<SponsorBlockSegment> = emptyList()
    private var loadGeneration = 0

    fun start() {
        if (preferenceJob?.isActive == true) return

        player.addListener(this)
        preferenceJob = scope.launch {
            preferences
                .map { it[SponsorBlockEnabledKey] ?: true }
                .distinctUntilChanged()
                .collect { isEnabled ->
                    enabled = isEnabled
                    if (isEnabled) {
                        loadSegmentsFor(player.currentMediaItem)
                    } else {
                        clearActiveSegments()
                    }
                    updateMonitor()
                }
        }
    }

    fun stop() {
        player.removeListener(this)
        preferenceJob?.cancel()
        fetchJob?.cancel()
        monitorJob?.cancel()
        preferenceJob = null
        fetchJob = null
        monitorJob = null
        segments = emptyList()
        currentVideoId = null
    }

    override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
        loadSegmentsFor(mediaItem)
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        updateMonitor()
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        updateMonitor()
    }

    private fun loadSegmentsFor(mediaItem: androidx.media3.common.MediaItem?) {
        fetchJob?.cancel()
        segments = emptyList()
        updateMonitor()

        val videoId = SponsorBlockVideoIdResolver.fromMediaItem(mediaItem)
        currentVideoId = videoId
        loadGeneration += 1

        if (!enabled || videoId == null) return

        val requestGeneration = loadGeneration
        fetchJob = scope.launch {
            val loadedSegments = withContext(ioDispatcher) {
                client.fetchSkipSegments(videoId)
            }
            if (requestGeneration != loadGeneration || currentVideoId != videoId || !enabled) {
                return@launch
            }
            segments = loadedSegments
            if (loadedSegments.isNotEmpty()) {
                Timber.tag("SponsorBlock").d(
                    "Loaded %s skip segments for %s",
                    loadedSegments.size,
                    videoId,
                )
            }
            maybeSkipCurrentSegment()
            updateMonitor()
        }
    }

    private fun clearActiveSegments() {
        fetchJob?.cancel()
        fetchJob = null
        loadGeneration += 1
        segments = emptyList()
        currentVideoId = null
        monitorJob?.cancel()
        monitorJob = null
    }

    private fun updateMonitor() {
        val shouldMonitor = enabled &&
            segments.isNotEmpty() &&
            player.isPlaying &&
            player.playbackState == Player.STATE_READY

        if (!shouldMonitor) {
            monitorJob?.cancel()
            monitorJob = null
            return
        }

        if (monitorJob?.isActive == true) return
        monitorJob = scope.launch {
            while (isActive) {
                maybeSkipCurrentSegment()
                delay(PositionCheckIntervalMs)
            }
        }
    }

    private fun maybeSkipCurrentSegment() {
        val videoId = SponsorBlockVideoIdResolver.fromMediaItem(player.currentMediaItem)
        if (videoId != currentVideoId) {
            loadSegmentsFor(player.currentMediaItem)
            return
        }
        if (!player.isCommandAvailable(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM)) return

        val targetMs = SponsorBlockSkipPolicy.seekTargetMs(
            segments = segments,
            positionMs = player.currentPosition,
            durationMs = player.duration,
        ) ?: return

        player.seekTo(targetMs)
        Timber.tag("SponsorBlock").i("Skipped segment for %s to %sms", currentVideoId, targetMs)
    }

    private companion object {
        const val PositionCheckIntervalMs = 500L
    }
}

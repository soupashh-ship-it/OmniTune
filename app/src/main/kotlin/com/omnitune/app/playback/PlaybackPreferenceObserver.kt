/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.playback

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.omnitune.app.constants.AudioCrossfadeDurationKey
import com.omnitune.app.constants.AudioNormalizationKey
import com.omnitune.app.constants.AudioOffload
import com.omnitune.app.constants.AutoSkipNextOnErrorKey
import com.omnitune.app.constants.PlayerVolumeKey
import com.omnitune.app.constants.RepeatModeKey
import com.omnitune.app.constants.ShuffleEnabledKey
import com.omnitune.app.constants.SkipSilenceKey
import com.omnitune.app.constants.SeekExtraSeconds as SeekExtraSecondsKey
import com.omnitune.app.extensions.setOffloadEnabled
import com.omnitune.app.utils.dataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber

class PlaybackPreferenceObserver internal constructor(
    private val preferences: Flow<Preferences>,
    private val player: ExoPlayer,
    private val scope: CoroutineScope,
    private val playerVolume: MutableStateFlow<Float>,
    private val playbackFadeFactor: MutableStateFlow<Float>,
    private val normalizationFactor: StateFlow<Float>,
    private val crossfadeDurationMs: MutableStateFlow<Int>,
    private val audioNormalizationEnabled: MutableStateFlow<Boolean>,
    private val onAutoSkipNextOnErrorChanged: (Boolean) -> Unit,
) {
    constructor(
        context: Context,
        player: ExoPlayer,
        scope: CoroutineScope,
        playerVolume: MutableStateFlow<Float>,
        playbackFadeFactor: MutableStateFlow<Float>,
        normalizationFactor: StateFlow<Float>,
        crossfadeDurationMs: MutableStateFlow<Int>,
        audioNormalizationEnabled: MutableStateFlow<Boolean>,
        onAutoSkipNextOnErrorChanged: (Boolean) -> Unit,
    ) : this(
        preferences = context.applicationContext.dataStore.data,
        player = player,
        scope = scope,
        playerVolume = playerVolume,
        playbackFadeFactor = playbackFadeFactor,
        normalizationFactor = normalizationFactor,
        crossfadeDurationMs = crossfadeDurationMs,
        audioNormalizationEnabled = audioNormalizationEnabled,
        onAutoSkipNextOnErrorChanged = onAutoSkipNextOnErrorChanged,
    )

    private val jobs = mutableListOf<Job>()

    fun start() {
        stop()

        // Skip Silence
        jobs += scope.launch {
            preferences.map { it[SkipSilenceKey] ?: false }.distinctUntilChanged().collect { skipSilence ->
                player.skipSilenceEnabled = skipSilence
                Timber.tag("MusicService").d("Skip silence: $skipSilence")
            }
        }

        // Progressive seek increments (replaces the former blocking read in PlayerFactory)
        jobs += scope.launch {
            preferences.map { it[SeekExtraSecondsKey] ?: false }.distinctUntilChanged().collect { progressiveSeek ->
                player.setSeekBackIncrementMs(if (progressiveSeek) 10_000L else 5_000L)
                player.setSeekForwardIncrementMs(if (progressiveSeek) 15_000L else 10_000L)
                Timber.tag("MusicService").d("Progressive seek: $progressiveSeek")
            }
        }

        // Crossfade needs decoded PCM so both players can mix without an offload transition gap.
        jobs += scope.launch {
            combine(
                preferences.map { it[AudioOffload] ?: false }.distinctUntilChanged(),
                preferences.map { (it[AudioCrossfadeDurationKey] ?: 0) * 1000 }.distinctUntilChanged(),
                preferences.map { it[SkipSilenceKey] ?: false }.distinctUntilChanged(),
            ) { offload, durationMs, skipSilence -> Triple(offload, durationMs, skipSilence) }
                .distinctUntilChanged()
                .collect { (offload, durationMs, skipSilence) ->
                    crossfadeDurationMs.value = durationMs
                    player.setOffloadEnabled(offload && durationMs == 0 && !skipSilence)
                    Timber.tag("MusicService").d(
                        "Audio offload: ${offload && durationMs == 0 && !skipSilence}, crossfade: ${durationMs}ms"
                    )
                }
            }

        // Player Volume
        jobs += scope.launch {
            preferences.map { it[PlayerVolumeKey] ?: 1f }.distinctUntilChanged().collect { volume ->
                setPlayerVolume(volume.coerceIn(0f, 1f))
            }
        }

        // Combine volumes for crossfade + normalization
        jobs += scope.launch {
            combine(playerVolume, playbackFadeFactor, normalizationFactor) { vol, fade, norm ->
                (vol * fade * norm).coerceIn(0f, 1f)
            }.collectLatest { finalVolume ->
                player.volume = finalVolume
            }
        }

        // Repeat Mode
        jobs += scope.launch {
            preferences.map { it[RepeatModeKey] ?: Player.REPEAT_MODE_OFF }.distinctUntilChanged().collect { mode ->
                player.repeatMode = mode
                Timber.tag("MusicService").d("Repeat mode: $mode")
            }
        }

        // Shuffle Mode
        jobs += scope.launch {
            preferences.map { it[ShuffleEnabledKey] ?: false }.distinctUntilChanged().collect { enabled ->
                player.shuffleModeEnabled = enabled
                Timber.tag("OmniTuneQueue").d("Shuffle restored/changed: enabled=$enabled")
            }
        }

        // Audio Normalization
        jobs += scope.launch {
            preferences.map { it[AudioNormalizationKey] ?: true }.distinctUntilChanged().collect { enabled ->
                audioNormalizationEnabled.value = enabled
            }
        }

        // Auto skip on error
        jobs += scope.launch {
            preferences.map { it[AutoSkipNextOnErrorKey] ?: true }.distinctUntilChanged().collect { autoSkip ->
                onAutoSkipNextOnErrorChanged(autoSkip)
            }
        }
    }

    fun stop() {
        jobs.forEach { it.cancel() }
        jobs.clear()
    }

    private fun setPlayerVolume(volume: Float) {
        playerVolume.value = volume
        player.volume = volume
    }
}

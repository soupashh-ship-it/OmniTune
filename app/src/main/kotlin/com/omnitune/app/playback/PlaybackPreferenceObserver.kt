/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.playback

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
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
    private val nextSongPreloadingEnabled: MutableStateFlow<Boolean>,
    private val onAutoSkipNextOnErrorChanged: (Boolean) -> Unit,
    private val onNextSongPreloadingChanged: (Boolean) -> Unit,
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
        nextSongPreloadingEnabled: MutableStateFlow<Boolean>,
        onAutoSkipNextOnErrorChanged: (Boolean) -> Unit,
        onNextSongPreloadingChanged: (Boolean) -> Unit,
    ) : this(
        preferences = context.applicationContext.dataStore.data,
        player = player,
        scope = scope,
        playerVolume = playerVolume,
        playbackFadeFactor = playbackFadeFactor,
        normalizationFactor = normalizationFactor,
        crossfadeDurationMs = crossfadeDurationMs,
        audioNormalizationEnabled = audioNormalizationEnabled,
        nextSongPreloadingEnabled = nextSongPreloadingEnabled,
        onAutoSkipNextOnErrorChanged = onAutoSkipNextOnErrorChanged,
        onNextSongPreloadingChanged = onNextSongPreloadingChanged,
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
            preferences
                .map { PlaybackEnginePreferenceMapper.fromPreferences(it) }
                .distinctUntilChanged()
                .collect { enginePrefs ->
                    crossfadeDurationMs.value = enginePrefs.crossfadeDurationMs
                    nextSongPreloadingEnabled.value = enginePrefs.nextSongPreloadingEnabled
                    onNextSongPreloadingChanged(enginePrefs.nextSongPreloadingEnabled)
                    player.setOffloadEnabled(enginePrefs.shouldEnableAudioOffload)
                    Timber.tag("MusicService").d(
                        "Audio offload: %s, crossfade: %dms, next preload: %s",
                        enginePrefs.shouldEnableAudioOffload,
                        enginePrefs.crossfadeDurationMs,
                        enginePrefs.nextSongPreloadingEnabled,
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
            combine(playerVolume, playbackFadeFactor, normalizationFactor, audioNormalizationEnabled) { vol, fade, norm, normalizationEnabled ->
                (vol * fade * if (normalizationEnabled) norm else 1f).coerceIn(0f, 1f)
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
            preferences
                .map { PlaybackEnginePreferenceMapper.fromPreferences(it).volumeNormalizationEnabled }
                .distinctUntilChanged()
                .collect { enabled ->
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

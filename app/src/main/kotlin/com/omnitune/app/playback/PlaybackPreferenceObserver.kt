/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.playback

import android.content.Context
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
import com.omnitune.app.extensions.setOffloadEnabled
import com.omnitune.app.utils.dataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber

class PlaybackPreferenceObserver(
    context: Context,
    private val player: ExoPlayer,
    private val scope: CoroutineScope,
    private val playerVolume: MutableStateFlow<Float>,
    private val playbackFadeFactor: MutableStateFlow<Float>,
    private val crossfadeDurationMs: MutableStateFlow<Int>,
    private val audioNormalizationEnabled: MutableStateFlow<Boolean>,
    private val onAutoSkipNextOnErrorChanged: (Boolean) -> Unit,
) {
    private val dataStore = context.applicationContext.dataStore
    private val jobs = mutableListOf<Job>()

    fun start() {
        stop()

        // Skip Silence
        jobs += scope.launch {
            dataStore.data.map { it[SkipSilenceKey] ?: false }.distinctUntilChanged().collect { skipSilence ->
                player.skipSilenceEnabled = skipSilence
                Timber.tag("MusicService").d("Skip silence: $skipSilence")
            }
        }

        // Audio Offload
        jobs += scope.launch {
            dataStore.data.map { it[AudioOffload] ?: true }.distinctUntilChanged().collect { offload ->
                player.setOffloadEnabled(offload)
                Timber.tag("MusicService").d("Audio offload: $offload")
            }
        }

        // Player Volume
        jobs += scope.launch {
            dataStore.data.map { it[PlayerVolumeKey] ?: 1f }.distinctUntilChanged().collect { volume ->
                setPlayerVolume(volume.coerceIn(0f, 1f))
            }
        }

        // Combine volumes for crossfade
        jobs += scope.launch {
            combine(playerVolume, playbackFadeFactor) { vol, fade ->
                (vol * fade).coerceIn(0f, 1f)
            }.collectLatest { finalVolume ->
                player.volume = finalVolume
            }
        }

        // Repeat Mode
        jobs += scope.launch {
            dataStore.data.map { it[RepeatModeKey] ?: Player.REPEAT_MODE_OFF }.distinctUntilChanged().collect { mode ->
                player.repeatMode = mode
                Timber.tag("MusicService").d("Repeat mode: $mode")
            }
        }

        // Shuffle Mode
        jobs += scope.launch {
            dataStore.data.map { it[ShuffleEnabledKey] ?: false }.distinctUntilChanged().collect { enabled ->
                player.shuffleModeEnabled = enabled
                Timber.tag("OmniTuneQueue").d("Shuffle restored/changed: enabled=$enabled")
            }
        }

        // Crossfade
        jobs += scope.launch {
            dataStore.data.map { (it[AudioCrossfadeDurationKey] ?: 0) * 1000 }.distinctUntilChanged().collectLatest { durationMs ->
                crossfadeDurationMs.value = durationMs
            }
        }

        // Audio Normalization
        jobs += scope.launch {
            dataStore.data.map { it[AudioNormalizationKey] ?: true }.distinctUntilChanged().collect { enabled ->
                audioNormalizationEnabled.value = enabled
            }
        }

        // Auto skip on error
        jobs += scope.launch {
            dataStore.data.map { it[AutoSkipNextOnErrorKey] ?: true }.distinctUntilChanged().collect { autoSkip ->
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

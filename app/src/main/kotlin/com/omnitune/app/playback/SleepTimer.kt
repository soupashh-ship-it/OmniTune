/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.playback

import androidx.media3.common.Player
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Handles sleep timer logic for OmniTune.
 * Call [start] to begin a countdown. Call [cancel] to abort.
 * When timer expires, [player].pause() is called.
 * If [stopAtEndOfSong] is true, waits for current song to finish before pausing.
 */
class SleepTimer(private val player: Player, private val scope: CoroutineScope) {

    private var timerJob: Job? = null

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()
    private val _remainingMs = MutableStateFlow(0L)
    val remainingMs: StateFlow<Long> = _remainingMs.asStateFlow()

    fun start(durationMs: Long, stopAtEndOfSong: Boolean = false) {
        cancel()
        _isRunning.value = true
        _remainingMs.value = durationMs

        timerJob = scope.launch {
            val startTime = android.os.SystemClock.elapsedRealtime()
            while (_remainingMs.value > 0) {
                delay(1000L)
                _remainingMs.value = (durationMs - (android.os.SystemClock.elapsedRealtime() - startTime))
                    .coerceAtLeast(0L)
            }
            _isRunning.value = false
            if (stopAtEndOfSong) {
                // Let current song finish — attach a one-shot media transition listener
                val listener = object : Player.Listener {
                    override fun onMediaItemTransition(
                        mediaItem: androidx.media3.common.MediaItem?,
                        reason: Int
                    ) {
                        player.pause()
                        player.removeListener(this)
                    }
                }
                player.addListener(listener)
            } else {
                player.pause()
            }
        }
    }

    fun cancel() {
        timerJob?.cancel()
        timerJob = null
        _isRunning.value = false
        _remainingMs.value = 0L
    }
}

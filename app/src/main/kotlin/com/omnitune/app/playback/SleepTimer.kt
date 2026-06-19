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
import kotlinx.coroutines.launch

/**
 * Handles sleep timer logic for OmniTune.
 * Call [start] to begin a countdown. Call [cancel] to abort.
 * When timer expires, [player].pause() is called.
 * If [stopAtEndOfSong] is true, waits for current song to finish before pausing.
 */
class SleepTimer(private val player: Player) {

    private var timerJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    var isRunning: Boolean = false
        private set

    var remainingMs: Long = 0L
        private set

    fun start(durationMs: Long, stopAtEndOfSong: Boolean = false) {
        cancel()
        isRunning = true
        remainingMs = durationMs

        timerJob = scope.launch {
            val startTime = System.currentTimeMillis()
            while (remainingMs > 0) {
                delay(1000L)
                remainingMs = durationMs - (System.currentTimeMillis() - startTime)
                if (remainingMs < 0) remainingMs = 0
            }
            isRunning = false
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
        isRunning = false
        remainingMs = 0L
    }
}

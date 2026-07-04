/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 *
 * Based on Velune Discord integration
 */

package com.omnitune.app.discord

import android.content.Context
import com.omnitune.app.constants.DiscordTokenKey
import com.omnitune.app.constants.EnableDiscordRPCKey
import com.omnitune.app.utils.PreferenceStore
import com.omnitune.app.utils.dataStore
import com.omnitune.kizzy.KizzyLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min

data class SongPresenceData(
    val title: String = "",
    val videoId: String = "",
    val artist: String = "",
    val album: String = "",
    val thumbnail: String = "",
    val artistImage: String = "",
    val position: Long = 0,
    val duration: Long = 0,
    val isPaused: Boolean = false,
)

@Singleton
class DiscordPresenceManager @Inject constructor(
    private val discordRPC: DiscordRPC,
    private val logger: KizzyLogger,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var presenceJob: Job? = null
    private val _started = AtomicBoolean(false)
    private var consecutiveFailures = 0

    private val _lastUpdate = MutableStateFlow(0L)
    val lastUpdate: StateFlow<Long> = _lastUpdate.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private var lastSongData: SongPresenceData? = null
    private var token: String = ""
    private var interval: Long = 20_000L

    companion object {
        private const val MIN_PRESENCE_UPDATE_INTERVAL = 5_000L
        private const val MAX_CONSECUTIVE_FAILURES = 5
    }

    fun start(
        context: Context,
        token: String,
        songProvider: suspend () -> SongPresenceData,
        positionProvider: suspend () -> Long,
        pauseProvider: suspend () -> Boolean,
        intervalProvider: suspend () -> Long,
    ) {
        if (_started.getAndSet(true)) return

        this.token = token
        scope.launch {
            try {
                discordRPC.connect(token)
                _isRunning.value = true
                logger.info("Discord presence manager started")

                while (isActive) {
                    val updateInterval = intervalProvider().coerceAtLeast(MIN_PRESENCE_UPDATE_INTERVAL)
                    try {
                        val song = songProvider()
                        val position = positionProvider()
                        val paused = pauseProvider()

                        if (song.title.isNotBlank()) {
                    discordRPC.updateSong(
                        title = song.title,
                        videoId = song.videoId,
                        artist = song.artist,
                        album = song.album,
                        thumbnail = song.thumbnail,
                        artistImage = song.artistImage,
                        position = position,
                        duration = song.duration,
                        paused = paused,
                    )
                        } else if (lastSongData != null) {
                            discordRPC.stop()
                        }

                        lastSongData = song
                        _lastUpdate.value = System.currentTimeMillis()
                        consecutiveFailures = 0

                        delay(updateInterval)
                    } catch (e: Exception) {
                        consecutiveFailures++
                        logger.warning("Presence update failed ($consecutiveFailures/$MAX_CONSECUTIVE_FAILURES): ${e.message}")

                        if (consecutiveFailures >= MAX_CONSECUTIVE_FAILURES) {
                            logger.severe("Too many consecutive failures, stopping presence manager")
                            stop()
                            delay(60_000L)
                            if (_started.get()) {
                                start(context, token, songProvider, positionProvider, pauseProvider, intervalProvider)
                            }
                            return@launch
                        }

                        delay(min(updateInterval, 30_000L))
                    }
                }
            } catch (e: Exception) {
                logger.severe("Presence manager crashed: ${e.message}")
                _isRunning.value = false
                _started.set(false)
            }
        }
    }

    fun stop() {
        presenceJob?.cancel()
        presenceJob = null
        _started.set(false)
        _isRunning.value = false
        scope.launch {
            discordRPC.close()
        }
        logger.info("Discord presence manager stopped")
    }

    fun restart(
        context: Context,
        songProvider: suspend () -> SongPresenceData,
        positionProvider: suspend () -> Long,
        pauseProvider: suspend () -> Boolean,
        intervalProvider: suspend () -> Long,
    ) {
        if (token.isBlank()) return
        stop()
        start(context, token, songProvider, positionProvider, pauseProvider, intervalProvider)
    }

    fun isStarted(): Boolean = _started.get()

    fun destroy() {
        stop()
        scope.cancel()
    }
}

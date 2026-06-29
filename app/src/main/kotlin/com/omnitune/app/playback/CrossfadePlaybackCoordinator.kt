/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.playback

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.omnitune.app.db.MusicDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import okhttp3.OkHttpClient

class CrossfadePlaybackCoordinator(
    private val context: Context,
    private val player: ExoPlayer,
    private val database: MusicDatabase,
    private val okHttpClient: OkHttpClient,
    private val downloadUtil: DownloadUtil,
    private val crossfadeDurationMs: MutableStateFlow<Int>,
    private val playbackFadeFactor: MutableStateFlow<Float>,
    private val playerVolume: MutableStateFlow<Float>,
    private val audioFocusVolumeFactor: MutableStateFlow<Float>,
    private val audioNormalizationEnabled: MutableStateFlow<Boolean>,
) {
    private var crossfadeAudio: CrossfadeAudio? = null

    fun start(scope: CoroutineScope) {
        if (crossfadeAudio != null) return

        crossfadeAudio = CrossfadeAudio(
            player = player,
            database = database,
            crossfadeDurationMs = crossfadeDurationMs,
            playbackFadeFactor = playbackFadeFactor,
            playerVolume = playerVolume,
            audioFocusVolumeFactor = audioFocusVolumeFactor,
            audioNormalizationEnabled = audioNormalizationEnabled,
            maxSafeGainFactor = 3.16f,
            overlapPlayerFactory = {
                PlayerFactory.createOverlapPlayer(context, okHttpClient, downloadUtil)
            }
        ).also { it.start(scope) }
    }

    fun onPlaybackStateChanged(state: Int) {
        crossfadeAudio?.onPlaybackStateChanged(state)
    }

    fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        crossfadeAudio?.onMediaItemTransition(mediaItem, reason)
    }

    fun release() {
        crossfadeAudio?.release()
        crossfadeAudio = null
    }
}

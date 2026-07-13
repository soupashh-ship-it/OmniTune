/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.playback

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.omnitune.app.data.StreamExtractor
import com.omnitune.app.models.PlaybackQualityMode
import com.omnitune.app.playback.recovery.PlaybackErrorClassifier
import com.omnitune.app.playback.recovery.PlaybackErrorType
import com.omnitune.app.playback.recovery.PlaybackRecoveryPolicy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

class PlaybackRecoveryCoordinator(
    context: Context,
    private val player: ExoPlayer,
    private val scope: CoroutineScope,
    private val streamExtractor: StreamExtractor,
    private val downloadUtil: DownloadUtil,
    private val networkPlaybackMonitor: NetworkPlaybackMonitor,
    private val playbackQualityModeProvider: suspend () -> PlaybackQualityMode,
) {
    private val appContext = context.applicationContext
    private val playbackRecoveryPolicy = PlaybackRecoveryPolicy()
    private var autoSkipNextOnError = true
    private var playbackWatchdogJob: Job? = null

    fun setAutoSkipNextOnError(enabled: Boolean) {
        autoSkipNextOnError = enabled
    }

    fun onPlaybackStateChanged(state: Int) {
        if (state == Player.STATE_READY || state == Player.STATE_ENDED) {
            playbackWatchdogJob?.cancel()
        } else if (state == Player.STATE_BUFFERING && player.playWhenReady) {
            playbackWatchdogJob?.cancel()
            playbackWatchdogJob = scope.launch(Dispatchers.Main) {
                delay(15_000L)
                if (player.playbackState == Player.STATE_BUFFERING && player.playWhenReady) {
                    Timber.tag("MusicService").w("Playback watchdog timeout!")
                    val exception = PlaybackException(
                        "Buffering timeout",
                        null,
                        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT
                    )
                    onPlayerError(exception)
                }
            }
        }
    }

    fun onPlayerError(error: PlaybackException) {
        val errorType = PlaybackErrorClassifier.classify(error)
        val currentMediaItem = player.currentMediaItem
        val mediaId = currentMediaItem?.mediaId

        if (errorType == PlaybackErrorType.NetworkError) {
            if (networkPlaybackMonitor.handleNetworkError(mediaId)) {
                return
            }
        }

        if (errorType == PlaybackErrorType.Forbidden403 ||
            errorType == PlaybackErrorType.NotFound404 ||
            errorType == PlaybackErrorType.BotCheck) {
            if (mediaId != null) {
                StreamUrlResolver.invalidate(mediaId)
            }
        }

        if (mediaId != null && playbackRecoveryPolicy.canRetry(mediaId, errorType)) {
            Timber.tag("MusicService").w("Recovering from $errorType for mediaId: $mediaId")
            playbackRecoveryPolicy.incrementRetry(mediaId)

            // Invalidate caches
            StreamUrlResolver.invalidate(mediaId)
            streamExtractor.invalidate(mediaId)

            scope.launch(Dispatchers.Main) {
                try {
                    // Start from the original item, not the resolved one, so the resolver sees the yt ID
                    val originalItem = currentMediaItem.buildUpon().setUri(mediaId).build()
                    val resolved = withContext(Dispatchers.IO) {
                        StreamUrlResolver.resolveMediaItem(
                            originalItem,
                            streamExtractor,
                            downloadUtil,
                            playbackQualityModeProvider()
                        )
                    }
                    if (resolved != null) {
                        val pos = player.currentPosition
                        val index = player.currentMediaItemIndex
                        player.replaceMediaItem(index, resolved)
                        player.seekTo(index, pos)
                        player.prepare()
                        player.play()
                        return@launch
                    }
                } catch (e: Exception) {
                    Timber.tag("MusicService").e(e, "Failed to resolve during recovery")
                }
                fallbackSkip(errorType)
            }
        } else {
            Timber.tag("MusicService").w("Recovery policy denied retry for $mediaId, error: $errorType")
            fallbackSkip(errorType)
        }
    }

    fun resetRetry(mediaId: String) {
        playbackRecoveryPolicy.resetRetry(mediaId)
    }

    fun release() {
        playbackWatchdogJob?.cancel()
        playbackWatchdogJob = null
    }

    private fun fallbackSkip(errorType: PlaybackErrorType) {
        if (autoSkipNextOnError && errorType.shouldAutoSkipTrack() && player.hasNextMediaItem()) {
            Timber.tag("MusicService").i("Auto-skipping to next track after error")
            player.seekToNextMediaItem()
            player.prepare()
            player.play()
        } else {
            val message = when (errorType) {
                PlaybackErrorType.NetworkError -> "Network error during playback. Please check your connection."
                PlaybackErrorType.Timeout -> "Playback timed out. Please try again."
                else -> "Playback failed after retries."
            }
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(appContext, message, Toast.LENGTH_LONG).show()
            }
        }
    }
}

internal fun PlaybackErrorType.shouldAutoSkipTrack(): Boolean =
    this == PlaybackErrorType.Forbidden403 ||
        this == PlaybackErrorType.NotFound404 ||
        this == PlaybackErrorType.SignatureExpired ||
        this == PlaybackErrorType.Error2000 ||
        this == PlaybackErrorType.BotCheck

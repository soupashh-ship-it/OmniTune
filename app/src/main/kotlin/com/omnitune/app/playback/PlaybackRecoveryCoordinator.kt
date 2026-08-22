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
    private var streamRecoveryJob: Job? = null
    private var streamRecoveryTarget: StreamResolutionTarget? = null

    fun setAutoSkipNextOnError(enabled: Boolean) {
        autoSkipNextOnError = enabled
    }

    fun onPlaybackStateChanged(state: Int) {
        if (state == Player.STATE_READY || state == Player.STATE_ENDED) {
            playbackWatchdogJob?.cancel()
            if (state == Player.STATE_READY) {
                player.currentMediaItem?.mediaId
                    ?.takeIf { it.isNotBlank() }
                    ?.let(streamExtractor::reportPlaybackReady)
            }
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
        streamRecoveryJob?.cancel()
        streamRecoveryJob = null
        streamRecoveryTarget = null
        val errorType = PlaybackErrorClassifier.classify(error)
        val currentMediaItem = player.currentMediaItem ?: return
        val target = StreamResolutionTarget(
            mediaId = currentMediaItem.mediaId,
            mediaItemIndex = player.currentMediaItemIndex,
            resumePositionMs = player.currentPosition.coerceAtLeast(0L),
        )
        val mediaId = target.mediaId

        if (errorType == PlaybackErrorType.NetworkError) {
            if (networkPlaybackMonitor.handleNetworkError(mediaId)) {
                return
            }
        }

        if (errorType == PlaybackErrorType.Forbidden403 ||
            errorType == PlaybackErrorType.NotFound404 ||
            errorType == PlaybackErrorType.BotCheck) {
            StreamUrlResolver.invalidate(mediaId)
        }

        if (playbackRecoveryPolicy.canRetry(mediaId, errorType)) {
            Timber.tag("MusicService").w("Recovering from $errorType for mediaId: $mediaId")
            playbackRecoveryPolicy.incrementRetry(mediaId)

            // Invalidate caches
            StreamUrlResolver.invalidate(mediaId)
            streamExtractor.invalidate(mediaId)
            if (errorType == PlaybackErrorType.Timeout ||
                errorType == PlaybackErrorType.NetworkError ||
                errorType == PlaybackErrorType.Forbidden403 ||
                errorType == PlaybackErrorType.Error2000 ||
                errorType == PlaybackErrorType.SignatureExpired
            ) {
                streamExtractor.reportPlaybackFailure(mediaId)
            }

            streamRecoveryTarget = target
            streamRecoveryJob = scope.launch(Dispatchers.Main) {
                try {
                    // Start from the original item, not the resolved one, so the resolver sees the yt ID
                    val originalItem = currentMediaItem.buildUpon().setUri(target.mediaId).build()
                    val resolved = withContext(Dispatchers.IO) {
                        StreamUrlResolver.resolveMediaItem(
                            originalItem,
                            streamExtractor,
                            downloadUtil,
                            playbackQualityModeProvider()
                        )
                    }
                    if (resolved != null && target.isCurrent(
                            currentMediaId = player.currentMediaItem?.mediaId,
                            currentMediaItemIndex = player.currentMediaItemIndex,
                        )
                    ) {
                        player.replaceMediaItem(target.mediaItemIndex, resolved)
                        player.seekTo(target.mediaItemIndex, target.resumePositionMs)
                        player.prepare()
                        player.play()
                        return@launch
                    } else if (resolved != null) {
                        Timber.tag("MusicService").d("Ignored stale playback recovery")
                        return@launch
                    }
                } catch (cancelled: kotlinx.coroutines.CancellationException) {
                    throw cancelled
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
        if (streamRecoveryTarget?.mediaId != mediaId) {
            streamRecoveryJob?.cancel()
            streamRecoveryJob = null
            streamRecoveryTarget = null
        }
        playbackRecoveryPolicy.resetRetry(mediaId)
    }

    /** Retries only an item that was explicitly paused by an offline failure. */
    fun retryAfterNetworkRestored() {
        if (streamRecoveryJob?.isActive == true || !player.playWhenReady) return
        if (player.currentMediaItem == null) return
        onPlayerError(
            PlaybackException(
                "Network restored",
                null,
                PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
            ),
        )
    }

    fun release() {
        playbackWatchdogJob?.cancel()
        playbackWatchdogJob = null
        streamRecoveryJob?.cancel()
        streamRecoveryJob = null
        streamRecoveryTarget = null
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

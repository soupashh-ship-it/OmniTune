/*
 * OmniTune - based on Velune
 * Nikhil / Kòi Natsuko (github.com/koiverse)
 * Licensed Under GPL-3.0
 */

package com.omnitune.app.playback

import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.Futures
import com.omnitune.app.constants.MediaSessionConstants
import dagger.hilt.android.scopes.ServiceScoped
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import javax.inject.Inject

@ServiceScoped
class MusicSessionCallback @Inject constructor() : MediaLibraryService.MediaLibrarySession.Callback {

    private var player: Player? = null
    var onToggleLike: (() -> Unit)? = null
    var onToggleLibrary: (() -> Unit)? = null
    var onStartRadio: (() -> Unit)? = null

    private val _playbackState = MutableStateFlow(Player.STATE_IDLE)
    val playbackState: StateFlow<Int> = _playbackState.asStateFlow()

    private val _currentMediaItem = MutableStateFlow<MediaItem?>(null)
    val currentMediaItem: StateFlow<MediaItem?> = _currentMediaItem.asStateFlow()

    fun onPlayerReady(player: Player) {
        this.player = player
        player.addListener(
            object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    _playbackState.value = state
                    val label = when (state) {
                        Player.STATE_IDLE -> "IDLE"
                        Player.STATE_BUFFERING -> "BUFFERING"
                        Player.STATE_READY -> "READY"
                        Player.STATE_ENDED -> "ENDED"
                        else -> "UNKNOWN"
                    }
                    Timber.tag("OmniTunePlaybackTrace")
                        .i("Player state: %s", label)
                }

                override fun onMediaItemTransition(
                    mediaItem: MediaItem?,
                    reason: Int,
                ) {
                    _currentMediaItem.value = mediaItem
                    Timber.tag("OmniTunePlaybackTrace")
                        .i("MediaItem transition: id=%s reason=%d",
                            mediaItem?.mediaId, reason)
                }

                override fun onPlayerError(error: PlaybackException) {
                    Timber.tag("OmniTunePlaybackTrace")
                        .e(error, "Player error: code=%d msg=%s",
                            error.errorCode, error.message)
                }

                override fun onPlayWhenReadyChanged(
                    playWhenReady: Boolean,
                    reason: Int,
                ) {
                    Timber.tag("OmniTunePlaybackTrace")
                        .i("PlayWhenReady: %b reason=%d", playWhenReady, reason)
                }
            }
        )
    }

    fun onDestroy() {
        // Listener is garbage-collected with the player release
        player = null
    }

    override fun onConnect(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
    ): MediaSession.ConnectionResult {
        return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
            .setAvailableSessionCommands(
                MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS
                    .buildUpon()
                    .add(MediaSessionConstants.CommandToggleLike)
                    .add(MediaSessionConstants.CommandToggleLibrary)
                    .add(MediaSessionConstants.CommandToggleStartRadio)
                    .add(MediaSessionConstants.CommandToggleShuffle)
                    .add(MediaSessionConstants.CommandToggleRepeatMode)
                    .build()
            )
            .build()
    }

    override fun onAddMediaItems(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
        mediaItems: MutableList<MediaItem>,
    ): ListenableFuture<MutableList<MediaItem>> {
        // Items are enriched at the call site before being sent to the session
        // In-process controllers preserve the tag set by the UI
        return Futures.immediateFuture(mediaItems)
    }

    override fun onCustomCommand(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
        customCommand: SessionCommand,
        args: Bundle,
    ): ListenableFuture<SessionResult> {
        val p = player

        val result = if (p == null) {
            SessionResult(SessionResult.RESULT_ERROR_BAD_VALUE)
        } else {
            when (customCommand.customAction) {
                MediaSessionConstants.ACTION_TOGGLE_LIKE -> {
                    onToggleLike?.invoke()
                    SessionResult(SessionResult.RESULT_SUCCESS, Bundle.EMPTY)
                }

                MediaSessionConstants.ACTION_TOGGLE_LIBRARY -> {
                    onToggleLibrary?.invoke()
                    SessionResult(SessionResult.RESULT_SUCCESS, Bundle.EMPTY)
                }

                MediaSessionConstants.ACTION_TOGGLE_START_RADIO -> {
                    onStartRadio?.invoke()
                    SessionResult(SessionResult.RESULT_SUCCESS, Bundle.EMPTY)
                }

                MediaSessionConstants.ACTION_TOGGLE_SHUFFLE -> {
                    p.shuffleModeEnabled = !p.shuffleModeEnabled
                    SessionResult(SessionResult.RESULT_SUCCESS, Bundle.EMPTY)
                }

                MediaSessionConstants.ACTION_TOGGLE_REPEAT_MODE -> {
                    p.repeatMode = when (p.repeatMode) {
                        Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                        Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                        Player.REPEAT_MODE_ONE -> Player.REPEAT_MODE_OFF
                        else -> Player.REPEAT_MODE_OFF
                    }
                    SessionResult(SessionResult.RESULT_SUCCESS, Bundle.EMPTY)
                }

                else -> {
                    Timber.tag("MediaSession")
                        .w("Unknown custom command: %s", customCommand.customAction)
                    SessionResult(SessionResult.RESULT_ERROR_BAD_VALUE)
                }
            }
        }
        return Futures.immediateFuture(result)
    }
}

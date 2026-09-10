/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.playback

import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaLibraryService.LibraryParams
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionError
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.Futures
import com.omnitune.app.constants.MediaSessionConstants
import com.omnitune.app.db.MusicDatabase
import dagger.hilt.android.scopes.ServiceScoped
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.guava.future
import timber.log.Timber
import javax.inject.Inject

@ServiceScoped
class MusicSessionCallback @Inject constructor() : MediaLibraryService.MediaLibrarySession.Callback {

    private var player: Player? = null
    private var playerListener: Player.Listener? = null
    private var libraryBrowser: MediaLibraryBrowser? = null
    private var serviceScope: CoroutineScope? = null
    private var resolveExternalMediaItems: (suspend (List<MediaItem>) -> List<MediaItem>)? = null
    var onToggleLike: (() -> Unit)? = null
    var onToggleLibrary: (() -> Unit)? = null
    var onStartRadio: (() -> Unit)? = null

    private val _playbackState = MutableStateFlow(Player.STATE_IDLE)
    val playbackState: StateFlow<Int> = _playbackState.asStateFlow()

    private val _currentMediaItem = MutableStateFlow<MediaItem?>(null)
    val currentMediaItem: StateFlow<MediaItem?> = _currentMediaItem.asStateFlow()

    fun onPlayerReady(player: Player) {
        detachPlayerListener()
        this.player = player
        _playbackState.value = player.playbackState
        _currentMediaItem.value = player.currentMediaItem
        playerListener = object : Player.Listener {
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
        }.also(player::addListener)
    }

    fun configureLibrary(
        database: MusicDatabase,
        downloadUtil: DownloadUtil,
        scope: CoroutineScope,
        resolveExternalMediaItems: suspend (List<MediaItem>) -> List<MediaItem>,
    ) {
        libraryBrowser = MediaLibraryBrowser(database, downloadUtil)
        serviceScope = scope
        this.resolveExternalMediaItems = resolveExternalMediaItems
    }

    fun onDestroy() {
        detachPlayerListener()
        player = null
        libraryBrowser = null
        serviceScope = null
        resolveExternalMediaItems = null
        _playbackState.value = Player.STATE_IDLE
        _currentMediaItem.value = null
    }

    private fun detachPlayerListener() {
        playerListener?.let { listener -> player?.removeListener(listener) }
        playerListener = null
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
        val resolver = resolveExternalMediaItems ?: return Futures.immediateFuture(mediaItems)
        val scope = serviceScope ?: return Futures.immediateFuture(mediaItems)
        val requestedItems = mediaItems.toList()
        return scope.future(Dispatchers.IO) {
            runCatching {
                resolver(requestedItems).toMutableList()
            }.getOrElse { error ->
                Timber.tag("MediaSession").w(error, "External media item resolution failed")
                requestedItems.toMutableList()
            }
        }
    }

    override fun onGetLibraryRoot(
        session: MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        params: LibraryParams?,
    ): ListenableFuture<LibraryResult<MediaItem>> {
        return Futures.immediateFuture(
            LibraryResult.ofItem(MediaLibraryBrowser.rootItem(), MediaLibraryBrowser.rootParams(params))
        )
    }

    override fun onGetItem(
        session: MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        mediaId: String,
    ): ListenableFuture<LibraryResult<MediaItem>> {
        val library = libraryBrowser ?: return Futures.immediateFuture(
            LibraryResult.ofError(SessionError.ERROR_NOT_SUPPORTED)
        )
        val scope = serviceScope ?: return Futures.immediateFuture(
            LibraryResult.ofError(SessionError.ERROR_NOT_SUPPORTED)
        )

        return scope.future(Dispatchers.IO) {
            val item = library.item(mediaId)
            if (item != null) {
                LibraryResult.ofItem(item, null)
            } else {
                LibraryResult.ofError(SessionError.ERROR_BAD_VALUE)
            }
        }
    }

    override fun onGetChildren(
        session: MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        parentId: String,
        page: Int,
        pageSize: Int,
        params: LibraryParams?,
    ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
        val library = libraryBrowser ?: return Futures.immediateFuture(
            LibraryResult.ofError(SessionError.ERROR_NOT_SUPPORTED)
        )
        val scope = serviceScope ?: return Futures.immediateFuture(
            LibraryResult.ofError(SessionError.ERROR_NOT_SUPPORTED)
        )

        return scope.future(Dispatchers.IO) {
            val children = library.children(
                parentId = parentId,
                page = page,
                pageSize = pageSize,
                offlineOnly = params?.isOffline == true,
            )
            if (children != null) {
                LibraryResult.ofItemList(ImmutableList.copyOf(children), params)
            } else {
                LibraryResult.ofError(SessionError.ERROR_BAD_VALUE, params)
            }
        }
    }

    override fun onSearch(
        session: MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        query: String,
        params: LibraryParams?,
    ): ListenableFuture<LibraryResult<Void>> {
        val library = libraryBrowser ?: return Futures.immediateFuture(
            LibraryResult.ofError(SessionError.ERROR_NOT_SUPPORTED)
        )
        val scope = serviceScope ?: return Futures.immediateFuture(
            LibraryResult.ofError(SessionError.ERROR_NOT_SUPPORTED)
        )

        return scope.future(Dispatchers.IO) {
            val count = library.searchResultCount(query)
            session.notifySearchResultChanged(browser, query, count, params)
            LibraryResult.ofVoid(params)
        }
    }

    override fun onGetSearchResult(
        session: MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        query: String,
        page: Int,
        pageSize: Int,
        params: LibraryParams?,
    ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
        val library = libraryBrowser ?: return Futures.immediateFuture(
            LibraryResult.ofError(SessionError.ERROR_NOT_SUPPORTED)
        )
        val scope = serviceScope ?: return Futures.immediateFuture(
            LibraryResult.ofError(SessionError.ERROR_NOT_SUPPORTED)
        )

        return scope.future(Dispatchers.IO) {
            val results = library.search(
                query = query,
                page = page,
                pageSize = pageSize,
            )
            LibraryResult.ofItemList(ImmutableList.copyOf(results), params)
        }
    }

    override fun onCustomCommand(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
        customCommand: SessionCommand,
        args: Bundle,
    ): ListenableFuture<SessionResult> {
        val p = player

        val result = if (p == null) {
            SessionResult(androidx.media3.session.SessionError.ERROR_BAD_VALUE)
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
                    SessionResult(androidx.media3.session.SessionError.ERROR_BAD_VALUE)
                }
            }
        }
        return Futures.immediateFuture(result)
    }
}

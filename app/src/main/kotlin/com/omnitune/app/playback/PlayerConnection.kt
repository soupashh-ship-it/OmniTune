/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.playback

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM
import androidx.media3.common.Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM
import androidx.media3.common.Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM
import androidx.media3.common.Player.REPEAT_MODE_OFF
import androidx.media3.common.Player.STATE_ENDED
import androidx.media3.common.Timeline
import com.omnitune.app.db.MusicDatabase
import com.omnitune.app.extensions.currentMetadata
import com.omnitune.app.extensions.getCurrentQueueIndex
import com.omnitune.app.extensions.getQueueIndices
import com.omnitune.app.extensions.metadata
import com.omnitune.app.playback.MusicService.MusicBinder
import com.omnitune.app.playback.queues.Queue
import com.omnitune.app.utils.reportException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import com.omnitune.app.utils.dataStore
import androidx.core.net.toUri
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import com.omnitune.app.playback.ExoDownloadService
import com.omnitune.app.constants.PermanentShuffleKey
import com.omnitune.app.constants.PauseOnDeviceMuteKey
import com.omnitune.app.constants.AutoDownloadOnLikeKey

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
class PlayerConnection(
    context: Context,
    binder: MusicBinder,
    val database: MusicDatabase,
    scope: CoroutineScope,
) : Player.Listener {
    val service = binder.service
    internal val player = service.exoPlayer

    val playbackState = MutableStateFlow(player.playbackState)
    private val playWhenReady = MutableStateFlow(player.playWhenReady)
    val playbackParameters = MutableStateFlow(player.playbackParameters)
    val isPlaying =
        combine(playbackState, playWhenReady) { playbackState, playWhenReady ->
            playWhenReady && playbackState != STATE_ENDED
        }.stateIn(
            scope,
            SharingStarted.Lazily,
            player.playWhenReady && player.playbackState != STATE_ENDED
        )
    val mediaMetadata = MutableStateFlow(player.currentMetadata)
    val currentSong =
        mediaMetadata.flatMapLatest {
            database.song(it?.id)
        }
    val currentLyrics = mediaMetadata.flatMapLatest { mediaMetadata ->
        database.lyrics(mediaMetadata?.id)
    }
    val currentFormat =
        mediaMetadata.flatMapLatest { mediaMetadata ->
            database.format(mediaMetadata?.id)
        }

    val queueTitle = MutableStateFlow<String?>(null)
    val queueIndices = MutableStateFlow<List<Int>>(emptyList())
    val currentMediaItemIndex = MutableStateFlow(-1)
    val currentWindowIndex = MutableStateFlow(-1)

    val shuffleModeEnabled = MutableStateFlow(false)
    val repeatMode = MutableStateFlow(REPEAT_MODE_OFF)

    val canSkipPrevious = MutableStateFlow(true)
    val canSkipNext = MutableStateFlow(true)

    val error = MutableStateFlow<PlaybackException?>(null)
    val waitingForNetworkConnection = service.waitingForNetworkConnection
    val queueRestoreCompleted = service.queueRestoreCompleted

    // OMNITUNE: Sleep timer state
    val sleepTimerRunning: StateFlow<Boolean> = flow {
        while (true) {
            emit(service.sleepTimer.isRunning)
            kotlinx.coroutines.delay(500)
        }
    }.stateIn(scope, SharingStarted.Eagerly, false)

    val sleepTimerRemaining: StateFlow<Long> = flow {
        while (true) {
            emit(service.sleepTimer.remainingMs)
            kotlinx.coroutines.delay(1000)
        }
    }.stateIn(scope, SharingStarted.Eagerly, 0L)

    val discordPresenceRunning: StateFlow<Boolean> = service.discordPresenceManager.isRunning

    private var pausedByMute = false

    init {
        player.addListener(this)

        playbackState.value = player.playbackState
        playWhenReady.value = player.playWhenReady
        playbackParameters.value = player.playbackParameters
        val currentMeta = player.currentMetadata ?: service.currentMediaMetadata.value
        mediaMetadata.value = currentMeta
        queueTitle.value = service.queueTitle
        queueIndices.value = player.getQueueIndices()
        currentWindowIndex.value = player.getCurrentQueueIndex()
        currentMediaItemIndex.value = player.currentMediaItemIndex
        shuffleModeEnabled.value = player.shuffleModeEnabled
        repeatMode.value = player.repeatMode

        if (currentMeta == null && player.mediaItemCount > 0) {
            val mediaItem = player.currentMediaItem
            if (mediaItem != null) {
                mediaMetadata.value = mediaItem.metadata
            }
        }
    }

    fun playQueue(queue: Queue) {
        service.scope.launch {
            val prefs = service.dataStore.data.first()
            val permanentShuffle = prefs[PermanentShuffleKey] ?: false
            if (!permanentShuffle) {
                player.shuffleModeEnabled = false
            }
            service.playQueue(queue)
        }
    }

    fun playOrResolveCurrent() {
        service.playOrResolveCurrent()
    }

    fun pause() {
        service.pausePlayback()
    }

    fun applyEqualizerBands(bands: List<com.omnitune.app.playback.EqualizerBand>) {
        service.applyEqualizerBands(bands)
    }

    fun startRadioSeamlessly() {
        service.startRadioSeamlessly()
    }

    fun playNext(item: MediaItem) = playNext(listOf(item))

    fun playNext(items: List<MediaItem>) {
        service.playNext(items)
    }

    fun addToQueue(item: MediaItem) = addToQueue(listOf(item))

    fun addToQueue(items: List<MediaItem>) {
        service.addToQueue(items)
    }

    fun toggleLibrary() {
        val meta = mediaMetadata.value ?: return
        service.scope.launch(Dispatchers.IO) {
            val song = database.getSongById(meta.id)
            val inLibrary = song?.song?.inLibrary
            val updated = if (song != null) {
                song.song.copy(inLibrary = if (inLibrary == null) java.time.LocalDateTime.now() else null)
            } else {
                meta.toSongEntity().copy(inLibrary = java.time.LocalDateTime.now())
            }
            database.upsert(updated)
        }
        val newInLibrary = if (meta.inLibrary == null) java.time.LocalDateTime.now() else null
        mediaMetadata.value = meta.copy(inLibrary = newInLibrary, liked = if (newInLibrary != null) meta.liked else false)
    }

    fun toggleLike() {
        val meta = mediaMetadata.value ?: return
        service.scope.launch(Dispatchers.IO) {
            val song = database.getSongById(meta.id)
            if (song != null) {
                database.upsert(song.song.localToggleLike())
            } else {
                database.upsert(meta.toSongEntity().copy(liked = !meta.liked, likedDate = if (!meta.liked) java.time.LocalDateTime.now() else null))
            }
            com.omnitune.innertube.YouTube.likeVideo(meta.id, !meta.liked)

            if (!meta.liked) {
                val prefs = service.dataStore.data.first()
                val autoDownload = prefs[AutoDownloadOnLikeKey] ?: false
                if (autoDownload) {
                    val downloadRequest = DownloadRequest.Builder(meta.id, meta.id.toUri())
                        .setCustomCacheKey(meta.id)
                        .setData((meta.title ?: "").toString().toByteArray())
                        .build()
                    DownloadService.sendAddDownload(
                        service,
                        ExoDownloadService::class.java,
                        downloadRequest,
                        false
                    )
                }
            }
        }
        mediaMetadata.value = meta.copy(liked = !meta.liked, likedDate = if (!meta.liked) java.time.LocalDateTime.now() else null)
    }

    fun seekToNext() {
        player.seekToNext()
        player.playWhenReady = true
    }

    fun seekToPrevious() {
        player.seekToPrevious()
        player.playWhenReady = true
    }

    override fun onPlaybackStateChanged(state: Int) {
        playbackState.value = state
        error.value = player.playerError
    }

    override fun onPlayWhenReadyChanged(
        newPlayWhenReady: Boolean,
        reason: Int,
    ) {
        playWhenReady.value = newPlayWhenReady
    }

    override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
        this.playbackParameters.value = playbackParameters
    }

    override fun onMediaItemTransition(
        mediaItem: MediaItem?,
        reason: Int,
    ) {
        val meta = mediaItem?.metadata ?: service.currentMediaMetadata.value
        mediaMetadata.value = meta
        currentMediaItemIndex.value = player.currentMediaItemIndex
        currentWindowIndex.value = player.getCurrentQueueIndex()
        updateCanSkipPreviousAndNext()
    }

    override fun onTimelineChanged(
        timeline: Timeline,
        reason: Int,
    ) {
        queueIndices.value = player.getQueueIndices()
        queueTitle.value = service.queueTitle
        currentMediaItemIndex.value = player.currentMediaItemIndex
        currentWindowIndex.value = player.getCurrentQueueIndex()
        updateCanSkipPreviousAndNext()
    }

    override fun onShuffleModeEnabledChanged(enabled: Boolean) {
        shuffleModeEnabled.value = enabled
        queueIndices.value = player.getQueueIndices()
        currentWindowIndex.value = player.getCurrentQueueIndex()
        updateCanSkipPreviousAndNext()
    }

    override fun onRepeatModeChanged(mode: Int) {
        repeatMode.value = mode
        updateCanSkipPreviousAndNext()
    }

    override fun onPlayerErrorChanged(playbackError: PlaybackException?) {
        if (playbackError != null) {
            reportException(playbackError)
        }
        error.value = playbackError
    }

    private fun updateCanSkipPreviousAndNext() {
        if (!player.currentTimeline.isEmpty) {
            val window =
                player.currentTimeline.getWindow(player.currentMediaItemIndex, Timeline.Window())
            canSkipPrevious.value = player.isCommandAvailable(COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM) ||
                    !window.isLive ||
                    player.isCommandAvailable(COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
            canSkipNext.value = window.isLive &&
                    window.isDynamic ||
                    player.isCommandAvailable(COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
        } else {
            canSkipPrevious.value = false
            canSkipNext.value = false
        }
    }

    override fun onDeviceVolumeChanged(volume: Int, muted: Boolean) {
        service.scope.launch {
            val prefs = service.dataStore.data.first()
            val pauseOnMute = prefs[PauseOnDeviceMuteKey] ?: false
            if (pauseOnMute) {
                if (volume == 0 || muted) {
                    if (player.playWhenReady) {
                        pausedByMute = true
                        player.pause()
                    }
                } else if (pausedByMute) {
                    pausedByMute = false
                    player.play()
                }
            }
        }
    }

    fun dispose() {
        player.removeListener(this)
    }

    // Decoupled player accessors
    val duration: Long get() = player.duration
    val currentPosition: Long get() = player.currentPosition
    val mediaItemCount: Int get() = player.mediaItemCount
    fun seekTo(position: Long) = player.seekTo(position)
    fun seekTo(index: Int, position: Long) = player.seekTo(index, position)
    fun getMediaItemAt(index: Int): MediaItem = player.getMediaItemAt(index)
    fun removeMediaItem(index: Int) = player.removeMediaItem(index)
    fun moveMediaItem(fromIndex: Int, toIndex: Int) = player.moveMediaItem(fromIndex, toIndex)
    fun addMediaItem(mediaItem: androidx.media3.common.MediaItem) = player.addMediaItem(mediaItem)
    fun setShuffleModeEnabled(enabled: Boolean) { player.shuffleModeEnabled = enabled }
    fun setRepeatMode(mode: Int) { player.repeatMode = mode }
    fun prepare() = player.prepare()
    val currentMediaId: String? get() = player.currentMediaItem?.mediaId
    val activeUri: String? get() = player.currentMediaItem?.localConfiguration?.uri?.toString()
    fun toggleRepeatMode() { player.repeatMode = when (player.repeatMode) { Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL; Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE; else -> Player.REPEAT_MODE_OFF } }
    val skipSilenceEnabled: Boolean get() = player.skipSilenceEnabled
    fun setSkipSilenceEnabled(enabled: Boolean) { player.skipSilenceEnabled = enabled }
    val audioSessionId: Int get() = player.audioSessionId
    fun setPlaybackParameters(speed: Float, pitch: Float) { player.playbackParameters = PlaybackParameters(speed, pitch) }
    val playbackSpeed: Float get() = player.playbackParameters.speed
    val playbackPitch: Float get() = player.playbackParameters.pitch

    fun restartDiscordPresence() {
        service.restartDiscordPresence()
    }
}

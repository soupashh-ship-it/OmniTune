package com.omnitune.shared.playback

import com.omnitune.shared.domain.models.SongItem
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import platform.MediaPlayer.MPChangePlaybackPositionCommandEvent
import platform.MediaPlayer.MPMediaItemPropertyAlbumTitle
import platform.MediaPlayer.MPMediaItemPropertyArtist
import platform.MediaPlayer.MPMediaItemPropertyPlaybackDuration
import platform.MediaPlayer.MPMediaItemPropertyTitle
import platform.MediaPlayer.MPNowPlayingInfoCenter
import platform.MediaPlayer.MPNowPlayingInfoPropertyDefaultPlaybackRate
import platform.MediaPlayer.MPNowPlayingInfoPropertyElapsedPlaybackTime
import platform.MediaPlayer.MPNowPlayingInfoPropertyPlaybackRate
import platform.MediaPlayer.MPRemoteCommandHandlerStatusSuccess
import platform.MediaPlayer.MPRemoteCommandCenter

@OptIn(ExperimentalForeignApi::class)
class IosMediaRemoteController(
    private val onPlay: () -> Unit = {},
    private val onPause: () -> Unit = {},
    private val onTogglePlayPause: () -> Unit = {},
    private val onNext: () -> Unit = {},
    private val onPrevious: () -> Unit = {},
    private val onSeek: (positionMs: Long) -> Unit = {}
) {

    constructor(playbackController: PlaybackController) : this(
        onPlay = { playbackController.resume() },
        onPause = { playbackController.pause() },
        onTogglePlayPause = {
            if (playbackController.isPlaying.value) {
                playbackController.pause()
            } else {
                playbackController.resume()
            }
        },
        onNext = { playbackController.skipNext() },
        onPrevious = { playbackController.skipPrevious() },
        onSeek = { positionMs -> playbackController.seekTo(positionMs) }
    )

    private var bindingJob: Job? = null

    init {
        setupRemoteCommands()
    }

    fun bind(playbackController: PlaybackController, scope: CoroutineScope) {
        bindingJob?.cancel()
        bindingJob = scope.launch {
            // Update only on track change, playback state change, or duration change
            launch {
                combine(
                    playbackController.currentItem,
                    playbackController.isPlaying,
                    playbackController.playbackState,
                    playbackController.durationMs
                ) { item, playing, state, dur ->
                    Triple(item, playing to state, dur)
                }.distinctUntilChanged().collect { (item, statePair, dur) ->
                    val (playing, _) = statePair
                    updateNowPlaying(
                        songItem = item,
                        isPlaying = playing,
                        currentPositionMs = playbackController.currentPositionMs.value,
                        durationMs = dur
                    )
                }
            }

            // Update on explicit seek
            launch {
                playbackController.seekEvents.collect { seekPos ->
                    updateNowPlaying(
                        songItem = playbackController.currentItem.value,
                        isPlaying = playbackController.isPlaying.value,
                        currentPositionMs = seekPos,
                        durationMs = playbackController.durationMs.value
                    )
                }
            }
        }
    }

    fun unbind() {
        bindingJob?.cancel()
        bindingJob = null
        clearNowPlaying()
    }

    fun updateNowPlaying(
        songItem: SongItem?,
        isPlaying: Boolean,
        currentPositionMs: Long,
        durationMs: Long
    ) {
        val center = MPNowPlayingInfoCenter.defaultCenter()
        if (songItem == null) {
            center.nowPlayingInfo = null
            return
        }

        val info = mutableMapOf<Any?, Any>()
        info[MPMediaItemPropertyTitle] = songItem.title
        info[MPMediaItemPropertyArtist] = songItem.artist
        songItem.album?.title?.let { albumTitle ->
            info[MPMediaItemPropertyAlbumTitle] = albumTitle
        }
        val durationSec = if (durationMs > 0L) {
            durationMs.toDouble() / 1000.0
        } else {
            songItem.durationSec.toDouble()
        }
        info[MPMediaItemPropertyPlaybackDuration] = durationSec
        info[MPNowPlayingInfoPropertyElapsedPlaybackTime] = currentPositionMs.toDouble() / 1000.0
        info[MPNowPlayingInfoPropertyPlaybackRate] = if (isPlaying) 1.0 else 0.0
        info[MPNowPlayingInfoPropertyDefaultPlaybackRate] = 1.0

        center.nowPlayingInfo = info
    }

    fun clearNowPlaying() {
        MPNowPlayingInfoCenter.defaultCenter().nowPlayingInfo = null
    }

    private fun setupRemoteCommands() {
        val commandCenter = MPRemoteCommandCenter.sharedCommandCenter()

        commandCenter.playCommand.enabled = true
        commandCenter.playCommand.addTargetWithHandler {
            onPlay()
            MPRemoteCommandHandlerStatusSuccess
        }

        commandCenter.pauseCommand.enabled = true
        commandCenter.pauseCommand.addTargetWithHandler {
            onPause()
            MPRemoteCommandHandlerStatusSuccess
        }

        commandCenter.togglePlayPauseCommand.enabled = true
        commandCenter.togglePlayPauseCommand.addTargetWithHandler {
            onTogglePlayPause()
            MPRemoteCommandHandlerStatusSuccess
        }

        commandCenter.nextTrackCommand.enabled = true
        commandCenter.nextTrackCommand.addTargetWithHandler {
            onNext()
            MPRemoteCommandHandlerStatusSuccess
        }

        commandCenter.previousTrackCommand.enabled = true
        commandCenter.previousTrackCommand.addTargetWithHandler {
            onPrevious()
            MPRemoteCommandHandlerStatusSuccess
        }

        commandCenter.changePlaybackPositionCommand.enabled = true
        commandCenter.changePlaybackPositionCommand.addTargetWithHandler { event ->
            val posEvent = event as? MPChangePlaybackPositionCommandEvent
            val targetSec = posEvent?.positionTime ?: 0.0
            onSeek((targetSec * 1000.0).toLong())
            MPRemoteCommandHandlerStatusSuccess
        }
    }
}

package com.omnitune.shared.playback

import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryOptionAllowAirPlay
import platform.AVFAudio.AVAudioSessionCategoryOptionAllowBluetooth
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.AVFAudio.AVAudioSessionModeDefault
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.AVPlayerItem
import platform.AVFoundation.AVPlayerItemDidPlayToEndTimeNotification
import platform.AVFoundation.AVURLAsset
import platform.AVFoundation.addPeriodicTimeObserverForInterval
import platform.AVFoundation.duration
import platform.AVFoundation.pause
import platform.AVFoundation.play
import platform.AVFoundation.removeTimeObserver
import platform.AVFoundation.replaceCurrentItemWithPlayerItem
import platform.AVFoundation.seekToTime
import platform.CoreMedia.CMTimeGetSeconds
import platform.CoreMedia.CMTimeMakeWithSeconds
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSURL
import platform.darwin.NSEC_PER_SEC
import platform.darwin.dispatch_get_main_queue

@OptIn(ExperimentalForeignApi::class)
class IosAudioPlayer : AudioPlayer {

    private var player: AVPlayer? = null
    private var playerItem: AVPlayerItem? = null
    private var timeObserverToken: Any? = null
    private var itemEndObserverToken: Any? = null
    private var listener: AudioPlayerListener? = null
    private var _isPlaying: Boolean = false
    private var _currentPositionMs: Long = 0L
    private var _durationMs: Long = 0L

    override val isPlaying: Boolean
        get() = _isPlaying

    override val currentPositionMs: Long
        get() = _currentPositionMs

    override val durationMs: Long
        get() = _durationMs

    init {
        configureAudioSession()
    }

    private fun configureAudioSession() {
        try {
            val session = AVAudioSession.sharedInstance()
            session.setCategory(
                category = AVAudioSessionCategoryPlayback,
                mode = AVAudioSessionModeDefault,
                options = AVAudioSessionCategoryOptionAllowBluetooth or AVAudioSessionCategoryOptionAllowAirPlay,
                error = null
            )
        } catch (_: Throwable) {
            // Audio session configuration fallback
        }
    }

    override fun setListener(listener: AudioPlayerListener?) {
        this.listener = listener
    }

    override fun prepare(streamUrl: String, headers: Map<String, String>) {
        cleanObservers()
        listener?.onPlaybackStateChanged(PlaybackState.BUFFERING)

        val nsUrl = NSURL.URLWithString(streamUrl) ?: run {
            listener?.onError("Invalid stream URL: $streamUrl")
            listener?.onPlaybackStateChanged(PlaybackState.IDLE)
            return
        }

        val assetOptions: Map<Any?, Any>? = if (headers.isNotEmpty()) {
            mapOf("AVURLAssetHTTPHeaderFieldsKey" to headers)
        } else null

        val asset = AVURLAsset(uRL = nsUrl, options = assetOptions)
        val item = AVPlayerItem(asset)
        this.playerItem = item

        val currentPlayer = player
        val avPlayer = if (currentPlayer == null) {
            val newPlayer = AVPlayer(playerItem = item)
            player = newPlayer
            newPlayer
        } else {
            currentPlayer.replaceCurrentItemWithPlayerItem(item)
            currentPlayer
        }

        if (timeObserverToken == null) {
            setupTimeObserver(avPlayer)
        }

        itemEndObserverToken = NSNotificationCenter.defaultCenter.addObserverForName(
            name = AVPlayerItemDidPlayToEndTimeNotification,
            `object` = item,
            queue = NSOperationQueue.mainQueue
        ) { _ ->
            _isPlaying = false
            listener?.onPlaybackStateChanged(PlaybackState.ENDED)
        }

        listener?.onPlaybackStateChanged(PlaybackState.READY)
    }

    override fun play() {
        configureAudioSession()
        player?.play()
        _isPlaying = true
        listener?.onPlaybackStateChanged(PlaybackState.READY)
    }

    override fun pause() {
        player?.pause()
        _isPlaying = false
    }

    override fun seekTo(positionMs: Long) {
        _currentPositionMs = positionMs.coerceAtLeast(0L)
        val seconds = _currentPositionMs.toDouble() / 1000.0
        val targetTime = CMTimeMakeWithSeconds(seconds, NSEC_PER_SEC.toInt())
        player?.seekToTime(targetTime)
        listener?.onPositionDiscontinuity(_currentPositionMs)
    }

    override fun stop() {
        player?.pause()
        _isPlaying = false
        _currentPositionMs = 0L
        listener?.onPlaybackStateChanged(PlaybackState.IDLE)
    }

    override fun release() {
        stop()
        cleanObservers()
        player = null
        playerItem = null
        listener = null
    }

    private fun setupTimeObserver(avPlayer: AVPlayer) {
        val interval = CMTimeMakeWithSeconds(0.5, NSEC_PER_SEC.toInt())
        timeObserverToken = avPlayer.addPeriodicTimeObserverForInterval(
            interval = interval,
            queue = dispatch_get_main_queue()
        ) { time ->
            val currentSeconds = CMTimeGetSeconds(time)
            if (!currentSeconds.isNaN() && currentSeconds >= 0.0) {
                _currentPositionMs = (currentSeconds * 1000.0).toLong()
            }
            playerItem?.duration?.let { dur ->
                val durationSec = CMTimeGetSeconds(dur)
                if (!durationSec.isNaN() && durationSec > 0.0) {
                    _durationMs = (durationSec * 1000.0).toLong()
                }
            }
        }
    }

    private fun cleanObservers() {
        timeObserverToken?.let { token ->
            player?.removeTimeObserver(token)
            timeObserverToken = null
        }
        itemEndObserverToken?.let { token ->
            NSNotificationCenter.defaultCenter.removeObserver(token)
            itemEndObserverToken = null
        }
    }
}

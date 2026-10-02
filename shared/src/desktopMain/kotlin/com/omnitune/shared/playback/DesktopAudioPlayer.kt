package com.omnitune.shared.playback

class DesktopAudioPlayer : AudioPlayer {

    private var listener: AudioPlayerListener? = null
    private var _isPlaying: Boolean = false
    private var _currentPositionMs: Long = 0L
    private var _durationMs: Long = 0L
    private var _streamUrl: String? = null
    private var _headers: Map<String, String> = emptyMap()

    override val isPlaying: Boolean
        get() = _isPlaying

    override val currentPositionMs: Long
        get() = _currentPositionMs

    override val durationMs: Long
        get() = _durationMs

    val streamUrl: String?
        get() = _streamUrl

    val headers: Map<String, String>
        get() = _headers

    override fun setListener(listener: AudioPlayerListener?) {
        this.listener = listener
    }

    override fun prepare(streamUrl: String, headers: Map<String, String>) {
        _streamUrl = streamUrl
        _headers = headers
        _currentPositionMs = 0L
        listener?.onPlaybackStateChanged(PlaybackState.BUFFERING)
        listener?.onPlaybackStateChanged(PlaybackState.READY)
    }

    override fun play() {
        _isPlaying = true
        listener?.onPlaybackStateChanged(PlaybackState.READY)
    }

    override fun pause() {
        _isPlaying = false
    }

    override fun seekTo(positionMs: Long) {
        _currentPositionMs = positionMs.coerceAtLeast(0L)
        listener?.onPositionDiscontinuity(_currentPositionMs)
    }

    override fun stop() {
        _isPlaying = false
        _currentPositionMs = 0L
        listener?.onPlaybackStateChanged(PlaybackState.IDLE)
    }

    override fun release() {
        stop()
        listener = null
    }

    fun simulateTrackCompletion() {
        _isPlaying = false
        listener?.onPlaybackStateChanged(PlaybackState.ENDED)
    }

    fun setSimulatedDuration(durationMs: Long) {
        _durationMs = durationMs
    }

    fun setSimulatedPosition(positionMs: Long) {
        _currentPositionMs = positionMs
    }

    fun simulateError(message: String) {
        _isPlaying = false
        listener?.onError(message)
    }
}

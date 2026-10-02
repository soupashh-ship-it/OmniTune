package com.omnitune.shared.playback

interface AudioPlayer {
    fun prepare(streamUrl: String, headers: Map<String, String> = emptyMap())
    fun play()
    fun pause()
    fun seekTo(positionMs: Long)
    fun stop()
    fun release()
    val isPlaying: Boolean
    val currentPositionMs: Long
    val durationMs: Long
    fun setListener(listener: AudioPlayerListener?)
}

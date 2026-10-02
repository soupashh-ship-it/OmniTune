package com.omnitune.shared.playback

interface AudioPlayerListener {
    fun onPlaybackStateChanged(state: PlaybackState)
    fun onPositionDiscontinuity(positionMs: Long)
    fun onError(message: String)
}

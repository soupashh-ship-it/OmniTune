package com.omnitune.shared.playback

import com.omnitune.shared.domain.models.SongItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow

interface PlaybackController {
    val playbackState: StateFlow<PlaybackState>
    val currentItem: StateFlow<SongItem?>
    val currentPositionMs: StateFlow<Long>
    val durationMs: StateFlow<Long>
    val queue: StateFlow<List<SongItem>>
    val currentIndex: StateFlow<Int>
    val isPlaying: StateFlow<Boolean>
    val shuffleMode: StateFlow<Boolean>
    val repeatMode: StateFlow<RepeatMode>
    val seekEvents: Flow<Long> get() = emptyFlow()

    fun play(item: SongItem)
    fun playQueue(items: List<SongItem>, startIndex: Int = 0)
    fun pause()
    fun resume()
    fun seekTo(positionMs: Long)
    fun skipNext()
    fun skipPrevious()
    fun setShuffle(enabled: Boolean)
    fun setRepeat(mode: RepeatMode)
    fun reorderQueue(fromIndex: Int, toIndex: Int)
    fun removeFromQueue(index: Int)
}

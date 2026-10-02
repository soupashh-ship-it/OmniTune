package com.omnitune.shared.playback

import com.omnitune.shared.data.innertube.StreamResolver
import com.omnitune.shared.domain.models.SongItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class PlaybackControllerImpl(
    private val audioPlayer: AudioPlayer,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob()),
    private val queueManager: QueueManager = QueueManager(),
    private val streamResolverFn: (suspend (videoId: String) -> Pair<String, Map<String, String>>)? = null,
    private val trackingScope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob()),
) : PlaybackController {

    private val _playbackState = MutableStateFlow(PlaybackState.IDLE)
    override val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val _currentItem = MutableStateFlow<SongItem?>(null)
    override val currentItem: StateFlow<SongItem?> = _currentItem.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    override val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    override val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _queue = MutableStateFlow<List<SongItem>>(emptyList())
    override val queue: StateFlow<List<SongItem>> = _queue.asStateFlow()

    private val _currentIndex = MutableStateFlow(-1)
    override val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    override val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _shuffleMode = MutableStateFlow(false)
    override val shuffleMode: StateFlow<Boolean> = _shuffleMode.asStateFlow()

    private val _repeatMode = MutableStateFlow(RepeatMode.OFF)
    override val repeatMode: StateFlow<RepeatMode> = _repeatMode.asStateFlow()

    private val _seekEvents = MutableSharedFlow<Long>(extraBufferCapacity = 16)
    override val seekEvents: SharedFlow<Long> = _seekEvents.asSharedFlow()

    private var trackingJob: Job? = null
    private var loadJob: Job? = null

    init {
        audioPlayer.setListener(object : AudioPlayerListener {
            override fun onPlaybackStateChanged(state: PlaybackState) {
                if (state == PlaybackState.IDLE && _playbackState.value == PlaybackState.ENDED) {
                    return
                }
                _playbackState.value = state
                if (state == PlaybackState.ENDED) {
                    skipNext()
                }
            }

            override fun onPositionDiscontinuity(positionMs: Long) {
                _currentPositionMs.value = positionMs
            }

            override fun onError(message: String) {
                _playbackState.value = PlaybackState.IDLE
                _isPlaying.value = false
            }
        })
    }

    override fun play(item: SongItem) {
        playQueue(listOf(item), 0)
    }

    override fun playQueue(items: List<SongItem>, startIndex: Int) {
        if (items.isEmpty()) {
            loadJob?.cancel()
            loadJob = null
            stopPositionTracking()
            queueManager.clear()
            audioPlayer.stop()
            syncQueueState()
            _playbackState.value = PlaybackState.IDLE
            _isPlaying.value = false
            _currentPositionMs.value = 0L
            _durationMs.value = 0L
            return
        }

        queueManager.setQueue(items, startIndex)
        syncQueueState()
        loadAndPlayCurrentItem()
    }

    override fun pause() {
        _isPlaying.value = false
        stopPositionTracking()
        audioPlayer.pause()
    }

    override fun resume() {
        if (_queue.value.isNotEmpty() && _currentIndex.value >= 0) {
            _isPlaying.value = true
            _playbackState.value = PlaybackState.READY
            audioPlayer.play()
            startPositionTracking()
        }
    }

    override fun seekTo(positionMs: Long) {
        val maxDur = _durationMs.value
        val clamped = if (maxDur > 0L) {
            positionMs.coerceIn(0L, maxDur)
        } else {
            0L
        }
        _currentPositionMs.value = clamped
        _seekEvents.tryEmit(clamped)
        audioPlayer.seekTo(clamped)
    }

    override fun skipNext() {
        val nextItem = queueManager.next()
        if (nextItem != null) {
            if (queueManager.repeatMode == RepeatMode.ONE) {
                seekTo(0L)
                audioPlayer.play()
                _isPlaying.value = true
                _playbackState.value = PlaybackState.READY
            } else {
                syncQueueState()
                loadAndPlayCurrentItem()
            }
        } else {
            _isPlaying.value = false
            stopPositionTracking()
            audioPlayer.stop()
            _playbackState.value = PlaybackState.ENDED
        }
    }

    override fun skipPrevious() {
        if (queueManager.queue.isEmpty() || queueManager.currentIndex < 0) {
            return
        }
        if (_currentPositionMs.value > 3000L) {
            seekTo(0L)
            audioPlayer.play()
            _isPlaying.value = true
            _playbackState.value = PlaybackState.READY
            return
        }
        val prevItem = queueManager.currentItem
        val changed = queueManager.skipPrevious()
        if (changed && queueManager.currentItem != prevItem) {
            syncQueueState()
            loadAndPlayCurrentItem()
        } else {
            seekTo(0L)
            audioPlayer.play()
            _isPlaying.value = true
            _playbackState.value = PlaybackState.READY
        }
    }

    override fun setShuffle(enabled: Boolean) {
        queueManager.setShuffle(enabled)
        syncQueueState()
    }

    override fun setRepeat(mode: RepeatMode) {
        queueManager.setRepeat(mode)
        _repeatMode.value = queueManager.repeatMode
    }

    override fun reorderQueue(fromIndex: Int, toIndex: Int) {
        if (queueManager.reorder(fromIndex, toIndex)) {
            syncQueueState()
        }
    }

    override fun removeFromQueue(index: Int) {
        val wasPlayingIndex = (index == _currentIndex.value)
        if (queueManager.remove(index)) {
            syncQueueState()
            if (_queue.value.isEmpty()) {
                loadJob?.cancel()
                loadJob = null
                stopPositionTracking()
                audioPlayer.stop()
                _isPlaying.value = false
                _playbackState.value = PlaybackState.IDLE
                _currentPositionMs.value = 0L
                _durationMs.value = 0L
            } else if (wasPlayingIndex) {
                loadAndPlayCurrentItem()
            }
        }
    }

    private fun syncQueueState() {
        _queue.value = queueManager.queue
        _currentIndex.value = queueManager.currentIndex
        _currentItem.value = queueManager.currentItem
        _shuffleMode.value = queueManager.shuffleMode
        _repeatMode.value = queueManager.repeatMode
    }

    private fun loadAndPlayCurrentItem() {
        val item = queueManager.currentItem ?: return
        _currentItem.value = item
        _currentIndex.value = queueManager.currentIndex
        _queue.value = queueManager.queue
        _currentPositionMs.value = 0L
        stopPositionTracking()
        audioPlayer.seekTo(0L)
        val itemDurMs = (item.durationSec * 1000L).coerceAtLeast(0L)
        _durationMs.value = itemDurMs
        _playbackState.value = PlaybackState.BUFFERING

        loadJob?.cancel()
        loadJob = scope.launch {
            try {
                val (url, headers) = streamResolverFn?.invoke(item.id) ?: run {
                    val streamInfo = StreamResolver.resolveStream(item.id)
                    Pair(streamInfo.streamUrl, streamInfo.headers)
                }
                audioPlayer.prepare(url, headers)
                audioPlayer.play()
                _isPlaying.value = true
                _playbackState.value = PlaybackState.READY
                if (audioPlayer.durationMs > 0L) {
                    _durationMs.value = audioPlayer.durationMs
                }
                startPositionTracking()
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                _playbackState.value = PlaybackState.IDLE
                _isPlaying.value = false
            }
        }
    }

    private fun startPositionTracking() {
        trackingJob?.cancel()
        trackingJob = trackingScope.launch {
            while (isActive && _isPlaying.value) {
                val pos = audioPlayer.currentPositionMs
                if (pos >= 0L) {
                    _currentPositionMs.value = pos
                }
                val dur = audioPlayer.durationMs
                if (dur > 0L) {
                    _durationMs.value = dur
                }
                delay(200L)
            }
        }
    }

    private fun stopPositionTracking() {
        trackingJob?.cancel()
        trackingJob = null
    }

    fun release() {
        loadJob?.cancel()
        loadJob = null
        stopPositionTracking()
        audioPlayer.stop()
        _isPlaying.value = false
        _playbackState.value = PlaybackState.IDLE
    }
}

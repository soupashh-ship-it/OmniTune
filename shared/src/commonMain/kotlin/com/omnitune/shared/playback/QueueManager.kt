package com.omnitune.shared.playback

import com.omnitune.shared.domain.models.SongItem

class QueueManager {

    private val _originalQueue = mutableListOf<SongItem>()
    private val _queue = mutableListOf<SongItem>()

    var currentIndex: Int = -1
        private set

    var shuffleMode: Boolean = false
        private set

    var repeatMode: RepeatMode = RepeatMode.OFF
        private set

    val queue: List<SongItem>
        get() = _queue.toList()

    val originalQueue: List<SongItem>
        get() = _originalQueue.toList()

    val currentItem: SongItem?
        get() = if (currentIndex in _queue.indices) _queue[currentIndex] else null

    fun setQueue(items: List<SongItem>, startIndex: Int = 0) {
        _originalQueue.clear()
        _originalQueue.addAll(items)
        _queue.clear()

        if (items.isEmpty()) {
            currentIndex = -1
            return
        }

        val validStartIndex = startIndex.coerceIn(0, items.size - 1)
        if (shuffleMode && items.size > 1) {
            val selected = items[validStartIndex]
            val rest = items.filterIndexed { index, _ -> index != validStartIndex }.shuffled()
            _queue.add(selected)
            _queue.addAll(rest)
            currentIndex = 0
        } else {
            _queue.addAll(items)
            currentIndex = validStartIndex
        }
    }

    fun enqueue(item: SongItem) {
        _originalQueue.add(item)
        _queue.add(item)
        if (currentIndex == -1) {
            currentIndex = 0
        }
    }

    fun enqueueAll(items: List<SongItem>) {
        _originalQueue.addAll(items)
        _queue.addAll(items)
        if (currentIndex == -1 && _queue.isNotEmpty()) {
            currentIndex = 0
        }
    }

    fun remove(index: Int): Boolean {
        if (index !in _queue.indices) return false
        val removed = _queue.removeAt(index)
        _originalQueue.remove(removed)

        if (_queue.isEmpty()) {
            currentIndex = -1
        } else if (index < currentIndex) {
            currentIndex--
        } else if (index == currentIndex) {
            if (currentIndex >= _queue.size) {
                currentIndex = 0
            }
        }
        return true
    }

    fun reorder(fromIndex: Int, toIndex: Int): Boolean {
        if (fromIndex !in _queue.indices || toIndex !in _queue.indices) return false
        if (fromIndex == toIndex) return true

        val activeItem = currentItem
        val item = _queue.removeAt(fromIndex)
        _queue.add(toIndex, item)

        if (!shuffleMode) {
            val origFrom = _originalQueue.indexOf(item)
            if (origFrom != -1) {
                _originalQueue.removeAt(origFrom)
                val safeTo = toIndex.coerceIn(0, _originalQueue.size)
                _originalQueue.add(safeTo, item)
            }
        }

        if (activeItem != null) {
            val newIdx = _queue.indexOf(activeItem)
            if (newIdx != -1) {
                currentIndex = newIdx
            }
        }
        return true
    }

    fun setShuffle(enabled: Boolean) {
        if (shuffleMode == enabled) return
        shuffleMode = enabled

        if (enabled) {
            if (_queue.size <= 1) return
            val current = currentItem
            if (current != null) {
                val others = _queue.filterIndexed { index, _ -> index != currentIndex }.shuffled()
                _queue.clear()
                _queue.add(current)
                _queue.addAll(others)
                currentIndex = 0
            } else {
                val shuffled = _queue.shuffled()
                _queue.clear()
                _queue.addAll(shuffled)
                currentIndex = if (_queue.isNotEmpty()) 0 else -1
            }
        } else {
            val current = currentItem
            _queue.clear()
            _queue.addAll(_originalQueue)
            currentIndex = if (current != null) {
                val origIdx = _queue.indexOf(current)
                if (origIdx != -1) origIdx else if (_queue.isNotEmpty()) 0 else -1
            } else {
                if (_queue.isNotEmpty()) 0 else -1
            }
        }
    }

    fun setRepeat(mode: RepeatMode) {
        repeatMode = mode
    }

    fun next(): SongItem? {
        if (_queue.isEmpty()) return null

        if (repeatMode == RepeatMode.ONE) {
            return currentItem
        }

        if (currentIndex < _queue.size - 1) {
            currentIndex++
            return _queue[currentIndex]
        } else if (repeatMode == RepeatMode.ALL) {
            currentIndex = 0
            return _queue[0]
        } else {
            return null
        }
    }

    fun previous(currentPositionMs: Long = 0L): SongItem? {
        if (_queue.isEmpty()) return null

        if (currentPositionMs > 3000L) {
            return currentItem
        }

        if (repeatMode == RepeatMode.ONE) {
            return currentItem
        }

        if (currentIndex > 0) {
            currentIndex--
            return _queue[currentIndex]
        } else if (repeatMode == RepeatMode.ALL) {
            currentIndex = _queue.size - 1
            return _queue[currentIndex]
        } else {
            return currentItem
        }
    }

    fun skipPrevious(currentPositionMs: Long = 0L): Boolean {
        if (_queue.isEmpty()) return false
        val oldIndex = currentIndex
        val oldItem = currentItem
        val newItem = previous(currentPositionMs)
        return newItem != null && (currentIndex != oldIndex || newItem != oldItem)
    }

    fun seekToIndex(index: Int): SongItem? {
        if (index !in _queue.indices) return null
        currentIndex = index
        return _queue[index]
    }

    fun clear() {
        _originalQueue.clear()
        _queue.clear()
        currentIndex = -1
    }
}

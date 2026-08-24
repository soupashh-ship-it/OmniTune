/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.playback

import androidx.media3.common.MediaItem

/**
 * Tracks the in-memory "previous track" history used by seek-to-previous.
 *
 * Pure state holder extracted from MusicService so the transition/suppression rules can be
 * unit tested without a Player. The service remains responsible for resolving entries against
 * the live timeline and performing seeks.
 */
class PlaybackHistoryTracker(
    private val maxEntries: Int = DEFAULT_MAX_ENTRIES,
) {
    data class Entry(
        val mediaId: String,
        val index: Int,
    )

    private val history = ArrayDeque<Entry>()
    private var current: Entry? = null
    private var suppressNextRecord = false

    fun onTransition(mediaItem: MediaItem?, currentIndex: Int) {
        val nextEntry = mediaItem
            ?.takeIf { it.mediaId.isNotBlank() }
            ?.let { Entry(it.mediaId, currentIndex) }
        val previousEntry = current

        if (suppressNextRecord) {
            suppressNextRecord = false
        } else if (previousEntry != null && previousEntry != nextEntry) {
            history.addLast(previousEntry)
            while (history.size > maxEntries) {
                history.removeFirst()
            }
        }

        current = nextEntry
    }

    fun reset() {
        history.clear()
        current = null
        suppressNextRecord = false
    }

    fun hasPrevious(): Boolean = history.isNotEmpty()

    fun popPrevious(): Entry? = if (history.isEmpty()) null else history.removeLast()

    fun suppressNextRecord() {
        suppressNextRecord = true
    }

    fun clearSuppressNextRecord() {
        suppressNextRecord = false
    }

    companion object {
        const val DEFAULT_MAX_ENTRIES = 80
    }
}

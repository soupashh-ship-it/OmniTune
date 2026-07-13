/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.playback.continuation

object LikedSongsPlaybackPlanner {
    fun <T> orderedQueue(
        items: List<T>,
        selectedIndex: Int,
        shuffled: Boolean,
    ): Pair<List<T>, Int> {
        if (items.isEmpty()) return emptyList<T>() to 0
        val safeIndex = selectedIndex.coerceIn(0, items.lastIndex)
        if (!shuffled || items.size == 1) return items to safeIndex

        val selected = items[safeIndex]
        val shuffledItems = (items.take(safeIndex) + items.drop(safeIndex + 1)).shuffled()
        return listOf(selected) + shuffledItems to 0
    }

    fun nextLoopIndex(itemCount: Int): Int? =
        if (itemCount > 0) 0 else null
}

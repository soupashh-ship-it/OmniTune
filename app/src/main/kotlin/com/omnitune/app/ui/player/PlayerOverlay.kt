/*
 * Adapted from SuvMusic (https://github.com/SuvojeetDev/SuvMusic)
 * Copyright (c) SuvMusic contributors
 * Licensed under GPL-3.0
 */

package com.omnitune.app.ui.player

import com.omnitune.app.models.Song

/**
 * Represents the currently active overlay on the Player screen.
 * Only one overlay can be visible at a time, preventing sheet "pileups"
 * and simplifying back-press handling.
 */
sealed interface PlayerOverlay {
    data object None : PlayerOverlay
    data object Queue : PlayerOverlay
    data object Lyrics : PlayerOverlay
    data object Related : PlayerOverlay
    data class SongInfo(val song: Song) : PlayerOverlay
    data class Actions(
        val targetSong: Song? = null,
        val fromQueue: Boolean = false,
        val fromRelated: Boolean = false
    ) : PlayerOverlay
    data object SleepTimer : PlayerOverlay
    data object OutputDevice : PlayerOverlay
    data object PlaybackSpeed : PlayerOverlay
    data object Equalizer : PlayerOverlay
}

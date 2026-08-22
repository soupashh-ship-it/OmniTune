package com.omnitune.app.ui.navigation

import com.omnitune.app.playback.continuation.PlaybackSourceType
import com.omnitune.innertube.models.SongItem

/**
 * A validated request produced when a song row is selected. Keeping this separate from the
 * Compose callback makes the selected item and its playback context deterministic at the UI
 * boundary, including while the player service is still connecting.
 */
internal data class SongPlaybackRequest(
    val title: String,
    val songs: List<SongItem>,
    val startIndex: Int,
    val sourceType: PlaybackSourceType,
    val verifiedGenre: String? = null,
    val verifiedMood: String? = null,
) {
    val selectedSong: SongItem
        get() = songs[startIndex]
}

internal fun songPlaybackRequest(
    title: String,
    songs: List<SongItem>,
    startIndex: Int,
    sourceType: PlaybackSourceType,
    verifiedGenre: String? = null,
    verifiedMood: String? = null,
): SongPlaybackRequest? {
    if (songs.getOrNull(startIndex) == null) return null

    return SongPlaybackRequest(
        title = title,
        songs = songs,
        startIndex = startIndex,
        sourceType = sourceType,
        verifiedGenre = verifiedGenre,
        verifiedMood = verifiedMood,
    )
}

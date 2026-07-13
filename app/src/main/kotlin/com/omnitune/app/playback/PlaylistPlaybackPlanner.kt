/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.playback

import androidx.media3.common.MediaItem
import com.omnitune.app.db.entities.PlaylistSong
import com.omnitune.app.extensions.toMediaItem
import com.omnitune.app.playback.continuation.PlaybackContext
import com.omnitune.app.playback.continuation.PlaybackSourceType
import com.omnitune.app.playback.queues.ListQueue
import kotlin.random.Random

data class PlaylistPlaybackPlan(
    val playlistId: String,
    val playlistName: String,
    val items: List<MediaItem>,
    val startIndex: Int,
    val shuffled: Boolean,
) {
    fun toQueue() = ListQueue(
        title = playlistName,
        items = items,
        startIndex = startIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0)),
        playbackContext = PlaybackContext(
            sourceType = PlaybackSourceType.PLAYLIST,
            sourceId = playlistId,
            sourceTitle = playlistName,
            seedSongId = items.getOrNull(startIndex)?.mediaId,
            allowAutoplay = true,
            shuffledCollection = shuffled,
            sessionItems = items,
        ),
    )
}

object PlaylistPlaybackPlanner {
    fun ordered(
        playlistId: String,
        playlistName: String,
        songs: List<PlaylistSong>,
        selectedMapId: Int? = null,
    ): PlaylistPlaybackPlan {
        val orderedSongs = songs.inSavedOrder()
        val selectedIndex = selectedMapId
            ?.let { mapId -> orderedSongs.indexOfFirst { it.map.id == mapId } }
            ?.takeIf { it >= 0 }
            ?: 0

        return PlaylistPlaybackPlan(
            playlistId = playlistId,
            playlistName = playlistName,
            items = orderedSongs.map { it.song.toMediaItem() },
            startIndex = selectedIndex,
            shuffled = false,
        )
    }

    fun shuffled(
        playlistId: String,
        playlistName: String,
        songs: List<PlaylistSong>,
        random: Random = Random.Default,
    ): PlaylistPlaybackPlan {
        val orderedSongs = songs.inSavedOrder()
        val shuffledSongs = orderedSongs.shuffled(random).avoidNoOpShuffle(orderedSongs)

        return PlaylistPlaybackPlan(
            playlistId = playlistId,
            playlistName = playlistName,
            items = shuffledSongs.map { it.song.toMediaItem() },
            startIndex = 0,
            shuffled = true,
        )
    }

    fun orderedSongIds(songs: List<PlaylistSong>, selectedMapId: Int? = null): Pair<List<String>, Int> {
        val orderedSongs = songs.inSavedOrder()
        val startIndex = selectedMapId
            ?.let { mapId -> orderedSongs.indexOfFirst { it.map.id == mapId } }
            ?.takeIf { it >= 0 }
            ?: 0
        return orderedSongs.map { it.song.id } to startIndex
    }

    fun shuffledSongIds(songs: List<PlaylistSong>, random: Random = Random.Default): List<String> {
        val orderedSongs = songs.inSavedOrder()
        return orderedSongs.shuffled(random).avoidNoOpShuffle(orderedSongs).map { it.song.id }
    }

    private fun List<PlaylistSong>.inSavedOrder(): List<PlaylistSong> =
        sortedWith(compareBy<PlaylistSong> { it.map.position }.thenBy { it.map.id })

    private fun List<PlaylistSong>.avoidNoOpShuffle(original: List<PlaylistSong>): List<PlaylistSong> {
        if (size <= 1 || map { it.map.id } != original.map { it.map.id }) return this
        return drop(1) + first()
    }
}

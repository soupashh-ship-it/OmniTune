/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.playback.queues

import androidx.media3.common.MediaItem
import com.omnitune.app.db.MusicDatabase
import com.omnitune.app.extensions.toMediaItem
import com.omnitune.app.models.MediaMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first

class LocalMixQueue(
    private val database: MusicDatabase,
    private val playlistId: String,
    private val maxMixSize: Int = 50,
) : Queue {
    override val preloadItem: MediaMetadata? = null

    override suspend fun getInitialStatus(): Queue.Status = withContext(Dispatchers.IO) {
        val playlistSongEntities = database.playlistSongs(playlistId).first()
        val playlistSongIds = playlistSongEntities.map { it.map.songId }

        val relatedSongs = playlistSongIds.flatMap { songId ->
            database.relatedSongs(songId)
        }
        val uniqueRelated = relatedSongs.filter { song -> song.id !in playlistSongIds }.distinctBy { it.id }
        val finalMix = uniqueRelated.smartShuffle(database).take(maxMixSize)

        Queue.Status(
            title = "Mix from Playlist",
            items = finalMix.map { it.toMediaItem() },
            mediaItemIndex = 0,
        )
    }

    override fun hasNextPage(): Boolean = false
    override suspend fun nextPage(): List<MediaItem> = emptyList()
}

suspend fun List<com.omnitune.app.db.entities.Song>.smartShuffle(database: MusicDatabase): List<com.omnitune.app.db.entities.Song> {
    val songIds = this.map { it.song.id }
    val playCounts = database.getPlayCounts(songIds).associateBy { it.songId }
    val skipCounts = database.getSkipCounts(songIds).associateBy { it.songId }

    val maxPlay = playCounts.values.maxOfOrNull { it.playCount }?.toFloat() ?: 1f
    val maxSkip = skipCounts.values.maxOfOrNull { it.skipCount }?.toFloat() ?: 1f

    return this
        .map { song ->
            val playScore = (playCounts[song.song.id]?.playCount?.toFloat() ?: 0f) / maxPlay
            val skipScore = (skipCounts[song.song.id]?.skipCount?.toFloat() ?: 0f) / maxSkip
            val weight = (0.7f * playScore) - (0.3f * skipScore) + 0.1f  // base weight floor
            song to weight
        }
        .sortedByDescending { (_, weight) -> weight + (Math.random().toFloat() * 0.4f) } // weighted random
        .map { (song, _) -> song }
}

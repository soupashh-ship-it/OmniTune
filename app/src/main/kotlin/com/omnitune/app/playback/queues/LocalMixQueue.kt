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
        val finalMix = uniqueRelated.take(maxMixSize)

        Queue.Status(
            title = "Mix from Playlist",
            items = finalMix.map { it.toMediaItem() },
            mediaItemIndex = 0,
        )
    }

    override fun hasNextPage(): Boolean = false
    override suspend fun nextPage(): List<MediaItem> = emptyList()
}

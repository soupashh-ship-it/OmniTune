/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.playback.continuation

import androidx.media3.common.MediaItem
import com.omnitune.app.db.MusicDatabase
import com.omnitune.app.extensions.toMediaItem
import com.omnitune.app.models.MediaMetadata
import com.omnitune.innertube.YouTube
import com.omnitune.innertube.models.SongItem
import com.omnitune.innertube.models.WatchEndpoint
import kotlinx.coroutines.flow.first

interface AutoplayRecommendationProvider {
    suspend fun songsForVerifiedGenre(genre: String): List<MediaItem>
    suspend fun songsForVerifiedMoodOrTag(tag: String): List<MediaItem>
    suspend fun songsForArtist(artist: String): List<MediaItem>
    suspend fun songsRelatedToTrack(track: MediaMetadata): List<MediaItem>
    suspend fun songsForTitleSearch(track: MediaMetadata): List<MediaItem>
    suspend fun quickPicks(seed: MediaMetadata): List<MediaItem>
}

class OmniAutoplayRecommendationProvider(
    private val database: MusicDatabase,
) : AutoplayRecommendationProvider {
    override suspend fun songsForVerifiedGenre(genre: String): List<MediaItem> =
        searchSongs("$genre music")

    override suspend fun songsForVerifiedMoodOrTag(tag: String): List<MediaItem> =
        searchSongs("$tag music")

    override suspend fun songsForArtist(artist: String): List<MediaItem> =
        searchSongs("$artist top songs")

    override suspend fun songsRelatedToTrack(track: MediaMetadata): List<MediaItem> {
        val related = runCatching {
            val next = YouTube.next(WatchEndpoint(videoId = track.id)).getOrThrow()
            val relatedEndpoint = next.relatedEndpoint
            if (relatedEndpoint != null) {
                YouTube.related(relatedEndpoint).getOrThrow().songs
            } else {
                next.items.drop((next.currentIndex ?: -1) + 1)
            }
        }.getOrElse { emptyList() }

        return related.toMediaItems()
    }

    override suspend fun songsForTitleSearch(track: MediaMetadata): List<MediaItem> {
        val artist = track.artists.firstOrNull()?.name.orEmpty()
        val query = listOf(track.title, artist)
            .filter { it.isNotBlank() }
            .joinToString(" ")
        return searchSongs(query)
    }

    override suspend fun quickPicks(seed: MediaMetadata): List<MediaItem> {
        val local = database.quickPicks().first().map { it.toMediaItem() }
        if (local.isNotEmpty()) return local

        val artist = seed.artists.firstOrNull()?.name.orEmpty()
        val query = artist.takeIf { it.isNotBlank() }?.let { "$it songs" } ?: "music discovery"
        return searchSongs(query)
    }

    private suspend fun searchSongs(query: String): List<MediaItem> =
        runCatching {
            YouTube.search(query, YouTube.SearchFilter.FILTER_SONG)
                .getOrThrow()
                .items
                .filterIsInstance<SongItem>()
                .toMediaItems()
        }.getOrElse { emptyList() }

    private fun List<SongItem>.toMediaItems(): List<MediaItem> =
        map { it.toMediaItem() }
}


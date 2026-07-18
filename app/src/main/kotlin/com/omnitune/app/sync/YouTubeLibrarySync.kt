/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.sync

import com.omnitune.app.db.entities.AlbumEntity
import com.omnitune.app.db.entities.ArtistEntity
import com.omnitune.app.db.entities.PlaylistEntity
import com.omnitune.app.db.entities.SongEntity
import com.omnitune.innertube.YouTube
import kotlinx.coroutines.CancellationException
import timber.log.Timber

object YouTubeLibrarySync {
    suspend fun syncSongLike(song: SongEntity, liked: Boolean = song.liked) {
        runCatching {
            YouTube.likeVideo(song.id, liked)
        }.onFailure { error ->
            if (error is CancellationException) throw error
            Timber.tag(TAG).w(error, "Failed to sync YouTube song like for ${song.id}")
        }
    }

    suspend fun syncPlaylistBookmark(playlist: PlaylistEntity, bookmarked: Boolean = playlist.bookmarkedAt != null) {
        val browseId = playlist.browseId ?: return
        runCatching {
            YouTube.likePlaylist(browseId, bookmarked)
        }.onFailure { error ->
            if (error is CancellationException) throw error
            Timber.tag(TAG).w(error, "Failed to sync YouTube playlist bookmark for $browseId")
        }
    }

    suspend fun syncArtistBookmark(artist: ArtistEntity, bookmarked: Boolean = artist.bookmarkedAt != null) {
        runCatching {
            val channelId = artist.channelId ?: YouTube.getChannelId(artist.id)
            YouTube.subscribeChannel(channelId, bookmarked)
        }.onFailure { error ->
            if (error is CancellationException) throw error
            Timber.tag(TAG).w(error, "Failed to sync YouTube artist bookmark for ${artist.id}")
        }
    }

    suspend fun syncAlbumBookmark(album: AlbumEntity, bookmarked: Boolean = album.bookmarkedAt != null) {
        val playlistId = album.playlistId ?: return
        runCatching {
            YouTube.likePlaylist(playlistId, bookmarked)
        }.onFailure { error ->
            if (error is CancellationException) throw error
            Timber.tag(TAG).w(error, "Failed to sync YouTube album bookmark for ${album.id}")
        }
    }

    private const val TAG = "YouTubeLibrarySync"
}

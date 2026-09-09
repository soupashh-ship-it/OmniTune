/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.playback

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.omnitune.app.db.MusicDatabase
import com.omnitune.app.db.entities.Playlist
import com.omnitune.app.db.entities.Song
import com.omnitune.app.extensions.toMediaItem
import kotlinx.coroutines.flow.first
import kotlin.math.min

internal class MediaLibraryBrowser(
    private val database: MusicDatabase,
    private val downloadUtil: DownloadUtil,
) {
    suspend fun item(mediaId: String): MediaItem? {
        return when {
            mediaId == ROOT_ID -> rootItem()
            mediaId in categoryIds -> categoryItem(mediaId)
            mediaId.startsWith(PLAYLIST_PREFIX) -> {
                val playlistId = mediaId.removePrefix(PLAYLIST_PREFIX)
                database.getPlaylistById(playlistId)?.toBrowsableItem()
            }
            else -> database.getSongById(mediaId)?.toPlayableItem()
        }
    }

    suspend fun children(
        parentId: String,
        page: Int,
        pageSize: Int,
        offlineOnly: Boolean,
    ): List<MediaItem>? {
        val allItems = when {
            parentId == ROOT_ID -> rootChildren(offlineOnly)
            parentId == SONGS_ID -> database.songsByRowIdAsc().first().map { it.toPlayableItem() }
            parentId == LIKED_ID -> database.likedSongsByRowIdAsc().first().map { it.toPlayableItem() }
            parentId == DOWNLOADS_ID -> completedDownloads()
            parentId == PLAYLISTS_ID -> database.playlistsByCustomOrderAsc().first().map { it.toBrowsableItem() }
            parentId.startsWith(PLAYLIST_PREFIX) -> {
                val playlistId = parentId.removePrefix(PLAYLIST_PREFIX)
                database.playlistSongs(playlistId).first().map { it.song.toPlayableItem() }
            }
            else -> return null
        }

        return pageItems(allItems, page, pageSize)
    }

    suspend fun search(query: String, page: Int, pageSize: Int): List<MediaItem> {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isBlank()) return emptyList()

        val songs = database.searchSongs(trimmedQuery, previewSize = MAX_SEARCH_RESULTS)
            .first()
            .map { it.toPlayableItem() }
        val playlists = database.searchPlaylists(trimmedQuery, previewSize = MAX_SEARCH_RESULTS)
            .first()
            .map { it.toBrowsableItem() }

        return pageItems(songs + playlists, page, pageSize)
    }

    suspend fun searchResultCount(query: String): Int {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isBlank()) return 0
        val songCount = database.searchSongs(trimmedQuery, previewSize = MAX_SEARCH_RESULTS).first().size
        val playlistCount = database.searchPlaylists(trimmedQuery, previewSize = MAX_SEARCH_RESULTS).first().size
        return (songCount + playlistCount).coerceAtMost(MAX_SEARCH_RESULTS * 2)
    }

    private fun rootChildren(offlineOnly: Boolean): List<MediaItem> {
        return if (offlineOnly) {
            listOf(categoryItem(DOWNLOADS_ID))
        } else {
            listOf(
                categoryItem(LIKED_ID),
                categoryItem(SONGS_ID),
                categoryItem(DOWNLOADS_ID),
                categoryItem(PLAYLISTS_ID),
            )
        }
    }

    private suspend fun completedDownloads(): List<MediaItem> {
        return database.allSongs()
            .first()
            .filter { song -> downloadUtil.findPlayableDownload(song.id) != null }
            .map { song -> song.toPlayableItem(offline = true) }
    }

    companion object {
        const val ROOT_ID = "omnitune:library"
        const val LIKED_ID = "omnitune:library:liked"
        const val SONGS_ID = "omnitune:library:songs"
        const val DOWNLOADS_ID = "omnitune:library:downloads"
        const val PLAYLISTS_ID = "omnitune:library:playlists"
        const val PLAYLIST_PREFIX = "omnitune:library:playlist:"
        const val MAX_SEARCH_RESULTS = 50

        private val categoryIds = setOf(LIKED_ID, SONGS_ID, DOWNLOADS_ID, PLAYLISTS_ID)

        fun <T> pageItems(items: List<T>, page: Int, pageSize: Int): List<T> {
            if (page < 0 || pageSize <= 0) return emptyList()
            val fromIndex = page.toLong() * pageSize.toLong()
            if (fromIndex >= items.size || fromIndex > Int.MAX_VALUE) return emptyList()
            val from = fromIndex.toInt()
            val to = min(items.size, from + pageSize)
            return items.subList(from, to)
        }

        fun rootItem(): MediaItem = browsableItem(
            mediaId = ROOT_ID,
            title = "OmniTune",
            subtitle = "Music library",
        )

        fun categoryItem(mediaId: String): MediaItem = when (mediaId) {
            LIKED_ID -> browsableItem(mediaId, "Liked Songs", "Songs you liked")
            SONGS_ID -> browsableItem(mediaId, "Library Songs", "Saved songs")
            DOWNLOADS_ID -> browsableItem(mediaId, "Downloads", "Available offline")
            PLAYLISTS_ID -> browsableItem(mediaId, "Playlists", "Saved playlists")
            else -> browsableItem(mediaId, "OmniTune", "Music library")
        }

        private fun browsableItem(
            mediaId: String,
            title: String,
            subtitle: String? = null,
            artworkUri: String? = null,
        ): MediaItem {
            return MediaItem.Builder()
                .setMediaId(mediaId)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(title)
                        .setSubtitle(subtitle)
                        .setArtworkUri(artworkUri?.let(Uri::parse))
                        .setIsBrowsable(true)
                        .setIsPlayable(false)
                        .build()
                )
                .build()
        }

        private fun Song.toPlayableItem(offline: Boolean = false): MediaItem {
            val item = toMediaItem()
            val metadataBuilder = item.mediaMetadata
                .buildUpon()
                .setIsBrowsable(false)
                .setIsPlayable(true)
            if (offline) {
                metadataBuilder.setIsBrowsable(false)
            }
            return item.buildUpon()
                .setMediaMetadata(metadataBuilder.build())
                .build()
        }

        private fun Playlist.toBrowsableItem(): MediaItem {
            val subtitle = when (songCount) {
                1 -> "1 song"
                else -> "$songCount songs"
            }
            return browsableItem(
                mediaId = PLAYLIST_PREFIX + id,
                title = title,
                subtitle = subtitle,
                artworkUri = thumbnails.firstOrNull(),
            )
        }
    }
}

/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.playback

import android.net.Uri
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.MediaConstants
import androidx.media3.session.MediaLibraryService.LibraryParams
import com.omnitune.app.db.MusicDatabase
import com.omnitune.app.db.entities.Playlist
import com.omnitune.app.db.entities.Song
import com.omnitune.app.extensions.toMediaItem
import kotlinx.coroutines.flow.first
import kotlin.math.min

internal data class MediaContentStyleHints(
    val singleItemStyle: Int = MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_LIST_ITEM,
    val browsableChildrenStyle: Int = MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_LIST_ITEM,
    val playableChildrenStyle: Int = MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_LIST_ITEM,
)

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

        internal val RootContentStyleHints = MediaContentStyleHints(
            singleItemStyle = MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_CATEGORY_LIST_ITEM,
            browsableChildrenStyle = MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_CATEGORY_LIST_ITEM,
            playableChildrenStyle = MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_LIST_ITEM,
        )
        internal val CategoryContentStyleHints = MediaContentStyleHints(
            singleItemStyle = MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_CATEGORY_LIST_ITEM,
            browsableChildrenStyle = MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_LIST_ITEM,
            playableChildrenStyle = MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_LIST_ITEM,
        )

        private val categoryIds = setOf(LIKED_ID, SONGS_ID, DOWNLOADS_ID, PLAYLISTS_ID)

        fun rootParams(requested: LibraryParams?): LibraryParams =
            LibraryParams.Builder()
                .setRecent(requested?.isRecent == true)
                .setOffline(requested?.isOffline == true)
                .setSuggested(requested?.isSuggested == true)
                .setExtras(rootExtras(requested?.extras))
                .build()

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
            contentStyleHints = RootContentStyleHints,
        )

        fun categoryItem(mediaId: String): MediaItem = when (mediaId) {
            LIKED_ID -> categoryBrowsableItem(mediaId, "Liked Songs", "Songs you liked")
            SONGS_ID -> categoryBrowsableItem(mediaId, "Library Songs", "Saved songs")
            DOWNLOADS_ID -> categoryBrowsableItem(mediaId, "Downloads", "Available offline")
            PLAYLISTS_ID -> categoryBrowsableItem(mediaId, "Playlists", "Saved playlists")
            else -> categoryBrowsableItem(mediaId, "OmniTune", "Music library")
        }

        private fun rootExtras(requestedExtras: Bundle?): Bundle =
            Bundle(requestedExtras ?: Bundle.EMPTY).apply {
                putInt(
                    MediaConstants.EXTRAS_KEY_CONTENT_STYLE_BROWSABLE,
                    RootContentStyleHints.browsableChildrenStyle,
                )
                putInt(
                    MediaConstants.EXTRAS_KEY_CONTENT_STYLE_PLAYABLE,
                    RootContentStyleHints.playableChildrenStyle,
                )
            }

        private fun categoryBrowsableItem(
            mediaId: String,
            title: String,
            subtitle: String? = null,
        ): MediaItem =
            browsableItem(
                mediaId = mediaId,
                title = title,
                subtitle = subtitle,
                contentStyleHints = CategoryContentStyleHints,
            )

        private fun browsableItem(
            mediaId: String,
            title: String,
            subtitle: String? = null,
            artworkUri: String? = null,
            contentStyleHints: MediaContentStyleHints = MediaContentStyleHints(),
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
                        .setExtras(
                            contentStyleExtras(
                                hints = contentStyleHints,
                            )
                        )
                        .build()
                )
                .build()
        }

        private fun contentStyleExtras(hints: MediaContentStyleHints): Bundle =
            Bundle().apply {
                putInt(MediaConstants.EXTRAS_KEY_CONTENT_STYLE_SINGLE_ITEM, hints.singleItemStyle)
                putInt(MediaConstants.EXTRAS_KEY_CONTENT_STYLE_BROWSABLE, hints.browsableChildrenStyle)
                putInt(MediaConstants.EXTRAS_KEY_CONTENT_STYLE_PLAYABLE, hints.playableChildrenStyle)
            }

        private fun Song.toPlayableItem(offline: Boolean = false): MediaItem {
            val item = toMediaItem()
            val extras = Bundle(item.mediaMetadata.extras ?: Bundle.EMPTY).apply {
                putLong(
                    MediaConstants.EXTRAS_KEY_DOWNLOAD_STATUS,
                    if (offline) {
                        MediaConstants.EXTRAS_VALUE_STATUS_DOWNLOADED
                    } else {
                        MediaConstants.EXTRAS_VALUE_STATUS_NOT_DOWNLOADED
                    },
                )
            }
            val metadataBuilder = item.mediaMetadata
                .buildUpon()
                .setIsBrowsable(false)
                .setIsPlayable(true)
                .setExtras(extras)
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

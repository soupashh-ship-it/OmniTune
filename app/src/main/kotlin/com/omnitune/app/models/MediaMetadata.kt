/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */



package com.omnitune.app.models

import androidx.compose.runtime.Immutable
import com.omnitune.innertube.models.WatchEndpoint.WatchEndpointMusicSupportedConfigs.WatchEndpointMusicConfig.Companion.MUSIC_VIDEO_TYPE_OMV
import com.omnitune.innertube.models.WatchEndpoint.WatchEndpointMusicSupportedConfigs.WatchEndpointMusicConfig.Companion.MUSIC_VIDEO_TYPE_UGC
import com.omnitune.innertube.models.SongItem
import com.omnitune.app.db.entities.Song as DbSong
import com.omnitune.app.db.entities.SongEntity
import com.omnitune.app.ui.utils.resize
import java.io.Serializable
import java.time.LocalDateTime

@Immutable
data class MediaMetadata(
    val id: String,
    val title: String,
    val artists: List<Artist>,
    val duration: Int,
    val thumbnailUrl: String? = null,
    val album: Album? = null,
    val setVideoId: String? = null,
    val explicit: Boolean = false,
    val liked: Boolean = false,
    val likedDate: LocalDateTime? = null,
    val inLibrary: LocalDateTime? = null,
    val genre: String? = null,
    val mood: String? = null,
    val isVideo: Boolean = false,
) : Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }

    data class Artist(
        val id: String?,
        val name: String,
        val thumbnailUrl: String? = null,
    ) : Serializable {
        companion object {
            private const val serialVersionUID = 1L
        }
    }

    data class Album(
        val id: String,
        val title: String,
    ) : Serializable {
        companion object {
            private const val serialVersionUID = 1L
        }
    }

    fun toSongEntity() =
        SongEntity(
            id = id,
            title = title,
            duration = duration,
            thumbnailUrl = thumbnailUrl,
            albumId = album?.id,
            albumName = album?.title,
            explicit = explicit,
            liked = liked,
            likedDate = likedDate,
            inLibrary = inLibrary,
            isVideo = isVideo,
        )
}

fun DbSong.toMediaMetadata() =
    MediaMetadata(
        id = song.id,
        title = song.title,
        artists =
        artists.map {
            MediaMetadata.Artist(
                id = it.id,
                name = it.name,
                thumbnailUrl = it.thumbnailUrl,
            )
        },
        duration = song.duration,
        thumbnailUrl = song.thumbnailUrl,
        album =
        album?.let {
            MediaMetadata.Album(
                id = it.id,
                title = it.title,
            )
        } ?: song.albumId?.let { albumId ->
            MediaMetadata.Album(
                id = albumId,
                title = song.albumName.orEmpty(),
            )
        },
        isVideo = song.isVideo,
    )

fun SongItem.toMediaMetadata() =
    MediaMetadata(
        id = id,
        title = title,
        artists =
        artists.map {
            MediaMetadata.Artist(
                id = it.id,
                name = it.name,
                thumbnailUrl = null,
            )
        },
        duration = duration ?: -1,
        thumbnailUrl = thumbnail.resize(544, 544),
        album =
        album?.let {
            MediaMetadata.Album(
                id = it.id,
                title = it.name,
            )
        },
        explicit = explicit,
        setVideoId = setVideoId,
        isVideo = isMusicVideo(),
    )

fun MediaMetadata.toDomainSong(): Song =
    Song(
        id = id,
        title = title,
        artist = artists.joinToString(", ") { it.name },
        album = album?.title.orEmpty(),
        duration = duration.toLong() * 1000L,
        thumbnailUrl = thumbnailUrl,
        setVideoId = setVideoId,
        artistId = artists.firstOrNull()?.id,
        isVideo = isVideo,
    )

fun Song.toMediaItem(): androidx.media3.common.MediaItem {
    return androidx.media3.common.MediaItem.Builder()
        .setMediaId(id)
        .setUri(localUri?.let { android.net.Uri.parse(it) } ?: android.net.Uri.parse("https://youtube.com/watch?v=$id"))
        .setMediaMetadata(
            androidx.media3.common.MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artist)
                .setAlbumTitle(album)
                .setArtworkUri(thumbnailUrl?.let { android.net.Uri.parse(it) })
                .setExtras(android.os.Bundle().apply {
                    putBoolean("com.omnitune.app.extra.IS_MUSIC_VIDEO", isVideo)
                })
                .build()
        )
        .build()
}

private fun SongItem.isMusicVideo(): Boolean {
    val musicVideoType = endpoint?.watchEndpointMusicSupportedConfigs?.watchEndpointMusicConfig?.musicVideoType
    return musicVideoType == MUSIC_VIDEO_TYPE_OMV || musicVideoType == MUSIC_VIDEO_TYPE_UGC
}

/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */



package com.omnitune.app.extensions

import android.os.Bundle
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata.MEDIA_TYPE_MUSIC
import com.omnitune.innertube.models.SongItem
import com.omnitune.innertube.models.WatchEndpoint.WatchEndpointMusicSupportedConfigs.WatchEndpointMusicConfig.Companion.MUSIC_VIDEO_TYPE_OMV
import com.omnitune.innertube.models.WatchEndpoint.WatchEndpointMusicSupportedConfigs.WatchEndpointMusicConfig.Companion.MUSIC_VIDEO_TYPE_UGC
import com.omnitune.app.db.entities.Song
import com.omnitune.app.models.MediaMetadata
import com.omnitune.app.models.toMediaMetadata
import com.omnitune.app.ui.utils.resize

const val ExtraIsMusicVideo = "com.omnitune.app.extra.IS_MUSIC_VIDEO"
const val ExtraGenre = "com.omnitune.app.extra.GENRE"
const val ExtraMood = "com.omnitune.app.extra.MOOD"

val MediaItem.metadata: MediaMetadata?
    get() = localConfiguration?.tag as? MediaMetadata ?: mediaMetadata.toOmniMetadata(mediaId)

private fun androidx.media3.common.MediaMetadata.toOmniMetadata(mediaId: String): MediaMetadata? {
    val title = title?.toString()?.takeIf { it.isNotBlank() } ?: return null
    val artistNames = (artist ?: subtitle)
        ?.toString()
        ?.split(",")
        ?.map { it.trim() }
        ?.filter { it.isNotBlank() }
        .orEmpty()
    return MediaMetadata(
        id = mediaId,
        title = title,
        artists = artistNames.map { MediaMetadata.Artist(id = null, name = it) },
        duration = -1,
        thumbnailUrl = artworkUri?.toString(),
        album = albumTitle?.toString()?.takeIf { it.isNotBlank() }?.let {
            MediaMetadata.Album(id = "", title = it)
        },
        genre = extras?.getString(ExtraGenre),
        mood = extras?.getString(ExtraMood),
    )
}

private fun playbackExtras(
    isMusicVideo: Boolean,
    genre: String?,
    mood: String?,
) = Bundle().apply {
    putBoolean(ExtraIsMusicVideo, isMusicVideo)
    genre?.takeIf { it.isNotBlank() }?.let { putString(ExtraGenre, it) }
    mood?.takeIf { it.isNotBlank() }?.let { putString(ExtraMood, it) }
}

fun MediaItem.withPlaybackMetadata(
    genre: String? = null,
    mood: String? = null,
): MediaItem {
    val current = metadata ?: return this
    val updated = current.copy(
        genre = genre?.takeIf { it.isNotBlank() } ?: current.genre,
        mood = mood?.takeIf { it.isNotBlank() } ?: current.mood,
    )
    val updatedExtras = Bundle(mediaMetadata.extras ?: Bundle()).apply {
        updated.genre?.takeIf { it.isNotBlank() }?.let { putString(ExtraGenre, it) }
        updated.mood?.takeIf { it.isNotBlank() }?.let { putString(ExtraMood, it) }
    }
    return MediaItem.Builder()
        .setMediaId(mediaId)
        .setUri(localConfiguration?.uri ?: mediaId.toUri())
        .setCustomCacheKey(localConfiguration?.customCacheKey ?: mediaId)
        .setMimeType(localConfiguration?.mimeType)
        .setTag(updated)
        .setMediaMetadata(mediaMetadata.buildUpon().setExtras(updatedExtras).build())
        .build()
}

fun Song.toMediaItem() =
    MediaItem
        .Builder()
        .setMediaId(song.id)
        .setUri(song.id)
        .setCustomCacheKey(song.id)
        .setTag(toMediaMetadata())
        .setMediaMetadata(
            androidx.media3.common.MediaMetadata
                .Builder()
                .setTitle(song.title)
                .setSubtitle(artists.joinToString { it.name })
                .setArtist(artists.joinToString { it.name })
                .setArtworkUri(song.thumbnailUrl?.resize(800, 800)?.toUri())
                .setAlbumTitle(song.albumName)
                .setMediaType(MEDIA_TYPE_MUSIC)
                .setExtras(playbackExtras(isMusicVideo = false, genre = null, mood = null))
                .build(),
        ).build()

fun SongItem.toMediaItem() =
    MediaItem
        .Builder()
        .setMediaId(id)
        .setUri(id)
        .setCustomCacheKey(id)
        .setTag(toMediaMetadata())
        .setMediaMetadata(
            androidx.media3.common.MediaMetadata
                .Builder()
                .setTitle(title)
                .setSubtitle(artists.joinToString { it.name })
                .setArtist(artists.joinToString { it.name })
                .setArtworkUri(thumbnail.resize(800, 800).toUri())
                .setAlbumTitle(album?.name)
                .setMediaType(MEDIA_TYPE_MUSIC)
                .setExtras(playbackExtras(isMusicVideo = isMusicVideo(), genre = null, mood = null))
                .build(),
        ).build()

fun MediaMetadata.toMediaItem() =
    MediaItem
        .Builder()
        .setMediaId(id)
        .setUri(id)
        .setCustomCacheKey(id)
        .setTag(this)
        .setMediaMetadata(
            androidx.media3.common.MediaMetadata
                .Builder()
                .setTitle(title)
                .setSubtitle(artists.joinToString { it.name })
                .setArtist(artists.joinToString { it.name })
                .setArtworkUri(thumbnailUrl?.resize(800, 800)?.toUri())
                .setAlbumTitle(album?.title)
                .setMediaType(MEDIA_TYPE_MUSIC)
                .setExtras(playbackExtras(isMusicVideo = false, genre = genre, mood = mood))
                .build(),
        ).build()

private fun SongItem.isMusicVideo(): Boolean {
    val musicVideoType = endpoint?.watchEndpointMusicSupportedConfigs?.watchEndpointMusicConfig?.musicVideoType
    return musicVideoType == MUSIC_VIDEO_TYPE_OMV || musicVideoType == MUSIC_VIDEO_TYPE_UGC
}

/*
 * Adapted from SuvMusic (https://github.com/SuvojeetDev/SuvMusic)
 * Copyright (c) SuvMusic contributors
 * Licensed under GPL-3.0
 */

package com.omnitune.app.models

import com.omnitune.innertube.models.SongItem as InnerSongItem
import com.omnitune.innertube.models.AlbumItem as InnerAlbumItem
import com.omnitune.innertube.models.PlaylistItem as InnerPlaylistItem
import com.omnitune.innertube.models.ArtistItem as InnerArtistItem
import com.omnitune.app.db.entities.Song as DbSong
import com.omnitune.app.models.MediaMetadata

data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val album: String = "",
    val duration: Long = 0L,
    val thumbnailUrl: String? = null,
    val source: SongSource = SongSource.YOUTUBE,
    val streamUrl: String? = null,
    val localUri: String? = null,
    val setVideoId: String? = null,
    val removalFeedbackToken: String? = null,
    val artistId: String? = null,
    val originalSource: SongSource? = null,
    val isVideo: Boolean = false,
    val customFolderPath: String? = null,
    val collectionId: String? = null,
    val collectionName: String? = null,
    val isMembersOnly: Boolean = false,
    val releaseDate: String? = null,
    val addedAt: Long = 0L,
    val remoteAudioMetadata: Any? = null
)

enum class SongSource {
    YOUTUBE,
    YOUTUBE_MUSIC,
    LOCAL,
    DOWNLOADED,
    REMOTE
}

data class Album(
    val id: String,
    val title: String,
    val artist: String,
    val year: String? = null,
    val thumbnailUrl: String? = null,
    val description: String? = null,
    val songs: List<Song> = emptyList()
)

data class Artist(
    val id: String,
    val name: String,
    val thumbnailUrl: String? = null,
    val description: String? = null,
    val subscribers: String? = null,
    val songs: List<Song> = emptyList(),
    val albums: List<Album> = emptyList(),
    val singles: List<Album> = emptyList(),
    val isSubscribed: Boolean = false,
    val channelId: String? = null,
    val views: String? = null,
    val videos: List<Song> = emptyList(),
    val relatedArtists: List<ArtistPreview> = emptyList(),
    val featuredPlaylists: List<Playlist> = emptyList()
)

data class ArtistPreview(
    val id: String,
    val name: String,
    val thumbnailUrl: String?,
    val subscribers: String? = null
)

data class Playlist(
    val id: String,
    val title: String,
    val author: String,
    val thumbnailUrl: String?,
    val songs: List<Song> = emptyList(),
    val description: String? = null,
    val totalSongCount: Int? = null
)

data class PlaylistDisplayItem(
    val id: String = "",
    val name: String,
    val url: String = "",
    val uploaderName: String = "",
    val thumbnailUrl: String? = null,
    val songCount: Int = 0,
    val description: String? = null
) {
    fun getPlaylistId(): String {
        if (id.isNotBlank()) return id
        return url.substringAfter("list=").substringBefore("&")
    }
}

enum class HomeSectionType {
    HorizontalCarousel,
    Grid,
    LargeCardWithList,
    VerticalList,
    CommunityCarousel,
    ExploreGrid,
    QuickPicks
}

data class HomeSection(
    val title: String,
    val items: List<HomeItem>,
    val type: HomeSectionType = HomeSectionType.HorizontalCarousel
)

sealed class HomeItem {
    abstract val id: String
    data class SongItem(val song: Song) : HomeItem() {
        override val id: String = song.id
    }
    data class PlaylistItem(val playlist: PlaylistDisplayItem, val previewSongs: List<Song> = emptyList()) : HomeItem() {
        override val id: String = playlist.id
    }
    data class AlbumItem(val album: Album) : HomeItem() {
        override val id: String = album.id
    }
    data class ArtistItem(val artist: Artist) : HomeItem() {
        override val id: String = artist.id
    }
    data class ExploreItem(val title: String, val iconRes: Int, val browseId: String) : HomeItem() {
        override val id: String = browseId
    }
}

enum class MiniPlayerStyle(val label: String) {
    STANDARD("Standard"),
    FLOATING_PILL("Floating Pill"),
    YT_MUSIC("YouTube Music"),
    LIQUID_GLASS("Liquid Glass")
}

enum class PlayerStyle {
    YT_MUSIC,
    CLASSIC,
    LIQUID_GLASS
}

enum class PlayerBackgroundStyle(val label: String, val description: String) {
    AMBIENT("Ambient", "Blurred album art fills the screen"),
    CUSTOM("Custom image", "Use your own image as the player background"),
    BLACK("Black", "Solid black - easy on AMOLED screens"),
    LIGHT("Light", "Solid light surface with dark text"),
}

enum class SeekbarStyle {
    WAVEFORM,
    WAVE_LINE,
    CLASSIC,
    DOTS,
    GRADIENT_BAR,
    NEON,
    BLOCKS,
    MATERIAL,
    M3E_WAVY,
}

enum class MusicSource {
    YOUTUBE,
    REMOTE
}

enum class DownloadState {
    NOT_DOWNLOADED,
    DOWNLOADING,
    DOWNLOADED,
    FAILED
}

enum class RepeatMode {
    OFF,
    ALL,
    ONE
}

enum class ArtworkShape {
    ROUNDED_SQUARE,
    CIRCLE,
    VINYL,
    SQUARE,
}

enum class ArtworkSize(val fraction: Float, val label: String) {
    SMALL(0.65f, "Small"),
    MEDIUM(0.75f, "Medium"),
    LARGE(0.85f, "Large"),
    EXTRA_LARGE(0.92f, "Extra Large"),
    FULL(1.0f, "Full Width");

    companion object {
        val MAX_FRACTION: Float by lazy { entries.maxOf { it.fraction } }
    }
}

enum class SleepTimerOption(val minutes: Int, val label: String) {
    OFF(0, "Off"),
    FIVE_MIN(5, "5 minutes"),
    TEN_MIN(10, "10 minutes"),
    FIFTEEN_MIN(15, "15 minutes"),
    THIRTY_MIN(30, "30 minutes"),
    FORTY_FIVE_MIN(45, "45 minutes"),
    ONE_HOUR(60, "1 hour"),
    TWO_HOURS(120, "2 hours"),
    CUSTOM(-2, "Custom"),
    FADE_OUT_GENTLE(-3, "Fade out (5% every 2 min)"),
    FADE_OUT_FAST(-4, "Fade out (5% every 1 min)"),
    END_OF_SONG(-1, "End of song")
}

// Mapper extensions from OmniTune backend models to SuvMusic presentation models:
fun MediaMetadata.toSuvSong(): Song = Song(
    id = id,
    title = title,
    artist = artists.firstOrNull()?.name ?: "",
    album = album?.title ?: "",
    duration = duration * 1000L,
    thumbnailUrl = thumbnailUrl,
    source = SongSource.YOUTUBE
)

fun DbSong.toSuvSong(): Song = Song(
    id = song.id,
    title = song.title,
    artist = artists.joinToString(", ") { it.name },
    album = album?.title ?: "",
    duration = (song.duration ?: 0) * 1000L,
    thumbnailUrl = song.thumbnailUrl,
    source = if (song.isLocal) SongSource.LOCAL else SongSource.YOUTUBE
)

fun InnerSongItem.toSuvSong(): Song = Song(
    id = id,
    title = title,
    artist = artists.joinToString(", ") { it.name },
    album = album?.name ?: "",
    duration = (duration ?: 0) * 1000L,
    thumbnailUrl = thumbnail,
    source = SongSource.YOUTUBE,
    isMembersOnly = false
)

fun InnerAlbumItem.toSuvAlbum(): Album = Album(
    id = id,
    title = title,
    artist = artists?.joinToString(", ") { it.name } ?: "",
    year = year?.toString(),
    thumbnailUrl = thumbnail
)

fun InnerPlaylistItem.toSuvPlaylistDisplayItem(): PlaylistDisplayItem = PlaylistDisplayItem(
    id = id,
    name = title,
    url = "https://music.youtube.com/playlist?list=$id",
    uploaderName = author?.name ?: "",
    thumbnailUrl = thumbnail,
    songCount = songCountText?.filter { it.isDigit() }?.toIntOrNull() ?: 0
)

data class SponsorSegment(
    val start: Long = 0L,
    val end: Long = 0L,
    val category: String = "sponsor"
)

fun Song.toMediaMetadata(): MediaMetadata = MediaMetadata(
    id = id,
    title = title,
    artists = listOf(MediaMetadata.Artist(id = null, name = artist)),
    duration = (duration / 1000L).toInt(),
    thumbnailUrl = thumbnailUrl
)



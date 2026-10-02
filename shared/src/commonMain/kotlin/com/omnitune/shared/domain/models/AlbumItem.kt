package com.omnitune.shared.domain.models

import kotlinx.serialization.Serializable

@Serializable
data class AlbumItem(
    val browseId: String,
    val title: String,
    val year: Int? = null,
    val thumbnailUrl: String? = null,
    val artists: List<Artist> = emptyList(),
    val songs: List<SongItem> = emptyList(),
) {
    val id: String get() = browseId
    val artist: String get() = artists.joinToString(", ") { it.name }
    val trackCount: Int get() = songs.size
    val tracks: List<SongItem> get() = songs
    val shareLink: String get() = "https://music.youtube.com/browse/$browseId"
}

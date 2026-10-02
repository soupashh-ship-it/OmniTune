package com.omnitune.shared.domain.models

import kotlinx.serialization.Serializable

@Serializable
data class ArtistItem(
    val id: String,
    val name: String,
    val thumbnailUrl: String? = null,
    val subscribers: String? = null,
    val topSongs: List<SongItem> = emptyList(),
    val albums: List<AlbumItem> = emptyList(),
) {
    val title: String get() = name
    val shareLink: String get() = "https://music.youtube.com/channel/$id"
}

package com.omnitune.shared.domain.models

import kotlinx.serialization.Serializable

@Serializable
data class PlaylistItem(
    val id: String,
    val title: String,
    val author: String? = null,
    val songCount: Int = 0,
    val thumbnailUrl: String? = null,
    val songs: List<SongItem> = emptyList(),
) {
    val shareLink: String get() = "https://music.youtube.com/playlist?list=$id"
}

package com.omnitune.shared.domain.models

import kotlinx.serialization.Serializable

@Serializable
data class SongItem(
    val id: String,
    val title: String,
    val artists: List<Artist> = emptyList(),
    val album: Album? = null,
    val duration: Int? = null,
    val thumbnailUrl: String? = null,
    val explicit: Boolean = false,
) {
    constructor(
        id: String,
        title: String,
        artist: String,
        album: String? = null,
        duration: Int? = null,
        thumbnailUrl: String? = null,
        explicit: Boolean = false,
    ) : this(
        id = id,
        title = title,
        artists = listOf(Artist(name = artist)),
        album = album?.let { Album(title = it) },
        duration = duration,
        thumbnailUrl = thumbnailUrl,
        explicit = explicit,
    )

    val artist: String
        get() = artists.joinToString(", ") { it.name }

    val durationSec: Int
        get() = duration ?: 0

    val shareLink: String
        get() = "https://music.youtube.com/watch?v=$id"
}

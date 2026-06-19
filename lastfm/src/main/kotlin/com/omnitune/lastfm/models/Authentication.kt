package com.omnitune.lastfm.models

import kotlinx.serialization.Serializable

@Serializable
data class Session(
    val name: String? = null,
    val key: String? = null,
    val subscriber: Boolean = false
)

@Serializable
data class TokenResponse(
    val session: Session? = null,
    val token: String? = null
)

@Serializable
data class ScrobbleRequest(
    val artist: String,
    val track: String,
    val album: String? = null,
    val timestamp: Long,
    val albumArtist: String? = null,
    val trackNumber: Int? = null,
    val duration: Int? = null
)

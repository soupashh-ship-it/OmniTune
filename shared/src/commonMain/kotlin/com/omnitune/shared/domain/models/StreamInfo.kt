package com.omnitune.shared.domain.models

import kotlinx.serialization.Serializable

@Serializable
data class StreamInfo(
    val videoId: String,
    val streamUrl: String,
    val durationMs: Long = 0L,
    val itag: Int = 140,
    val mimeType: String = "audio/mp4",
    val bitrate: Int = 128000,
    val expiresAtMs: Long = 0L,
    val headers: Map<String, String> = emptyMap(),
) {
    fun isExpired(currentTimeMs: Long): Boolean =
        expiresAtMs > 0L && currentTimeMs >= expiresAtMs
}

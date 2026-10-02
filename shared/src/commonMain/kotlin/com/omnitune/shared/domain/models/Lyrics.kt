package com.omnitune.shared.domain.models

import kotlinx.serialization.Serializable

@Serializable
data class LyricLine(
    val timestampMs: Long,
    val text: String,
) {
    val timeMs: Long get() = timestampMs
}

@Serializable
data class Lyrics(
    val videoId: String,
    val plainText: String? = null,
    val syncedLyrics: List<LyricLine> = emptyList(),
    val source: String = "UNKNOWN",
) {
    val isSynced: Boolean get() = syncedLyrics.isNotEmpty()
    val lines: List<LyricLine> get() = syncedLyrics
}

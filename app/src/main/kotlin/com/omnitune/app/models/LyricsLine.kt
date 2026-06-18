package com.omnitune.app.models

data class LyricsLine(
    val timestamp: Long,
    val text: String,
    val isTranslated: Boolean = false
)

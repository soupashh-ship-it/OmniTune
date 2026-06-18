package com.omnitune.app.models

data class StreamInfo(
    val url: String,
    val contentType: String,
    val contentLength: Long? = null,
    val bitrate: Int? = null,
    val cacheHit: Boolean = false
)

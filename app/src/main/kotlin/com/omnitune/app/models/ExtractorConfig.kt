package com.omnitune.app.models

data class ExtractorConfig(
    val preferredQuality: StreamQuality = StreamQuality.HIGH,
    val enableCache: Boolean = true,
    val timeoutMs: Long = 30_000L,
    val maxRetries: Int = 3
)

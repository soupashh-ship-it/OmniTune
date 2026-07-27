package com.omnitune.app.playback

import com.omnitune.app.constants.AudioQuality
import com.omnitune.app.constants.DownloadMaxParallelKey
import com.omnitune.app.constants.DownloadQualityKey
import com.omnitune.app.models.StreamQuality
import com.omnitune.app.utils.PreferenceStore

internal fun preferredDownloadStreamQuality(): StreamQuality {
    val quality = PreferenceStore.get(DownloadQualityKey)
        ?.let { name -> runCatching { AudioQuality.valueOf(name) }.getOrNull() }
        ?: AudioQuality.HIGH
    return when (quality) {
        AudioQuality.LOW -> StreamQuality.LOW
        AudioQuality.AUTO -> StreamQuality.HIGH
        AudioQuality.HIGH -> StreamQuality.HIGH
        AudioQuality.HIGHEST -> StreamQuality.BEST
    }
}

internal fun preferredDownloadParallelism(): Int =
    (PreferenceStore.get(DownloadMaxParallelKey) ?: 3).coerceIn(1, 8)

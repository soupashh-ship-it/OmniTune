/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.playback

import androidx.media3.common.MediaItem

internal object SponsorBlockVideoIdResolver {
    private val YouTubeVideoIdRegex = Regex("^[a-zA-Z0-9_-]{11}$")

    fun fromMediaItem(mediaItem: MediaItem?): String? {
        if (mediaItem == null) return null
        return mediaItem.mediaId.takeIf(::isYouTubeVideoId)
            ?: mediaItem.localConfiguration?.customCacheKey?.takeIf(::isYouTubeVideoId)
            ?: mediaItem.localConfiguration?.uri?.toString()?.takeIf(::isYouTubeVideoId)
    }

    fun isYouTubeVideoId(value: String): Boolean = YouTubeVideoIdRegex.matches(value.trim())
}

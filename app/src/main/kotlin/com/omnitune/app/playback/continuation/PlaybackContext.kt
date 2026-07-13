/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.playback.continuation

import androidx.media3.common.MediaItem

enum class PlaybackSourceType {
    QUICK_PICKS,
    SEARCH_RESULTS,
    LIKED_SONGS,
    PLAYLIST,
    ALBUM,
    ARTIST,
    LIBRARY,
    DOWNLOADS,
    USER_QUEUE,
    AUTOPLAY_RADIO,
    HOME_DISCOVERY,
    UNKNOWN,
}

data class PlaybackContext(
    val sourceType: PlaybackSourceType = PlaybackSourceType.UNKNOWN,
    val sourceId: String? = null,
    val sourceTitle: String? = null,
    val seedSongId: String? = null,
    val genre: String? = null,
    val mood: String? = null,
    val artist: String? = null,
    val allowAutoplay: Boolean = true,
    val shuffledCollection: Boolean = false,
    val sessionItems: List<MediaItem> = emptyList(),
) {
    companion object {
        val Unknown = PlaybackContext()
    }
}

enum class AutoplayCandidateSource {
    VERIFIED_GENRE,
    VERIFIED_MOOD_OR_TAG,
    SAME_ARTIST,
    RELATED_TITLE_SEARCH,
    CURRENT_SESSION_POOL,
    QUICK_PICKS_DISCOVERY,
    NONE,
}

data class AutoplayCandidate(
    val mediaItem: MediaItem,
    val source: AutoplayCandidateSource,
    val reason: String,
)

data class TasteSignal(
    val songId: String,
    val sourceType: PlaybackSourceType,
    val listenedMillis: Long,
    val durationMillis: Long?,
    val completed: Boolean,
    val skippedQuickly: Boolean,
    val positive: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
)

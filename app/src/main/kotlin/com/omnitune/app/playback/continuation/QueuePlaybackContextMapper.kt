/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.playback.continuation

import androidx.media3.common.MediaItem
import com.omnitune.app.db.entities.QueueEntity
import com.omnitune.app.extensions.metadata
import com.omnitune.app.models.MediaMetadata

object QueuePlaybackContextMapper {
    fun fromEntity(
        queue: QueueEntity,
        sessionItems: List<MediaItem>,
    ): PlaybackContext {
        val sourceType = runCatching {
            PlaybackSourceType.valueOf(queue.playbackSourceType.orEmpty())
        }.getOrDefault(PlaybackSourceType.UNKNOWN)

        return PlaybackContext(
            sourceType = sourceType,
            sourceId = queue.playbackSourceId,
            sourceTitle = queue.playbackSourceTitle ?: queue.title,
            seedSongId = queue.playbackSeedSongId,
            genre = queue.playbackGenre,
            mood = queue.playbackMood,
            artist = queue.playbackArtist,
            allowAutoplay = queue.playbackAllowAutoplay,
            shuffledCollection = queue.playbackShuffledCollection,
            sessionItems = sessionItems,
        )
    }
}

fun PlaybackContext.withSeedMetadata(seed: MediaMetadata?): PlaybackContext =
    copy(
        seedSongId = seedSongId ?: seed?.id,
        genre = genre ?: seed?.genre,
        mood = mood ?: seed?.mood,
        artist = artist ?: seed?.artists?.firstOrNull()?.name,
    )

fun PlaybackContext.withSeedItem(seed: MediaItem?): PlaybackContext =
    withSeedMetadata(seed?.metadata)


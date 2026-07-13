/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.playback.continuation

import androidx.media3.common.MediaItem
import com.omnitune.app.extensions.metadata
import com.omnitune.app.models.MediaMetadata

class AutoplayRecommendationResolver(
    private val provider: AutoplayRecommendationProvider,
) {
    suspend fun getNextAutoplayCandidate(
        currentTrack: MediaMetadata,
        playbackContext: PlaybackContext,
        recentlyPlayedIds: Set<String>,
        failedCandidateIds: Set<String>,
    ): AutoplayCandidate? {
        val verifiedGenre = playbackContext.genre?.trim().orEmpty()
        if (verifiedGenre.isNotBlank()) {
            pickFirstPlayable(
                currentTrack = currentTrack,
                candidates = provider.songsForVerifiedGenre(verifiedGenre),
                source = AutoplayCandidateSource.VERIFIED_GENRE,
                reason = "Verified genre metadata: $verifiedGenre",
                recentlyPlayedIds = recentlyPlayedIds,
                failedCandidateIds = failedCandidateIds,
            )?.let { return it }
        }

        val verifiedMood = playbackContext.mood?.trim().orEmpty()
        if (verifiedMood.isNotBlank()) {
            pickFirstPlayable(
                currentTrack = currentTrack,
                candidates = provider.songsForVerifiedMoodOrTag(verifiedMood),
                source = AutoplayCandidateSource.VERIFIED_MOOD_OR_TAG,
                reason = "Verified mood/tag metadata: $verifiedMood",
                recentlyPlayedIds = recentlyPlayedIds,
                failedCandidateIds = failedCandidateIds,
            )?.let { return it }
        }

        val artist = playbackContext.artist?.trim().orEmpty()
            .ifBlank { currentTrack.artists.firstOrNull()?.name?.trim().orEmpty() }
        if (artist.isNotBlank()) {
            pickFirstPlayable(
                currentTrack = currentTrack,
                candidates = provider.songsForArtist(artist),
                source = AutoplayCandidateSource.SAME_ARTIST,
                reason = "Artist continuation: $artist",
                recentlyPlayedIds = recentlyPlayedIds,
                failedCandidateIds = failedCandidateIds,
                requireArtist = true,
            )?.let { return it }
        }

        val relatedCandidates = provider.songsRelatedToTrack(currentTrack)
            .ifEmpty { provider.songsForTitleSearch(currentTrack) }
        pickFirstPlayable(
            currentTrack = currentTrack,
            candidates = relatedCandidates,
            source = AutoplayCandidateSource.RELATED_TITLE_SEARCH,
            reason = "Related/title continuation for ${currentTrack.title}",
            recentlyPlayedIds = recentlyPlayedIds,
            failedCandidateIds = failedCandidateIds,
        )?.let { return it }

        pickFirstPlayable(
            currentTrack = currentTrack,
            candidates = playbackContext.sessionItems,
            source = AutoplayCandidateSource.CURRENT_SESSION_POOL,
            reason = "Remaining current session pool",
            recentlyPlayedIds = recentlyPlayedIds,
            failedCandidateIds = failedCandidateIds,
        )?.let { return it }

        return pickFirstPlayable(
            currentTrack = currentTrack,
            candidates = provider.quickPicks(currentTrack),
            source = AutoplayCandidateSource.QUICK_PICKS_DISCOVERY,
            reason = "General discovery fallback",
            recentlyPlayedIds = recentlyPlayedIds,
            failedCandidateIds = failedCandidateIds,
        )
    }

    private fun pickFirstPlayable(
        currentTrack: MediaMetadata,
        candidates: List<MediaItem>,
        source: AutoplayCandidateSource,
        reason: String,
        recentlyPlayedIds: Set<String>,
        failedCandidateIds: Set<String>,
        requireArtist: Boolean = false,
    ): AutoplayCandidate? {
        val distinctCandidates = candidates.distinctBy { it.mediaId }
        val strict = distinctCandidates
            .firstOrNull { candidate ->
                candidate.isValidAutoplayCandidate(
                    currentTrack = currentTrack,
                    recentlyPlayedIds = recentlyPlayedIds,
                    failedCandidateIds = failedCandidateIds,
                    requireArtist = requireArtist,
                    allowRecentlyPlayed = false,
                )
            }
        val fallback = strict ?: distinctCandidates
            .firstOrNull { candidate ->
                candidate.isValidAutoplayCandidate(
                    currentTrack = currentTrack,
                    recentlyPlayedIds = recentlyPlayedIds,
                    failedCandidateIds = failedCandidateIds,
                    requireArtist = requireArtist,
                    allowRecentlyPlayed = true,
                )
            }
        return fallback?.let { AutoplayCandidate(it, source, reason) }
    }

    private fun MediaItem.isValidAutoplayCandidate(
        currentTrack: MediaMetadata,
        recentlyPlayedIds: Set<String>,
        failedCandidateIds: Set<String>,
        requireArtist: Boolean,
        allowRecentlyPlayed: Boolean,
    ): Boolean {
        val id = mediaId.takeIf { it.isNotBlank() } ?: metadata?.id.orEmpty()
        if (id.isBlank()) return false
        if (id == currentTrack.id) return false
        if (id in failedCandidateIds) return false
        if (!allowRecentlyPlayed && id in recentlyPlayedIds) return false

        val title = metadata?.title ?: mediaMetadata.title?.toString()
        if (title.isNullOrBlank()) return false

        if (requireArtist) {
            val artist = metadata?.artists?.firstOrNull()?.name ?: mediaMetadata.artist?.toString()
            if (artist.isNullOrBlank()) return false
        }

        return true
    }
}

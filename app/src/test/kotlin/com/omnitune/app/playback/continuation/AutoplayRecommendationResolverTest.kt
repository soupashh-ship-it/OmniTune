/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.playback.continuation

import androidx.media3.common.MediaItem
import com.omnitune.app.extensions.toMediaItem
import com.omnitune.app.models.MediaMetadata
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoplayRecommendationResolverTest {
    private val current = track("current", "Current Song", "Seed Artist")

    @Test
    fun `genre is used only when verified genre metadata exists`() = runBlocking {
        val provider = FakeProvider(
            genreSongs = listOf(mediaItem("genre_1", "Genre Song", "Genre Artist")),
            artistSongs = listOf(mediaItem("artist_1", "Artist Song", "Seed Artist")),
        )
        val resolver = AutoplayRecommendationResolver(provider)

        val withoutGenre = resolver.getNextAutoplayCandidate(
            currentTrack = current,
            playbackContext = PlaybackContext(artist = "Seed Artist"),
            recentlyPlayedIds = emptySet(),
            failedCandidateIds = emptySet(),
        )

        assertFalse(provider.genreCalled)
        assertEquals(AutoplayCandidateSource.SAME_ARTIST, withoutGenre?.source)

        val withGenre = resolver.getNextAutoplayCandidate(
            currentTrack = current,
            playbackContext = PlaybackContext(genre = "jazz", artist = "Seed Artist"),
            recentlyPlayedIds = emptySet(),
            failedCandidateIds = emptySet(),
        )

        assertTrue(provider.genreCalled)
        assertEquals(AutoplayCandidateSource.VERIFIED_GENRE, withGenre?.source)
        assertEquals("genre_1", withGenre?.mediaItem?.mediaId)
    }

    @Test
    fun `missing genre falls back to artist`() = runBlocking {
        val provider = FakeProvider(
            artistSongs = listOf(mediaItem("artist_1", "Artist Song", "Seed Artist")),
        )
        val candidate = AutoplayRecommendationResolver(provider).getNextAutoplayCandidate(
            currentTrack = current,
            playbackContext = PlaybackContext(),
            recentlyPlayedIds = emptySet(),
            failedCandidateIds = emptySet(),
        )

        assertEquals(AutoplayCandidateSource.SAME_ARTIST, candidate?.source)
    }

    @Test
    fun `missing artist falls back to related title search then session discovery`() = runBlocking {
        val artistless = current.copy(artists = emptyList())
        val provider = FakeProvider(
            relatedSongs = listOf(mediaItem("related_1", "Related Song", "Other Artist")),
        )
        val related = AutoplayRecommendationResolver(provider).getNextAutoplayCandidate(
            currentTrack = artistless,
            playbackContext = PlaybackContext(),
            recentlyPlayedIds = emptySet(),
            failedCandidateIds = emptySet(),
        )

        assertEquals(AutoplayCandidateSource.RELATED_TITLE_SEARCH, related?.source)

        val session = AutoplayRecommendationResolver(FakeProvider()).getNextAutoplayCandidate(
            currentTrack = artistless,
            playbackContext = PlaybackContext(
                sessionItems = listOf(mediaItem("session_1", "Session Song", "Session Artist")),
            ),
            recentlyPlayedIds = emptySet(),
            failedCandidateIds = emptySet(),
        )

        assertEquals(AutoplayCandidateSource.CURRENT_SESSION_POOL, session?.source)
    }

    @Test
    fun `resolver never returns current track and skips failed candidates`() = runBlocking {
        val provider = FakeProvider(
            artistSongs = listOf(
                mediaItem("current", "Current Song", "Seed Artist"),
                mediaItem("failed_1", "Failed Song", "Seed Artist"),
                mediaItem("valid_1", "Valid Song", "Seed Artist"),
            ),
        )

        val candidate = AutoplayRecommendationResolver(provider).getNextAutoplayCandidate(
            currentTrack = current,
            playbackContext = PlaybackContext(artist = "Seed Artist"),
            recentlyPlayedIds = emptySet(),
            failedCandidateIds = setOf("failed_1"),
        )

        assertEquals("valid_1", candidate?.mediaItem?.mediaId)
    }

    @Test
    fun `recently played candidate is allowed only after pool exhaustion`() = runBlocking {
        val provider = FakeProvider(
            artistSongs = listOf(mediaItem("recent_1", "Recent Song", "Seed Artist")),
        )

        val candidate = AutoplayRecommendationResolver(provider).getNextAutoplayCandidate(
            currentTrack = current,
            playbackContext = PlaybackContext(artist = "Seed Artist"),
            recentlyPlayedIds = setOf("recent_1"),
            failedCandidateIds = emptySet(),
        )

        assertEquals("recent_1", candidate?.mediaItem?.mediaId)
    }

    @Test
    fun `manual queue and disabled autoplay prevent recommendation`() {
        assertFalse(
            PlaybackContinuationPolicy.shouldRunAutoplay(
                autoplayEnabled = true,
                playbackContext = PlaybackContext(),
                hasNextItem = true,
            ),
        )
        assertFalse(
            PlaybackContinuationPolicy.shouldRunAutoplay(
                autoplayEnabled = false,
                playbackContext = PlaybackContext(),
                hasNextItem = false,
            ),
        )
    }

    @Test
    fun `retry limit prevents infinite loops`() {
        assertEquals(3, AutoplayRetryPolicy.MAX_STREAM_RESOLUTION_ATTEMPTS)
    }

    @Test
    fun `taste classifier promotes positive listens and detects quick skips`() {
        assertTrue(TasteSignalClassifier.isPositiveListen(60_000L, 240_000L))
        assertTrue(TasteSignalClassifier.isPositiveListen(48_000L, 120_000L))
        assertFalse(TasteSignalClassifier.isPositiveListen(14_000L, 240_000L))
        assertTrue(TasteSignalClassifier.isQuickSkip(10_000L, completed = false))
        assertFalse(TasteSignalClassifier.isQuickSkip(10_000L, completed = true))
    }

    @Test
    fun `liked songs planner starts at selected index shuffles and loops`() {
        val songs = listOf("a", "b", "c")
        val ordered = LikedSongsPlaybackPlanner.orderedQueue(songs, selectedIndex = 1, shuffled = false)
        assertEquals(listOf("a", "b", "c"), ordered.first)
        assertEquals(1, ordered.second)

        val shuffled = LikedSongsPlaybackPlanner.orderedQueue(songs, selectedIndex = 1, shuffled = true)
        assertEquals("b", shuffled.first.first())
        assertEquals(0, shuffled.second)
        assertEquals(0, LikedSongsPlaybackPlanner.nextLoopIndex(songs.size))
        assertNull(LikedSongsPlaybackPlanner.nextLoopIndex(0))
    }

    private fun track(id: String, title: String, artist: String): MediaMetadata =
        MediaMetadata(
            id = id,
            title = title,
            artists = listOf(MediaMetadata.Artist(id = null, name = artist)),
            duration = 180,
        )

    private fun mediaItem(id: String, title: String, artist: String): MediaItem =
        track(id, title, artist).toMediaItem()

    private class FakeProvider(
        private val genreSongs: List<MediaItem> = emptyList(),
        private val moodSongs: List<MediaItem> = emptyList(),
        private val artistSongs: List<MediaItem> = emptyList(),
        private val relatedSongs: List<MediaItem> = emptyList(),
        private val titleSongs: List<MediaItem> = emptyList(),
        private val quickPickSongs: List<MediaItem> = emptyList(),
    ) : AutoplayRecommendationProvider {
        var genreCalled = false

        override suspend fun songsForVerifiedGenre(genre: String): List<MediaItem> {
            genreCalled = true
            return genreSongs
        }

        override suspend fun songsForVerifiedMoodOrTag(tag: String): List<MediaItem> = moodSongs

        override suspend fun songsForArtist(artist: String): List<MediaItem> = artistSongs

        override suspend fun songsRelatedToTrack(track: MediaMetadata): List<MediaItem> = relatedSongs

        override suspend fun songsForTitleSearch(track: MediaMetadata): List<MediaItem> = titleSongs

        override suspend fun quickPicks(seed: MediaMetadata): List<MediaItem> = quickPickSongs
    }
}


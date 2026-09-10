package com.omnitune.app.ui.screens

import com.omnitune.app.db.entities.ArtistEntity
import com.omnitune.app.db.entities.SongEntity
import com.omnitune.app.models.AccountSessionStateMapper
import com.omnitune.app.models.HomeItem
import com.omnitune.app.models.HomeSection
import com.omnitune.app.models.HomeSectionType
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import com.omnitune.app.db.entities.Song as DbSong

class HomeStateMappersTest {
    @Test
    fun accountState_defaultsToGuestWithoutSignedInCookie() {
        val state = AccountSessionStateMapper.fromStoredAccount(
            plainCookie = "VISITOR_INFO1_LIVE=value",
            accountName = "Saved User",
            accountEmail = "saved@example.com",
            channelHandle = "@saved",
        )

        assertFalse(state.isLoggedIn)
        assertNull(state.userName)
    }

    @Test
    fun accountState_usesStoredNameOnlyWhenCookieIsSignedIn() {
        val state = AccountSessionStateMapper.fromStoredAccount(
            plainCookie = "SID=one; SAPISID=two",
            accountName = "Saved User",
            accountEmail = "saved@example.com",
            channelHandle = "@saved",
        )

        assertTrue(state.isLoggedIn)
        assertEquals("Saved User", state.userName)
    }

    @Test
    fun paginationFailureClearsLoadingButPreservesFeed() {
        val existingSection = HomeSection(
            title = "Quick Picks",
            items = listOf(HomeItem.SongItem(testSong("song-a", "Song A"))),
            type = HomeSectionType.QuickPicks,
        )
        val current = HomeUiState(
            homeSections = listOf(existingSection),
            filteredSections = listOf(existingSection),
            isLoadingMore = true,
        )

        val reduced = HomePaginationStateReducer.failure(current, "Network dropped")

        assertFalse(reduced.isLoadingMore)
        assertEquals(listOf(existingSection), reduced.homeSections)
        assertEquals("Network dropped", reduced.paginationError)
    }

    @Test
    fun paginationSuccessMergesSectionsAndClearsError() {
        val existingSection = HomeSection(
            title = "Quick Picks",
            items = listOf(HomeItem.SongItem(testSong("song-a", "Song A"))),
            type = HomeSectionType.QuickPicks,
        )
        val nextSection = HomeSection(
            title = "New Music",
            items = listOf(HomeItem.SongItem(testSong("song-b", "Song B"))),
            type = HomeSectionType.HorizontalCarousel,
        )
        val current = HomeUiState(
            homeSections = listOf(existingSection),
            filteredSections = listOf(existingSection),
            isLoadingMore = true,
            paginationError = "Old error",
        )

        val reduced = HomePaginationStateReducer.success(
            current = current,
            nextSections = listOf(nextSection),
            nextContinuation = "next-page",
        )

        assertFalse(reduced.isLoadingMore)
        assertNull(reduced.paginationError)
        assertEquals(listOf(existingSection, nextSection), reduced.homeSections)
        assertFalse(reduced.hasReachedEnd)
    }

    @Test
    fun localRecommendationsPreferLikedAndPlayedSongsOverFirstRows() {
        val oldPlainSong = testDbSong(
            id = "row-1",
            title = "First Row",
            inLibrary = LocalDateTime.parse("2026-01-01T00:00:00"),
        )
        val playedSong = testDbSong(
            id = "played",
            title = "Played",
            totalPlayTime = 900_000L,
        )
        val likedSong = testDbSong(
            id = "liked",
            title = "Liked",
            liked = true,
            likedDate = LocalDateTime.parse("2026-02-01T00:00:00"),
        )

        val recommendations = HomeLocalRecommendationMapper.recommendations(
            listOf(oldPlainSong, playedSong, likedSong),
        )

        assertEquals(listOf("liked", "played", "row-1"), recommendations.map { it.id })
    }

    private fun testSong(id: String, title: String): com.omnitune.app.models.Song =
        com.omnitune.app.models.Song(id = id, title = title, artist = "Artist")

    private fun testDbSong(
        id: String,
        title: String,
        liked: Boolean = false,
        likedDate: LocalDateTime? = null,
        totalPlayTime: Long = 0L,
        inLibrary: LocalDateTime? = null,
        dateDownload: LocalDateTime? = null,
    ): DbSong = DbSong(
        song = SongEntity(
            id = id,
            title = title,
            liked = liked,
            likedDate = likedDate,
            totalPlayTime = totalPlayTime,
            inLibrary = inLibrary,
            dateDownload = dateDownload,
        ),
        artists = listOf(ArtistEntity(id = "artist-$id", name = "Artist")),
    )
}

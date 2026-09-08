package com.omnitune.app.ui.screens

import com.omnitune.app.models.HomeSection
import com.omnitune.app.models.HomeSectionType
import com.omnitune.app.models.HomeItem
import com.omnitune.app.models.Song
import com.omnitune.app.models.SongSource
import com.omnitune.app.models.PlaylistDisplayItem
import com.omnitune.app.models.RecentSearchItem
import com.omnitune.app.models.ResultFilter
import com.omnitune.app.models.LibraryFilter
import com.omnitune.app.models.LibrarySortOption
import com.omnitune.app.models.LibraryViewMode
import com.omnitune.app.models.SmartPlaylistType
import org.junit.Assert.*
import org.junit.Test

class TopLevelScreensEdgeCaseTest {

    // --- SearchScreen Edge Cases ---

    @Test
    fun searchUiState_defaultState_hasEmptyResultsAndBlankQuery() {
        val state = SearchUiState()
        assertTrue("Default query should be blank", state.query.isBlank())
        assertTrue("Results should initially be empty", state.results.isEmpty())
        assertTrue("Artist results should initially be empty", state.artistResults.isEmpty())
        assertTrue("Album results should initially be empty", state.albumResults.isEmpty())
        assertFalse("Suggestions should not be shown when empty", state.showSuggestions)
        assertFalse("Should not be loading initially", state.isLoading)
        assertEquals("Default filter should be ALL", ResultFilter.ALL, state.resultFilter)
    }

    @Test
    fun searchUiState_emptySearchResults_correctlyIdentified() {
        val state = SearchUiState(
            query = "NonExistentSongOrArtistXYZ123",
            results = emptyList(),
            artistResults = emptyList(),
            albumResults = emptyList(),
            isLoading = false
        )
        assertTrue("Query is not blank", state.query.isNotBlank())
        assertFalse("Loading is complete", state.isLoading)
        assertTrue("No song results found", state.results.isEmpty())
        assertTrue("No artist results found", state.artistResults.isEmpty())
        assertTrue("No album results found", state.albumResults.isEmpty())
    }

    @Test
    fun searchUiState_filterSwitching_maintainsStateIntegrity() {
        var state = SearchUiState(query = "Rock")
        assertEquals(ResultFilter.ALL, state.resultFilter)

        ResultFilter.entries.forEach { filter ->
            state = state.copy(resultFilter = filter)
            assertEquals(filter, state.resultFilter)
        }
    }

    @Test
    fun searchUiState_recentSearches_removalAndClear() {
        val item1 = RecentSearchItem.QueryItem("Jazz")
        val item2 = RecentSearchItem.QueryItem("Classical")
        val item3 = RecentSearchItem.QueryItem("Metal")

        var recentList = listOf(item1, item2, item3)
        var state = SearchUiState(recentSearches = recentList)
        assertEquals(3, state.recentSearches.size)

        // Remove single item
        recentList = recentList.filterNot { it == item2 }
        state = state.copy(recentSearches = recentList)
        assertEquals(2, state.recentSearches.size)
        assertFalse("Item 2 was removed", state.recentSearches.contains(item2))

        // Clear all
        state = state.copy(recentSearches = emptyList())
        assertTrue("Recent searches cleared", state.recentSearches.isEmpty())
    }

    // --- HomeScreen & Mood Filter Edge Cases ---

    @Test
    fun homeUiState_nullMoodFilter_presentsAllSections() {
        val section1 = HomeSection(title = "Sleep Music", items = emptyList(), type = HomeSectionType.QuickPicks)
        val section2 = HomeSection(title = "Party Hits", items = emptyList(), type = HomeSectionType.ChartPodium)
        val allSections = listOf(section1, section2)

        val state = HomeUiState(
            homeSections = allSections,
            filteredSections = allSections,
            selectedMood = null
        )

        assertNull("Selected mood should be null", state.selectedMood)
        assertEquals("Filtered sections should equal all sections when mood is null", 2, state.filteredSections.size)
    }

    @Test
    fun homeUiState_moodSelectionAndDeselection_revertsToAllSections() {
        val sleepSong = Song("1", "Sleep Sound", "Artist A")
        val partySong = Song("2", "Party Beat", "Artist B")

        val sectionSleep = HomeSection(title = "Sleep Time", items = listOf(HomeItem.SongItem(sleepSong)), type = HomeSectionType.HorizontalCarousel)
        val sectionParty = HomeSection(title = "Party Vibes", items = listOf(HomeItem.SongItem(partySong)), type = HomeSectionType.HorizontalCarousel)
        val allSections = listOf(sectionSleep, sectionParty)

        var state = HomeUiState(
            homeSections = allSections,
            filteredSections = allSections,
            selectedMood = null
        )

        // Select "Sleep"
        val mood = "Sleep"
        val filtered = state.homeSections.filter { it.title.contains(mood, ignoreCase = true) }
        state = state.copy(selectedMood = mood, filteredSections = filtered)
        assertEquals("Sleep", state.selectedMood)
        assertEquals(1, state.filteredSections.size)
        assertEquals("Sleep Time", state.filteredSections.first().title)

        // Deselect "Sleep" (toggle again -> reverts to null)
        val toggledMood = if (state.selectedMood == mood) null else mood
        state = state.copy(
            selectedMood = toggledMood,
            filteredSections = if (toggledMood == null) state.homeSections else filtered
        )
        assertNull("Mood should revert to null on second tap", state.selectedMood)
        assertEquals("All sections restored", 2, state.filteredSections.size)
    }

    @Test
    fun homeUiState_moodWithNoMatches_fallsBackSafely() {
        val section = HomeSection(title = "General Music", items = emptyList(), type = HomeSectionType.HorizontalCarousel)
        val allSections = listOf(section)

        val nonExistentMood = "NonExistentMood"
        val matched = allSections.filter { it.title.contains(nonExistentMood, ignoreCase = true) }
        val finalSections = if (matched.isNotEmpty()) matched else allSections

        val state = HomeUiState(
            homeSections = allSections,
            filteredSections = finalSections,
            selectedMood = nonExistentMood
        )

        assertEquals("NonExistentMood", state.selectedMood)
        assertEquals("Falls back to all sections when mood yields 0 matches", 1, state.filteredSections.size)
    }

    @Test
    fun homeUiState_emptyFeedAndRecommendations_handledCleanly() {
        val state = HomeUiState(
            homeSections = emptyList(),
            filteredSections = emptyList(),
            recommendations = emptyList(),
            isLoading = false,
            error = null
        )
        assertTrue("Sections are empty", state.filteredSections.isEmpty())
        assertTrue("Recommendations are empty", state.recommendations.isEmpty())
        assertFalse("Not loading", state.isLoading)
        assertNull("No error", state.error)
    }

    // --- LibraryScreen Edge Cases ---

    @Test
    fun libraryUiState_emptyLibrary_smartPlaylistsRemainAvailable() {
        val state = LibraryUiState(
            playlists = emptyList(),
            userPlaylists = emptyList(),
            librarySongs = emptyList(),
            likedSongs = emptyList(),
            likedSongsCount = 0,
            downloadedSongsCount = 0,
            deviceSongsCount = 0,
            top50SongCount = 0,
            cachedSongCount = 0,
            libraryArtists = emptyList(),
            libraryAlbums = emptyList(),
            localFolders = emptyMap(),
            isLoading = false
        )

        assertTrue("User playlists empty", state.userPlaylists.isEmpty())
        assertTrue("Library songs empty", state.librarySongs.isEmpty())
        assertEquals("Liked songs count is 0", 0, state.likedSongsCount)
        assertEquals("Downloaded count is 0", 0, state.downloadedSongsCount)
        assertEquals("Top 50 count is 0", 0, state.top50SongCount)
        assertEquals("Cached count is 0", 0, state.cachedSongCount)
        assertEquals(LibraryFilter.PLAYLISTS, state.selectedFilter)
        assertEquals(LibraryViewMode.GRID, state.viewMode)
    }

    @Test
    fun libraryUiState_filterAndSortTransitions() {
        var state = LibraryUiState()

        // Filter transitions
        LibraryFilter.entries.forEach { filter ->
            state = state.copy(selectedFilter = filter)
            assertEquals(filter, state.selectedFilter)
        }

        // View mode transitions
        state = state.copy(viewMode = LibraryViewMode.LIST)
        assertEquals(LibraryViewMode.LIST, state.viewMode)
        state = state.copy(viewMode = LibraryViewMode.GRID)
        assertEquals(LibraryViewMode.GRID, state.viewMode)

        // Sort transitions
        state = state.copy(sortOption = LibrarySortOption.NAME)
        assertEquals(LibrarySortOption.NAME, state.sortOption)
        state = state.copy(sortOption = LibrarySortOption.DATE_ADDED)
        assertEquals(LibrarySortOption.DATE_ADDED, state.sortOption)
    }

    @Test
    fun libraryUiState_searchFiltering_filtersByNameCaseInsensitively() {
        val p1 = PlaylistDisplayItem(id = "1", name = "Workout Beats", url = "", uploaderName = "You", thumbnailUrl = null, songCount = 10)
        val p2 = PlaylistDisplayItem(id = "2", name = "Chill Lounge", url = "", uploaderName = "You", thumbnailUrl = null, songCount = 5)
        val p3 = PlaylistDisplayItem(id = "3", name = "Night Drive", url = "", uploaderName = "You", thumbnailUrl = null, songCount = 8)
        val rawPlaylists = listOf(p1, p2, p3)

        // Search for "chill" (lowercase)
        val query1 = "chill"
        val filtered1 = rawPlaylists.filter { it.name.contains(query1.trim(), ignoreCase = true) }
        assertEquals(1, filtered1.size)
        assertEquals("Chill Lounge", filtered1.first().name)

        // Search for non-existent
        val query2 = "NonExistent123"
        val filtered2 = rawPlaylists.filter { it.name.contains(query2.trim(), ignoreCase = true) }
        assertTrue("No match yields empty list", filtered2.isEmpty())

        // Blank query restores all
        val query3 = "   "
        val filtered3 = if (query3.isBlank()) rawPlaylists else rawPlaylists.filter { it.name.contains(query3.trim(), ignoreCase = true) }
        assertEquals(3, filtered3.size)
    }

    // --- SettingsScreen Search Filtering Edge Cases ---

    data class MockSettingsSearchEntry(
        val title: String,
        val subtitle: String,
        val keywords: String
    )

    private val sampleSettingsIndex = listOf(
        MockSettingsSearchEntry("Appearance", "Theme, dark mode, colors, liquid glass", "theme dark mode light colors dynamic material amoled gradient glass"),
        MockSettingsSearchEntry("Playback", "Audio quality, gapless, equalizer, crossfade", "audio quality bitrate gapless equalizer eq crossfade normalization loudness"),
        MockSettingsSearchEntry("Customization", "Player UI, artwork shape/size, seekbar style", "player ui artwork shape size seekbar style mini player vinyl glass"),
        MockSettingsSearchEntry("AI Assistant", "Google Gemini, OpenAI, Anthropic", "ai assistant openai anthropic gemini equalizer smart"),
        MockSettingsSearchEntry("SponsorBlock", "Skip non-music segments", "sponsorblock skip segments intro outro sponsor"),
        MockSettingsSearchEntry("Storage Manager", "Manage downloads & cache", "storage downloads cache clear space data"),
        MockSettingsSearchEntry("Listening Insights", "Your listening stats & habits", "stats statistics listening history wrapped activity")
    )

    private fun filterSettings(query: String): List<MockSettingsSearchEntry> {
        if (query.isBlank()) return emptyList()
        val q = query.lowercase().trim()
        return sampleSettingsIndex.filter {
            it.title.lowercase().contains(q) ||
            it.subtitle.lowercase().contains(q) ||
            it.keywords.contains(q)
        }
    }

    @Test
    fun settingsSearch_blankQuery_returnsEmptyFilteredList() {
        assertTrue(filterSettings("").isEmpty())
        assertTrue(filterSettings("   ").isEmpty())
        assertTrue(filterSettings("\t\n").isEmpty())
    }

    @Test
    fun settingsSearch_matchesTitleCaseInsensitively() {
        val results = filterSettings("appearance")
        assertEquals(1, results.size)
        assertEquals("Appearance", results.first().title)

        val resultsUpper = filterSettings("PLAYBACK")
        assertEquals(1, resultsUpper.size)
        assertEquals("Playback", resultsUpper.first().title)
    }

    @Test
    fun settingsSearch_matchesSubtitle() {
        val results = filterSettings("gapless")
        assertEquals(1, results.size)
        assertEquals("Playback", results.first().title)
    }

    @Test
    fun settingsSearch_matchesKeywords() {
        val results = filterSettings("amoled")
        assertEquals(1, results.size)
        assertEquals("Appearance", results.first().title)

        val resultsGemini = filterSettings("gemini")
        assertEquals(1, resultsGemini.size)
        assertEquals("AI Assistant", resultsGemini.first().title)
    }

    @Test
    fun settingsSearch_noMatchReturnsEmpty() {
        val results = filterSettings("xyzNonExistentFeature999")
        assertTrue("Non matching query returns empty list", results.isEmpty())
    }
}

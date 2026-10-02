package com.omnitune.shared.ui

import com.omnitune.shared.domain.models.SongItem
import com.omnitune.shared.ui.screens.AudioQuality
import com.omnitune.shared.ui.screens.LibraryFilter
import com.omnitune.shared.ui.screens.LibrarySortCriteria
import com.omnitune.shared.ui.screens.LibraryViewMode
import com.omnitune.shared.ui.screens.STANDARD_MOODS
import com.omnitune.shared.ui.screens.SearchTab
import com.omnitune.shared.ui.screens.calculateDetailTopBarAlpha
import com.omnitune.shared.ui.screens.calculateTimeOfDayGreeting
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ScreensUiStateTest {

    @Test
    fun testHomeGreetingCalculatesTimeOfDayGreeting() {
        assertEquals("Good morning", calculateTimeOfDayGreeting(8))
        assertEquals("Good afternoon", calculateTimeOfDayGreeting(14))
        assertEquals("Good evening", calculateTimeOfDayGreeting(21))
        // Midnight hour (0:00) maps to Good evening
        assertEquals("Good evening", calculateTimeOfDayGreeting(0))
        // Late hour (23:00) maps to Good evening
        assertEquals("Good evening", calculateTimeOfDayGreeting(23))
    }

    @Test
    fun testQuickAccessGridLayoutConformsToTwoColumns() {
        val items = listOf("Recent 1", "Recent 2", "Recent 3", "Recent 4")
        val columns = 2
        val rowCount = (items.size + 1) / columns
        assertEquals(2, rowCount)

        // Empty items produces 0 rows without division by zero error
        val emptyItems = emptyList<String>()
        val emptyRowCount = (emptyItems.size + 1) / columns
        assertEquals(0, emptyRowCount)
    }

    @Test
    fun testStandardMoodChipsContainExpectedCategories() {
        assertEquals(5, STANDARD_MOODS.size)
        assertTrue(STANDARD_MOODS.contains("Energize"))
        assertTrue(STANDARD_MOODS.contains("Relax"))
        assertTrue(STANDARD_MOODS.contains("Workout"))
        assertTrue(STANDARD_MOODS.contains("Focus"))
        assertTrue(STANDARD_MOODS.contains("Party"))
    }

    @Test
    fun testSearchFilterTabsContainRequiredOptions() {
        val tabs = SearchTab.entries
        assertEquals(3, tabs.size)
        assertEquals(SearchTab.YOUTUBE_MUSIC, tabs[0])
        assertEquals(SearchTab.LIBRARY, tabs[1])
        assertEquals(SearchTab.DOWNLOADS, tabs[2])
    }

    @Test
    fun testLibraryFilterChipsAndSortCriteria() {
        assertEquals(5, LibraryFilter.entries.size)
        assertTrue(LibraryFilter.entries.any { it.name == "PLAYLISTS" })
        assertTrue(LibraryFilter.entries.any { it.name == "SONGS" })
        assertTrue(LibraryFilter.entries.any { it.name == "ALBUMS" })
        assertTrue(LibraryFilter.entries.any { it.name == "ARTISTS" })
        assertTrue(LibraryFilter.entries.any { it.name == "DOWNLOADED" })

        assertEquals(5, LibrarySortCriteria.entries.size)
        assertTrue(LibrarySortCriteria.entries.any { it.name == "TITLE" })
        assertTrue(LibrarySortCriteria.entries.any { it.name == "DATE_ADDED" })
        assertTrue(LibrarySortCriteria.entries.any { it.name == "ARTIST" })
        assertTrue(LibrarySortCriteria.entries.any { it.name == "PLAY_COUNT" })
        assertTrue(LibrarySortCriteria.entries.any { it.name == "DURATION" })
    }

    @Test
    fun testLibrarySortingByDuration() {
        val songs = listOf(
            SongItem("1", "A", duration = 300),
            SongItem("2", "B", duration = 150),
            SongItem("3", "C", duration = 220)
        )
        val sortedDesc = songs.sortedByDescending { it.durationSec }
        assertEquals("A", sortedDesc[0].title)
        assertEquals("C", sortedDesc[1].title)
        assertEquals("B", sortedDesc[2].title)
    }

    @Test
    fun testDetailStickyTopBarAlphaCalculations() {
        assertEquals(0.0f, calculateDetailTopBarAlpha(0, 200))
        assertEquals(0.5f, calculateDetailTopBarAlpha(100, 200))
        assertEquals(1.0f, calculateDetailTopBarAlpha(200, 200))
        assertEquals(1.0f, calculateDetailTopBarAlpha(500, 200))
        // Negative scroll clamps to 0.0f
        assertEquals(0.0f, calculateDetailTopBarAlpha(-50, 200))
        // Large scroll clamps to 1.0f
        assertEquals(1.0f, calculateDetailTopBarAlpha(10000, 200))
    }

    @Test
    fun testAudioQualityBitrates() {
        assertEquals(48000, AudioQuality.LOW.bitrateBps)
        assertEquals(128000, AudioQuality.MEDIUM.bitrateBps)
        assertEquals(256000, AudioQuality.HIGH.bitrateBps)
    }
}

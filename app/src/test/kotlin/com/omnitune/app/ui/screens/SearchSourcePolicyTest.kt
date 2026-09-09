package com.omnitune.app.ui.screens

import com.omnitune.app.models.ResultFilter
import com.omnitune.app.models.SearchFilterTab
import com.omnitune.app.models.SearchTab
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchSourcePolicyTest {
    @Test
    fun `hq audio tab is hidden when no real remote backend is wired`() {
        assertFalse(SearchSourcePolicy.isHqAudioSearchEnabled)
        assertEquals(listOf(SearchTab.YOUTUBE_MUSIC), SearchSourcePolicy.visibleTabs)
    }

    @Test
    fun `remote tab requests are coerced to the visible youtube source`() {
        assertEquals(SearchTab.YOUTUBE_MUSIC, SearchSourcePolicy.normalize(SearchTab.REMOTE))
        assertEquals(SearchTab.YOUTUBE_MUSIC, SearchSourcePolicy.normalize(SearchTab.YOUTUBE_MUSIC))
    }

    @Test
    fun `filters expose youtube categories while hq audio is unavailable`() {
        val filters = SearchSourcePolicy.filtersFor(SearchTab.REMOTE).map { it.first }

        assertTrue(ResultFilter.VIDEOS in filters)
        assertTrue(ResultFilter.FEATURED_PLAYLISTS in filters)
        assertEquals(ResultFilter.ALL, SearchSourcePolicy.normalizeFilter(SearchTab.REMOTE, ResultFilter.ALL))
    }

    @Test
    fun `request gate rejects stale search responses`() {
        val gate = SearchRequestGate()
        val first = gate.begin("first", SearchFilterTab.ALL)
        val second = gate.begin("second", SearchFilterTab.ALL)

        assertFalse(gate.accepts(first))
        assertTrue(gate.accepts(second))
    }
}

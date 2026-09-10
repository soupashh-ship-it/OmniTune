package com.omnitune.app.ui.screens

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.omnitune.app.models.ResultFilter
import com.omnitune.app.models.SearchFilterTab
import com.omnitune.app.models.SearchTab
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SearchViewModelInstrumentedTest {

    @Test
    fun hqAudioTabIsHiddenUntilItHasARealRepository() {
        assertFalse(SearchSourcePolicy.isHqAudioSearchEnabled)
        assertEquals(listOf(SearchTab.YOUTUBE_MUSIC), SearchSourcePolicy.visibleTabs)
        assertEquals(SearchTab.YOUTUBE_MUSIC, SearchSourcePolicy.normalize(SearchTab.REMOTE))
    }

    @Test
    fun visibleYoutubeTabKeepsAllSupportedFilters() {
        assertEquals(
            listOf(
                ResultFilter.ALL,
                ResultFilter.SONGS,
                ResultFilter.VIDEOS,
                ResultFilter.ALBUMS,
                ResultFilter.ARTISTS,
                ResultFilter.COMMUNITY_PLAYLISTS,
                ResultFilter.FEATURED_PLAYLISTS,
            ),
            SearchSourcePolicy.filtersFor(SearchTab.YOUTUBE_MUSIC).map { it.first },
        )
    }

    @Test
    fun resultFiltersMapToCurrentRequestFilters() {
        assertEquals(SearchFilterTab.ALL, SearchSourcePolicy.toRequestFilter(ResultFilter.ALL))
        assertEquals(SearchFilterTab.SONGS, SearchSourcePolicy.toRequestFilter(ResultFilter.SONGS))
        assertEquals(SearchFilterTab.VIDEOS, SearchSourcePolicy.toRequestFilter(ResultFilter.VIDEOS))
        assertEquals(SearchFilterTab.ALBUMS, SearchSourcePolicy.toRequestFilter(ResultFilter.ALBUMS))
        assertEquals(SearchFilterTab.ARTISTS, SearchSourcePolicy.toRequestFilter(ResultFilter.ARTISTS))
        assertEquals(SearchFilterTab.PLAYLISTS, SearchSourcePolicy.toRequestFilter(ResultFilter.COMMUNITY_PLAYLISTS))
        assertEquals(SearchFilterTab.FEATURED, SearchSourcePolicy.toRequestFilter(ResultFilter.FEATURED_PLAYLISTS))
    }
}

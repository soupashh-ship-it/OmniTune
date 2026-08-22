package com.omnitune.app.ui.navigation

import com.omnitune.app.playback.continuation.PlaybackSourceType
import com.omnitune.innertube.models.Artist
import com.omnitune.innertube.models.SongItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SongPlaybackRequestTest {
    @Test
    fun `keeps the selected search song and playback provenance`() {
        val songs = listOf(song("searchSong01", "First"), song("searchSong02", "Second"))

        val request = songPlaybackRequest(
            title = "Search Results",
            songs = songs,
            startIndex = 1,
            sourceType = PlaybackSourceType.SEARCH_RESULTS,
        )

        requireNotNull(request)
        assertEquals("searchSong02", request.selectedSong.id)
        assertEquals(1, request.startIndex)
        assertEquals(PlaybackSourceType.SEARCH_RESULTS, request.sourceType)
    }

    @Test
    fun `rejects invalid selections before they can reach playback`() {
        val songs = listOf(song("searchSong01", "First"))

        assertNull(
            songPlaybackRequest(
                title = "Search Results",
                songs = songs,
                startIndex = -1,
                sourceType = PlaybackSourceType.SEARCH_RESULTS,
            ),
        )
        assertNull(
            songPlaybackRequest(
                title = "Search Results",
                songs = songs,
                startIndex = 1,
                sourceType = PlaybackSourceType.SEARCH_RESULTS,
            ),
        )
    }

    private fun song(id: String, title: String) = SongItem(
        id = id,
        title = title,
        artists = listOf(Artist(name = "Fixture Artist", id = "fixtureArtist")),
        thumbnail = "https://example.invalid/$id.jpg",
    )
}

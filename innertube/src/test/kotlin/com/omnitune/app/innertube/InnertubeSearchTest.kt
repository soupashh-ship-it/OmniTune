package com.omnitune.innertube

import com.omnitune.innertube.models.AlbumItem
import com.omnitune.innertube.models.ArtistItem
import com.omnitune.innertube.models.PlaylistItem
import com.omnitune.innertube.models.SongItem
import com.omnitune.innertube.models.YTItem
import kotlinx.coroutines.runBlocking
import org.junit.Test

class InnertubeSearchTest {

    @Test
    fun searchReturnsResults() = runBlocking {
        val result: Result<com.omnitune.innertube.pages.SearchResult> =
            YouTube.search("shape of you", YouTube.SearchFilter.FILTER_SONG)
        val searchResult: com.omnitune.innertube.pages.SearchResult =
            result.getOrThrow()
        val items: List<YTItem> = searchResult.items
        println("Search returned ${items.size} items")
        items.forEachIndexed { index: Int, item: YTItem ->
            val label: String = when (item) {
                is SongItem ->
                    "Song: ${item.title} - ${item.artists?.firstOrNull()?.name}"
                is AlbumItem ->
                    "Album: ${item.title} - ${item.artists?.firstOrNull()?.name}"
                is ArtistItem ->
                    "Artist: ${item.title}"
                is PlaylistItem ->
                    "Playlist: ${item.title}"
                else -> "Other: $item"
            }
            println("  [$index] $label")
        }
        assert(items.isNotEmpty()) { "Search should return results" }
    }
}

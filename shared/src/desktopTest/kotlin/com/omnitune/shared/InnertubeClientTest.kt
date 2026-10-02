package com.omnitune.shared

import com.omnitune.shared.data.innertube.InnertubeClient
import com.omnitune.shared.domain.models.SearchFilter
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class InnertubeClientTest {

    @Test
    fun testSearchSuggestions() = runBlocking {
        val client = InnertubeClient()
        val result = client.getSearchSuggestions("Ed Sheeran")
        assertTrue(result.isSuccess)
        val suggestions = result.getOrNull()
        assertNotNull(suggestions)
        assertTrue(suggestions.isNotEmpty())
        assertTrue(suggestions.any { it.contains("ed sheeran", ignoreCase = true) })
    }

    @Test
    fun testSearchSongs() = runBlocking {
        val client = InnertubeClient()
        val result = client.search("Shape of You", SearchFilter.SONGS)
        assertTrue(result.isSuccess)
        val songs = result.getOrNull()
        assertNotNull(songs)
        assertTrue(songs.isNotEmpty())
        assertEquals("Shape of You", songs.first().title.substringBefore(" -"))
    }

    @Test
    fun testGetAlbum() = runBlocking {
        val client = InnertubeClient()
        val result = client.getAlbum("MPREb_divide")
        assertTrue(result.isSuccess)
        val album = result.getOrNull()
        assertNotNull(album)
        assertEquals("MPREb_divide", album.browseId)
        assertTrue(album.songs.isNotEmpty())
    }

    @Test
    fun testGetArtist() = runBlocking {
        val client = InnertubeClient()
        val result = client.getArtist("UC_edsheeran")
        assertTrue(result.isSuccess)
        val artist = result.getOrNull()
        assertNotNull(artist)
        assertEquals("UC_edsheeran", artist.id)
        assertTrue(artist.topSongs.isNotEmpty())
    }

    @Test
    fun testResolveStreamUrl() = runBlocking {
        val client = InnertubeClient()
        val result = client.resolveStreamUrl("shape_of_you_video")
        assertTrue(result.isSuccess)
        val stream = result.getOrNull()
        assertNotNull(stream)
        assertEquals(140, stream.itag)
        assertTrue(stream.streamUrl.contains("&pot="))
        assertEquals("audio/mp4", stream.mimeType)
    }
}

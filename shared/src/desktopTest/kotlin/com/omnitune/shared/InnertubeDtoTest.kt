package com.omnitune.shared

import com.omnitune.shared.data.innertube.models.AdaptiveFormatDto
import com.omnitune.shared.data.innertube.models.BrowseRequestBody
import com.omnitune.shared.data.innertube.models.FormatDto
import com.omnitune.shared.data.innertube.models.SearchRequestBody
import com.omnitune.shared.data.innertube.models.SearchRunText
import com.omnitune.shared.data.innertube.models.SearchRuns
import com.omnitune.shared.data.innertube.models.SearchSuggestionContent
import com.omnitune.shared.data.innertube.models.SearchSuggestionItem
import com.omnitune.shared.data.innertube.models.SearchSuggestionRenderer
import com.omnitune.shared.data.innertube.models.SearchSuggestionsResponse
import com.omnitune.shared.data.innertube.models.SearchSuggestionsSectionRenderer
import com.omnitune.shared.data.innertube.models.StreamingDataResponse
import com.omnitune.shared.domain.models.Album
import com.omnitune.shared.domain.models.AlbumItem
import com.omnitune.shared.domain.models.Artist
import com.omnitune.shared.domain.models.ArtistItem
import com.omnitune.shared.domain.models.LyricLine
import com.omnitune.shared.domain.models.Lyrics
import com.omnitune.shared.domain.models.PlaylistItem
import com.omnitune.shared.domain.models.SearchFilter
import com.omnitune.shared.domain.models.SongItem
import com.omnitune.shared.domain.models.StreamInfo
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class InnertubeDtoTest {

    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
        isLenient = true
        prettyPrint = false
    }

    @Test
    fun testSearchRequestBodySerialization() {
        val request = SearchRequestBody(
            query = "Imagine Dragons",
            params = SearchFilter.SONGS.param
        )
        val serialized = json.encodeToString(request)
        assertTrue(serialized.contains("Imagine Dragons"))
        assertTrue(serialized.contains("WEB_REMIX"))

        val deserialized = json.decodeFromString<SearchRequestBody>(serialized)
        assertEquals("Imagine Dragons", deserialized.query)
        assertEquals(SearchFilter.SONGS.param, deserialized.params)
        assertEquals("WEB_REMIX", deserialized.context.client.clientName)
    }

    @Test
    fun testBrowseRequestBodySerialization() {
        val request = BrowseRequestBody(browseId = "MPREb_12345678")
        val serialized = json.encodeToString(request)
        val deserialized = json.decodeFromString<BrowseRequestBody>(serialized)
        assertEquals("MPREb_12345678", deserialized.browseId)
    }

    @Test
    fun testSearchSuggestionsResponseSerialization() {
        val response = SearchSuggestionsResponse(
            contents = listOf(
                SearchSuggestionContent(
                    searchSuggestionsSectionRenderer = SearchSuggestionsSectionRenderer(
                        contents = listOf(
                            SearchSuggestionItem(
                                searchSuggestionRenderer = SearchSuggestionRenderer(
                                    suggestion = SearchRuns(
                                        runs = listOf(SearchRunText(text = "coldplay live"))
                                    )
                                )
                            )
                        )
                    )
                )
            )
        )
        val serialized = json.encodeToString(response)
        val deserialized = json.decodeFromString<SearchSuggestionsResponse>(serialized)
        val suggestion = deserialized.contents?.firstOrNull()
            ?.searchSuggestionsSectionRenderer?.contents?.firstOrNull()
            ?.searchSuggestionRenderer?.suggestion?.runs?.firstOrNull()?.text
        assertEquals("coldplay live", suggestion)
    }

    @Test
    fun testStreamingDataResponseSerialization() {
        val streamingData = StreamingDataResponse(
            formats = listOf(
                FormatDto(itag = 18, url = "https://example.com/18", mimeType = "video/mp4", bitrate = 300000)
            ),
            adaptiveFormats = listOf(
                AdaptiveFormatDto(itag = 140, url = "https://example.com/140", mimeType = "audio/mp4", bitrate = 128000),
                AdaptiveFormatDto(itag = 251, url = "https://example.com/251", mimeType = "audio/webm", bitrate = 160000)
            ),
            expiresInSeconds = "21600"
        )
        val serialized = json.encodeToString(streamingData)
        val deserialized = json.decodeFromString<StreamingDataResponse>(serialized)

        assertEquals(1, deserialized.formats.size)
        assertEquals(2, deserialized.adaptiveFormats.size)
        assertEquals(140, deserialized.adaptiveFormats[0].itag)
        assertEquals(251, deserialized.adaptiveFormats[1].itag)
    }

    @Test
    fun testDomainModelsSerializationRoundTrip() {
        val song = SongItem(
            id = "song_123",
            title = "Starboy",
            artists = listOf(Artist(name = "The Weeknd", id = "artist_1"), Artist(name = "Daft Punk", id = "artist_2")),
            album = Album(id = "album_1", title = "Starboy"),
            duration = 230,
            thumbnailUrl = "https://example.com/thumb.jpg",
            explicit = true
        )
        val songJson = json.encodeToString(song)
        val decodedSong = json.decodeFromString<SongItem>(songJson)
        assertEquals("song_123", decodedSong.id)
        assertEquals("The Weeknd, Daft Punk", decodedSong.artist)
        assertEquals("Starboy", decodedSong.album?.title)
        assertTrue(decodedSong.explicit)

        val album = AlbumItem(
            browseId = "MPREb_album",
            title = "After Hours",
            year = 2020,
            thumbnailUrl = "https://example.com/album.jpg",
            artists = listOf(Artist(name = "The Weeknd")),
            songs = listOf(song)
        )
        val albumJson = json.encodeToString(album)
        val decodedAlbum = json.decodeFromString<AlbumItem>(albumJson)
        assertEquals("MPREb_album", decodedAlbum.browseId)
        assertEquals(1, decodedAlbum.trackCount)

        val artist = ArtistItem(
            id = "UC_artist",
            name = "The Weeknd",
            thumbnailUrl = "https://example.com/artist.jpg",
            subscribers = "30M",
            topSongs = listOf(song),
            albums = listOf(album)
        )
        val artistJson = json.encodeToString(artist)
        val decodedArtist = json.decodeFromString<ArtistItem>(artistJson)
        assertEquals("The Weeknd", decodedArtist.name)
        assertEquals(1, decodedArtist.topSongs.size)

        val playlist = PlaylistItem(
            id = "PL_playlist",
            title = "Today's Hits",
            author = "OmniTune Curators",
            songCount = 1,
            thumbnailUrl = "https://example.com/pl.jpg",
            songs = listOf(song)
        )
        val playlistJson = json.encodeToString(playlist)
        val decodedPlaylist = json.decodeFromString<PlaylistItem>(playlistJson)
        assertEquals("Today's Hits", decodedPlaylist.title)

        val streamInfo = StreamInfo(
            videoId = "song_123",
            streamUrl = "https://example.com/stream?id=song_123&itag=140",
            durationMs = 230000L,
            itag = 140,
            mimeType = "audio/mp4",
            bitrate = 128000,
            expiresAtMs = 1800000000000L
        )
        val streamJson = json.encodeToString(streamInfo)
        val decodedStream = json.decodeFromString<StreamInfo>(streamJson)
        assertEquals(140, decodedStream.itag)
        assertEquals(230000L, decodedStream.durationMs)

        val lyrics = Lyrics(
            videoId = "song_123",
            plainText = "I'm tryna put you in the worst mood, ah",
            syncedLyrics = listOf(
                LyricLine(12000L, "I'm tryna put you in the worst mood, ah"),
                LyricLine(16000L, "P1 cleaner than your church shoes, ah")
            ),
            source = "LRCLIB"
        )
        val lyricsJson = json.encodeToString(lyrics)
        val decodedLyrics = json.decodeFromString<Lyrics>(lyricsJson)
        assertTrue(decodedLyrics.isSynced)
        assertEquals(2, decodedLyrics.syncedLyrics.size)
        assertEquals(12000L, decodedLyrics.syncedLyrics[0].timestampMs)
    }
}

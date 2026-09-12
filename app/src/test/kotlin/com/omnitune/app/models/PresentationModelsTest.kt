package com.omnitune.app.models

import com.omnitune.app.extensions.metadata
import com.omnitune.innertube.models.Artist
import com.omnitune.innertube.models.SongItem
import com.omnitune.innertube.models.WatchEndpoint
import com.omnitune.innertube.models.WatchEndpoint.WatchEndpointMusicSupportedConfigs
import com.omnitune.innertube.models.WatchEndpoint.WatchEndpointMusicSupportedConfigs.WatchEndpointMusicConfig
import com.omnitune.innertube.models.WatchEndpoint.WatchEndpointMusicSupportedConfigs.WatchEndpointMusicConfig.Companion.MUSIC_VIDEO_TYPE_ATV
import com.omnitune.innertube.models.WatchEndpoint.WatchEndpointMusicSupportedConfigs.WatchEndpointMusicConfig.Companion.MUSIC_VIDEO_TYPE_OMV
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PresentationModelsTest {
    @Test
    fun `innertube music videos map to presentation video songs`() {
        val song = songItem(musicVideoType = MUSIC_VIDEO_TYPE_OMV).toPresentationSong()

        assertTrue(song.isVideo)
    }

    @Test
    fun `innertube audio tracks stay audio even with playlist set video id`() {
        val song = songItem(musicVideoType = MUSIC_VIDEO_TYPE_ATV, setVideoId = "playlist-entry").toPresentationSong()

        assertFalse(song.isVideo)
    }

    @Test
    fun `presentation YouTube songs create resolver friendly media items`() {
        val song = Song(
            id = "dQw4w9WgXcQ",
            title = "Fixture",
            artist = "Fixture Artist",
            thumbnailUrl = "https://example.invalid/thumb.jpg",
        )
        val mediaItem = song.toMediaItem()

        assertEquals("dQw4w9WgXcQ", song.playbackUriString())
        assertEquals("dQw4w9WgXcQ", mediaItem.mediaId)
        assertEquals("Fixture", mediaItem.metadata?.title)
    }

    @Test
    fun `presentation local songs keep their local uri`() {
        val song = Song(
            id = "local-track",
            title = "Local Fixture",
            artist = "Fixture Artist",
            source = SongSource.LOCAL,
            localUri = "content://media/audio/1",
        )
        val mediaItem = song.toMediaItem()

        assertEquals("content://media/audio/1", song.playbackUriString())
        assertEquals("local-track", mediaItem.metadata?.id)
    }

    private fun songItem(
        musicVideoType: String,
        setVideoId: String? = null,
    ) = SongItem(
        id = "video123456",
        title = "Fixture",
        artists = listOf(Artist(name = "Artist", id = "artist")),
        duration = 180,
        thumbnail = "https://example.invalid/thumb.jpg",
        endpoint = WatchEndpoint(
            videoId = "video123456",
            watchEndpointMusicSupportedConfigs = WatchEndpointMusicSupportedConfigs(
                watchEndpointMusicConfig = WatchEndpointMusicConfig(musicVideoType),
            ),
        ),
        setVideoId = setVideoId,
    )
}

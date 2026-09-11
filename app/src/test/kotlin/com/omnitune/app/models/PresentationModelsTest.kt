package com.omnitune.app.models

import com.omnitune.innertube.models.Artist
import com.omnitune.innertube.models.SongItem
import com.omnitune.innertube.models.WatchEndpoint
import com.omnitune.innertube.models.WatchEndpoint.WatchEndpointMusicSupportedConfigs
import com.omnitune.innertube.models.WatchEndpoint.WatchEndpointMusicSupportedConfigs.WatchEndpointMusicConfig
import com.omnitune.innertube.models.WatchEndpoint.WatchEndpointMusicSupportedConfigs.WatchEndpointMusicConfig.Companion.MUSIC_VIDEO_TYPE_ATV
import com.omnitune.innertube.models.WatchEndpoint.WatchEndpointMusicSupportedConfigs.WatchEndpointMusicConfig.Companion.MUSIC_VIDEO_TYPE_OMV
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

package com.omnitune.app.viewmodels

import androidx.media3.common.MediaItem
import com.omnitune.app.models.MediaMetadata
import org.junit.Assert.assertEquals
import org.junit.Test

class RelatedSongsMapperTest {
    @Test
    fun `related mapper removes current track and duplicate candidates`() {
        val songs = RelatedSongsMapper.fromMediaItems(
            currentSongId = "current",
            mediaItems = listOf(
                mediaItem("current", "Current"),
                mediaItem("next", "Next"),
                mediaItem("next", "Next duplicate"),
                mediaItem("", "No id"),
            ),
        )

        assertEquals(listOf("next"), songs.map { it.id })
        assertEquals("Next", songs.single().title)
    }

    @Test
    fun `related mapper respects requested limit`() {
        val songs = RelatedSongsMapper.fromMediaItems(
            currentSongId = "seed",
            mediaItems = (1..5).map { mediaItem("song-$it", "Song $it") },
            limit = 3,
        )

        assertEquals(listOf("song-1", "song-2", "song-3"), songs.map { it.id })
    }

    private fun mediaItem(id: String, title: String): MediaItem {
        val metadata = MediaMetadata(
            id = id,
            title = title,
            artists = listOf(MediaMetadata.Artist(id = null, name = "Artist")),
            duration = 180,
        )
        return MediaItem.Builder()
            .setMediaId(id)
            .setUri(id.ifBlank { "about:blank" })
            .setTag(metadata)
            .setMediaMetadata(
                androidx.media3.common.MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtist("Artist")
                    .build()
            )
            .build()
    }
}

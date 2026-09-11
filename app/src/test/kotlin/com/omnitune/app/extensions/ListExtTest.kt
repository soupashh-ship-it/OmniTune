package com.omnitune.app.extensions

import com.omnitune.app.db.entities.SongEntity
import org.junit.Assert.assertEquals
import org.junit.Test
import com.omnitune.app.db.entities.Song as DbSong

class ListExtTest {
    @Test
    fun `filterVideo removes local songs marked as videos`() {
        val audio = dbSong("audio", isVideo = false)
        val video = dbSong("video", isVideo = true)

        assertEquals(listOf(audio), listOf(audio, video).filterVideo())
    }

    @Test
    fun `filterVideo can be disabled`() {
        val audio = dbSong("audio", isVideo = false)
        val video = dbSong("video", isVideo = true)

        assertEquals(listOf(audio, video), listOf(audio, video).filterVideo(enabled = false))
    }

    private fun dbSong(id: String, isVideo: Boolean): DbSong =
        DbSong(
            song = SongEntity(
                id = id,
                title = id,
                isVideo = isVideo,
            ),
            artists = emptyList(),
        )
}

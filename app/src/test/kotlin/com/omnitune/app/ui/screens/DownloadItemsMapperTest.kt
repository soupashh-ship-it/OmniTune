package com.omnitune.app.ui.screens

import androidx.media3.exoplayer.offline.Download
import com.omnitune.app.models.Song
import com.omnitune.app.models.SongSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadItemsMapperTest {
    @Test
    fun `completed download is only marked completed when the cache is playable`() {
        val items = DownloadItemsMapper.build(
            snapshots = listOf(
                snapshot(
                    id = "complete",
                    state = Download.STATE_COMPLETED,
                    isPlayable = true,
                ),
                snapshot(
                    id = "stale",
                    state = Download.STATE_COMPLETED,
                    isPlayable = false,
                ),
            ),
            resolving = emptyMap(),
            songsById = mapOf(
                "complete" to song("complete", "Complete song"),
                "stale" to song("stale", "Stale song"),
            ),
            videoIds = emptySet(),
            waitingForNetwork = false,
        )

        val complete = items.first { it.song.id == "complete" }
        val stale = items.first { it.song.id == "stale" }

        assertEquals(DownloadUiStatus.COMPLETED, complete.status)
        assertEquals(1f, complete.progress, 0.001f)
        assertEquals(SongSource.DOWNLOADED, complete.song.source)
        assertNull(complete.failureReason)
        assertEquals(DownloadUiStatus.FAILED, stale.status)
        assertEquals("Downloaded file is incomplete. Tap retry.", stale.failureReason)
    }

    @Test
    fun `video mapping comes from explicit media type flag instead of playlist identity`() {
        val items = DownloadItemsMapper.build(
            snapshots = listOf(
                snapshot(
                    id = "video-id",
                    state = Download.STATE_COMPLETED,
                    isPlayable = true,
                ),
            ),
            resolving = emptyMap(),
            songsById = mapOf("video-id" to song("video-id", "Music video")),
            videoIds = setOf("video-id"),
            waitingForNetwork = false,
        )

        assertTrue(items.single().song.isVideo)
    }

    @Test
    fun `playlist set video ids do not force audio downloads into videos tab`() {
        val items = DownloadItemsMapper.build(
            snapshots = listOf(
                snapshot(
                    id = "audio-id",
                    state = Download.STATE_COMPLETED,
                    isPlayable = true,
                ),
            ),
            resolving = emptyMap(),
            songsById = mapOf("audio-id" to song("audio-id", "Audio song").copy(setVideoId = "playlist-entry-id")),
            videoIds = emptySet(),
            waitingForNetwork = false,
        )

        assertFalse(items.single().song.isVideo)
    }

    @Test
    fun `resolving download appears until Media3 has an index record`() {
        val items = DownloadItemsMapper.build(
            snapshots = emptyList(),
            resolving = mapOf("starting" to "Starting song"),
            songsById = emptyMap(),
            videoIds = emptySet(),
            waitingForNetwork = false,
        )

        val item = items.single()
        assertEquals("Starting song", item.song.title)
        assertEquals(DownloadUiStatus.RESOLVING, item.status)
        assertEquals(0f, item.progress, 0.001f)
    }

    @Test
    fun `queued downloads surface network requirements when DownloadManager is blocked`() {
        val items = DownloadItemsMapper.build(
            snapshots = listOf(snapshot(id = "queued", state = Download.STATE_QUEUED)),
            resolving = emptyMap(),
            songsById = mapOf("queued" to song("queued", "Queued song")),
            videoIds = emptySet(),
            waitingForNetwork = true,
        )

        assertEquals(DownloadUiStatus.WAITING_FOR_NETWORK, items.single().status)
    }

    private fun snapshot(
        id: String,
        state: Int,
        isPlayable: Boolean = false,
        title: String = id,
        progressPercent: Float = -1f,
    ) = DownloadSnapshot(
        id = id,
        title = title,
        state = state,
        progressPercent = progressPercent,
        bytesDownloaded = 0L,
        contentLength = -1L,
        failureReason = 0,
        isPlayable = isPlayable,
    )

    private fun song(id: String, title: String) = Song(
        id = id,
        title = title,
        artist = "Artist",
    )
}

/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.playback

import com.omnitune.app.db.entities.ArtistEntity
import com.omnitune.app.db.entities.PlaylistSong
import com.omnitune.app.db.entities.PlaylistSongMap
import com.omnitune.app.db.entities.Song
import com.omnitune.app.db.entities.SongEntity
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class PlaylistPlaybackPlannerTest {
    @Test
    fun orderedSongIdsStartsAtSelectedIndexInSavedOrder() {
        val songs = listOf(
            playlistSong(mapId = 3, songId = "c", position = 2),
            playlistSong(mapId = 1, songId = "a", position = 0),
            playlistSong(mapId = 2, songId = "b", position = 1),
        )

        val (ids, startIndex) = PlaylistPlaybackPlanner.orderedSongIds(songs, selectedMapId = 2)

        assertEquals(listOf("a", "b", "c"), ids)
        assertEquals(1, startIndex)
    }

    @Test
    fun shuffledSongIdsIncludesEverySongOnceAndAvoidsNoOpWhenPossible() {
        val songs = listOf(
            playlistSong(mapId = 1, songId = "a", position = 0),
            playlistSong(mapId = 2, songId = "b", position = 1),
            playlistSong(mapId = 3, songId = "c", position = 2),
        )

        val shuffled = PlaylistPlaybackPlanner.shuffledSongIds(songs, Random(7))

        assertEquals(setOf("a", "b", "c"), shuffled.toSet())
        assertEquals(3, shuffled.size)
        assertNotEquals(listOf("a", "b", "c"), shuffled)
    }

    private fun playlistSong(mapId: Int, songId: String, position: Int) = PlaylistSong(
        map = PlaylistSongMap(
            id = mapId,
            playlistId = "playlist",
            songId = songId,
            position = position,
        ),
        song = Song(
            song = SongEntity(id = songId, title = songId),
            artists = listOf(ArtistEntity(id = "artist", name = "Artist")),
        ),
    )
}

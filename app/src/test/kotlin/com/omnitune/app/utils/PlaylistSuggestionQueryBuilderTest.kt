/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.utils

import com.omnitune.app.db.entities.ArtistEntity
import com.omnitune.app.db.entities.PlaylistSong
import com.omnitune.app.db.entities.PlaylistSongMap
import com.omnitune.app.db.entities.Song
import com.omnitune.app.db.entities.SongEntity
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaylistSuggestionQueryBuilderTest {
    @Test
    fun buildSuggestionQueriesUsesPlaylistNameGenreMoodAndTopArtists() {
        val songs = listOf(
            playlistSong("a", "Beach House"),
            playlistSong("b", "Beach House"),
            playlistSong("c", "Slowdive"),
        )

        val queries = PlaylistSuggestionQueryBuilder
            .buildSuggestionQueries("Chill pop", songs)
            .map { it.query }

        assertTrue("Chill pop" in queries)
        assertTrue("pop music" in queries)
        assertTrue("chill music" in queries)
        assertTrue("songs like Beach House" in queries)
    }

    private fun playlistSong(songId: String, artistName: String) = PlaylistSong(
        map = PlaylistSongMap(playlistId = "playlist", songId = songId),
        song = Song(
            song = SongEntity(id = songId, title = songId),
            artists = listOf(ArtistEntity(id = "artist-$songId", name = artistName)),
        ),
    )
}

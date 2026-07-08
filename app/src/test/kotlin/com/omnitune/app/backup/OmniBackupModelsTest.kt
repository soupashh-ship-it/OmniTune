/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.backup

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OmniBackupModelsTest {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Test
    fun snapshotRoundTripPreservesLibraryData() {
        val snapshot = OmniBackupSnapshot(
            createdAtEpochMillis = 1_725_000_000_000L,
            songs = listOf(
                BackupSong(
                    id = "song_1",
                    title = "Track One",
                    liked = true,
                    inLibraryEpochMillis = 1_725_000_000_000L,
                ),
            ),
            playlists = listOf(
                BackupPlaylist(id = "playlist_1", name = "Favorites"),
            ),
            playlistSongs = listOf(
                BackupPlaylistSong(playlistId = "playlist_1", songId = "song_1", position = 0),
            ),
            history = listOf(
                BackupHistoryItem(songId = "song_1", timestampEpochMillis = 1_725_000_001_000L, playTime = 180_000),
            ),
            stats = listOf(
                BackupStatsItem(songId = "song_1", year = 2026, month = 7, count = 4),
            ),
            tags = listOf(
                BackupTag(id = "tag_1", name = "Road trip"),
            ),
            playlistTags = listOf(
                BackupPlaylistTag(playlistId = "playlist_1", tagId = "tag_1"),
            ),
        )

        val encoded = json.encodeToString(snapshot)
        val decoded = json.decodeFromString<OmniBackupSnapshot>(encoded)

        assertEquals(OMNI_BACKUP_FORMAT_VERSION, decoded.formatVersion)
        assertEquals("OmniTune", decoded.appName)
        assertEquals("Track One", decoded.songs.single().title)
        assertTrue(decoded.songs.single().liked)
        assertEquals("song_1", decoded.playlistSongs.single().songId)
        assertEquals(4, decoded.stats.single().count)
        assertEquals("Road trip", decoded.tags.single().name)
        assertEquals("tag_1", decoded.playlistTags.single().tagId)
    }

    @Test
    fun unknownFutureFieldsAreIgnored() {
        val decoded = json.decodeFromString<OmniBackupSnapshot>(
            """
            {
              "formatVersion": 1,
              "appName": "OmniTune",
              "createdAtEpochMillis": 1725000000000,
              "futureField": "ignored",
              "songs": [
                {
                  "id": "song_1",
                  "title": "Track One",
                  "futureSongField": true
                }
              ]
            }
            """.trimIndent(),
        )

        assertEquals("song_1", decoded.songs.single().id)
        assertEquals("Track One", decoded.songs.single().title)
    }
}

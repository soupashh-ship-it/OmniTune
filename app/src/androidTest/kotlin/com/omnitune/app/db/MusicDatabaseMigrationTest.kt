package com.omnitune.app.db

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MusicDatabaseMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        InternalDatabase::class.java,
    )

    @Test
    fun everySupportedVersionMigratesToCurrentSchema() {
        for (startVersion in 1 until 7) {
            val name = "migration-$startVersion"
            helper.createDatabase(name, startVersion).close()
            helper.runMigrationsAndValidate(
                name,
                7,
                true,
                *InternalDatabase.ALL_MIGRATIONS,
            ).close()
        }
    }

    @Test
    fun seededPersistedDataSurvivesEverySupportedMigration() {
        for (startVersion in 1 until 7) {
            val name = "seeded-migration-$startVersion"
            helper.createDatabase(name, startVersion).apply {
                seedPersistedData(startVersion)
                close()
            }

            helper.runMigrationsAndValidate(
                name,
                7,
                true,
                *InternalDatabase.ALL_MIGRATIONS,
            ).use { db ->
                assertEquals(1, db.queryLong("SELECT COUNT(*) FROM song WHERE id = 'song_1' AND title = 'Song One'"))
                assertEquals(1, db.queryLong("SELECT COUNT(*) FROM artist WHERE id = 'artist_1' AND bookmarkedAt IS NOT NULL"))
                assertEquals(1, db.queryLong("SELECT COUNT(*) FROM album WHERE id = 'album_1' AND bookmarkedAt IS NOT NULL"))
                assertEquals(1, db.queryLong("SELECT COUNT(*) FROM playlist WHERE id = 'playlist_1' AND bookmarkedAt IS NOT NULL"))
                assertEquals(1, db.queryLong("SELECT COUNT(*) FROM song_artist_map WHERE songId = 'song_1' AND artistId = 'artist_1'"))
                assertEquals(1, db.queryLong("SELECT COUNT(*) FROM song_album_map WHERE songId = 'song_1' AND albumId = 'album_1'"))
                assertEquals(1, db.queryLong("SELECT COUNT(*) FROM playlist_song_map WHERE playlistId = 'playlist_1' AND songId = 'song_1'"))
                assertEquals(
                    if (startVersion >= 3) 2 else 0,
                    db.queryLong("SELECT download_state FROM song WHERE id = 'song_1'"),
                )
                assertEquals(1, db.queryLong("SELECT COUNT(*) FROM search_history WHERE query = 'private search'"))
                assertEquals(1, db.queryLong("SELECT COUNT(*) FROM format WHERE id = 'song_1' AND itag = 140"))
                assertEquals(1, db.queryLong("SELECT COUNT(*) FROM lyrics WHERE id = 'song_1' AND lyrics = 'seeded lyrics'"))
                assertEquals(1, db.queryLong("SELECT COUNT(*) FROM event WHERE songId = 'song_1' AND playTime = 42000"))
                assertEquals(1, db.queryLong("SELECT COUNT(*) FROM related_song_map WHERE songId = 'song_1' AND relatedSongId = 'song_2'"))
                assertEquals(1, db.queryLong("SELECT COUNT(*) FROM set_video_id WHERE videoId = 'song_1' AND setVideoId = 'set_1'"))
                assertEquals(1, db.queryLong("SELECT COUNT(*) FROM playCount WHERE song = 'song_1' AND year = 2026 AND month = 7 AND count = 3"))
                assertEquals(1, db.queryLong("SELECT COUNT(*) FROM song_skip WHERE songId = 'song_1' AND skipCount = 2"))
                assertEquals(1, db.queryLong("SELECT COUNT(*) FROM tag WHERE id = 'tag_1' AND name = 'Road trip'"))
                assertEquals(1, db.queryLong("SELECT COUNT(*) FROM playlist_tag_map WHERE playlistId = 'playlist_1' AND tagId = 'tag_1'"))
                assertEquals(0, db.queryLong("SELECT isDownloaded FROM playlist WHERE id = 'playlist_1'"))
                if (startVersion >= 4) {
                    assertEquals(1, db.queryLong("""
                        SELECT COUNT(*) FROM queue
                        WHERE mediaIdList = 'song_1,song_2'
                          AND startIndex = 1
                          AND position = 42000
                          AND playbackAllowAutoplay = 1
                          AND playbackShuffledCollection = 0
                    """.trimIndent()))
                }
            }
        }
    }

    private fun SupportSQLiteDatabase.seedPersistedData(startVersion: Int) {
        execSQL(
            """
            INSERT INTO song (
                id, title, duration, thumbnailUrl, albumId, albumName, explicit, year,
                date, dateModified, liked, likedDate, totalPlayTime, inLibrary, dateDownload, isLocal
            ) VALUES (
                'song_1', 'Song One', 180, 'thumb', 'album_1', 'Album One', 0, 2026,
                1000, 1000, 1, 1000, 42000, 1000, NULL, 0
            )
            """.trimIndent(),
        )
        execSQL(
            """
            INSERT INTO song (
                id, title, duration, thumbnailUrl, albumId, albumName, explicit, year,
                date, dateModified, liked, likedDate, totalPlayTime, inLibrary, dateDownload, isLocal
            ) VALUES (
                'song_2', 'Song Two', 200, 'thumb-2', NULL, NULL, 0, 2026,
                1001, 1001, 0, NULL, 0, NULL, NULL, 0
            )
            """.trimIndent(),
        )
        execSQL(
            """
            INSERT INTO artist (
                id, name, thumbnailUrl, channelId, lastUpdateTime, bookmarkedAt, isLocal
            ) VALUES (
                'artist_1', 'Artist One', 'artist-thumb', 'channel_1', 1000, 1000, 0
            )
            """.trimIndent(),
        )
        execSQL(
            """
            INSERT INTO album (
                id, playlistId, title, year, thumbnailUrl, themeColor, songCount, duration,
                explicit, lastUpdateTime, bookmarkedAt, likedDate, inLibrary, isLocal
            ) VALUES (
                'album_1', 'playlist_album_1', 'Album One', 2026, 'album-thumb', NULL, 1, 180,
                0, 1000, 1000, 1000, 1000, 0
            )
            """.trimIndent(),
        )
        execSQL(
            """
            INSERT INTO playlist (
                id, name, browseId, createdAt, lastUpdateTime, isEditable, bookmarkedAt,
                remoteSongCount, playEndpointParams, thumbnailUrl, shuffleEndpointParams,
                radioEndpointParams, customOrder, isLocal, isAutoSync
            ) VALUES (
                'playlist_1', 'Playlist One', 'browse_1', 1000, 1000, 1, 1000,
                1, NULL, 'playlist-thumb', NULL, NULL, 1, 0, 0
            )
            """.trimIndent(),
        )
        execSQL("INSERT INTO song_artist_map (songId, artistId, position) VALUES ('song_1', 'artist_1', 0)")
        execSQL("INSERT INTO song_album_map (songId, albumId, `index`) VALUES ('song_1', 'album_1', 0)")
        execSQL("INSERT INTO album_artist_map (albumId, artistId, `order`) VALUES ('album_1', 'artist_1', 0)")
        execSQL("INSERT INTO playlist_song_map (playlistId, songId, position, setVideoId) VALUES ('playlist_1', 'song_1', 0, NULL)")
        execSQL("INSERT INTO search_history (id, query) VALUES (1, 'private search')")
        execSQL("""
            INSERT INTO format (id, itag, mimeType, codecs, bitrate, sampleRate, contentLength, loudnessDb, perceptualLoudnessDb, playbackUrl)
            VALUES ('song_1', 140, 'audio/mp4', 'mp4a.40.2', 128000, 44100, 1234, -6.0, -5.0, 'https://example.invalid/stream')
        """.trimIndent())
        execSQL("INSERT INTO lyrics (id, lyrics) VALUES ('song_1', 'seeded lyrics')")
        execSQL("INSERT INTO event (id, songId, timestamp, playTime) VALUES (1, 'song_1', 1000, 42000)")
        execSQL("INSERT INTO related_song_map (id, songId, relatedSongId) VALUES (1, 'song_1', 'song_2')")
        execSQL("INSERT INTO set_video_id (videoId, setVideoId) VALUES ('song_1', 'set_1')")
        execSQL("INSERT INTO playCount (song, year, month, count) VALUES ('song_1', 2026, 7, 3)")
        execSQL("INSERT INTO song_skip (songId, skipCount, lastSkippedAt) VALUES ('song_1', 2, 1000)")
        execSQL("INSERT INTO tag (id, name, color, createdAt) VALUES ('tag_1', 'Road trip', '#FF6B6B', 1000)")
        execSQL("INSERT INTO playlist_tag_map (playlistId, tagId, createdAt) VALUES ('playlist_1', 'tag_1', 1000)")
        if (startVersion >= 3) {
            execSQL("UPDATE song SET download_state = 2 WHERE id = 'song_1'")
        }
        if (startVersion >= 4) {
            execSQL("INSERT INTO queue (title, mediaIdList, startIndex, position) VALUES ('Seed queue', 'song_1,song_2', 1, 42000)")
        }
    }

    private fun SupportSQLiteDatabase.queryLong(sql: String): Long {
        query(sql).use { cursor ->
            cursor.moveToFirst()
            return cursor.getLong(0)
        }
    }
}

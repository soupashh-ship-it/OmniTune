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
    fun seededLibraryDataSurvivesEverySupportedMigration() {
        for (startVersion in 1 until 7) {
            val name = "seeded-migration-$startVersion"
            helper.createDatabase(name, startVersion).apply {
                seedLibraryData()
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
                assertEquals(0, db.queryLong("SELECT download_state FROM song WHERE id = 'song_1'"))
            }
        }
    }

    private fun SupportSQLiteDatabase.seedLibraryData() {
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
    }

    private fun SupportSQLiteDatabase.queryLong(sql: String): Long {
        query(sql).use { cursor ->
            cursor.moveToFirst()
            return cursor.getLong(0)
        }
    }
}

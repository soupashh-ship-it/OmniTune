/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.db.dao

import androidx.room.Dao
import androidx.room.Query
import com.omnitune.app.db.entities.AlbumArtistMap
import com.omnitune.app.db.entities.AlbumEntity
import com.omnitune.app.db.entities.ArtistEntity
import com.omnitune.app.db.entities.Event
import com.omnitune.app.db.entities.PlayCountEntity
import com.omnitune.app.db.entities.PlaylistEntity
import com.omnitune.app.db.entities.PlaylistSongMap
import com.omnitune.app.db.entities.PlaylistTagMap
import com.omnitune.app.db.entities.SongAlbumMap
import com.omnitune.app.db.entities.SongArtistMap
import com.omnitune.app.db.entities.SongEntity
import com.omnitune.app.db.entities.TagEntity
import java.time.LocalDateTime

@Dao
interface BackupDao {
    @Query(
        """
        SELECT * FROM song
        WHERE liked = 1
           OR inLibrary IS NOT NULL
           OR totalPlayTime > 0
           OR download_state > 0
           OR id IN (SELECT songId FROM playlist_song_map)
           OR id IN (SELECT songId FROM event)
           OR id IN (SELECT song FROM playCount)
        ORDER BY rowid
        """,
    )
    suspend fun backupSongs(): List<SongEntity>

    @Query(
        """
        SELECT * FROM artist
        WHERE bookmarkedAt IS NOT NULL
           OR id IN (SELECT artistId FROM song_artist_map)
           OR id IN (SELECT artistId FROM album_artist_map)
        ORDER BY rowid
        """,
    )
    suspend fun backupArtists(): List<ArtistEntity>

    @Query(
        """
        SELECT * FROM album
        WHERE bookmarkedAt IS NOT NULL
           OR inLibrary IS NOT NULL
           OR id IN (SELECT albumId FROM song_album_map)
           OR id IN (SELECT albumId FROM album_artist_map)
        ORDER BY rowid
        """,
    )
    suspend fun backupAlbums(): List<AlbumEntity>

    @Query(
        """
        SELECT * FROM playlist
        WHERE bookmarkedAt IS NOT NULL
           OR isLocal = 1
           OR id IN (SELECT playlistId FROM playlist_song_map)
        ORDER BY COALESCE(customOrder, rowid), rowid
        """,
    )
    suspend fun backupPlaylists(): List<PlaylistEntity>

    @Query(
        """
        SELECT * FROM playlist_song_map
        WHERE playlistId IN (
            SELECT id FROM playlist
            WHERE bookmarkedAt IS NOT NULL
               OR isLocal = 1
               OR id IN (SELECT playlistId FROM playlist_song_map)
        )
        ORDER BY playlistId, position, id
        """,
    )
    suspend fun backupPlaylistSongMaps(): List<PlaylistSongMap>

    @Query(
        """
        SELECT * FROM song_artist_map
        WHERE songId IN (
            SELECT id FROM song
            WHERE liked = 1
               OR inLibrary IS NOT NULL
               OR totalPlayTime > 0
               OR download_state > 0
               OR id IN (SELECT songId FROM playlist_song_map)
               OR id IN (SELECT songId FROM event)
               OR id IN (SELECT song FROM playCount)
        )
        ORDER BY songId, position
        """,
    )
    suspend fun backupSongArtistMaps(): List<SongArtistMap>

    @Query(
        """
        SELECT * FROM song_album_map
        WHERE songId IN (
            SELECT id FROM song
            WHERE liked = 1
               OR inLibrary IS NOT NULL
               OR totalPlayTime > 0
               OR download_state > 0
               OR id IN (SELECT songId FROM playlist_song_map)
               OR id IN (SELECT songId FROM event)
               OR id IN (SELECT song FROM playCount)
        )
        ORDER BY songId, `index`
        """,
    )
    suspend fun backupSongAlbumMaps(): List<SongAlbumMap>

    @Query(
        """
        SELECT * FROM album_artist_map
        WHERE albumId IN (
            SELECT id FROM album
            WHERE bookmarkedAt IS NOT NULL
               OR inLibrary IS NOT NULL
               OR id IN (SELECT albumId FROM song_album_map)
               OR id IN (SELECT albumId FROM album_artist_map)
        )
        ORDER BY albumId, `order`
        """,
    )
    suspend fun backupAlbumArtistMaps(): List<AlbumArtistMap>

    @Query("SELECT * FROM event ORDER BY timestamp, id")
    suspend fun backupEvents(): List<Event>

    @Query("SELECT * FROM playCount ORDER BY song, year, month")
    suspend fun backupPlayCounts(): List<PlayCountEntity>

    @Query("SELECT * FROM tag ORDER BY name")
    suspend fun backupTags(): List<TagEntity>

    @Query("SELECT * FROM playlist_tag_map ORDER BY playlistId, tagId")
    suspend fun backupPlaylistTagMaps(): List<PlaylistTagMap>

    @Query("SELECT * FROM song WHERE id = :id LIMIT 1")
    suspend fun backupSongById(id: String): SongEntity?

    @Query("SELECT * FROM artist WHERE id = :id LIMIT 1")
    suspend fun backupArtistById(id: String): ArtistEntity?

    @Query("SELECT * FROM album WHERE id = :id LIMIT 1")
    suspend fun backupAlbumById(id: String): AlbumEntity?

    @Query("SELECT * FROM playlist WHERE id = :id LIMIT 1")
    suspend fun backupPlaylistById(id: String): PlaylistEntity?

    @Query("SELECT * FROM playlist WHERE name = :name LIMIT 1")
    suspend fun backupPlaylistByName(name: String): PlaylistEntity?

    @Query("SELECT * FROM tag WHERE id = :id LIMIT 1")
    suspend fun backupTagById(id: String): TagEntity?

    @Query(
        """
        SELECT COUNT(1) FROM event
        WHERE songId = :songId
          AND timestamp = :timestamp
          AND playTime = :playTime
        """,
    )
    suspend fun backupEventExists(
        songId: String,
        timestamp: LocalDateTime,
        playTime: Long,
    ): Int

    @Query(
        """
        SELECT * FROM playCount
        WHERE song = :songId
          AND year = :year
          AND month = :month
        LIMIT 1
        """,
    )
    suspend fun backupPlayCount(
        songId: String,
        year: Int,
        month: Int,
    ): PlayCountEntity?

    @Query(
        """
        UPDATE playCount
        SET count = :count
        WHERE song = :songId
          AND year = :year
          AND month = :month
        """,
    )
    suspend fun backupUpdatePlayCount(
        songId: String,
        year: Int,
        month: Int,
        count: Int,
    )

    @Query("DELETE FROM playlist_tag_map")
    suspend fun backupClearPlaylistTagMaps()

    @Query("DELETE FROM queue")
    suspend fun backupClearQueue()

    @Query("DELETE FROM song_skip")
    suspend fun backupClearSongSkips()

    @Query("DELETE FROM set_video_id")
    suspend fun backupClearSetVideoIds()

    @Query("DELETE FROM related_song_map")
    suspend fun backupClearRelatedSongMaps()

    @Query("DELETE FROM format")
    suspend fun backupClearFormats()

    @Query("DELETE FROM lyrics")
    suspend fun backupClearLyrics()

    @Query("DELETE FROM tag")
    suspend fun backupClearTags()

    @Query("DELETE FROM event")
    suspend fun backupClearEvents()

    @Query("DELETE FROM playCount")
    suspend fun backupClearPlayCounts()

    @Query("DELETE FROM playlist_song_map")
    suspend fun backupClearPlaylistSongMaps()

    @Query("DELETE FROM playlist")
    suspend fun backupClearPlaylists()

    @Query("DELETE FROM song_artist_map")
    suspend fun backupClearSongArtistMaps()

    @Query("DELETE FROM song_album_map")
    suspend fun backupClearSongAlbumMaps()

    @Query("DELETE FROM album_artist_map")
    suspend fun backupClearAlbumArtistMaps()

    @Query("DELETE FROM song")
    suspend fun backupClearSongs()

    @Query("DELETE FROM album")
    suspend fun backupClearAlbums()

    @Query("DELETE FROM artist")
    suspend fun backupClearArtists()
}

package com.omnitune.app.db.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.Locale
import com.omnitune.app.db.entities.*
import com.omnitune.app.models.MediaMetadata
import com.omnitune.app.models.toMediaMetadata
import com.omnitune.innertube.models.PlaylistItem
import com.omnitune.innertube.models.SongItem
import com.omnitune.innertube.pages.AlbumPage
import com.omnitune.innertube.pages.ArtistPage
import com.omnitune.app.constants.*
import androidx.sqlite.db.SupportSQLiteQuery
import com.omnitune.app.extensions.*
import com.omnitune.app.ui.utils.resize
import java.text.Collator

@Dao
interface ArtistDao {
    @Transaction
    @Query(
        "SELECT song.* FROM song_artist_map JOIN song ON song_artist_map.songId = song.id WHERE artistId = :artistId AND inLibrary IS NOT NULL ORDER BY inLibrary",
    )
    fun artistSongsByCreateDateAsc(artistId: String): Flow<List<Song>>


    @Transaction
    @Query(
        "SELECT song.* FROM song_artist_map JOIN song ON song_artist_map.songId = song.id WHERE artistId = :artistId AND inLibrary IS NOT NULL ORDER BY title",
    )
    fun artistSongsByNameAsc(artistId: String): Flow<List<Song>>


    @Transaction
    @Query(
        "SELECT song.* FROM song_artist_map JOIN song ON song_artist_map.songId = song.id WHERE artistId = :artistId AND inLibrary IS NOT NULL ORDER BY totalPlayTime",
    )
    fun artistSongsByPlayTimeAsc(artistId: String): Flow<List<Song>>


    fun artistSongs(
        artistId: String,
        sortType: ArtistSongSortType,
        descending: Boolean,
    ) = when (sortType) {
        ArtistSongSortType.CREATE_DATE -> artistSongsByCreateDateAsc(artistId)
        ArtistSongSortType.NAME ->
            artistSongsByNameAsc(artistId).map { artistSongs ->
                val collator = Collator.getInstance(Locale.getDefault())
                collator.strength = Collator.PRIMARY
                artistSongs.sortedWith(compareBy(collator) { it.song.title })
            }

        ArtistSongSortType.PLAY_TIME -> artistSongsByPlayTimeAsc(artistId)
    }.map { it.reversed(descending) }


    @Transaction
    @Query(
        "SELECT song.* FROM song_artist_map JOIN song ON song_artist_map.songId = song.id WHERE artistId = :artistId AND inLibrary IS NOT NULL LIMIT :previewSize",
    )
    fun artistSongsPreview(
        artistId: String,
        previewSize: Int = 3,
    ): Flow<List<Song>>


    @Transaction
    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Query(
        """
        SELECT artist.*,
               (SELECT COUNT(1)
                FROM song_artist_map
                         JOIN event ON song_artist_map.songId = event.songId
                WHERE artistId = artist.id
                  AND timestamp > :fromTimeStamp AND timestamp <= :toTimeStamp) AS songCount,
               (SELECT SUM(event.playTime)
                FROM song_artist_map
                         JOIN event ON song_artist_map.songId = event.songId
                WHERE artistId = artist.id
                  AND timestamp > :fromTimeStamp AND timestamp <= :toTimeStamp) AS timeListened
        FROM artist
                 JOIN(SELECT artistId, SUM(songTotalPlayTime) AS totalPlayTime
                      FROM song_artist_map
                               JOIN (SELECT songId, SUM(playTime) AS songTotalPlayTime
                                     FROM event
                                     WHERE timestamp > :fromTimeStamp
                                     AND timestamp <= :toTimeStamp
                                     GROUP BY songId) AS e
                                    ON song_artist_map.songId = e.songId
                      GROUP BY artistId
                      ORDER BY totalPlayTime DESC
                      LIMIT :limit
                      OFFSET :offset)
                     ON artist.id = artistId
    """,
    )
    fun mostPlayedArtists(
        fromTimeStamp: Long,
        limit: Int = 6,
        offset: Int = 0,
        toTimeStamp: Long? = LocalDateTime.now().toInstant(ZoneOffset.UTC).toEpochMilli(),
    ): Flow<List<Artist>>


    @Transaction
    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Query("""
        SELECT album.*, count(song.dateDownload) downloadCount
        FROM album_artist_map
            JOIN album ON album_artist_map.albumId = album.id
            JOIN song ON album_artist_map.albumId = song.albumId
        WHERE artistId = :artistId
        GROUP BY album.id
        LIMIT :previewSize
    """)
    fun artistAlbumsPreview(artistId: String, previewSize: Int = 6): Flow<List<Album>>


    @Transaction
    @Query("SELECT * FROM song_artist_map WHERE songId = :songId")
    fun songArtistMap(songId: String): List<SongArtistMap>


    @Transaction
    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Query(
        """
        SELECT DISTINCT artist.*,
               (SELECT COUNT(1)
                FROM song_artist_map
                         JOIN event ON song_artist_map.songId = event.songId
                WHERE artistId = artist.id) AS songCount
        FROM artist
                 LEFT JOIN(SELECT artistId, SUM(songTotalPlayTime) AS totalPlayTime
                      FROM song_artist_map
                               JOIN (SELECT songId, SUM(playTime) AS songTotalPlayTime
                                     FROM event
                                     GROUP BY songId) AS e
                                    ON song_artist_map.songId = e.songId
                      GROUP BY artistId
                      ORDER BY totalPlayTime DESC) AS artistTotalPlayTime
                     ON artist.id = artistId
                     OR artist.bookmarkedAt IS NOT NULL
                     ORDER BY
                      CASE
                        WHEN artistTotalPlayTime.artistId IS NULL THEN 1
                        ELSE 0
                      END,
                      artistTotalPlayTime.totalPlayTime DESC
    """,
    )
    fun allArtistsByPlayTime(): Flow<List<Artist>>


    @Transaction
    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Query("SELECT *, (SELECT COUNT(1) FROM song_artist_map JOIN song ON song_artist_map.songId = song.id WHERE artistId = artist.id AND song.inLibrary IS NOT NULL) AS songCount FROM artist WHERE songCount > 0 OR bookmarkedAt IS NOT NULL ORDER BY rowId")
    fun artistsByCreateDateAsc(): Flow<List<Artist>>


    @Transaction
    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Query("SELECT *, (SELECT COUNT(1) FROM song_artist_map JOIN song ON song_artist_map.songId = song.id WHERE artistId = artist.id AND song.inLibrary IS NOT NULL) AS songCount FROM artist WHERE songCount > 0 OR bookmarkedAt IS NOT NULL ORDER BY name")
    fun artistsByNameAsc(): Flow<List<Artist>>


    @Transaction
    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Query("SELECT *, (SELECT COUNT(1) FROM song_artist_map JOIN song ON song_artist_map.songId = song.id WHERE artistId = artist.id AND song.inLibrary IS NOT NULL) AS songCount FROM artist WHERE songCount > 0 OR bookmarkedAt IS NOT NULL ORDER BY songCount")
    fun artistsBySongCountAsc(): Flow<List<Artist>>


    @Transaction
    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Query(
        """
        SELECT artist.*,
               (SELECT COUNT(1)
                FROM song_artist_map
                         JOIN song ON song_artist_map.songId = song.id
                WHERE artistId = artist.id
                  AND song.inLibrary IS NOT NULL) AS songCount
        FROM artist
                 LEFT JOIN(SELECT artistId, SUM(totalPlayTime) AS totalPlayTime
                      FROM song_artist_map
                               JOIN song
                                    ON song_artist_map.songId = song.id
                      GROUP BY artistId
                      ORDER BY totalPlayTime)
                     ON artist.id = artistId
        WHERE songCount > 0 OR bookmarkedAt IS NOT NULL
        ORDER BY COALESCE(totalPlayTime, 0)
    """
    )
    fun artistsByPlayTimeAsc(): Flow<List<Artist>>


    @Transaction
    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Query("SELECT *, (SELECT COUNT(1) FROM song_artist_map JOIN song ON song_artist_map.songId = song.id WHERE artistId = artist.id AND song.inLibrary IS NOT NULL) AS songCount FROM artist WHERE bookmarkedAt IS NOT NULL ORDER BY bookmarkedAt")
    fun artistsBookmarkedByCreateDateAsc(): Flow<List<Artist>>


    @Transaction
    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Query("SELECT *, (SELECT COUNT(1) FROM song_artist_map JOIN song ON song_artist_map.songId = song.id WHERE artistId = artist.id AND song.inLibrary IS NOT NULL) AS songCount FROM artist WHERE bookmarkedAt IS NOT NULL ORDER BY name")
    fun artistsBookmarkedByNameAsc(): Flow<List<Artist>>


    @Transaction
    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Query("SELECT *, (SELECT COUNT(1) FROM song_artist_map JOIN song ON song_artist_map.songId = song.id WHERE artistId = artist.id AND song.inLibrary IS NOT NULL) AS songCount FROM artist WHERE bookmarkedAt IS NOT NULL ORDER BY songCount")
    fun artistsBookmarkedBySongCountAsc(): Flow<List<Artist>>


    @Transaction
    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Query(
        """
        SELECT artist.*,
               (SELECT COUNT(1)
                FROM song_artist_map
                         JOIN song ON song_artist_map.songId = song.id
                WHERE artistId = artist.id
                  AND song.inLibrary IS NOT NULL) AS songCount
        FROM artist
                 JOIN(SELECT artistId, SUM(totalPlayTime) AS totalPlayTime
                      FROM song_artist_map
                               JOIN song
                                    ON song_artist_map.songId = song.id
                      GROUP BY artistId
                      ORDER BY totalPlayTime)
                     ON artist.id = artistId
        WHERE bookmarkedAt IS NOT NULL
    """
    )
    fun artistsBookmarkedByPlayTimeAsc(): Flow<List<Artist>>


    fun artists(sortType: ArtistSortType, descending: Boolean) =
        when (sortType) {
            ArtistSortType.CREATE_DATE -> artistsByCreateDateAsc()
            ArtistSortType.NAME -> artistsByNameAsc()
            ArtistSortType.SONG_COUNT -> artistsBySongCountAsc()
            ArtistSortType.PLAY_TIME -> artistsByPlayTimeAsc()
        }.map { artists ->
            artists
                .reversed(descending)
        }


    fun artistsBookmarked(sortType: ArtistSortType, descending: Boolean) =
        when (sortType) {
            ArtistSortType.CREATE_DATE -> artistsBookmarkedByCreateDateAsc()
            ArtistSortType.NAME -> artistsBookmarkedByNameAsc()
            ArtistSortType.SONG_COUNT -> artistsBookmarkedBySongCountAsc()
            ArtistSortType.PLAY_TIME -> artistsBookmarkedByPlayTimeAsc()
        }.map { artists ->
            artists
                .reversed(descending)
        }


    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Query("SELECT *, (SELECT COUNT(1) FROM song_artist_map JOIN song ON song_artist_map.songId = song.id WHERE artistId = artist.id AND song.inLibrary IS NOT NULL) AS songCount FROM artist WHERE id = :id")
    fun artist(id: String): Flow<Artist?>


    @Transaction
    @Query("SELECT * FROM album_artist_map WHERE albumId = :albumId")
    fun albumArtistMaps(albumId: String): List<AlbumArtistMap>


    @Transaction
    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Query(
        "SELECT *, (SELECT COUNT(1) FROM song_artist_map JOIN song ON song_artist_map.songId = song.id WHERE artistId = artist.id AND song.inLibrary IS NOT NULL) AS songCount FROM artist WHERE name LIKE '%' || :query || '%' AND songCount > 0 LIMIT :previewSize",
    )
    fun searchArtists(
        query: String,
        previewSize: Int = Int.MAX_VALUE,
    ): Flow<List<Artist>>


    @Transaction
    @Query("SELECT * FROM artist WHERE name = :name")
    fun artistByName(name: String): ArtistEntity?


    @Query("SELECT * FROM artist WHERE id = :id LIMIT 1")
    fun getArtistById(id: String): ArtistEntity?


    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(artist: ArtistEntity)


    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(map: SongArtistMap)


    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(map: AlbumArtistMap)


    @Update
    fun update(artist: ArtistEntity)


    @Upsert
    fun upsert(artist: ArtistEntity)


    @Upsert
    fun upsert(map: SongArtistMap)


    @Upsert
    fun upsert(map: AlbumArtistMap)


    @Delete
    fun delete(songArtistMap: SongArtistMap)


    @Delete
    fun delete(artist: ArtistEntity)


    @Delete
    fun delete(albumArtistMap: AlbumArtistMap)

}

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
interface SongDao {



    @Transaction
    @Query("SELECT * FROM song WHERE inLibrary IS NOT NULL ORDER BY rowId")
    fun songsByRowIdAsc(): Flow<List<Song>>


    @Transaction
    @Query("SELECT * FROM song WHERE inLibrary IS NOT NULL ORDER BY inLibrary")
    fun songsByCreateDateAsc(): Flow<List<Song>>


    @Transaction
    @Query("SELECT * FROM song WHERE inLibrary IS NOT NULL ORDER BY title")
    fun songsByNameAsc(): Flow<List<Song>>


    @Transaction
    @Query("SELECT * FROM song WHERE inLibrary IS NOT NULL ORDER BY totalPlayTime")
    fun songsByPlayTimeAsc(): Flow<List<Song>>


    fun songs(
        sortType: SongSortType,
        descending: Boolean,
        filterVideo: Boolean = false,
    ) = when (sortType) {
        SongSortType.CREATE_DATE ->
            if (filterVideo) {
                songsByCreateDateAscNoVideo()
            } else {
                songsByCreateDateAsc()
            }

        SongSortType.NAME ->
            (if (filterVideo) {
                songsByNameAscNoVideo()
            } else {
                songsByNameAsc()
            }).map { songs ->
                val collator = Collator.getInstance(Locale.getDefault())
                collator.strength = Collator.PRIMARY
                songs.sortedWith(compareBy(collator) { it.song.title })
            }

        SongSortType.ARTIST ->
            (if (filterVideo) {
                songsByRowIdAscNoVideo()
            } else {
                songsByRowIdAsc()
            }).map { songs ->
                val collator = Collator.getInstance(Locale.getDefault())
                collator.strength = Collator.PRIMARY
                songs
                    .sortedWith(
                        compareBy(collator) { song ->
                            song.artists.joinToString("") { artist -> artist.name }
                        },
                    ).groupBy { it.album?.title }
                    .flatMap { (_, songsByAlbum) ->
                        songsByAlbum.sortedBy { album ->
                            album.artists.joinToString(
                                "",
                            ) { artist -> artist.name }
                        }
                    }
            }

        SongSortType.PLAY_TIME -> songsByPlayTimeAsc()
    }.map { it.reversed(descending) }


    @Transaction
    @Query(
        """
        SELECT song.*
        FROM song
        LEFT JOIN set_video_id ON set_video_id.videoId = song.id
        WHERE song.inLibrary IS NOT NULL AND set_video_id.setVideoId IS NULL
        ORDER BY song.id
        """,
    )
    fun songsByRowIdAscNoVideo(): Flow<List<Song>>


    @Transaction
    @Query(
        """
        SELECT song.*
        FROM song
        LEFT JOIN set_video_id ON set_video_id.videoId = song.id
        WHERE song.inLibrary IS NOT NULL AND set_video_id.setVideoId IS NULL
        ORDER BY inLibrary
        """,
    )
    fun songsByCreateDateAscNoVideo(): Flow<List<Song>>


    @Transaction
    @Query(
        """
        SELECT song.*
        FROM song
        LEFT JOIN set_video_id ON set_video_id.videoId = song.id
        WHERE song.inLibrary IS NOT NULL AND set_video_id.setVideoId IS NULL
        ORDER BY title
        """,
    )
    fun songsByNameAscNoVideo(): Flow<List<Song>>


    @Transaction
    @Query(
        """
        SELECT song.*
        FROM song
        LEFT JOIN set_video_id ON set_video_id.videoId = song.id
        WHERE song.inLibrary IS NOT NULL AND set_video_id.setVideoId IS NULL
        ORDER BY totalPlayTime
        """,
    )
    fun songsByPlayTimeAscNoVideo(): Flow<List<Song>>


    @Transaction
    @Query("SELECT * FROM song WHERE liked ORDER BY rowId")
    fun likedSongsByRowIdAsc(): Flow<List<Song>>


    @Transaction
    @Query("SELECT * FROM song WHERE liked ORDER BY likedDate, rowId")
    fun likedSongsByCreateDateAsc(): Flow<List<Song>>


    @Transaction
    @Query("SELECT * FROM song WHERE liked ORDER BY title")
    fun likedSongsByNameAsc(): Flow<List<Song>>


    @Transaction
    @Query("SELECT * FROM song WHERE liked ORDER BY totalPlayTime")
    fun likedSongsByPlayTimeAsc(): Flow<List<Song>>


    fun likedSongs(
        sortType: SongSortType,
        descending: Boolean,
        filterVideo: Boolean = false,
    ) = when (sortType) {
        SongSortType.CREATE_DATE ->
            if (filterVideo) {
                likedSongsByCreateDateAscNoVideo()
            } else {
                likedSongsByCreateDateAsc()
            }

        SongSortType.NAME ->
            (if (filterVideo) {
                likedSongsByNameAscNoVideo()
            } else {
                likedSongsByNameAsc()
            }).map { songs ->
                val collator = Collator.getInstance(Locale.getDefault())
                collator.strength = Collator.PRIMARY
                songs.sortedWith(compareBy(collator) { it.song.title })
            }

        SongSortType.ARTIST ->
            (if (filterVideo) {
                likedSongsByRowIdAscNoVideo()
            } else {
                likedSongsByRowIdAsc()
            }).map { songs ->
                val collator = Collator.getInstance(Locale.getDefault())
                collator.strength = Collator.PRIMARY
                songs
                    .sortedWith(
                        compareBy(collator) { song ->
                            song.artists.joinToString("") { artist -> artist.name }
                        },
                    ).groupBy { it.album?.title }
                    .flatMap { (_, songsByAlbum) ->
                        songsByAlbum.sortedBy { album ->
                            album.artists.joinToString(
                                "",
                            ) { artist -> artist.name }
                        }
                    }
            }

        SongSortType.PLAY_TIME -> likedSongsByPlayTimeAsc()
    }.map { it.reversed(descending) }


    @Transaction
    @Query(
        """
        SELECT song.*
        FROM song
        LEFT JOIN set_video_id ON set_video_id.videoId = song.id
        WHERE liked AND set_video_id.setVideoId IS NULL
        ORDER BY song.rowid
        """,
    )
    fun likedSongsByRowIdAscNoVideo(): Flow<List<Song>>


    @Transaction
    @Query(
        """
        SELECT song.*
        FROM song
        LEFT JOIN set_video_id ON set_video_id.videoId = song.id
        WHERE liked AND set_video_id.setVideoId IS NULL
        ORDER BY likedDate, song.rowid
        """,
    )
    fun likedSongsByCreateDateAscNoVideo(): Flow<List<Song>>


    @Transaction
    @Query(
        """
        SELECT song.*
        FROM song
        LEFT JOIN set_video_id ON set_video_id.videoId = song.id
        WHERE liked AND set_video_id.setVideoId IS NULL
        ORDER BY title
        """,
    )
    fun likedSongsByNameAscNoVideo(): Flow<List<Song>>


    @Transaction
    @Query(
        """
        SELECT song.*
        FROM song
        LEFT JOIN set_video_id ON set_video_id.videoId = song.id
        WHERE liked AND set_video_id.setVideoId IS NULL
        ORDER BY totalPlayTime
        """,
    )
    fun likedSongsByPlayTimeAscNoVideo(): Flow<List<Song>>


    @Transaction
    @Query("SELECT COUNT(1) FROM song WHERE liked")
    fun likedSongsCount(): Flow<Int>


    @Transaction
    @Query(
        """
        SELECT song.*
        FROM (SELECT *, COUNT(1) AS referredCount
              FROM related_song_map
              GROUP BY relatedSongId) map
                 JOIN song ON song.id = map.relatedSongId
        WHERE songId IN (SELECT songId
                         FROM (SELECT songId
                               FROM event
                               ORDER BY ROWID DESC
                               LIMIT 5)
                         UNION
                         SELECT songId
                         FROM (SELECT songId
                               FROM event
                               WHERE timestamp > :now - 86400000 * 7
                               GROUP BY songId
                               ORDER BY SUM(playTime) DESC
                               LIMIT 5)
                         UNION
                         SELECT id
                         FROM (SELECT id
                               FROM song
                               ORDER BY totalPlayTime DESC
                               LIMIT 10))
        ORDER BY referredCount DESC
        LIMIT 100
    """,
    )
    fun quickPicks(now: Long = System.currentTimeMillis()): Flow<List<Song>>


    @Transaction
    @Query(
        """
             SELECT song.id, song.title, song.thumbnailUrl,
               (SELECT COUNT(1)
                FROM event
                WHERE songId = song.id
                  AND timestamp > :fromTimeStamp AND timestamp <= :toTimeStamp) AS songCountListened,
               (SELECT SUM(event.playTime)
                FROM event
                WHERE songId = song.id
                  AND timestamp > :fromTimeStamp AND timestamp <= :toTimeStamp) AS timeListened
        FROM song
        JOIN (SELECT songId
                     FROM event
                     WHERE timestamp > :fromTimeStamp
                     AND timestamp <= :toTimeStamp
                     GROUP BY songId
                     ORDER BY SUM(playTime) DESC
                     LIMIT :limit)
        ON song.id = songId
        LIMIT :limit
        OFFSET :offset
    """,
    )
    fun mostPlayedSongsStats(
        fromTimeStamp: Long,
        limit: Int = 6,
        offset: Int = 0,
        toTimeStamp: Long? = LocalDateTime.now().toInstant(ZoneOffset.UTC).toEpochMilli(),
    ): Flow<List<SongWithStats>>


    @Transaction
    @RewriteQueriesToDropUnusedColumns
    @Query(
        """
        SELECT song.*,
               (SELECT COUNT(1)
                FROM event
                WHERE songId = song.id
                  AND timestamp > :fromTimeStamp AND timestamp <= :toTimeStamp) AS songCountListened,
               (SELECT SUM(event.playTime)
                FROM event
                WHERE songId = song.id
                  AND timestamp > :fromTimeStamp AND timestamp <= :toTimeStamp) AS timeListened
        FROM song
        JOIN (SELECT songId
                     FROM event
                     WHERE timestamp > :fromTimeStamp
                     AND timestamp <= :toTimeStamp
                     GROUP BY songId
                     ORDER BY SUM(playTime) DESC
                     LIMIT :limit)
        ON song.id = songId
        LIMIT :limit
        OFFSET :offset
    """,
    )
    fun mostPlayedSongs(
        fromTimeStamp: Long,
        limit: Int = 6,
        offset: Int = 0,
        toTimeStamp: Long? = LocalDateTime.now().toInstant(ZoneOffset.UTC).toEpochMilli(),
    ): Flow<List<Song>>


    @Query("SELECT sum(count) from playCount WHERE song = :songId")
    fun getLifetimePlayCount(songId: String?): Flow<Int>

    @Query("SELECT sum(count) from playCount WHERE song = :songId AND year = :year")
    fun getPlayCountByYear(songId: String?, year: Int): Flow<Int>

    @Query("SELECT count from playCount WHERE song = :songId AND year = :year AND month = :month")
    fun getPlayCountByMonth(songId: String?, year: Int, month: Int): Flow<Int>


    @Transaction
    @Query(
        """
        SELECT song.*
        FROM (SELECT n.songId      AS eid,
                     SUM(playTime) AS oldPlayTime,
                     newPlayTime
              FROM event
                       JOIN
                   (SELECT songId, SUM(playTime) AS newPlayTime
                    FROM event
                    WHERE timestamp > (:now - 86400000 * 30 * 1)
                    GROUP BY songId
                    ORDER BY newPlayTime) as n
                   ON event.songId = n.songId
              WHERE timestamp < (:now - 86400000 * 30 * 1)
              GROUP BY n.songId
              ORDER BY oldPlayTime) AS t
                 JOIN song on song.id = t.eid
        WHERE 0.2 * t.oldPlayTime > t.newPlayTime
        LIMIT 100
    """
    )
    fun forgottenFavorites(now: Long = System.currentTimeMillis()): Flow<List<Song>>


    @Transaction
    @Query("SELECT * FROM song WHERE id = :songId")
    fun song(songId: String?): Flow<Song?>


    @Transaction
    @Query("SELECT * FROM song WHERE id = :songId LIMIT 1")
    suspend fun getSongById(songId: String): Song?


    @Transaction
    @Query("SELECT * FROM song WHERE id IN (:songIds)")
    suspend fun getSongsByIds(songIds: List<String>): List<Song>


    @Transaction
    @Query("SELECT * FROM song WHERE id = :songId LIMIT 1")
    fun getSongByIdBlocking(songId: String): Song?
    

    @Transaction
    @Query("SELECT * FROM song")
    fun allSongs(): Flow<List<Song>>


    @Query("SELECT * FROM set_video_id WHERE videoId = :videoId")
    suspend fun getSetVideoId(videoId: String): SetVideoIdEntity?




    @Transaction
    @Query("SELECT * FROM song WHERE title LIKE '%' || :query || '%' AND inLibrary IS NOT NULL LIMIT :previewSize")
    fun searchSongs(
        query: String,
        previewSize: Int = Int.MAX_VALUE,
    ): Flow<List<Song>>


    @Query("UPDATE song SET totalPlayTime = totalPlayTime + :playTime WHERE id = :songId")
    fun incrementTotalPlayTime(songId: String, playTime: Long)


    @Query("UPDATE playCount SET count = count + 1 WHERE song = :songId AND year = :year AND month = :month")
    suspend fun incrementPlayCount(songId: String, year: Int, month: Int)

    /**
     * Increment by one the play count with today's year and month.
     */

    suspend fun incrementPlayCount(songId: String) {
        val time = LocalDateTime.now().atOffset(ZoneOffset.UTC)
        val oldCount = getPlayCountByMonth(songId, time.year, time.monthValue).first()

        // add new
        if (oldCount <= 0) {
            insert(PlayCountEntity(songId, time.year, time.monthValue, 0))
        }
        incrementPlayCount(songId, time.year, time.monthValue)
    }


    @Transaction
    @Query("UPDATE song SET inLibrary = :inLibrary WHERE id = :songId")
    fun inLibrary(
        songId: String,
        inLibrary: LocalDateTime?,
    )


    @Transaction
    @Query("SELECT COUNT(1) FROM related_song_map WHERE songId = :songId LIMIT 1")
    fun hasRelatedSongs(songId: String): Boolean


    @Transaction
    @Query(
        "SELECT song.* FROM (SELECT * from related_song_map GROUP BY relatedSongId) map JOIN song ON song.id = map.relatedSongId where songId = :songId",
    )
    fun getRelatedSongs(songId: String): Flow<List<Song>>


    @Transaction
    @Query(
        """
        SELECT song.*
        FROM (SELECT *
              FROM related_song_map
              GROUP BY relatedSongId) map
                 JOIN
             song
             ON song.id = map.relatedSongId
        WHERE songId = :songId
        """
    )
    fun relatedSongs(songId: String): List<Song>


    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(song: SongEntity): Long


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(setVideoIdEntity: SetVideoIdEntity)






    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(map: RelatedSongMap)


    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(playCountEntity: PlayCountEntity): Long


    @Update
    fun update(song: SongEntity)


    @Upsert
    fun upsert(song: SongEntity)


    @Delete
    fun delete(song: SongEntity)


    @Query("SELECT * FROM song_skip")
    fun getAllSkips(): Flow<List<com.omnitune.app.db.entities.SongSkipEntity>>


    @Query("SELECT * FROM song_skip WHERE songId = :songId LIMIT 1")
    suspend fun getSkip(songId: String): com.omnitune.app.db.entities.SongSkipEntity?


    @Upsert
    suspend fun upsertSkip(skip: com.omnitune.app.db.entities.SongSkipEntity)


    @Query("DELETE FROM song_skip WHERE songId = :songId")
    suspend fun deleteSkip(songId: String)


    @Query("SELECT song AS songId, sum(count) AS playCount FROM playCount WHERE song IN (:songIds) GROUP BY song")
    suspend fun getPlayCounts(songIds: List<String>): List<com.omnitune.app.db.models.SongPlayCount>


    @Query("SELECT songId, skipCount FROM song_skip WHERE songId IN (:songIds)")
    suspend fun getSkipCounts(songIds: List<String>): List<com.omnitune.app.db.models.SongSkipCount>
}

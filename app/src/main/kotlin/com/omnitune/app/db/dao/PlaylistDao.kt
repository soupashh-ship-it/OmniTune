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
interface PlaylistDao {
    @Transaction
    @Query("SELECT * FROM playlist_song_map WHERE playlistId = :playlistId ORDER BY position")
    fun playlistSongs(playlistId: String): Flow<List<PlaylistSong>>


    @Transaction
    @Query("SELECT *, (SELECT COUNT(*) FROM playlist_song_map WHERE playlistId = playlist.id) AS songCount FROM playlist WHERE bookmarkedAt IS NOT NULL ORDER BY rowId")
    fun playlistsByCreateDateAsc(): Flow<List<Playlist>>


    @Transaction
    @Query(
        "SELECT *, (SELECT COUNT(*) FROM playlist_song_map WHERE playlistId = playlist.id) AS songCount FROM playlist WHERE bookmarkedAt IS NOT NULL ORDER BY lastUpdateTime",
    )
    fun playlistsByUpdatedDateAsc(): Flow<List<Playlist>>


    @Transaction
    @Query("SELECT *, (SELECT COUNT(*) FROM playlist_song_map WHERE playlistId = playlist.id) AS songCount FROM playlist WHERE bookmarkedAt IS NOT NULL ORDER BY name")
    fun playlistsByNameAsc(): Flow<List<Playlist>>


    @Transaction
    @Query("SELECT *, (SELECT COUNT(*) FROM playlist_song_map WHERE playlistId = playlist.id) AS songCount FROM playlist WHERE bookmarkedAt IS NOT NULL ORDER BY songCount")
    fun playlistsBySongCountAsc(): Flow<List<Playlist>>


    @Transaction
    @Query(
        "SELECT *, (SELECT COUNT(*) FROM playlist_song_map WHERE playlistId = playlist.id) AS songCount FROM playlist WHERE bookmarkedAt IS NOT NULL ORDER BY COALESCE(customOrder, rowId), rowId",
    )
    fun playlistsByCustomOrderAsc(): Flow<List<Playlist>>


    fun playlists(
        sortType: PlaylistSortType,
        descending: Boolean,
    ) = when (sortType) {
        PlaylistSortType.CREATE_DATE -> playlistsByCreateDateAsc()
        PlaylistSortType.NAME ->
            playlistsByNameAsc().map { playlists ->
                val collator = Collator.getInstance(Locale.getDefault())
                collator.strength = Collator.PRIMARY
                playlists.sortedWith(compareBy(collator) { it.playlist.name })
            }

        PlaylistSortType.SONG_COUNT -> playlistsBySongCountAsc()
        PlaylistSortType.LAST_UPDATED -> playlistsByUpdatedDateAsc()
        PlaylistSortType.CUSTOM -> playlistsByCustomOrderAsc()
    }.map { list ->
        if (descending && sortType != PlaylistSortType.CUSTOM) list.asReversed() else list
    }


    @Query("UPDATE playlist SET customOrder = :customOrder WHERE id = :playlistId")
    fun setPlaylistCustomOrder(
        playlistId: String,
        customOrder: Int?,
    )


    @Query("SELECT MAX(customOrder) FROM playlist WHERE bookmarkedAt IS NOT NULL")
    fun maxPlaylistCustomOrder(): Int?


    @Transaction
    @Query("SELECT *, (SELECT COUNT(*) FROM playlist_song_map WHERE playlistId = playlist.id) AS songCount FROM playlist WHERE id = :playlistId")
    fun playlist(playlistId: String): Flow<Playlist?>


    @Transaction
    @Query("SELECT *, (SELECT COUNT(*) FROM playlist_song_map WHERE playlistId = playlist.id) AS songCount FROM playlist WHERE id = :playlistId LIMIT 1")
    suspend fun getPlaylistById(playlistId: String): Playlist?


    @Transaction
    @Query("SELECT *, (SELECT COUNT(*) FROM playlist_song_map WHERE playlistId = playlist.id) AS songCount FROM playlist WHERE id = :playlistId LIMIT 1")
    fun getPlaylistByIdBlocking(playlistId: String): Playlist?


    @Transaction
    @Query("SELECT *, (SELECT COUNT(*) FROM playlist_song_map WHERE playlistId = playlist.id) AS songCount FROM playlist WHERE isEditable AND bookmarkedAt IS NOT NULL ORDER BY rowId")
    fun editablePlaylistsByCreateDateAsc(): Flow<List<Playlist>>


    @Transaction
    @Query("SELECT *, (SELECT COUNT(*) FROM playlist_song_map WHERE playlistId = playlist.id) AS songCount FROM playlist WHERE browseId = :browseId")
    fun playlistByBrowseId(browseId: String): Flow<Playlist?>


    @Transaction
    @Query("SELECT COUNT(*) from playlist_song_map WHERE playlistId = :playlistId AND songId = :songId LIMIT 1")
    fun checkInPlaylist(
        playlistId: String,
        songId: String,
    ): Int


    @Query("SELECT songId from playlist_song_map WHERE playlistId = :playlistId AND songId IN (:songIds)")
    fun playlistDuplicates(
        playlistId: String,
        songIds: List<String>,
    ): List<String>


    @Query("DELETE FROM playlist_song_map WHERE playlistId = :playlistId AND songId = :songId")
    fun removeSongFromPlaylist(playlistId: String, songId: String)


    @Transaction
    fun addSongToPlaylist(playlist: Playlist, songIds: List<String>) {
        var position = playlist.songCount
        songIds.forEach { id ->
            if (checkInPlaylist(playlist.id, id) == 0) {
                insert(
                    PlaylistSongMap(
                        songId = id,
                        playlistId = playlist.id,
                        position = position++
                    )
                )
            }
        }
    }


    @Transaction
    @Query(
        "SELECT *, (SELECT COUNT(*) FROM playlist_song_map WHERE playlistId = playlist.id) AS songCount FROM playlist WHERE name LIKE '%' || :query || '%' LIMIT :previewSize",
    )
    fun searchPlaylists(
        query: String,
        previewSize: Int = Int.MAX_VALUE,
    ): Flow<List<Playlist>>







    @Transaction
    @Query(
        """
        UPDATE playlist_song_map SET position =
            CASE
                WHEN position < :fromPosition THEN position + 1
                WHEN position > :fromPosition THEN position - 1
                ELSE :toPosition
            END
        WHERE playlistId = :playlistId AND position BETWEEN MIN(:fromPosition, :toPosition) AND MAX(:fromPosition, :toPosition)
    """,
    )
    fun move(
        playlistId: String,
        fromPosition: Int,
        toPosition: Int,
    )


    @Transaction
    @Query("DELETE FROM playlist_song_map WHERE playlistId = :playlistId")
    fun clearPlaylist(playlistId: String)


    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(playlist: PlaylistEntity)


    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(map: PlaylistSongMap)


    @Update
    fun update(playlist: PlaylistEntity)


    @Update
    fun update(map: PlaylistSongMap)


    @Delete
    fun delete(playlist: PlaylistEntity)


    @Delete
    fun delete(playlistSongMap: PlaylistSongMap)


    @Query("DELETE FROM playlist WHERE browseId = :browseId")
    fun deletePlaylistById(browseId: String)





    @Transaction
    @Query("SELECT * FROM playlist_song_map WHERE songId = :songId")
    fun playlistSongMaps(songId: String): List<PlaylistSongMap>

    @Query(
        """
        UPDATE playlist
        SET isDownloaded = CASE
            WHEN EXISTS (SELECT 1 FROM playlist_song_map WHERE playlistId = playlist.id)
             AND NOT EXISTS (
                SELECT 1 FROM playlist_song_map
                JOIN song ON song.id = playlist_song_map.songId
                WHERE playlist_song_map.playlistId = playlist.id AND song.download_state != 2
             ) THEN 1 ELSE 0 END
        WHERE id IN (SELECT playlistId FROM playlist_song_map WHERE songId = :songId)
        """
    )
    fun refreshDownloadedPlaylists(songId: String)


    @Transaction
    @Query("SELECT * FROM playlist_song_map WHERE playlistId = :playlistId AND position >= :from ORDER BY position")
    fun playlistSongMaps(
        playlistId: String,
        from: Int,
    ): List<PlaylistSongMap>


    @Query("SELECT MAX(position) FROM playlist_song_map WHERE playlistId = :playlistId")
    fun maxPlaylistSongPosition(playlistId: String): Int?


    @Transaction
    @Query("SELECT * FROM tag ORDER BY name")
    fun allTags(): Flow<List<TagEntity>>


    @Transaction
    @Query("SELECT * FROM tag WHERE id = :tagId")
    fun tag(tagId: String): Flow<TagEntity?>


    @Transaction
    @Query("SELECT * FROM tag WHERE id IN (SELECT tagId FROM playlist_tag_map WHERE playlistId = :playlistId)")
    fun playlistTags(playlistId: String): Flow<List<TagEntity>>


    @Transaction
    @Query("SELECT DISTINCT playlistId FROM playlist_tag_map WHERE tagId IN (:tagIds)")
    fun playlistIdsByTags(tagIds: List<String>): Flow<List<String>>


    @Transaction
    @Query("SELECT COUNT(*) FROM playlist_tag_map WHERE playlistId = :playlistId AND tagId = :tagId")
    fun isPlaylistTagged(playlistId: String, tagId: String): Flow<Int>


    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(tag: TagEntity)


    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(map: PlaylistTagMap)


    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertAllPlaylistTagMaps(maps: List<PlaylistTagMap>)


    @Update
    fun update(tag: TagEntity)


    @Delete
    fun delete(tag: TagEntity)


    @Delete
    fun delete(playlistTagMap: PlaylistTagMap)


    @Query("DELETE FROM playlist_tag_map WHERE playlistId = :playlistId")
    fun removeAllPlaylistTags(playlistId: String)


    @Query("DELETE FROM playlist_tag_map WHERE tagId = :tagId")
    fun removeAllTagPlaylists(tagId: String)


    @Query("DELETE FROM playlist_tag_map WHERE playlistId = :playlistId AND tagId = :tagId")
    fun removePlaylistTag(playlistId: String, tagId: String)


    @Transaction
    fun addTagToPlaylist(playlistId: String, tagId: String) {
        insert(PlaylistTagMap(playlistId = playlistId, tagId = tagId))
    }


    @Transaction
    fun addTagsToPlaylists(
        playlistIds: List<String>,
        tagIds: List<String>,
    ) {
        if (playlistIds.isEmpty() || tagIds.isEmpty()) return
        val maps =
            playlistIds.flatMap { playlistId ->
                tagIds.map { tagId ->
                    PlaylistTagMap(playlistId = playlistId, tagId = tagId)
                }
            }
        insertAllPlaylistTagMaps(maps)
    }


    @Transaction
    suspend fun togglePlaylistTag(playlistId: String, tagId: String) {
        val isTagged = isPlaylistTagged(playlistId, tagId).first()
        if (isTagged > 0) {
            removePlaylistTag(playlistId, tagId)
        } else {
            addTagToPlaylist(playlistId, tagId)
        }
    }

    // ─── For You Suggestion Engine ───────────────────────────────────────

}

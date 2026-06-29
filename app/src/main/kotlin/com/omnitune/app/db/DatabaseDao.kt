/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.db

import com.omnitune.app.db.dao.*

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.RewriteQueriesToDropUnusedColumns
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import androidx.room.RoomWarnings
import androidx.sqlite.db.SupportSQLiteQuery
import com.omnitune.innertube.models.PlaylistItem
import com.omnitune.innertube.models.SongItem
import com.omnitune.innertube.pages.AlbumPage
import com.omnitune.innertube.pages.ArtistPage
import com.omnitune.app.constants.AlbumSortType
import com.omnitune.app.constants.ArtistSongSortType
import com.omnitune.app.constants.ArtistSortType
import com.omnitune.app.constants.PlaylistSortType
import com.omnitune.app.constants.SongSortType
import com.omnitune.app.db.entities.Album
import com.omnitune.app.db.entities.AlbumArtistMap
import com.omnitune.app.db.entities.AlbumEntity
import com.omnitune.app.db.entities.PlayCountEntity
import com.omnitune.app.db.entities.AlbumWithSongs
import com.omnitune.app.db.entities.Artist
import com.omnitune.app.db.entities.ArtistEntity
import com.omnitune.app.db.entities.Event
import com.omnitune.app.db.entities.EventWithSong
import com.omnitune.app.db.entities.FormatEntity
import com.omnitune.app.db.entities.LyricsEntity
import com.omnitune.app.db.entities.Playlist
import com.omnitune.app.db.entities.PlaylistEntity
import com.omnitune.app.db.entities.PlaylistSong
import com.omnitune.app.db.entities.PlaylistSongMap
import com.omnitune.app.db.entities.RelatedSongMap
import com.omnitune.app.db.entities.SearchHistory
import com.omnitune.app.db.entities.SetVideoIdEntity
import com.omnitune.app.db.entities.Song
import com.omnitune.app.db.entities.SongAlbumMap
import com.omnitune.app.db.entities.SongArtistMap
import com.omnitune.app.db.entities.SongEntity
import com.omnitune.app.db.entities.SongWithStats
import com.omnitune.app.db.entities.TagEntity
import com.omnitune.app.db.entities.PlaylistTagMap
import com.omnitune.app.db.entities.PlaylistWithTags
import com.omnitune.app.extensions.reversed
import com.omnitune.app.extensions.toSQLiteQuery
import com.omnitune.app.models.MediaMetadata
import com.omnitune.app.models.toMediaMetadata
import com.omnitune.app.ui.utils.resize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.launch
import java.text.Collator
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.Locale

@Dao
interface DatabaseDao : QueueDao, LyricsDao, SearchHistoryDao, FormatDao, EventDao, SongDao, ArtistDao, AlbumDao, PlaylistDao {

    @Transaction
    fun insert(
        mediaMetadata: MediaMetadata,
        block: (SongEntity) -> SongEntity = { it },
    ) {
        if (insert(mediaMetadata.toSongEntity().let(block)) == -1L) return

        if (mediaMetadata.setVideoId != null) {
            insert(
                SetVideoIdEntity(
                    videoId = mediaMetadata.id,
                    setVideoId = mediaMetadata.setVideoId,
                ),
            )
        }

        mediaMetadata.artists.forEachIndexed { index, artist ->
            val artistId = artist.id ?: artistByName(artist.name)?.id ?: ArtistEntity.generateArtistId()
            
            insert(
                ArtistEntity(
                    id = artistId,
                    name = artist.name,
                    channelId = artist.id,
                )
            )

            insert(
                SongArtistMap(
                    songId = mediaMetadata.id,
                    artistId = artistId,
                    position = index,
                )
            )
        }
    }

    @Transaction
    fun insert(albumPage: AlbumPage) {
        if (insert(
                AlbumEntity(
                    id = albumPage.album.browseId,
                    playlistId = albumPage.album.playlistId,
                    title = albumPage.album.title,
                    year = albumPage.album.year,
                    thumbnailUrl = albumPage.album.thumbnail,
                    songCount = albumPage.songs.size,
                    duration = albumPage.songs.sumOf { song -> song.duration ?: 0 },
                    explicit = albumPage.album.explicit || albumPage.songs.any { it.explicit },
                ),
            ) == -1L
        ) {
            return
        }
        albumPage.songs
            .map(SongItem::toMediaMetadata)
            .onEach(::insert)
            .onEach {
                val existingSong = getSongByIdBlocking(it.id)
                if (existingSong != null) {
                    update(existingSong, it)
                }
            }.mapIndexed { index, song ->
                SongAlbumMap(
                    songId = song.id,
                    albumId = albumPage.album.browseId,
                    index = index,
                )
            }.forEach(::upsert)
        albumPage.album.artists
            ?.map { artist ->
                ArtistEntity(
                    id = artist.id ?: artistByName(artist.name)?.id
                    ?: ArtistEntity.generateArtistId(),
                    name = artist.name,
                )
            }?.onEach(::insert)
            ?.mapIndexed { index, artist ->
                AlbumArtistMap(
                    albumId = albumPage.album.browseId,
                    artistId = artist.id,
                    order = index,
                )
            }?.forEach(::insert)
    }

    @Transaction
    fun update(
        song: Song,
        mediaMetadata: MediaMetadata,
    ) {
        update(
            song.song.copy(
                title = mediaMetadata.title,
                duration = mediaMetadata.duration,
                thumbnailUrl = mediaMetadata.thumbnailUrl,
                albumId = mediaMetadata.album?.id,
                albumName = mediaMetadata.album?.title,
            ),
        )
        songArtistMap(song.id).forEach(::delete)
        mediaMetadata.artists.forEachIndexed { index, artist ->
            val artistId = artist.id ?: artistByName(artist.name)?.id ?: ArtistEntity.generateArtistId()
            
            insert(
                ArtistEntity(
                    id = artistId,
                    name = artist.name,
                    channelId = artist.id,
                ),
            )
            insert(
                SongArtistMap(
                    songId = song.id,
                    artistId = artistId,
                    position = index,
                ),
            )
        }
    }

    @Transaction
    fun update(
        artist: ArtistEntity,
        artistPage: ArtistPage
    ) {
        update(
            artist.copy(
                name = artistPage.artist.title,
                thumbnailUrl = artistPage.artist.thumbnail?.resize(544, 544),
                lastUpdateTime = LocalDateTime.now()
            )
        )
    }

    @Transaction
    fun update(
        album: AlbumEntity,
        albumPage: AlbumPage,
        artists: List<ArtistEntity>? = emptyList(),
    ) {
        update(
            album.copy(
                id = albumPage.album.browseId,
                playlistId = albumPage.album.playlistId,
                title = albumPage.album.title,
                year = albumPage.album.year,
                thumbnailUrl = albumPage.album.thumbnail,
                songCount = albumPage.songs.size,
                duration = albumPage.songs.sumOf { song -> song.duration ?: 0 },
                explicit = albumPage.album.explicit || albumPage.songs.any { it.explicit },
            ),
        )
        if (artists?.size != albumPage.album.artists?.size) {
            artists?.forEach(::delete)
        }
        albumPage.songs
            .map(SongItem::toMediaMetadata)
            .onEach(::insert)
            .onEach {
                val existingSong = getSongByIdBlocking(it.id)
                if (existingSong != null) {
                    update(existingSong, it)
                }
            }.mapIndexed { index, song ->
                SongAlbumMap(
                    songId = song.id,
                    albumId = albumPage.album.browseId,
                    index = index,
                )
            }.forEach(::upsert)

        albumPage.album.artists?.let { artists ->
            // Recreate album artists
            albumArtistMaps(album.id).forEach(::delete)
            artists
                .map { artist ->
                    ArtistEntity(
                        id = artist.id ?: artistByName(artist.name)?.id
                        ?: ArtistEntity.generateArtistId(),
                        name = artist.name,
                    )
                }.onEach(::insert)
                .mapIndexed { index, artist ->
                    AlbumArtistMap(
                        albumId = albumPage.album.browseId,
                        artistId = artist.id,
                        order = index,
                    )
                }.forEach(::insert)
        }
    }

    @Update
    fun update(playlistEntity: PlaylistEntity, playlistItem: PlaylistItem) {
        update(
            playlistEntity.copy(
                name = playlistItem.title,
                browseId = playlistItem.id,
                thumbnailUrl = playlistItem.thumbnail,
                isEditable = playlistItem.isEditable,
                remoteSongCount = playlistItem.songCountText?.let { Regex("""\d+""").find(it)?.value?.toIntOrNull() },
                playEndpointParams = playlistItem.playEndpoint?.params,
                shuffleEndpointParams = playlistItem.shuffleEndpoint?.params,
                radioEndpointParams = playlistItem.radioEndpoint?.params
            )
        )
    }

    @RawQuery
    fun raw(supportSQLiteQuery: SupportSQLiteQuery): Int

    fun checkpoint() {
        raw("PRAGMA wal_checkpoint(FULL)".toSQLiteQuery())
    }

}

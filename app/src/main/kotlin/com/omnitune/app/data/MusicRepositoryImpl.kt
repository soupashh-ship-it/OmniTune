package com.omnitune.app.data

import com.omnitune.app.db.MusicDatabase
import com.omnitune.app.db.entities.AlbumArtistMap
import com.omnitune.app.db.entities.AlbumEntity
import com.omnitune.app.db.entities.ArtistEntity
import com.omnitune.app.db.entities.Song
import com.omnitune.app.db.entities.SongAlbumMap
import com.omnitune.app.db.entities.SongArtistMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicRepositoryImpl @Inject constructor(
    private val musicDatabase: MusicDatabase
) : MusicRepository {

    override suspend fun saveSong(song: Song) {
        musicDatabase.withTransaction {
            musicDatabase.upsert(song.song)

            song.artists.forEachIndexed { index, artist ->
                musicDatabase.upsert(artist)
                musicDatabase.upsert(SongArtistMap(songId = song.song.id, artistId = artist.id, position = index))
            }

            song.album?.let { album ->
                musicDatabase.upsert(album)
                musicDatabase.upsert(SongAlbumMap(songId = song.song.id, albumId = album.id, index = 0))
                val primaryArtistId = song.artists.firstOrNull()?.id ?: return@let
                musicDatabase.upsert(AlbumArtistMap(albumId = album.id, artistId = primaryArtistId, order = 0))
            }
        }
    }
}

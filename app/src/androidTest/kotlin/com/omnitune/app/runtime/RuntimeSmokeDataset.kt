package com.omnitune.app.runtime

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.omnitune.app.constants.AudioCrossfadeDurationKey
import com.omnitune.app.constants.SkipSilenceKey
import com.omnitune.app.db.InternalDatabase
import com.omnitune.app.db.entities.PlayCountEntity
import com.omnitune.app.db.entities.PlaylistEntity
import com.omnitune.app.db.entities.PlaylistTagMap
import com.omnitune.app.db.entities.QueueEntity
import com.omnitune.app.db.entities.TagEntity
import com.omnitune.app.models.MediaMetadata
import com.omnitune.app.utils.dataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDateTime

/**
 * A deterministic database profile for manual runtime smoke runs of the debug application.
 *
 * This fixture never fabricates audio bytes or DownloadManager rows. RT-07 creates a completed
 * download, and an interrupted download, through the production download flow.
 */
object RuntimeSmokeDataset {
    const val SEARCH_QUERY = "OmniTune runtime fixture"
    const val LOCAL_PLAYLIST_ID = "runtime-local-playlist"
    const val FOLDER_TAG_ID = "runtime-folder-tag"

    val tracks = listOf(
        Track("dQw4w9WgXcQ", "Runtime Sunrise", "runtime-artist-a", "Runtime Artist A", "runtime-album-a", "Runtime Album A", liked = true),
        Track("3JZ_D3ELwOQ", "Runtime Noon", "runtime-artist-a", "Runtime Artist A", "runtime-album-a", "Runtime Album A"),
        Track("kJQP7kiw5Fk", "Runtime Dusk", "runtime-artist-b", "Runtime Artist B", "runtime-album-b", "Runtime Album B", liked = true),
        Track("fJ9rUzIMcZQ", "Runtime Night", "runtime-artist-b", "Runtime Artist B", "runtime-album-b", "Runtime Album B"),
    )

    data class Track(
        val id: String,
        val title: String,
        val artistId: String,
        val artistName: String,
        val albumId: String,
        val albumName: String,
        val liked: Boolean = false,
    )

    data class Summary(
        val songCount: Int,
        val localPlaylistId: String,
        val pendingCompletedDownloadId: String,
        val failedDownloadId: String,
    )

    suspend fun seed(context: Context): Summary {
        val database = InternalDatabase.newInstance(context)
        return try {
            database.withTransaction {
                tracks.forEach { track ->
                    insert(
                        MediaMetadata(
                            id = track.id,
                            title = track.title,
                            artists = listOf(MediaMetadata.Artist(track.artistId, track.artistName)),
                            duration = 180,
                            thumbnailUrl = "https://i.ytimg.com/vi/${track.id}/hqdefault.jpg",
                            album = MediaMetadata.Album(track.albumId, track.albumName),
                            liked = track.liked,
                            likedDate = if (track.liked) LocalDateTime.now() else null,
                            inLibrary = LocalDateTime.now(),
                        ),
                    )
                }

                insert(PlaylistEntity(id = LOCAL_PLAYLIST_ID, name = "Runtime Local Playlist", isLocal = true))
                val localPlaylist = requireNotNull(getPlaylistByIdBlocking(LOCAL_PLAYLIST_ID))
                addSongToPlaylist(localPlaylist, tracks.map(Track::id))

                insert(TagEntity(id = FOLDER_TAG_ID, name = "Runtime Folder"))
                insert(PlaylistTagMap(playlistId = LOCAL_PLAYLIST_ID, tagId = FOLDER_TAG_ID))

                insertRecentEvent(tracks.first().id, playTime = 45_000)
                insert(PlayCountEntity(song = tracks.first().id, year = 2026, month = 7, count = 3))
                updateDownloadState(tracks.last().id, state = 3, downloadedAt = null)
                saveQueue(
                    QueueEntity(
                        title = "Runtime Queue",
                        mediaIdList = tracks.joinToString(",") { it.id },
                        startIndex = 1,
                        position = 12_000,
                        playbackSourceType = "LOCAL_PLAYLIST",
                        playbackSourceId = LOCAL_PLAYLIST_ID,
                        playbackSourceTitle = "Runtime Local Playlist",
                    ),
                )
            }
            context.dataStore.edit { preferences ->
                preferences[SkipSilenceKey] = true
                preferences[AudioCrossfadeDurationKey] = 3
            }
            Summary(
                songCount = tracks.size,
                localPlaylistId = LOCAL_PLAYLIST_ID,
                pendingCompletedDownloadId = tracks.first().id,
                failedDownloadId = tracks.last().id,
            )
        } finally {
            database.close()
        }
    }
}

@RunWith(AndroidJUnit4::class)
class RuntimeSmokeDatasetTest {

    @Test
    fun seedDisposableDebugProfile() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val result = RuntimeSmokeDataset.seed(context)
        val database = InternalDatabase.newInstance(context)
        try {
            assertEquals(RuntimeSmokeDataset.tracks.size, database.songsByNameAsc().first().size)
            assertEquals(RuntimeSmokeDataset.tracks.size, database.playlistSongs(result.localPlaylistId).first().size)
            assertEquals(3, database.getSongById(result.failedDownloadId)?.song?.downloadState)
            assertEquals(1, database.getQueue()?.startIndex)
            assertTrue(context.dataStore.data.first()[SkipSilenceKey] == true)
        } finally {
            database.close()
        }
    }
}

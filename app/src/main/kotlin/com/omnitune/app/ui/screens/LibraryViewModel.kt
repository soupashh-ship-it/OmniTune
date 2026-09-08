package com.omnitune.app.ui.screens

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omnitune.app.db.MusicDatabase
import com.omnitune.app.models.*
import com.omnitune.app.playback.DownloadUtil
import com.omnitune.app.playback.PlayerConnection
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Instant
import javax.inject.Inject

data class LibraryUiState(
    val playlists: List<PlaylistDisplayItem> = emptyList(),
    val userPlaylists: List<PlaylistDisplayItem> = emptyList(),
    val librarySongs: List<Song> = emptyList(),
    val likedSongs: List<Song> = emptyList(),
    val likedSongsCount: Int = 0,
    val downloadedSongsCount: Int = 0,
    val deviceSongsCount: Int = 0,
    val top50SongCount: Int = 0,
    val cachedSongCount: Int = 0,
    val libraryArtists: List<Artist> = emptyList(),
    val libraryAlbums: List<Album> = emptyList(),
    val localFolders: Map<String, List<Song>> = emptyMap(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val isLoggedIn: Boolean = true,
    val viewMode: LibraryViewMode = LibraryViewMode.GRID,
    val sortOption: LibrarySortOption = LibrarySortOption.DATE_ADDED,
    val librarySearchQuery: String = "",
    val selectedFilter: LibraryFilter = LibraryFilter.PLAYLISTS,
    val recentlyPlayed: List<com.omnitune.app.db.entities.EventWithSong> = emptyList(),
)

enum class PlaylistExportFormat {
    M3U,
    SUV
}

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val database: MusicDatabase,
    private val downloadUtil: DownloadUtil
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    val libraryAlbums: kotlinx.coroutines.flow.Flow<List<com.omnitune.app.db.entities.Album>> =
        database.albums(com.omnitune.app.constants.AlbumSortType.CREATE_DATE, descending = true)

    val libraryArtists: kotlinx.coroutines.flow.Flow<List<com.omnitune.app.db.entities.Artist>> =
        database.artists(com.omnitune.app.constants.ArtistSortType.CREATE_DATE, descending = true)

    val playlists: kotlinx.coroutines.flow.Flow<List<com.omnitune.app.db.entities.Playlist>> =
        database.playlists(com.omnitune.app.constants.PlaylistSortType.CREATE_DATE, descending = true)

    private var rawPlaylists: List<PlaylistDisplayItem> = emptyList()


    init {
        loadData()
        observeDatabase()
    }

    private fun observeDatabase() {
        viewModelScope.launch {
            try {
                database.recentEvents().collect { events ->
                    _uiState.update { it.copy(recentlyPlayed = events) }
                }
            } catch (e: Exception) {
                // Ignore
            }
        }
        viewModelScope.launch {
            try {
                database.playlists(com.omnitune.app.constants.PlaylistSortType.CREATE_DATE, true).collect { dbPlaylists ->

                    val displayList = dbPlaylists.map { p ->
                        PlaylistDisplayItem(
                            id = p.id,
                            name = p.playlist.name,
                            url = "omnitune://playlist/${p.id}",
                            uploaderName = "You",
                            thumbnailUrl = p.thumbnails.firstOrNull(),
                            songCount = p.songCount
                        )
                    }
                    rawPlaylists = displayList
                    filterAndPresent()
                }
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    fun loadData(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                withContext(Dispatchers.IO) {
                    val allSongs: List<Song> = database.songsByRowIdAsc().first().map { it.toSuvSong() }
                    val liked: List<Song> = database.likedSongs(com.omnitune.app.constants.SongSortType.CREATE_DATE, descending = true).first().map { it.toSuvSong() }
                    val dbAlbums = database.albums(com.omnitune.app.constants.AlbumSortType.CREATE_DATE, descending = true).first().map {
                        Album(
                            id = it.id,
                            title = it.album.title,
                            artist = it.artists.joinToString(", ") { a -> a.name },
                            year = it.album.year?.toString(),
                            thumbnailUrl = it.album.thumbnailUrl
                        )
                    }
                    val dbArtists = database.artists(com.omnitune.app.constants.ArtistSortType.CREATE_DATE, descending = true).first().map {
                        Artist(
                            id = it.id,
                            name = it.artist.name,
                            thumbnailUrl = it.artist.thumbnailUrl
                        )
                    }


                    val folders = allSongs.groupBy { it.album.ifBlank { "Uncategorized" } }

                    _uiState.update {
                        it.copy(
                            librarySongs = allSongs,
                            likedSongs = liked,
                            likedSongsCount = liked.size,
                            deviceSongsCount = allSongs.count { s: Song -> s.source == SongSource.LOCAL },
                            top50SongCount = minOf(50, allSongs.size),
                            cachedSongCount = allSongs.size,
                            libraryAlbums = dbAlbums,
                            libraryArtists = dbArtists,
                            localFolders = folders,
                            isLoading = false,
                            isRefreshing = false
                        )
                    }
                }
                filterAndPresent()
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, isRefreshing = false, error = e.message) }
            }
        }
    }

    fun refresh() {
        _uiState.update { it.copy(isRefreshing = true) }
        loadData(forceRefresh = true)
    }

    fun setFilter(filter: LibraryFilter) {
        _uiState.update { it.copy(selectedFilter = filter) }
    }

    fun setSortOption(sortOption: LibrarySortOption) {
        _uiState.update { it.copy(sortOption = sortOption) }
        filterAndPresent()
    }

    fun setViewMode(viewMode: LibraryViewMode) {
        _uiState.update { it.copy(viewMode = viewMode) }
    }

    fun setLibrarySearchQuery(query: String) {
        _uiState.update { it.copy(librarySearchQuery = query) }
        filterAndPresent()
    }

    private fun filterAndPresent() {
        val query = _uiState.value.librarySearchQuery.trim()
        val sort = _uiState.value.sortOption

        var list = rawPlaylists
        if (query.isNotBlank()) {
            list = list.filter { it.name.contains(query, ignoreCase = true) }
        }

        list = when (sort) {
            LibrarySortOption.NAME -> list.sortedBy { it.name.lowercase() }
            LibrarySortOption.DATE_ADDED -> list
        }

        _uiState.update { it.copy(playlists = list) }
    }

    fun syncLikedSongs() {
        loadLikedSongs()
    }

    fun loadLikedSongs() {
        viewModelScope.launch {
            try {
                val liked = withContext(Dispatchers.IO) {
                    database.likedSongs(com.omnitune.app.constants.SongSortType.CREATE_DATE, descending = true).first().map { it.toSuvSong() }
                }
                _uiState.update { it.copy(likedSongs = liked, likedSongsCount = liked.size) }
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    fun renamePlaylist(playlistId: String, newName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val p = database.getPlaylistByIdBlocking(playlistId)?.playlist
                if (p != null) {
                    database.update(p.copy(name = newName))
                }
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    fun deletePlaylist(playlistId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                database.deletePlaylistById(playlistId)
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    fun withPlaylistSongs(
        playlistId: String,
        onSongsLoaded: (List<Song>) -> Unit
    ) {
        viewModelScope.launch {
            val songs = withContext(Dispatchers.IO) {
                playlistSongs(playlistId)
            }
            onSongsLoaded(songs)
        }
    }

    fun playPlaylistNext(playlistId: String, playerConnection: PlayerConnection) {
        viewModelScope.launch {
            val songs = withContext(Dispatchers.IO) {
                playlistSongs(playlistId)
            }
            if (songs.isNotEmpty()) {
                playerConnection.playNext(songs.map { it.toMediaItem() })
            }
        }
    }

    fun addPlaylistToQueue(playlistId: String, playerConnection: PlayerConnection) {
        viewModelScope.launch {
            val songs = withContext(Dispatchers.IO) {
                playlistSongs(playlistId)
            }
            if (songs.isNotEmpty()) {
                playerConnection.addToQueue(songs.map { it.toMediaItem() })
            }
        }
    }

    fun downloadPlaylist(playlistId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            playlistSongs(playlistId).forEach { song ->
                downloadUtil.enqueue(song.id, song.title)
            }
        }
    }

    fun exportPlaylist(
        context: Context,
        playlistId: String,
        playlistName: String,
        uri: Uri,
        format: PlaylistExportFormat,
        onComplete: (success: Boolean, message: String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val songs = withContext(Dispatchers.IO) {
                    playlistSongs(playlistId)
                }
                val body = when (format) {
                    PlaylistExportFormat.M3U -> buildM3UPlaylist(playlistName, songs)
                    PlaylistExportFormat.SUV -> buildSuvPlaylist(playlistId, playlistName, songs)
                }
                withContext(Dispatchers.IO) {
                    context.applicationContext.contentResolver.openOutputStream(uri)
                        ?.bufferedWriter(Charsets.UTF_8)
                        ?.use { writer -> writer.write(body) }
                        ?: error("Unable to open export file")
                }
                onComplete(true, "Exported $playlistName")
            } catch (error: Exception) {
                onComplete(false, error.message ?: "Export failed")
            }
        }
    }

    private suspend fun playlistSongs(playlistId: String): List<Song> {
        return database.playlistSongs(playlistId).first().map { it.song.toSuvSong() }
    }

    val allTags: kotlinx.coroutines.flow.Flow<List<com.omnitune.app.db.entities.TagEntity>> =
        database.allTags()

    fun playlistTags(playlistId: String): kotlinx.coroutines.flow.Flow<List<com.omnitune.app.db.entities.TagEntity>> =
        database.playlistTags(playlistId)

    fun removePlaylistTag(playlistId: String, tagId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                database.query {
                    delete(com.omnitune.app.db.entities.PlaylistTagMap(playlistId = playlistId, tagId = tagId))
                }
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    fun createTag(name: String, color: String = "#FF6B6B") {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                database.insert(com.omnitune.app.db.entities.TagEntity(name = name, color = color))
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    fun updateTag(tag: com.omnitune.app.db.entities.TagEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                database.update(tag)
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    fun deleteTag(tag: com.omnitune.app.db.entities.TagEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                database.delete(tag)
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    fun assignPlaylistTag(playlistId: String, tagId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                database.insert(com.omnitune.app.db.entities.PlaylistTagMap(playlistId = playlistId, tagId = tagId))
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    fun ensureSongExists(song: com.omnitune.app.models.MediaMetadata) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                database.insert(song.toSongEntity())
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    suspend fun addToPlaylist(playlist: com.omnitune.app.db.entities.Playlist, songId: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                database.query {
                    insert(com.omnitune.app.db.entities.PlaylistSongMap(playlistId = playlist.id, songId = songId, position = playlist.songCount))
                }
                true
            } catch (e: Exception) {
                false
            }
        }
    }

    fun createPlaylist(name: String, initialSongId: String? = null, onCreated: ((String) -> Unit)? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val playlistId = "PL_" + java.util.UUID.randomUUID().toString().take(8)
            val now = java.time.LocalDateTime.now()
            database.query {
                insert(com.omnitune.app.db.entities.PlaylistEntity(id = playlistId, name = name, bookmarkedAt = now))
                if (initialSongId != null) {
                    insert(com.omnitune.app.db.entities.PlaylistSongMap(playlistId = playlistId, songId = initialSongId, position = 0))
                }
            }
            if (onCreated != null) {
                withContext(Dispatchers.Main) {
                    onCreated(playlistId)
                }
            }
        }
    }
}

private val exportJson = Json {
    prettyPrint = true
    encodeDefaults = true
}

private fun buildM3UPlaylist(playlistName: String, songs: List<Song>): String = buildString {
    appendLine("#EXTM3U")
    appendLine("#PLAYLIST:${playlistName.toExportLine()}")
    songs.forEach { song ->
        val durationSeconds = (song.duration / 1000L).takeIf { it > 0L } ?: -1L
        appendLine("#EXTINF:$durationSeconds,${song.artist.toExportLine()} - ${song.title.toExportLine()}")
        appendLine(song.exportUrl())
    }
}

private fun buildSuvPlaylist(
    playlistId: String,
    playlistName: String,
    songs: List<Song>
): String {
    return exportJson.encodeToString(
        ExportedPlaylist(
            id = playlistId,
            title = playlistName,
            exportedAt = Instant.now().toString(),
            tracks = songs.mapIndexed { index, song ->
                ExportedTrack(
                    position = index,
                    id = song.id,
                    title = song.title,
                    artist = song.artist,
                    album = song.album,
                    durationMs = song.duration,
                    thumbnailUrl = song.thumbnailUrl,
                    source = song.source.name,
                    url = song.exportUrl()
                )
            }
        )
    )
}

private fun Song.exportUrl(): String {
    return localUri?.takeIf { it.isNotBlank() } ?: "https://music.youtube.com/watch?v=$id"
}

private fun String.toExportLine(): String = replace('\n', ' ').replace('\r', ' ').trim()

@Serializable
private data class ExportedPlaylist(
    val format: String = "omnitune-playlist",
    val version: Int = 1,
    val id: String,
    val title: String,
    val exportedAt: String,
    val tracks: List<ExportedTrack>
)

@Serializable
private data class ExportedTrack(
    val position: Int,
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val thumbnailUrl: String?,
    val source: String,
    val url: String
)

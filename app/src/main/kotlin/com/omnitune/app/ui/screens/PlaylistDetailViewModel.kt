/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.ui.screens

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omnitune.app.db.MusicDatabase
import com.omnitune.app.db.entities.PlaylistSong
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class PlaylistDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val database: MusicDatabase,
) : ViewModel() {
    val db: MusicDatabase get() = database
    private val playlistId: String = savedStateHandle["playlistId"] ?: ""
    val getPlaylistId: String get() = playlistId


    val playlist = database.playlist(playlistId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val songs: StateFlow<List<PlaylistSong>> = database.playlistSongs(playlistId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val playlistSuggestions = kotlinx.coroutines.flow.MutableStateFlow<com.omnitune.app.models.PlaylistSuggestion?>(null)
    val isLoadingSuggestions = kotlinx.coroutines.flow.MutableStateFlow(false)


    fun renamePlaylist(newName: String) {
        val current = playlist.value ?: return
        if (!current.playlist.isEditable) return
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            database.update(current.playlist.copy(name = newName))
        }
    }

    fun deletePlaylist() {
        val current = playlist.value ?: return
        if (!current.playlist.isEditable) return
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            database.delete(current.playlist)
        }
    }

    fun removeSong(songId: String) {
        val current = playlist.value ?: return
        if (!current.playlist.isEditable) return
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            database.removeSongFromPlaylist(current.playlist.id, songId)
        }
    }

    suspend fun addSongToPlaylist(song: com.omnitune.innertube.models.SongItem, browseId: String? = null): Boolean {
        val current = playlist.value ?: return false
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            database.addSongToPlaylist(current, listOf(song.id))
            true
        }
    }

    fun resetAndLoadPlaylistSuggestions() {
        // Stub for now
    }


    fun moveSong(fromPosition: Int, toPosition: Int) {
        val current = playlist.value ?: return
        if (!current.playlist.isEditable) return
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            database.move(current.playlist.id, fromPosition, toPosition)
        }
    }

    fun removeSongs(songIds: List<String>) {
        val current = playlist.value ?: return
        if (!current.playlist.isEditable) return
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            songIds.forEach { database.removeSongFromPlaylist(current.playlist.id, it) }
        }
    }
}

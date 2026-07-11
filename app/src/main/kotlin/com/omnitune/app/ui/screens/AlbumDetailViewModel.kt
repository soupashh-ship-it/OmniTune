package com.omnitune.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omnitune.app.db.MusicDatabase
import com.omnitune.app.db.entities.Album
import com.omnitune.app.db.entities.Song
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlbumDetailViewModel @Inject constructor(
    private val database: MusicDatabase,
) : ViewModel() {

    private val _isBookmarked = MutableStateFlow(false)
    val isBookmarked: StateFlow<Boolean> = _isBookmarked.asStateFlow()
    private val _album = MutableStateFlow<Album?>(null)
    val album: StateFlow<Album?> = _album.asStateFlow()
    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs.asStateFlow()

    fun loadAlbum(albumId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            database.album(albumId).collect { album ->
                _album.value = album
                _isBookmarked.value = album?.album?.bookmarkedAt != null
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            database.albumSongs(albumId).collect { _songs.value = it }
        }
    }

    fun toggleBookmark(albumId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val album = database.album(albumId).first()
            if (album != null) {
                val updated = album.album.localToggleLike()
                database.update(updated)
                _album.value = album.copy(album = updated)
                _isBookmarked.value = updated.bookmarkedAt != null
            }
        }
    }
}

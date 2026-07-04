package com.omnitune.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omnitune.app.db.MusicDatabase
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

    fun loadAlbum(albumId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val album = database.album(albumId).first()
            _isBookmarked.value = album?.album?.bookmarkedAt != null
        }
    }

    fun toggleBookmark(albumId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val album = database.album(albumId).first()
            if (album != null) {
                val updated = album.album.localToggleLike()
                database.update(updated)
                _isBookmarked.value = updated.bookmarkedAt != null
            }
        }
    }
}

package com.omnitune.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omnitune.app.db.MusicDatabase
import com.omnitune.app.db.entities.ArtistEntity
import com.omnitune.app.db.entities.Song
import com.omnitune.app.constants.ArtistSongSortType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ArtistDetailViewModel @Inject constructor(
    private val database: MusicDatabase,
) : ViewModel() {

    private val _isBookmarked = MutableStateFlow(false)
    val isBookmarked: StateFlow<Boolean> = _isBookmarked.asStateFlow()
    private val _artist = MutableStateFlow<ArtistEntity?>(null)
    val artist: StateFlow<ArtistEntity?> = _artist.asStateFlow()
    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs.asStateFlow()

    fun loadArtist(artistId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val entity = database.getArtistById(artistId)
            _artist.value = entity
            _isBookmarked.value = entity?.bookmarkedAt != null
        }
        viewModelScope.launch(Dispatchers.IO) {
            database.artistSongs(artistId, ArtistSongSortType.CREATE_DATE, false).collect {
                _songs.value = it
            }
        }
    }

    fun toggleBookmark(artistId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val entity = database.getArtistById(artistId)
            if (entity != null) {
                val updated = entity.localToggleLike()
                database.update(updated)
                _artist.value = updated
                _isBookmarked.value = updated.bookmarkedAt != null
            }
        }
    }
}

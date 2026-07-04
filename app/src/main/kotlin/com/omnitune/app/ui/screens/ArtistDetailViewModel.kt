package com.omnitune.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omnitune.app.db.MusicDatabase
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

    fun loadArtist(artistId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val entity = database.getArtistById(artistId)
            _isBookmarked.value = entity?.bookmarkedAt != null
        }
    }

    fun toggleBookmark(artistId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val entity = database.getArtistById(artistId)
            if (entity != null) {
                val updated = entity.localToggleLike()
                database.update(updated)
                _isBookmarked.value = updated.bookmarkedAt != null
            }
        }
    }
}

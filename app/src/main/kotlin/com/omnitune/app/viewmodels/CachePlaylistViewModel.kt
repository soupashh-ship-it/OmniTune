package com.omnitune.app.viewmodels

import androidx.lifecycle.ViewModel
import com.omnitune.app.db.entities.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class CachePlaylistViewModel @Inject constructor() : ViewModel() {
    private val _cachedSongs = MutableStateFlow<List<Song>>(emptyList())
    val cachedSongs: StateFlow<List<Song>> = _cachedSongs.asStateFlow()
}

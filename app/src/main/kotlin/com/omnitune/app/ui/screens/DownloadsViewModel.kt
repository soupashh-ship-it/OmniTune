package com.omnitune.app.ui.screens

import android.app.PendingIntent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omnitune.app.db.MusicDatabase
import com.omnitune.app.models.Song
import com.omnitune.app.models.toPresentationSong
import com.omnitune.app.playback.DownloadUtil
import dagger.hilt.android.lifecycle.HiltViewModel

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class SongStatus(
    val song: Song,
    val isDownloading: Boolean = false,
    val progress: Float = 1.0f,
    val failureReason: String? = null
)

sealed class DownloadItem {
    data class SongItem(
        val song: Song,
        val isDownloading: Boolean = false,
        val progress: Float = 1.0f,
        val failureReason: String? = null
    ) : DownloadItem()

    data class CollectionItem(
        val id: String,
        val title: String,
        val thumbnailUrl: String?,
        val songs: List<SongStatus>
    ) : DownloadItem()
}

@HiltViewModel
class DownloadsViewModel @Inject constructor(
    private val database: MusicDatabase,
    private val downloadUtil: DownloadUtil
) : ViewModel() {

    private val _pendingIntent = MutableStateFlow<PendingIntent?>(null)
    val pendingIntent = _pendingIntent.asStateFlow()

    fun consumePendingIntent() {
        _pendingIntent.value = null
    }

    val downloadedSongs: StateFlow<List<Song>> = flow {
        val songs = database.songsByRowIdAsc().first().map { it.toPresentationSong() }
        emit(songs)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())


    val downloadedVideos: StateFlow<List<Song>> = downloadedSongs
        .map { list -> list.filter { it.isVideo } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val downloadItems: StateFlow<List<DownloadItem>> = downloadedSongs.map { list ->
        list.map { DownloadItem.SongItem(it) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedSongIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedSongIds: StateFlow<Set<String>> = _selectedSongIds

    val isSelectionMode = _selectedSongIds.map { it.isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun deleteDownload(songId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                downloadUtil.removeDownload(songId)
                _selectedSongIds.update { it - songId }
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    fun retryDownload(songId: String) {
        // Retry
    }

    fun toggleSelection(songId: String) {
        _selectedSongIds.update { current ->
            if (current.contains(songId)) current - songId else current + songId
        }
    }

    fun selectAll() {
        val allIds = downloadedSongs.value.map { it.id }.toSet()
        _selectedSongIds.value = allIds
    }

    fun clearSelection() {
        _selectedSongIds.value = emptySet()
    }

    fun deleteSelected() {
        val toDelete = _selectedSongIds.value.toList()
        viewModelScope.launch(Dispatchers.IO) {
            toDelete.forEach { downloadUtil.removeDownload(it) }
            clearSelection()
        }
    }

    fun deleteAll() {
        val all = downloadedSongs.value.map { it.id }
        viewModelScope.launch(Dispatchers.IO) {
            all.forEach { downloadUtil.removeDownload(it) }
            clearSelection()
        }
    }

    fun refreshDownloads() {}

    fun startDownload(
        videoId: String,
        title: String,
        resolvedStreamUrl: String? = null,
        onResult: (success: Boolean, message: String) -> Unit = { _, _ -> }
    ) {
        downloadUtil.enqueue(videoId, title, resolvedStreamUrl, onResult)
    }
}

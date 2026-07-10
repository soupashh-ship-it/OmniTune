package com.omnitune.app.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omnitune.app.data.LyricsRepository
import com.omnitune.app.models.AppResult
import com.omnitune.app.models.LyricsLine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface LyricsUiState {
    data object Idle : LyricsUiState
    data object Loading : LyricsUiState
    data class Success(val lines: List<LyricsLine>) : LyricsUiState
    data class Error(val message: String) : LyricsUiState
    data object NoLyrics : LyricsUiState
}

@HiltViewModel
class LyricsViewModel @Inject constructor(
    private val lyricsRepository: LyricsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<LyricsUiState>(LyricsUiState.Idle)
    val uiState: StateFlow<LyricsUiState> = _uiState.asStateFlow()

    private var currentQueryId: String? = null
    private var loadJob: Job? = null

    fun loadLyrics(songId: String, title: String, artist: String, duration: Long) {
        if (currentQueryId == songId && _uiState.value !is LyricsUiState.Error && _uiState.value !is LyricsUiState.NoLyrics) {
            return // Already loaded or loading for this song
        }
        currentQueryId = songId
        _uiState.value = LyricsUiState.Loading

        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            try {
                val result = lyricsRepository.loadLyrics(
                    songId = songId,
                    title = title,
                    artist = artist,
                    duration = duration
                )
                if (currentQueryId != songId) return@launch
                when (result) {
                    is AppResult.Success -> {
                        if (result.data.isEmpty()) {
                            _uiState.value = LyricsUiState.NoLyrics
                        } else {
                            _uiState.value = LyricsUiState.Success(result.data)
                        }
                    }
                    is AppResult.Error -> {
                        _uiState.value = LyricsUiState.Error(result.message)
                    }
                }
            } catch (e: Exception) {
                if (currentQueryId != songId) return@launch
                _uiState.value = LyricsUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}

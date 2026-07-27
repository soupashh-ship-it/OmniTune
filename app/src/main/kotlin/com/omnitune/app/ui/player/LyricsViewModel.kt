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
import kotlinx.coroutines.delay
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
        val queryId = listOf(songId, title, artist, duration.toString()).joinToString(separator = "\u0000")
        if (currentQueryId == queryId && _uiState.value !is LyricsUiState.Error && _uiState.value !is LyricsUiState.NoLyrics) {
            return // Already loaded or loading for this song
        }
        currentQueryId = queryId
        _uiState.value = LyricsUiState.Loading

        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            var lastError = "Lyrics could not be loaded."
            repeat(MAX_LOAD_ATTEMPTS) { attempt ->
                val result = try {
                    lyricsRepository.loadLyrics(
                        songId = songId,
                        title = title,
                        artist = artist,
                        duration = duration,
                    )
                } catch (error: Exception) {
                    AppResult.Error(error.message ?: "Unknown error", error)
                }
                if (currentQueryId != queryId) return@launch
                when (result) {
                    is AppResult.Success -> {
                        if (result.data.isEmpty()) {
                            lastError = "Lyrics not found"
                        } else {
                            _uiState.value = LyricsUiState.Success(result.data)
                            return@launch
                        }
                    }
                    is AppResult.Error -> {
                        lastError = result.message
                    }
                }

                if (attempt < MAX_LOAD_ATTEMPTS - 1) {
                    delay(RETRY_DELAYS_MS[attempt])
                }
            }
            if (currentQueryId != queryId) return@launch
            _uiState.value = if (lastError.equals("Lyrics not found", ignoreCase = true)) {
                LyricsUiState.NoLyrics
            } else {
                LyricsUiState.Error(lastError)
            }
        }
    }

    private companion object {
        const val MAX_LOAD_ATTEMPTS = 3
        val RETRY_DELAYS_MS = longArrayOf(500L, 1_500L)
    }
}

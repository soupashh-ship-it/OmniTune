package com.omnitune.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omnitune.app.db.MusicDatabase
import com.omnitune.app.db.entities.EventWithSong
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HistoryUiState(
    val events: List<EventWithSong> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val database: MusicDatabase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                database.events().collect { events ->
                    _uiState.value = HistoryUiState(
                        events = events,
                        isLoading = false,
                    )
                }
            } catch (e: Exception) {
                _uiState.value = HistoryUiState(
                    isLoading = false,
                    error = e.localizedMessage,
                )
            }
        }
    }
}

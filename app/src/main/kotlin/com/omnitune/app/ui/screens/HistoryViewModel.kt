package com.omnitune.app.ui.screens

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omnitune.app.constants.IncognitoModeKey
import com.omnitune.app.db.MusicDatabase
import com.omnitune.app.db.entities.EventWithSong
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
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
    private val dataStore: DataStore<Preferences>,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    private val _selectedSongIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedSongIds: StateFlow<Set<String>> = _selectedSongIds.asStateFlow()

    private val _incognitoModeEnabled = MutableStateFlow(false)
    val incognitoModeEnabled: StateFlow<Boolean> = _incognitoModeEnabled.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                database.events().collect { events ->
                    _uiState.value = HistoryUiState(
                        // History is chronological: repeated plays are meaningful and must
                        // retain their individual timestamps for the grouped UI and stats.
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

        viewModelScope.launch {
            dataStore.data
                .map { preferences -> preferences[IncognitoModeKey] ?: false }
                .distinctUntilChanged()
                .collect { enabled -> _incognitoModeEnabled.value = enabled }
        }
    }

    fun clearListenHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            database.clearListenHistory()
        }
    }

    fun toggleSelection(songId: String) {
        _selectedSongIds.update { selected ->
            if (songId in selected) selected - songId else selected + songId
        }
    }

    fun clearSelection() {
        _selectedSongIds.value = emptySet()
    }

    fun setIncognitoMode(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            dataStore.edit { preferences ->
                preferences[IncognitoModeKey] = enabled
            }
        }
    }
}

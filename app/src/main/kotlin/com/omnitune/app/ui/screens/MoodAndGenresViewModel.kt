/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omnitune.app.utils.classifyProviderError
import com.omnitune.app.utils.reportException
import com.omnitune.innertube.YouTube
import com.omnitune.innertube.pages.MoodAndGenres
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import javax.inject.Inject

data class MoodAndGenresUiState(
    val groups: List<MoodAndGenres> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
) {
    val totalCategories: Int get() = groups.sumOf { it.items.size }
}

@HiltViewModel
class MoodAndGenresViewModel @Inject constructor() : ViewModel() {
    private val _uiState = MutableStateFlow(MoodAndGenresUiState())
    val uiState: StateFlow<MoodAndGenresUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun retry() {
        load(forceRefresh = true)
    }

    private fun load(forceRefresh: Boolean = false) {
        if (_uiState.value.isLoading && !forceRefresh) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            runCatching {
                withTimeout(20_000L) {
                    YouTube.moodAndGenres().getOrThrow()
                }
            }
                .onSuccess { groups ->
                    _uiState.update {
                        it.copy(
                            groups = groups.filter { group -> group.items.isNotEmpty() },
                            isLoading = false,
                            error = null,
                        )
                    }
                }
                .onFailure { throwable ->
                    reportException(throwable)
                    val providerError = classifyProviderError(throwable)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = providerError.message,
                        )
                    }
                }
        }
    }
}

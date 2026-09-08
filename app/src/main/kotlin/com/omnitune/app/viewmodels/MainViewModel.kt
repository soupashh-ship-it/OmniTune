package com.omnitune.app.viewmodels

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omnitune.app.utils.NetworkMonitor
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class MainEvent {
    data class PlayFromDeepLink(val videoId: String) : MainEvent()
    data class PlayFromLocalUri(val uri: Uri) : MainEvent()
    data class ShowToast(val message: String) : MainEvent()
    data class NavigateToAlbum(val browseId: String) : MainEvent()
    data class NavigateToPlaylist(val playlistId: String) : MainEvent()
    data class NavigateToArtist(val channelId: String) : MainEvent()
    data class NavigateToSearch(val query: String) : MainEvent()
}

data class MainUiState(
    val currentVersion: String = "",
    val isInPictureInPictureMode: Boolean = false,
    val isReady: Boolean = false
)

@HiltViewModel
class MainViewModel @Inject constructor(
    networkMonitor: NetworkMonitor,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    /** Live connectivity for the app shell's offline banner. */
    val isOnline: StateFlow<Boolean> = networkMonitor.isConnected
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    private val _events = MutableSharedFlow<MainEvent>()
    val events: SharedFlow<MainEvent> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            _uiState.update { it.copy(isReady = true) }
        }
    }

    fun handleDeepLink(uri: Uri?) {
        if (uri == null) return
        
        viewModelScope.launch {
            if (isYouTubeLink(uri)) {
                val videoId = extractVideoId(uri)
                if (videoId != null) {
                    _events.emit(MainEvent.PlayFromDeepLink(videoId))
                }
            }
        }
    }

    fun handleAudioIntent(uri: Uri?) {
        if (uri == null) return
        viewModelScope.launch {
            _events.emit(MainEvent.PlayFromLocalUri(uri))
        }
    }

    private fun isYouTubeLink(uri: Uri): Boolean {
        val host = uri.host ?: return false
        return host.contains("youtube.com") || host.contains("youtu.be") || host.contains("music.youtube.com")
    }

    private fun extractVideoId(uri: Uri): String? {
        return try {
            val url = uri.toString()
            when {
                url.contains("youtu.be/") -> {
                    url.substringAfter("youtu.be/").substringBefore("?").substringBefore("&")
                }
                url.contains("/shorts/") -> {
                    url.substringAfter("/shorts/").substringBefore("?").substringBefore("&")
                }
                url.contains("v=") -> {
                    uri.getQueryParameter("v")
                }
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }

    fun setPictureInPictureMode(inPip: Boolean) {
        _uiState.update { it.copy(isInPictureInPictureMode = inPip) }
    }
}

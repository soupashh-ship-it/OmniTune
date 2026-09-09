package com.omnitune.app.viewmodels

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omnitune.app.utils.NetworkMonitor
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
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

    private val _events = Channel<MainEvent>(Channel.BUFFERED)
    val events: Flow<MainEvent> = _events.receiveAsFlow()
    private var lastHandledIntentKey: String? = null

    init {
        viewModelScope.launch {
            _uiState.update { it.copy(isReady = true) }
        }
    }

    fun handleDeepLink(uri: Uri?) {
        if (uri == null) return

        val videoId = IncomingIntentParser.extractYouTubeVideoId(uri)
        val playlistId = IncomingIntentParser.extractYouTubePlaylistId(uri)
        when {
            videoId != null -> sendEvent(MainEvent.PlayFromDeepLink(videoId))
            playlistId != null -> sendEvent(MainEvent.NavigateToPlaylist(playlistId))
        }
    }

    fun handleAudioIntent(uri: Uri?, mimeType: String? = null) {
        if (uri == null) return
        if (!IncomingIntentParser.isSupportedAudioUri(uri, mimeType)) return
        sendEvent(MainEvent.PlayFromLocalUri(uri))
    }

    fun handleIntent(intent: Intent?) {
        val uri = intent?.data ?: return
        val intentKey = listOf(intent.action, intent.type, uri.toString()).joinToString("|")
        if (lastHandledIntentKey == intentKey) return
        lastHandledIntentKey = intentKey
        val mimeType = intent.type ?: runCatching { context.contentResolver.getType(uri) }.getOrNull()

        when {
            intent.action == Intent.ACTION_VIEW && IncomingIntentParser.isAcceptedYouTubeHost(uri.host) -> {
                handleDeepLink(uri)
            }
            intent.action == Intent.ACTION_VIEW && IncomingIntentParser.isSupportedAudioUri(uri, mimeType) -> {
                handleAudioIntent(uri, mimeType)
            }
        }
    }

    private fun sendEvent(event: MainEvent) {
        _events.trySend(event)
    }

    fun setPictureInPictureMode(inPip: Boolean) {
        _uiState.update { it.copy(isInPictureInPictureMode = inPip) }
    }
}

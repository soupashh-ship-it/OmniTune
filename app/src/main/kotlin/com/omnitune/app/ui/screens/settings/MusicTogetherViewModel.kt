package com.omnitune.app.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omnitune.app.together.TogetherClient
import com.omnitune.app.together.TogetherOnlineHost
import com.omnitune.app.together.TogetherServer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MusicTogetherViewModel @Inject constructor() : ViewModel() {
    private var server: TogetherServer? = null
    private var client: TogetherClient? = null
    private var onlineHost: TogetherOnlineHost? = null

    private val _isSessionActive = MutableStateFlow(false)
    val isSessionActive: StateFlow<Boolean> = _isSessionActive.asStateFlow()

    private val _isJoining = MutableStateFlow(false)
    val isJoining: StateFlow<Boolean> = _isJoining.asStateFlow()

    fun toggleSession() {
        viewModelScope.launch {
            if (_isSessionActive.value) {
                server?.stop()
                server = null
                onlineHost?.disconnect()
                onlineHost = null
                _isSessionActive.value = false
            } else {
                val settings = com.omnitune.app.together.TogetherRoomSettings(
                    allowGuestsToAddTracks = true,
                    allowGuestsToControlPlayback = false,
                    requireHostApprovalToJoin = false
                )
                val hostId = java.util.UUID.randomUUID().toString()
                server = TogetherServer(viewModelScope, "local_session", "secret", "Host", settings)
                onlineHost = TogetherOnlineHost(viewModelScope, "local_session", "secret", hostId, "Host", settings)
                server?.start(12345)
                onlineHost?.connect("wss://together.omnitune.app")
                _isSessionActive.value = true
            }
        }
    }

    fun joinSession(code: String) {
        viewModelScope.launch {
            _isJoining.value = true
            client = TogetherClient(viewModelScope)
            try {
                val joinInfo = com.omnitune.app.together.TogetherJoinInfo("together.omnitune.app", 12345, code, "secret")
                client?.connect(joinInfo, "Guest")
            } catch (e: Exception) {}
            _isJoining.value = false
        }
    }

    fun updateSettings(allowAdd: Boolean, allowControl: Boolean, requireApproval: Boolean) {
        viewModelScope.launch {
            server?.updateSettings(
                com.omnitune.app.together.TogetherRoomSettings(
                    allowGuestsToAddTracks = allowAdd,
                    allowGuestsToControlPlayback = allowControl,
                    requireHostApprovalToJoin = requireApproval
                )
            )
        }
    }
}

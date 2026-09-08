/*
 * Adapted from SuvMusic (https://github.com/SuvojeetDev/SuvMusic)
 * Copyright (c) SuvMusic contributors
 * Licensed under GPL-3.0
 */

package com.omnitune.app.viewmodels

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omnitune.app.constants.AlbumArtDynamicColorsEnabledKey
import com.omnitune.app.constants.ArtworkShapeKey
import com.omnitune.app.constants.ArtworkSizeKey
import com.omnitune.app.constants.MiniPlayerStyleKey
import com.omnitune.app.constants.PlayerAnimatedBackgroundEnabledKey
import com.omnitune.app.constants.PlayerStyleKey
import com.omnitune.app.constants.RotatingVinylAnimationEnabledKey
import com.omnitune.app.constants.SeekbarStyleKey
import com.omnitune.app.constants.SwipeDownToDismissPlayerKey
import com.omnitune.app.db.MusicDatabase
import com.omnitune.app.models.ArtworkShape
import com.omnitune.app.models.ArtworkSize
import com.omnitune.app.models.MiniPlayerStyle
import com.omnitune.app.models.PlayerStyle
import com.omnitune.app.models.RepeatMode
import com.omnitune.app.models.SeekbarStyle
import com.omnitune.app.models.SleepTimerOption
import com.omnitune.app.models.Song
import com.omnitune.app.models.SponsorSegment
import com.omnitune.app.models.toDomainSong
import com.omnitune.app.playback.PlayerConnection
import com.omnitune.app.ui.component.DominantColors
import com.omnitune.app.ui.player.OutputDevice
import com.omnitune.app.ui.player.PlayerOverlay
import com.omnitune.app.ui.player.PlayerState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class PlayerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    val database: MusicDatabase,
    private val dataStore: DataStore<Preferences>
) : ViewModel() {

    private val _activeOverlay = MutableStateFlow<PlayerOverlay>(PlayerOverlay.None)
    val activeOverlay: StateFlow<PlayerOverlay> = _activeOverlay.asStateFlow()

    private val _sponsorSegments = MutableStateFlow<List<SponsorSegment>>(emptyList())
    val sponsorSegments: StateFlow<List<SponsorSegment>> = _sponsorSegments.asStateFlow()

    private val _relatedSongs = MutableStateFlow<List<Song>>(emptyList())
    val relatedSongs: StateFlow<List<Song>> = _relatedSongs.asStateFlow()

    private val _isFetchingRelated = MutableStateFlow(false)
    val isFetchingRelated: StateFlow<Boolean> = _isFetchingRelated.asStateFlow()

    private val _selectedRelatedIndices = MutableStateFlow<Set<Int>>(emptySet())
    val selectedRelatedIndices: StateFlow<Set<Int>> = _selectedRelatedIndices.asStateFlow()

    private val _selectedQueueIndices = MutableStateFlow<Set<Int>>(emptySet())
    val selectedQueueIndices: StateFlow<Set<Int>> = _selectedQueueIndices.asStateFlow()

    private val _audioArEnabled = MutableStateFlow(false)
    val audioArEnabled: StateFlow<Boolean> = _audioArEnabled.asStateFlow()

    private val _isFullScreen = MutableStateFlow(false)
    val isFullScreen: StateFlow<Boolean> = _isFullScreen.asStateFlow()

    private val _isVideoMode = MutableStateFlow(false)
    val isVideoMode: StateFlow<Boolean> = _isVideoMode.asStateFlow()

    private val _availableDevices = MutableStateFlow<List<OutputDevice>>(emptyList())
    val availableDevices: StateFlow<List<OutputDevice>> = _availableDevices.asStateFlow()

    val playerStyle: StateFlow<PlayerStyle> = dataStore.data.map { prefs ->
        val name = prefs[PlayerStyleKey] ?: PlayerStyle.YT_MUSIC.name
        try { PlayerStyle.valueOf(name) } catch (e: Exception) { PlayerStyle.YT_MUSIC }
    }.stateIn(viewModelScope, SharingStarted.Lazily, PlayerStyle.YT_MUSIC)

    val miniPlayerStyle: StateFlow<MiniPlayerStyle> = dataStore.data.map { prefs ->
        val name = prefs[MiniPlayerStyleKey] ?: MiniPlayerStyle.YT_MUSIC.name
        try { MiniPlayerStyle.valueOf(name) } catch (e: Exception) { MiniPlayerStyle.YT_MUSIC }
    }.stateIn(viewModelScope, SharingStarted.Lazily, MiniPlayerStyle.YT_MUSIC)

    val seekbarStyle: StateFlow<SeekbarStyle> = dataStore.data.map { prefs ->
        val name = prefs[SeekbarStyleKey] ?: SeekbarStyle.M3E_WAVY.name
        try { SeekbarStyle.valueOf(name) } catch (e: Exception) { SeekbarStyle.M3E_WAVY }
    }.stateIn(viewModelScope, SharingStarted.Lazily, SeekbarStyle.M3E_WAVY)

    val artworkShape: StateFlow<ArtworkShape> = dataStore.data.map { prefs ->
        val name = prefs[ArtworkShapeKey] ?: ArtworkShape.ROUNDED_SQUARE.name
        try { ArtworkShape.valueOf(name) } catch (e: Exception) { ArtworkShape.ROUNDED_SQUARE }
    }.stateIn(viewModelScope, SharingStarted.Lazily, ArtworkShape.ROUNDED_SQUARE)

    val artworkSize: StateFlow<ArtworkSize> = dataStore.data.map { prefs ->
        val name = prefs[ArtworkSizeKey] ?: ArtworkSize.FULL.name
        try { ArtworkSize.valueOf(name) } catch (e: Exception) { ArtworkSize.FULL }
    }.stateIn(viewModelScope, SharingStarted.Lazily, ArtworkSize.FULL)

    val swipeDownToDismissEnabled: StateFlow<Boolean> = dataStore.data.map { prefs ->
        prefs[SwipeDownToDismissPlayerKey] ?: true
    }.stateIn(viewModelScope, SharingStarted.Lazily, true)

    val animatedBackgroundEnabled: StateFlow<Boolean> = dataStore.data.map { prefs ->
        prefs[PlayerAnimatedBackgroundEnabledKey] ?: true
    }.stateIn(viewModelScope, SharingStarted.Lazily, true)

    val albumArtDynamicColorsEnabled: StateFlow<Boolean> = dataStore.data.map { prefs ->
        prefs[AlbumArtDynamicColorsEnabledKey] ?: true
    }.stateIn(viewModelScope, SharingStarted.Lazily, true)

    val isRotatingEnabled: StateFlow<Boolean> = dataStore.data.map { prefs ->
        prefs[RotatingVinylAnimationEnabledKey] ?: true
    }.stateIn(viewModelScope, SharingStarted.Lazily, true)

    fun setActiveOverlay(overlay: PlayerOverlay) {
        _activeOverlay.value = overlay
    }

    fun dismissOverlay() {
        _activeOverlay.value = PlayerOverlay.None
    }

    fun setPlayerStyle(style: PlayerStyle) {
        viewModelScope.launch {
            dataStore.edit { it[PlayerStyleKey] = style.name }
        }
    }

    fun setMiniPlayerStyle(style: MiniPlayerStyle) {
        viewModelScope.launch {
            dataStore.edit { it[MiniPlayerStyleKey] = style.name }
        }
    }

    fun setSeekbarStyle(style: SeekbarStyle) {
        viewModelScope.launch {
            dataStore.edit { it[SeekbarStyleKey] = style.name }
        }
    }

    fun setArtworkShape(shape: ArtworkShape) {
        viewModelScope.launch {
            dataStore.edit { it[ArtworkShapeKey] = shape.name }
        }
    }

    fun setArtworkSize(size: ArtworkSize) {
        viewModelScope.launch {
            dataStore.edit { it[ArtworkSizeKey] = size.name }
        }
    }

    fun setFullScreen(fullScreen: Boolean) {
        _isFullScreen.value = fullScreen && _isVideoMode.value
    }

    fun setVideoMode(enabled: Boolean) {
        _isVideoMode.value = enabled
        if (!enabled) {
            _isFullScreen.value = false
        }
    }

    fun toggleVideoMode() {
        setVideoMode(!_isVideoMode.value)
    }

    fun toggleRelatedSelection(index: Int) {
        _selectedRelatedIndices.value = if (_selectedRelatedIndices.value.contains(index)) {
            _selectedRelatedIndices.value - index
        } else {
            _selectedRelatedIndices.value + index
        }
    }

    fun selectAllRelated() {
        _selectedRelatedIndices.value = _relatedSongs.value.indices.toSet()
    }

    fun clearRelatedSelection() {
        _selectedRelatedIndices.value = emptySet()
    }

    fun toggleQueueSelection(index: Int) {
        if (index < 0) return
        _selectedQueueIndices.value = if (_selectedQueueIndices.value.contains(index)) {
            _selectedQueueIndices.value - index
        } else {
            _selectedQueueIndices.value + index
        }
    }

    fun selectAllQueue(queueSize: Int) {
        _selectedQueueIndices.value = (0 until queueSize).toSet()
    }

    fun clearQueueSelection() {
        _selectedQueueIndices.value = emptySet()
    }

    fun pruneQueueSelection(queueSize: Int) {
        _selectedQueueIndices.value = _selectedQueueIndices.value.filter { it in 0 until queueSize }.toSet()
    }

    fun refreshRelatedSongs(currentSongId: String?) {
        if (currentSongId.isNullOrBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            _isFetchingRelated.value = true
            try {
                _relatedSongs.value = emptyList()
            } catch (e: Exception) {
                _relatedSongs.value = emptyList()
            } finally {
                _isFetchingRelated.value = false
            }
        }
    }

    fun refreshDevices() {
        // Output device detection logic
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? android.media.AudioManager
        val devices = mutableListOf<OutputDevice>()
        devices.add(OutputDevice(id = "phone_speaker", name = "This Device", type = com.omnitune.app.ui.player.DeviceType.PHONE, isSelected = true))
        _availableDevices.value = devices
    }
}

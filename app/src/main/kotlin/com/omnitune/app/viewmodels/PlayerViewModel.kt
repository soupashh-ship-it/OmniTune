/*
 * Adapted from SuvMusic (https://github.com/SuvojeetDev/SuvMusic)
 * Copyright (c) SuvMusic contributors
 * Licensed under GPL-3.0
 */

package com.omnitune.app.viewmodels

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import androidx.media3.common.MediaItem
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
import com.omnitune.app.models.PlayerPresentationPreferenceMapper
import com.omnitune.app.models.PlayerStyle
import com.omnitune.app.models.RepeatMode
import com.omnitune.app.models.SeekbarStyle
import com.omnitune.app.models.SleepTimerOption
import com.omnitune.app.models.Song
import com.omnitune.app.models.SponsorSegment
import com.omnitune.app.models.toDomainSong
import com.omnitune.app.models.toMediaMetadata
import com.omnitune.app.models.toPresentationSong
import com.omnitune.app.extensions.metadata
import com.omnitune.app.playback.PlayerConnection
import com.omnitune.app.playback.continuation.OmniAutoplayRecommendationProvider
import com.omnitune.app.ui.player.AudioOutputDeviceMapper
import com.omnitune.app.ui.player.AudioOutputRoute
import com.omnitune.app.ui.component.DominantColors
import com.omnitune.app.ui.player.OutputDevice
import com.omnitune.app.ui.player.PlayerOverlay
import com.omnitune.app.ui.player.PlayerState
import com.omnitune.app.ui.player.VideoModeSession
import com.omnitune.app.ui.player.VideoModeStateReducer
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
import timber.log.Timber

internal object RelatedSongsMapper {
    fun fromMediaItems(currentSongId: String, mediaItems: List<MediaItem>, limit: Int = 50): List<Song> =
        mediaItems
            .mapNotNull { item -> item.metadata?.toDomainSong() ?: item.toFallbackSong() }
            .filter { song -> song.id.isNotBlank() && song.id != currentSongId }
            .distinctBy { song -> song.id }
            .take(limit)

    private fun MediaItem.toFallbackSong(): Song? {
        val id = mediaId.takeIf { it.isNotBlank() } ?: return null
        val title = mediaMetadata.title?.toString()?.takeIf { it.isNotBlank() } ?: return null
        val artist = mediaMetadata.artist?.toString()
            ?: mediaMetadata.subtitle?.toString()
            ?: ""
        return Song(
            id = id,
            title = title,
            artist = artist,
            album = mediaMetadata.albumTitle?.toString().orEmpty(),
            thumbnailUrl = mediaMetadata.artworkUri?.toString(),
            isVideo = mediaMetadata.extras?.getBoolean(com.omnitune.app.extensions.ExtraIsMusicVideo, false) == true,
        )
    }
}

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

    private val _relatedError = MutableStateFlow<String?>(null)
    val relatedError: StateFlow<String?> = _relatedError.asStateFlow()

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
    private var videoModeSession = VideoModeSession()

    private val _availableDevices = MutableStateFlow<List<OutputDevice>>(emptyList())
    val availableDevices: StateFlow<List<OutputDevice>> = _availableDevices.asStateFlow()
    private var preferredOutputRouteId: Int? = null

    val playerStyle: StateFlow<PlayerStyle> = dataStore.data.map { prefs ->
        PlayerPresentationPreferenceMapper.resolvePlayerStyle(prefs[PlayerStyleKey])
    }.stateIn(
        viewModelScope,
        SharingStarted.Lazily,
        PlayerPresentationPreferenceMapper.DefaultPlayerStyle
    )

    val miniPlayerStyle: StateFlow<MiniPlayerStyle> = dataStore.data.map { prefs ->
        PlayerPresentationPreferenceMapper.resolveMiniPlayerStyle(prefs[MiniPlayerStyleKey])
    }.stateIn(
        viewModelScope,
        SharingStarted.Lazily,
        PlayerPresentationPreferenceMapper.DefaultMiniPlayerStyle
    )

    val seekbarStyle: StateFlow<SeekbarStyle> = dataStore.data.map { prefs ->
        PlayerPresentationPreferenceMapper.resolveSeekbarStyle(prefs[SeekbarStyleKey])
    }.stateIn(
        viewModelScope,
        SharingStarted.Lazily,
        PlayerPresentationPreferenceMapper.DefaultSeekbarStyle
    )

    val artworkShape: StateFlow<ArtworkShape> = dataStore.data.map { prefs ->
        PlayerPresentationPreferenceMapper.resolveArtworkShape(prefs[ArtworkShapeKey])
    }.stateIn(
        viewModelScope,
        SharingStarted.Lazily,
        PlayerPresentationPreferenceMapper.DefaultArtworkShape
    )

    val artworkSize: StateFlow<ArtworkSize> = dataStore.data.map { prefs ->
        PlayerPresentationPreferenceMapper.resolveArtworkSize(prefs[ArtworkSizeKey])
    }.stateIn(
        viewModelScope,
        SharingStarted.Lazily,
        PlayerPresentationPreferenceMapper.DefaultArtworkSize
    )

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

    fun setVideoMode(enabled: Boolean, mediaId: String? = null) {
        applyVideoModeSession(VideoModeStateReducer.setEnabled(videoModeSession, enabled, mediaId))
    }

    fun onMediaItemChanged(mediaId: String?) {
        applyVideoModeSession(VideoModeStateReducer.onMediaItemChanged(videoModeSession, mediaId))
    }

    private fun applyVideoModeSession(session: VideoModeSession) {
        videoModeSession = session
        _isVideoMode.value = session.enabled
        if (!session.enabled) {
            _isFullScreen.value = false
        }
    }

    fun toggleVideoMode(mediaId: String? = null) {
        setVideoMode(!_isVideoMode.value, mediaId)
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
        refreshRelatedSongs(Song(id = currentSongId, title = currentSongId, artist = ""))
    }

    fun refreshRelatedSongs(currentSong: Song?) {
        if (currentSong?.id.isNullOrBlank()) return
        val seed = currentSong
        viewModelScope.launch(Dispatchers.IO) {
            _isFetchingRelated.value = true
            _relatedError.value = null
            try {
                val cached = runCatching {
                    database.relatedSongs(seed.id).map { it.toPresentationSong() }
                }.getOrDefault(emptyList())
                    .filter { it.id != seed.id }
                    .distinctBy { it.id }

                if (cached.isNotEmpty()) {
                    _relatedSongs.value = cached
                    return@launch
                }

                val provider = OmniAutoplayRecommendationProvider(database)
                val seedMetadata = seed.toMediaMetadata()
                val mediaItems = provider.songsRelatedToTrack(seedMetadata)
                    .ifEmpty { provider.songsForTitleSearch(seedMetadata) }
                    .ifEmpty { provider.quickPicks(seedMetadata) }

                _relatedSongs.value = RelatedSongsMapper.fromMediaItems(seed.id, mediaItems)
            } catch (e: Exception) {
                Timber.w(e, "Unable to load related songs for %s", seed.id)
                _relatedSongs.value = emptyList()
                _relatedError.value = "Couldn't load related tracks. Check your connection and try again."
            } finally {
                _isFetchingRelated.value = false
            }
        }
    }

    fun refreshDevices() {
        val routes = currentAudioOutputRoutes()
        val selectedRouteId = preferredOutputRouteId?.takeIf { selected ->
            routes.any { it.id == selected }
        }
        if (selectedRouteId == null && preferredOutputRouteId != null) {
            preferredOutputRouteId = null
        }
        _availableDevices.value = AudioOutputDeviceMapper.build(routes, selectedRouteId)
    }

    fun switchOutputDevice(device: OutputDevice, playerConnection: PlayerConnection?) {
        val targetRouteId = device.routeId
        val targetDevice = targetRouteId?.let(::findAudioOutputDevice)
        if (targetRouteId != null && targetDevice == null) {
            Timber.w("Requested output route %s is no longer available", targetRouteId)
            refreshDevices()
            return
        }

        try {
            playerConnection?.setPreferredAudioDevice(targetDevice)
            preferredOutputRouteId = targetRouteId
            refreshDevices()
        } catch (e: Exception) {
            Timber.w(e, "Unable to switch audio output route")
            refreshDevices()
        }
    }

    private fun currentAudioOutputRoutes(): List<AudioOutputRoute> {
        val audioManager = context.getSystemService(AudioManager::class.java) ?: return emptyList()
        return audioManager
            .getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            .filter { it.isSink }
            .map(AudioOutputDeviceMapper::descriptorFrom)
    }

    private fun findAudioOutputDevice(routeId: Int): AudioDeviceInfo? {
        val audioManager = context.getSystemService(AudioManager::class.java) ?: return null
        return audioManager
            .getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            .firstOrNull { it.id == routeId && it.isSink }
    }
}

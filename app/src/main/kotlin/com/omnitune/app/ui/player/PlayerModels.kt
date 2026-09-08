/*
 * Adapted from SuvMusic (https://github.com/SuvojeetDev/SuvMusic)
 * Copyright (c) SuvMusic contributors
 * Licensed under GPL-3.0
 */

package com.omnitune.app.ui.player

import com.omnitune.app.models.DownloadState
import com.omnitune.app.models.MusicSource
import com.omnitune.app.models.RepeatMode
import com.omnitune.app.models.SleepTimerOption
import com.omnitune.app.models.Song
import com.omnitune.app.models.VideoQuality

data class OutputDevice(
    val id: String,
    val name: String,
    val type: DeviceType,
    val isSelected: Boolean = false
)

enum class DeviceType {
    PHONE,
    SPEAKER,
    BLUETOOTH,
    HEADPHONES,
    CAST,
    UNKNOWN
}

enum class AudioQuality(val label: String, val bitrateRange: IntRange) {
    AUTO("Auto (Adaptive)", 0..160),
    LOW("Low (48-64 kbps)", 0..70),
    MEDIUM("Normal (128 kbps)", 71..160),
    HIGH("Always High (256 kbps)", 161..512);

    companion object {
        fun fromBitrate(bitrate: Int): AudioQuality {
            return entries.find { bitrate in it.bitrateRange } ?: MEDIUM
        }
    }
}

/**
 * Full player state for UI updates matching SuvMusic.
 */
data class PlayerState(
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val bufferedPercentage: Int = 0,
    val queue: List<Song> = emptyList(),
    val currentIndex: Int = -1,
    val shuffleEnabled: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val audioQuality: AudioQuality = AudioQuality.HIGH,
    val videoQuality: VideoQuality = VideoQuality.HIGH,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isLiked: Boolean = false,
    val isDisliked: Boolean = false,
    val downloadState: DownloadState = DownloadState.NOT_DOWNLOADED,
    val isAutoplayEnabled: Boolean = false,
    val isVideoMode: Boolean = false,
    val availableDevices: List<OutputDevice> = emptyList(),
    val selectedDevice: OutputDevice? = null,
    val playbackSpeed: Float = 1.0f,
    val pitch: Float = 1.0f,
    val audioCodec: String? = null,
    val audioBitrate: Int? = null,
    val dominantColor: Int = -16777216,
    val videoNotFound: Boolean = false,
    val isRadioMode: Boolean = false,
    val activeAudioSource: MusicSource? = null,
    val isSwitchingSource: Boolean = false
) {
    val progress: Float
        get() = if (duration > 0) currentPosition.toFloat() / duration else 0f

    val hasNext: Boolean
        get() = currentIndex < queue.size - 1 || repeatMode == RepeatMode.ALL || isAutoplayEnabled || isRadioMode

    val hasPrevious: Boolean
        get() = currentIndex > 0 || repeatMode == RepeatMode.ALL

    val audioFormatDisplay: String
        get() {
            val codec = audioCodec?.uppercase() ?: return "Unknown"
            val bitrate = audioBitrate?.let { "${it}kbps" } ?: ""
            return if (bitrate.isNotEmpty()) "$codec • $bitrate" else codec
        }
}

/**
 * State object for PlayerScreen to reduce parameter count.
 */
data class PlayerScreenState(
    val playbackInfo: PlayerState,
    val playerState: PlayerState,
    val lyrics: String? = null,
    val isFetchingLyrics: Boolean = false,
    val relatedSongs: List<Song> = emptyList(),
    val isFetchingRelated: Boolean = false,
    val selectedRelatedIndices: Set<Int> = emptySet(),
    val isLoggedIn: Boolean = false,
    val sleepTimerOption: SleepTimerOption = SleepTimerOption.OFF,
    val sleepTimerRemainingMs: Long? = null,
    val isRadioMode: Boolean = false,
    val isLoadingMoreSongs: Boolean = false,
    val listenTogetherBufferingUsers: List<String> = emptyList()
)

/**
 * Actions for PlayerScreen to reduce parameter count.
 */
data class PlayerScreenActions(
    val onBack: () -> Unit,
    val onPlayPause: () -> Unit,
    val onNext: () -> Unit,
    val onPrevious: () -> Unit,
    val onSeekTo: (Long) -> Unit,
    val onToggleLike: () -> Unit,
    val onToggleDislike: () -> Unit = {},
    val onShuffleToggle: () -> Unit,
    val onRepeatToggle: () -> Unit,
    val onDownload: () -> Unit = {},
    val onToggleVideoMode: () -> Unit = {},
    val onDismissVideoError: () -> Unit = {},
    val onArtistClick: (String) -> Unit = {},
    val onAlbumClick: (String) -> Unit = {},
    val onAlbumClickWithSong: (String, String?) -> Unit = { albumId, _ -> onAlbumClick(albumId) },
    val onPlayFromQueue: (Int) -> Unit = {},
    val onToggleAutoplay: () -> Unit = {},
    val onLoadMoreRadioSongs: () -> Unit = {},
    val onSetSleepTimer: (SleepTimerOption, Int?) -> Unit = { _, _ -> },
    val onSwitchDevice: (OutputDevice) -> Unit = {},
    val onRefreshDevices: () -> Unit = {},
    val onSetPlaybackParameters: (Float, Float) -> Unit = { _, _ -> },
    val onShowAIEqualizer: () -> Unit = {},
    val onStartRadio: () -> Unit = {},
    val onToggleRelatedSelection: (Int) -> Unit = {},
    val onSelectAllRelated: () -> Unit = {},
    val onClearRelatedSelection: () -> Unit = {},
    val onAddRelatedToQueue: (List<Song>) -> Unit = {},
    val onAddRelatedToPlaylist: (List<Song>) -> Unit = {},
    val onPlayRelated: (Song) -> Unit = {},
    val onClearQueue: () -> Unit = {},
    val onSwitchAudioSource: () -> Unit = {}
)

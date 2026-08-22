/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.ui.screens.settings

/**
 * Source-of-truth inventory for preference-backed controls shown in Settings.
 * The companion test compares this registry with direct rememberPreference calls so a new
 * visible preference cannot be added without recording its runtime owner and behavior.
 */
internal data class SettingsBehaviorEntry(
    val keyName: String,
    val screen: String,
    val defaultValue: String,
    val runtimeOwner: String,
    val effect: String,
)

internal object SettingsBehaviorRegistry {
    val entries = listOf(
        entry("AutoplaySimilarSongsKey", "Behavior", "true", "MusicService", "Next-track recommendations"),
        entry("PersistentQueueKey", "Behavior", "true", "QueuePersistenceManager", "Queue survives service recreation"),
        entry("AutoSkipNextOnErrorKey", "Behavior", "true", "MusicService", "Skips failed tracks"),
        entry("AutoStartOnBluetoothKey", "Behavior", "false", "MusicService", "Bluetooth connect playback"),
        entry("PauseOnDeviceMuteKey", "Behavior", "false", "MusicService", "Pause on device mute"),
        entry("PermanentShuffleKey", "Behavior", "false", "MusicService", "Persistent shuffle mode"),
        entry("StopMusicOnTaskClearKey", "Behavior", "false", "MusicService", "Task-clear service policy"),
        entry("DynamicSongColorsKey", "Appearance", "true", "Theme", "Dynamic player palette"),
        entry("PureBlackKey", "Appearance", "false", "Theme", "Pure-black dark surface"),
        entry("UseSystemFontKey", "Appearance", "false", "Theme", "System typography"),
        entry("HidePlayerThumbnailKey", "Appearance", "false", "PlayerScreen", "Artwork visibility"),
        entry("SwipeSensitivityKey", "Appearance", "0.73", "PlayerScreen", "Swipe gesture threshold"),
        entry("ShowLikedPlaylistKey", "Appearance / Library", "true", "LibraryScreen", "Liked shortcut visibility"),
        entry("ShowDownloadedPlaylistKey", "Appearance / Library", "true", "LibraryScreen", "Downloads shortcut visibility"),
        entry("ShowTopPlaylistKey", "Appearance / Library", "true", "LibraryScreen", "Top shortcut visibility"),
        entry("ShowCachedPlaylistKey", "Appearance / Library", "true", "LibraryScreen", "Cached shortcut visibility"),
        entry("ShowTagsInLibraryKey", "Appearance / Library", "true", "LibraryScreen", "Folder/tag shortcut visibility"),
        entry("PlayerCustomImageUriKey", "Background", "empty", "PlayerScreen", "Custom backdrop image"),
        entry("PlayerCustomBlurKey", "Background", "0", "PlayerScreen", "Backdrop blur"),
        entry("PlayerCustomContrastKey", "Background", "1", "PlayerScreen", "Backdrop contrast"),
        entry("PlayerCustomBrightnessKey", "Background", "1", "PlayerScreen", "Backdrop brightness"),
        entry("NetworkMeteredKey", "Playback", "false", "MusicService", "Metered playback policy"),
        entry("HistoryDuration", "Playback", "30", "PlaybackEventRecorder", "History listening threshold"),
        entry("SkipSilenceKey", "Playback", "false", "PlaybackPreferenceObserver", "Silence skipping"),
        entry("AudioNormalizationKey", "Playback", "true", "MusicService", "Audio normalization"),
        entry("AudioOffload", "Playback", "false", "PlayerFactory", "Audio offload configuration"),
        entry("SeekExtraSeconds", "Playback", "false", "PlayerConnection", "Progressive seek behavior"),
        entry("PlayerStreamClientKey", "Playback", "ANDROID_VR", "StreamExtractor", "Streaming client"),
        entry("ArtistSeparatorsKey", "Playback", ", ; / &", "Metadata parser", "Artist splitting"),
        entry("AudioCrossfadeDurationKey", "Playback", "0", "CrossfadePlaybackCoordinator", "Crossfade duration"),
        entry("PlaybackQualityModeKey", "Playback", "AUTO", "MusicService", "Resolved-stream quality selection"),
        entry("RestrictExplicitContentKey", "Parental Controls", "false", "Search/Home/Playback", "Explicit-content filtering"),
        entry("SafeSearchKey", "Parental Controls", "true", "SearchViewModel", "Safe search filtering"),
        entry("ListenBrainzEnabledKey", "ListenBrainz", "false", "ScrobblingManager", "Completed-list submission"),
        entry("ListenBrainzNowPlayingKey", "ListenBrainz", "true", "ScrobblingManager", "Now-playing submission"),
        entry("ListenBrainzTokenKey", "ListenBrainz", "empty", "SecurePreferenceCipher + ScrobblingManager", "Encrypted user credential"),
        entry("ScrobbleDelayPercentKey", "ListenBrainz", "50", "MusicService", "Scrobble threshold percent"),
        entry("ScrobbleDelaySecondsKey", "ListenBrainz", "30", "MusicService", "Scrobble threshold seconds"),
        entry("ScrobbleMinSongDurationKey", "ListenBrainz", "30", "MusicService", "Minimum scrobble duration"),
        entry("EnableLrcLibKey", "Lyrics", "true", "LyricsHelper", "LrcLib provider availability"),
        entry("EnableKugouKey", "Lyrics", "true", "LyricsHelper", "KuGou provider availability"),
        entry("EnableBetterLyricsKey", "Lyrics", "true", "LyricsHelper", "BetterLyrics provider availability"),
        entry("EnableSimpMusicLyricsKey", "Lyrics", "true", "LyricsHelper", "SimpMusic provider availability"),
        entry("LyricsScrollKey", "Lyrics", "true", "LyricsScreen", "Automatic lyric scrolling"),
        entry("AutoDownloadOnLikeKey", "Downloads", "false", "DownloadUtil", "Liked-song download enqueue"),
        entry("SmartTrimmerKey", "Downloads / Storage", "true", "OmniTuneApp + DownloadUtil", "Cache trimming"),
        entry("DownloadWifiOnlyKey", "Downloads", "true", "DownloadUtil", "Network admission"),
        entry("RetryFailedDownloadsKey", "Downloads", "true", "ExoDownloadService", "Bounded retry policy"),
        entry("DownloadMaxParallelKey", "Downloads", "3", "DownloadUtil", "Concurrent download limit"),
        entry("DownloadQualityKey", "Downloads", "AUTO", "DownloadUtil", "Download stream quality"),
        entry("QuickPicksKey", "Library", "QUICK_PICKS", "HomeDiscoveryViewModel", "Quick-pick source"),
        entry("OmniLibraryDesignKey", "Appearance", "default", "Library surfaces", "Library presentation"),
        entry("OmniLyricsPresentationKey", "Appearance", "default", "Lyrics screen", "Lyrics presentation"),
        entry("OmniMiniPlayerDesignKey", "Appearance", "default", "MiniPlayer", "Mini-player presentation"),
        entry("OmniPlayerBackgroundStyleKey", "Appearance", "default", "PlayerScreen", "Player background style"),
        entry("OmniPlayerButtonColorModeKey", "Appearance", "default", "PlayerScreen", "Player button color mode"),
        entry("OmniPlayerDesignStyleKey", "Appearance", "default", "PlayerScreen", "Player layout style"),
        entry("OmniSliderStyleKey", "Appearance", "default", "PlayerScreen", "Player slider style"),
        entry("UseLoginForBrowse", "Account", "true", "OmniTuneApp", "Authenticated browsing"),
        entry("YtmSyncKey", "Account", "false", "YouTubePlaylistSync", "Signed-in playlist sync"),
        entry("SelectedYtmPlaylistsKey", "Account", "empty", "YouTubePlaylistSync", "Sync playlist selection"),
        entry("AccountNameKey", "Account", "empty", "Account screen", "Signed-in account display"),
        entry("AccountEmailKey", "Account", "empty", "Account screen", "Signed-in account display"),
        entry("AccountChannelHandleKey", "Account", "empty", "Account screen", "Signed-in account display"),
        entry("YtmLastSyncAtKey", "Account", "0", "YouTubePlaylistSync", "Last sync time display"),
        entry("YtmLastSyncStatusKey", "Account", "empty", "YouTubePlaylistSync", "Last sync status display"),
        entry("YtmLastSyncErrorKey", "Account", "empty", "YouTubePlaylistSync", "Last sync error display"),
        entry("InnerTubeCookieKey", "Account", "empty", "OmniTuneApp", "Encrypted account session"),
        entry("LastUpdateCheckKey", "Updates", "0", "UpdatesSettings", "Read-only last-check display"),
        entry("UpdateChannelKey", "Updates", "STABLE", "UpdatesSettings", "Release channel selection"),
    )

    private fun entry(
        keyName: String,
        screen: String,
        defaultValue: String,
        runtimeOwner: String,
        effect: String,
    ) = SettingsBehaviorEntry(keyName, screen, defaultValue, runtimeOwner, effect)
}

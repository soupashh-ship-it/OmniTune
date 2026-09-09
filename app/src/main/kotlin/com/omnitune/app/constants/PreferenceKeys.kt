/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.constants

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import java.time.LocalDateTime
import java.time.ZoneOffset

val PureBlackKey = booleanPreferencesKey("pureBlack")
val DynamicThemeKey = booleanPreferencesKey("dynamicTheme")
val DynamicSongColorsKey = booleanPreferencesKey("dynamicSongColors")
val CustomThemeColorKey = stringPreferencesKey("customThemeColor")
val RandomThemeOnStartupKey = booleanPreferencesKey("randomThemeOnStartup")
val DarkModeKey = stringPreferencesKey("darkMode")
val UseSystemFontKey = booleanPreferencesKey("useSystemFont")
val DefaultOpenTabKey = stringPreferencesKey("defaultOpenTab")
val SlimNavBarKey = booleanPreferencesKey("slimNavBar")
val SliderStyleKey = stringPreferencesKey("sliderStyle")
val SwipeToSongKey = booleanPreferencesKey("SwipeToSong")
val PlayerDesignStyleKey = stringPreferencesKey("playerDesignStyle")
val UseNewLibraryDesignKey = booleanPreferencesKey("useNewLibraryDesign")
val UseNewMiniPlayerDesignKey = booleanPreferencesKey("useNewMiniPlayerDesign")
val HidePlayerThumbnailKey = booleanPreferencesKey("hidePlayerThumbnail")
val OmniTuneCanvasKey = booleanPreferencesKey("veluneCanvas")
val PlaybackQualityModeKey = stringPreferencesKey("playbackQualityMode")

val ThumbnailCornerRadiusKey = floatPreferencesKey("thumbnailCornerRadius")
val CropThumbnailToSquareKey = booleanPreferencesKey("cropThumbnailToSquare")
val SeekExtraSeconds = booleanPreferencesKey("seekExtraSeconds")
val GlassNavigationBarKey = booleanPreferencesKey("glassNavigationBar")
val GlassMiniPlayerKey = booleanPreferencesKey("glassMiniPlayer")
val MaxImageCacheSizeKey = intPreferencesKey("maxImageCacheSize")

enum class SliderStyle {
    Standard,
    Wavy,
    Thick,
    Circular,
    Simple,
}

const val SYSTEM_DEFAULT = "SYSTEM_DEFAULT"
val AppLanguageKey = stringPreferencesKey("appLanguage")
val ContentLanguageKey = stringPreferencesKey("contentLanguage")
val ContentCountryKey = stringPreferencesKey("contentCountry")
val EnableKugouKey = booleanPreferencesKey("enableKugou")
val EnableLrcLibKey = booleanPreferencesKey("enableLrclib")
val EnableBetterLyricsKey = booleanPreferencesKey("enableBetterLyrics")
val EnableSimpMusicLyricsKey = booleanPreferencesKey("enableSimpMusicLyrics")
val ProxyEnabledKey = booleanPreferencesKey("proxyEnabled")
val ProxyUrlKey = stringPreferencesKey("proxyUrl")
val ProxyTypeKey = stringPreferencesKey("proxyType")
val StreamBypassProxyKey = booleanPreferencesKey("streamBypassProxy")
val YtmSyncKey = booleanPreferencesKey("ytmSync")
val SelectedYtmPlaylistsKey = stringPreferencesKey("ytm_selected_playlists")
val YtmLastSyncAtKey = longPreferencesKey("ytm_last_sync_at")
val YtmLastSyncStatusKey = stringPreferencesKey("ytm_last_sync_status")
val YtmLastSyncErrorKey = stringPreferencesKey("ytm_last_sync_error")

// ListenBrainz scrobbling
val ListenBrainzEnabledKey = booleanPreferencesKey("listenbrainz_enabled")
val ListenBrainzTokenKey = stringPreferencesKey("listenbrainz_token")
val ListenBrainzNowPlayingKey = booleanPreferencesKey("listenbrainz_now_playing")
val ScrobbleDelayPercentKey = floatPreferencesKey("scrobbleDelayPercent")
val ScrobbleMinSongDurationKey = intPreferencesKey("scrobbleMinSongDuration")
val ScrobbleDelaySecondsKey = intPreferencesKey("scrobbleDelaySeconds")
val IncognitoModeKey = booleanPreferencesKey("incognito_mode_enabled")

val AudioQualityKey = stringPreferencesKey("audioQuality")
val NetworkMeteredKey = booleanPreferencesKey("networkMetered")
val PlayerCacheLimitKey = longPreferencesKey("player_cache_limit")

enum class AudioQuality(val label: String = "") {
    AUTO("Auto"),
    LOW("Low"),
    MEDIUM("Medium"),
    HIGH("High"),
    HIGHEST("Highest");
}

enum class UpdateChannel {
    STABLE,
    BETA,
    NIGHTLY,
}

enum class MyTopFilter {
    ALL_TIME,
    DAY,
    WEEK,
    MONTH,
    YEAR,
}

val LastLibraryBackupAtKey = longPreferencesKey("lastLibraryBackupAt")
val RestrictExplicitContentKey = booleanPreferencesKey("restrictExplicitContent")
val SafeSearchKey = booleanPreferencesKey("safeSearch")
val SmartTrimmerKey = booleanPreferencesKey("smartTrimmer")

enum class SongSortType {
    CREATE_DATE,
    NAME,
    ARTIST,
    PLAY_TIME,
}

enum class ArtistSongSortType {
    CREATE_DATE,
    NAME,
    PLAY_TIME,
}

enum class ArtistSortType {
    CREATE_DATE,
    NAME,
    SONG_COUNT,
    PLAY_TIME,
}

enum class AlbumSortType {
    CREATE_DATE,
    NAME,
    ARTIST,
    YEAR,
    SONG_COUNT,
    LENGTH,
    PLAY_TIME,
}

enum class PlaylistSortType {
    CUSTOM,
    CREATE_DATE,
    NAME,
    SONG_COUNT,
    LAST_UPDATED,
}

val SongSortTypeKey = stringPreferencesKey("songSortType")

val SongSortDescendingKey = booleanPreferencesKey("songSortDescending")
val TopPlaylistSortDescendingKey = booleanPreferencesKey("topPlaylistSortDescending")

enum class LibraryViewType {
    LIST,
    GRID;

    fun toggle() = when (this) {
        LIST -> GRID
        GRID -> LIST
    }
}

enum class MixSortType {
    CREATE_DATE,
    NAME,
    LAST_UPDATED,
    ARTIST,
}

val MixSortTypeKey = stringPreferencesKey("mixSortType")
val MixSortDescendingKey = booleanPreferencesKey("mixSortDescending")
val AlbumViewTypeKey = stringPreferencesKey("albumViewType")
val ArtistViewTypeKey = stringPreferencesKey("artistViewType")
val PlaylistViewTypeKey = stringPreferencesKey("playlistViewType")
val GridItemsSizeKey = stringPreferencesKey("gridItemsSize")
val GridItemSizeKey = stringPreferencesKey("gridItemSize")

val ArtistSortTypeKey = stringPreferencesKey("artistSortType")
val ArtistSortDescendingKey = booleanPreferencesKey("artistSortDescending")
val PlaylistSortTypeKey = stringPreferencesKey("playlistSortType")
val PlaylistSortDescendingKey = booleanPreferencesKey("playlistSortDescending")
val AlbumSortTypeKey = stringPreferencesKey("albumSortType")
val AlbumSortDescendingKey = booleanPreferencesKey("albumSortDescending")




val PlayerStreamClientKey = stringPreferencesKey("playerStreamClient")

enum class PlayerStreamClient {
    ANDROID_VR,
    WEB_REMIX,
    IOS,
    MOBILE,
    TVHTML5,
    ANDROID_MUSIC,
}

val PersistentQueueKey = booleanPreferencesKey("persistentQueue")
val PermanentShuffleKey = booleanPreferencesKey("permanentShuffle")
val SkipSilenceKey = booleanPreferencesKey("skipSilence")
val AudioNormalizationKey = booleanPreferencesKey("audioNormalization")
val AudioOffload = booleanPreferencesKey("audioOffloadV2")
val AudioCrossfadeDurationKey = intPreferencesKey("audioCrossfadeDuration")
val AutoLoadMoreKey = booleanPreferencesKey("autoLoadMore")
val AutoDownloadOnLikeKey = booleanPreferencesKey("autoDownloadOnLike")
val AutoSkipNextOnErrorKey = booleanPreferencesKey("autoSkipNextOnError")
val AutoplaySimilarSongsKey = booleanPreferencesKey("autoplaySimilarSongs")
val LikedSongsShuffleKey = booleanPreferencesKey("likedSongsShuffle")
val PauseOnDeviceMuteKey = booleanPreferencesKey("pauseOnDeviceMute")
val KeepScreenOnKey = booleanPreferencesKey("keepScreenOn")
val AutoStartOnBluetoothKey = booleanPreferencesKey("autoStartOnBluetooth")
val StopMusicOnTaskClearKey = booleanPreferencesKey("stopMusicOnTaskClear")
val PictureInPictureEnabledKey = booleanPreferencesKey("pictureInPictureEnabled")
val ArtistSeparatorsKey = stringPreferencesKey("artistSeparators")
val PlaylistTagsFilterKey = stringPreferencesKey("playlistTagsFilter")
val ShowHomeCategoryChipsKey = booleanPreferencesKey("showHomeCategoryChips")
val ShowTagsInLibraryKey = booleanPreferencesKey("showTagsInLibrary")

// Downloads settings
val DownloadMaxParallelKey = intPreferencesKey("downloadMaxParallel")
val DownloadQualityKey = stringPreferencesKey("downloadQuality")
val DownloadWifiOnlyKey = booleanPreferencesKey("downloadWifiOnly")
val RetryFailedDownloadsKey = booleanPreferencesKey("retryFailedDownloads")

// Content & Quick Picks settings
val QuickPicksKey = stringPreferencesKey("quickPicks")
enum class QuickPicks {
    QUICK_PICKS,
    LAST_LISTEN,
    TRENDING,
    LAST_LISTENED,
}

// Omni appearance enums
enum class OmniPlayerBackgroundStyle {
    DYNAMIC_GRADIENT,
    SOLID_DARK,
}

enum class OmniPlayerDesignStyle {
    DEFAULT,
    COMPACT,
    IMMERSIVE,
}

enum class OmniMiniPlayerDesign {
    DEFAULT,
    COMPACT,
}

enum class OmniPlayerButtonColorMode {
    DYNAMIC,
    DEFAULT,
    MONOCHROME,
}

enum class OmniSliderStyle {
    DEFAULT,
    THIN,
    ROUNDED,
}

enum class OmniLyricsPresentation {
    DEFAULT,
    COMPACT,
    LARGE,
}

// Library settings
enum class OmniLibraryDesign {
    DEFAULT,
    COMPACT_LIST,
    CLASSIC,
    MODERN,
}


val ShowCachedPlaylistKey = booleanPreferencesKey("showCachedPlaylist")
val ShowDownloadedPlaylistKey = booleanPreferencesKey("showDownloadedPlaylist")
val ShowLikedPlaylistKey = booleanPreferencesKey("showLikedPlaylist")
val ShowTopPlaylistKey = booleanPreferencesKey("showTopPlaylist")


val ShowFloatingLyricsKey = booleanPreferencesKey("showFloatingLyrics")
val FloatingLyricsPositionKey = stringPreferencesKey("floatingLyricsPosition")
val FloatingLyricsOpacityKey = floatPreferencesKey("floatingLyricsOpacity")
val FloatingLyricsTextSizeKey = floatPreferencesKey("floatingLyricsTextSize")
val FloatingLyricsTextColorKey = stringPreferencesKey("floatingLyricsTextColor")
val FloatingLyricsBackgroundColorKey = stringPreferencesKey("floatingLyricsBackgroundColor")

// Queue layout customisation
val QueueHeaderStyleKey = stringPreferencesKey("queueHeaderStyle")
val QueueArtworkShapeKey = stringPreferencesKey("queueArtworkShape")
val QueueArtworkCornerRadiusKey = floatPreferencesKey("queueArtworkCornerRadius")
val QueueItemStyleKey = stringPreferencesKey("queueItemStyle")
val QueueItemVerticalPaddingKey = floatPreferencesKey("queueItemVerticalPadding")
val QueueBackgroundStyleKey = stringPreferencesKey("queueBackgroundStyle")
val QueueCustomBackgroundColorKey = stringPreferencesKey("queueCustomBackgroundColor")
val QueueGlassBlurAmountKey = floatPreferencesKey("queueGlassBlurAmount")
val QueueGlassAlphaKey = floatPreferencesKey("queueGlassAlpha")
val QueueShowSongDurationKey = booleanPreferencesKey("queueShowSongDuration")
val QueueShowDragHandleKey = booleanPreferencesKey("queueShowDragHandle")

// App rating / star prompt preferences
val LaunchCountKey = intPreferencesKey("launch_count")
val HasPressedStarKey = booleanPreferencesKey("has_pressed_star")
val RemindAfterKey = intPreferencesKey("remind_after")
val SupportDialogDismissedKey = booleanPreferencesKey("support_dialog_dismissed")
val SupportDialogSnoozedUntilKey = longPreferencesKey("support_dialog_snoozed_until")

// Player & Theme Design
enum class PlayerButtonsStyle {
    DEFAULT,
    SECONDARY,
}

enum class PlayerDesignStyle {
    V1,
    V2,
    V3,
    V4,
    V5,
}

enum class PlayerBackgroundStyle {
    DEFAULT,
    GRADIENT,
    CUSTOM,
    BLUR,
    COLORING,
    BLUR_GRADIENT,
    GLOW,
    GLOW_ANIMATED,
}

// Keys for customized background
val PlayerCustomImageUriKey = stringPreferencesKey("playerCustomImageUri")
val PlayerCustomBlurKey = floatPreferencesKey("playerCustomBlur")
val PlayerCustomContrastKey = floatPreferencesKey("playerCustomContrast")
val PlayerCustomBrightnessKey = floatPreferencesKey("playerCustomBrightness")

val LyricsAnimationStyleKey = stringPreferencesKey("lyricsAnimationStyle")
enum class LyricsAnimationStyle {
    NONE,
    FADE,
    GLOW,
    SLIDE,
    KARAOKE,
    APPLE,
}

val LyricsTextSizeKey = floatPreferencesKey("lyricsTextSize")
val LyricsLineSpacingKey = floatPreferencesKey("lyricsLineSpacing")

val TopSize = stringPreferencesKey("topSize")
val HistoryDuration = floatPreferencesKey("historyDuration")

val PlayerButtonsStyleKey = stringPreferencesKey("player_buttons_style")
val PlayerBackgroundStyleKey = stringPreferencesKey("playerBackgroundStyle")
val OmniPlayerBackgroundStyleKey = stringPreferencesKey("omniPlayerBackgroundStyle")
val OmniPlayerDesignStyleKey = stringPreferencesKey("omniPlayerDesignStyle")
val OmniMiniPlayerDesignKey = stringPreferencesKey("omniMiniPlayerDesign")
val OmniLibraryDesignKey = stringPreferencesKey("omniLibraryDesign")
val OmniPlayerButtonColorModeKey = stringPreferencesKey("omniPlayerButtonColorMode")
val OmniSliderStyleKey = stringPreferencesKey("omniSliderStyle")
val OmniLyricsPresentationKey = stringPreferencesKey("omniLyricsPresentation")
val ShowLyricsKey = booleanPreferencesKey("showLyrics")
val LyricsTextPositionKey = stringPreferencesKey("lyricsTextPosition")
val LyricsClickKey = booleanPreferencesKey("lyricsClick")
val LyricsScrollKey = booleanPreferencesKey("lyricsScrollKey")
val LyricsRomanizeJapaneseKey = booleanPreferencesKey("lyricsRomanizeJapanese")
val LyricsRomanizeKoreanKey = booleanPreferencesKey("lyricsRomanizeKorean")
val TranslateLyricsKey = booleanPreferencesKey("translateLyrics")
val UseLyricsV2Key = booleanPreferencesKey("useLyricsV2")

// Queue lyrics pre-load settings
val PreloadQueueLyricsEnabledKey = booleanPreferencesKey("preload_queue_lyrics_enabled")
val QueueLyricsPreloadCountKey = intPreferencesKey("queue_lyrics_preload_count")

val PlayerVolumeKey = floatPreferencesKey("playerVolume")
val RepeatModeKey = intPreferencesKey("repeatMode")
val ShuffleEnabledKey = booleanPreferencesKey("shuffleEnabled")

val SearchSourceKey = stringPreferencesKey("searchSource")
val SwipeThumbnailKey = booleanPreferencesKey("swipeThumbnail")
val SwipeSensitivityKey = floatPreferencesKey("swipeSensitivity")

enum class SearchSource {
    LOCAL,
    ONLINE,
    ;

    fun toggle() =
        when (this) {
            LOCAL -> ONLINE
            ONLINE -> LOCAL
        }
}

val VisitorDataKey = stringPreferencesKey("visitorData")
val DataSyncIdKey = stringPreferencesKey("dataSyncId")
val InnerTubeCookieKey = stringPreferencesKey("innerTubeCookie")
val PoTokenKey = stringPreferencesKey("poToken")
val AccountNameKey = stringPreferencesKey("accountName")
val AccountEmailKey = stringPreferencesKey("accountEmail")
val AccountChannelHandleKey = stringPreferencesKey("accountChannelHandle")
val UseLoginForBrowse = booleanPreferencesKey("useLoginForBrowse")

val WebClientPoTokenEnabledKey = booleanPreferencesKey("webClientPoTokenEnabled")
val PoTokenGvsKey = stringPreferencesKey("poTokenGvs")
val PoTokenPlayerKey = stringPreferencesKey("poTokenPlayer")
val UseVisitorDataKey = booleanPreferencesKey("useVisitorData")
val PoTokenSourceUrlKey = stringPreferencesKey("poTokenSourceUrl")

// Update settings
val EnableUpdateNotificationKey = booleanPreferencesKey("enableUpdateNotification")
val UpdateChannelKey = stringPreferencesKey("updateChannel")
val LastUpdateCheckKey = longPreferencesKey("lastUpdateCheck")
val LastNotifiedVersionKey = stringPreferencesKey("lastNotifiedVersion")

val GitHubContributorsEtagKey = stringPreferencesKey("github_contributors_etag")
val GitHubContributorsJsonKey = stringPreferencesKey("github_contributors_json")
val GitHubContributorsLastCheckedAtKey = longPreferencesKey("github_contributors_last_checked_at")
val GitHubReleasesEtagKey = stringPreferencesKey("github_releases_etag")

// Liquid Glass and Player customisation keys
val LiquidGlassEnabledKey = booleanPreferencesKey("liquidGlassEnabled")
val SeekbarStyleKey = stringPreferencesKey("seekbarStyle")
val ArtworkShapeKey = stringPreferencesKey("artworkShape")
val ArtworkSizeKey = stringPreferencesKey("artworkSize")
val MiniPlayerStyleKey = stringPreferencesKey("miniPlayerStyle")
val MiniPlayerAlphaKey = floatPreferencesKey("miniPlayerAlpha")
val MiniPlayerBlurKey = floatPreferencesKey("miniPlayerBlur")
val PlayerStyleKey = stringPreferencesKey("playerStyle")
val AppThemeKey = stringPreferencesKey("appTheme")
val ThemeModeKey = stringPreferencesKey("themeMode")
val LogoVariantKey = stringPreferencesKey("logoVariant")
val ForceMaxRefreshRateKey = booleanPreferencesKey("forceMaxRefreshRate")
val WifiAudioQualityKey = stringPreferencesKey("wifiAudioQuality")
val MobileAudioQualityKey = stringPreferencesKey("mobileAudioQuality")
val DoubleTapSeekSecondsKey = intPreferencesKey("doubleTapSeekSeconds")
val SwipeDownToDismissPlayerKey = booleanPreferencesKey("swipeDownToDismissPlayer")
val PlayerAnimatedBackgroundEnabledKey = booleanPreferencesKey("playerAnimatedBackgroundEnabled")
val AlbumArtDynamicColorsEnabledKey = booleanPreferencesKey("albumArtDynamicColorsEnabled")
val RotatingVinylAnimationEnabledKey = booleanPreferencesKey("rotatingVinylAnimationEnabled")
val NavBarAlphaKey = floatPreferencesKey("navBarAlpha")
val NavBarBlurKey = floatPreferencesKey("navBarBlur")
val VolumeSliderEnabledKey = booleanPreferencesKey("volumeSliderEnabled")
val LyricsAnimationTypeKey = stringPreferencesKey("lyricsAnimationType")
val PreferredLyricsProviderKey = stringPreferencesKey("preferredLyricsProvider")

enum class PreferredLyricsProvider {
    LRCLIB,
    KUGOU,
    BETTER_LYRICS,
    SIMPMUSIC,
}

enum class LyricsPosition {
    LEFT,
    CENTER,
    RIGHT,
}
val LyricsPositionKey = stringPreferencesKey("lyricsPosition")

val LyricsBlurKey = floatPreferencesKey("lyricsBlur")
val SponsorBlockEnabledKey = booleanPreferencesKey("sponsorBlockEnabled")
val LastFmUsernameKey = stringPreferencesKey("lastFmUsername")
val LastFmScrobblingEnabledKey = booleanPreferencesKey("lastFmScrobblingEnabled")
val VolumeBoostEnabledKey = booleanPreferencesKey("volumeBoostEnabled")
val VolumeBoostAmountKey = intPreferencesKey("volumeBoostAmount")
val AudioOffloadEnabledKey = booleanPreferencesKey("audioOffloadEnabled")
val GaplessPlaybackKey = booleanPreferencesKey("gaplessPlayback")
val AutomixKey = booleanPreferencesKey("automix")
val VolumeNormalizationKey = booleanPreferencesKey("volumeNormalization")
val CrossfadeMsKey = intPreferencesKey("crossfadeMs")
val CrossfeedEnabledKey = booleanPreferencesKey("crossfeedEnabled")
val NextSongPreloadingKey = booleanPreferencesKey("nextSongPreloading")

val EqualizerBassBoostEnabledKey = booleanPreferencesKey("equalizer_bass_boost_enabled")
val EqualizerBassBoostStrengthKey = intPreferencesKey("equalizer_bass_boost_strength")
val EqualizerVirtualizerEnabledKey = booleanPreferencesKey("equalizer_virtualizer_enabled")
val EqualizerVirtualizerStrengthKey = intPreferencesKey("equalizer_virtualizer_strength")
val EqualizerPreampLevelMbKey = intPreferencesKey("equalizer_preamp_level_mb")

val LanguageCodeToName =
    mapOf(
        "en" to "English (US)",
        "en-GB" to "English (UK)",
        "ja" to "日本語",
        "ko" to "한국어",
        "vi" to "Tiếng Việt",
        "zh" to "中文",
        "zh-CN" to "简体中文",
        "zh-TW" to "繁體中文",
        "fr" to "Français",
        "de" to "Deutsch",
        "es" to "Español",
        "pt" to "Português",
        "pt-BR" to "Português (Brasil)",
        "ru" to "Русский",
        "it" to "Italiano",
        "nl" to "Nederlands",
        "pl" to "Polski",
        "tr" to "Türkçe",
        "ar" to "العربية",
        "hi" to "हिन्दी",
        "th" to "ไทย",
        "id" to "Bahasa Indonesia",
        "ms" to "Bahasa Melayu",
        "uk" to "Українська",
        "cs" to "Čeština",
        "el" to "Ελληνικά",
        "he" to "עברית",
        "hu" to "Magyar",
        "ro" to "Română",
        "fi" to "Suomi",
        "da" to "Dansk",
        "no" to "Norsk",
        "sv" to "Svenska",
        "sk" to "Slovenčina",
        "bg" to "Български",
        "hr" to "Hrvatski",
        "sr" to "Срpsки",
        "lt" to "Lietuvių",
        "lv" to "Latviešu",
        "et" to "Eesti",
    )

val CountryCodeToName =
    mapOf(
        "JP" to "Japan",
        "KR" to "South Korea",
        "US" to "United States",
        "GB" to "United Kingdom",
        "CN" to "China",
        "TW" to "Taiwan",
        "HK" to "Hong Kong",
        "FR" to "France",
        "DE" to "Germany",
        "ES" to "Spain",
        "MX" to "Mexico",
        "BR" to "Brazil",
        "RU" to "Russia",
        "IT" to "Italy",
        "NL" to "Netherlands",
        "PL" to "Poland",
        "TR" to "Turkey",
        "AU" to "Australia",
        "CA" to "Canada",
        "IN" to "India",
        "ID" to "Indonesia",
        "TH" to "Thailand",
        "VN" to "Vietnam",
        "PH" to "Philippines",
        "MY" to "Malaysia",
        "SG" to "Singapore",
        "AR" to "Argentina",
        "CL" to "Chile",
        "CO" to "Colombia",
        "PE" to "Peru",
        "ZA" to "South Africa",
        "EG" to "Egypt",
        "SA" to "Saudi Arabia",
        "AE" to "United Arab Emirates",
    )

val EqualizerBandLevelsMbKey = stringPreferencesKey("equalizer_band_levels_mb")
val EqualizerEnabledKey = booleanPreferencesKey("equalizer_enabled")
val EqualizerSelectedProfileIdKey = stringPreferencesKey("equalizer_selected_profile_id")
val AIEqualizerAutoModeKey = booleanPreferencesKey("ai_equalizer_auto_mode")
val AIEqualizerPromptKey = stringPreferencesKey("ai_equalizer_prompt")

/*
 * This file was adapted from SuvMusic.
 * Original copyright follows:
 * 
 * Copyright (C) Suvojeet
 * Licensed under the GNU General Public License v3.0 (GPLv3)
 */

package com.omnitune.app.viewmodels

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omnitune.app.constants.*
import com.omnitune.app.models.*
import com.omnitune.app.utils.PreferenceStore
import com.omnitune.app.utils.LauncherIconSwitcher
import com.omnitune.app.utils.SecurePreferenceCipher
import com.omnitune.app.utils.SensitivePreferenceCodec
import com.omnitune.app.utils.dataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

private data class SensitiveSettings(
    val openaiSecret: String,
    val anthropicSecret: String,
    val geminiSecret: String,
)

data class SettingsUiState(
    val isLoggedIn: Boolean = false,
    val userName: String? = null,
    val userAvatarUrl: String? = null,
    val audioQuality: AudioQuality = AudioQuality.HIGH,
    val wifiAudioQuality: AudioQuality = AudioQuality.HIGH,
    val mobileAudioQuality: AudioQuality = AudioQuality.MEDIUM,
    val videoQuality: VideoQuality = VideoQuality.MEDIUM,
    val downloadQuality: AudioQuality = AudioQuality.HIGH,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val appTheme: AppTheme = AppTheme.DEFAULT,
    val logoVariant: LogoVariant = LogoVariant.DEFAULT,
    val dynamicColorEnabled: Boolean = true,
    val pureBlackEnabled: Boolean = false,
    val volumeNormalizationEnabled: Boolean = true,
    val volumeSliderEnabled: Boolean = true,
    val musicSource: MusicSource = MusicSource.YOUTUBE,
    val preferRemoteAudio: Boolean = true,
    val doubleTapSeekSeconds: Int = 10,
    val playerCacheLimit: Long = -1L,
    val playerCacheAutoClearInterval: Int = 5,
    val musicHapticsEnabled: Boolean = false,
    val hapticsMode: HapticsMode = HapticsMode.BASIC,
    val hapticsIntensity: HapticsIntensity = HapticsIntensity.MEDIUM,
    val stopMusicOnTaskClear: Boolean = false,
    val pictureInPictureEnabled: Boolean = true,
    val pauseMusicOnMediaMuted: Boolean = false,
    val keepScreenOn: Boolean = false,
    val swipeDownToDismissEnabled: Boolean = true,
    val playerAnimatedBackgroundEnabled: Boolean = true,
    val albumArtDynamicColorsEnabled: Boolean = true,
    val rotatingVinylAnimationEnabled: Boolean = true,
    val albumArtColorFlashingEnabled: Boolean = false,
    val forceMaxRefreshRateEnabled: Boolean = false,
    val preferredLyricsProvider: String = PreferredLyricsProvider.BETTER_LYRICS.name,
    val lyricsTextPosition: LyricsTextPosition = LyricsTextPosition.CENTER,
    val lyricsAnimationType: LyricsAnimationType = LyricsAnimationType.WORD,
    val lyricsLineSpacing: Float = 1.5f,
    val lyricsFontSize: Float = 26f,
    val lyricsBlur: Float = 2.5f,
    val audioOffloadEnabled: Boolean = false,
    val sponsorBlockEnabled: Boolean = true,
    val sponsorBlockCategories: Set<String> = setOf("sponsor", "selfpromo", "interaction", "intro", "outro", "preview", "music_offtopic"),
    val lastFmUsername: String? = null,
    val lastFmScrobblingEnabled: Boolean = false,
    val lastFmRecommendationsEnabled: Boolean = true,
    val lastFmUseNowPlaying: Boolean = true,
    val lastFmSendLikes: Boolean = false,
    val scrobbleDelayPercent: Float = 0.5f,
    val scrobbleMinDuration: Int = 30,
    val scrobbleDelaySeconds: Int = 180,
    val openaiSecret: String = "",
    val openaiModel: String = "gpt-4o",
    val anthropicSecret: String = "",
    val anthropicModel: String = "claude-3-5-sonnet-20240620",
    val geminiSecret: String = "",
    val geminiModel: String = "gemini-1.5-pro",
    val selectedAiProvider: String = "gemini",
    val nextSongPreloadingEnabled: Boolean = true,
    val nextSongPreloadDelay: Int = 10,
    val crossfadeMs: Int = 0,
    val eqEnabled: Boolean = false,
    val eqBands: FloatArray = floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f),
    val navBarAlpha: Float = 1.0f,
    val navBarBlur: Float = 60.0f,
    val iosLiquidGlassEnabled: Boolean = true,
    val miniPlayerAlpha: Float = 0.0f,
    val miniPlayerBlur: Float = 50.0f,
    val miniPlayerStyle: MiniPlayerStyle = MiniPlayerStyle.LIQUID_GLASS,
    val playerStyle: PlayerStyle = PlayerStyle.LIQUID_GLASS,
    val artworkShape: ArtworkShape = ArtworkShape.ROUNDED_SQUARE,
    val artworkSize: ArtworkSize = ArtworkSize.FULL,
    val seekbarStyle: SeekbarStyle = SeekbarStyle.M3E_WAVY
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: com.omnitune.app.db.MusicDatabase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            context.dataStore.data.collect { prefs ->
                val sensitiveSettings = readSensitiveSettings(prefs)
                _uiState.update { current ->
                    current.copy(
                        pureBlackEnabled = prefs[PureBlackKey] ?: false,
                        dynamicColorEnabled = prefs[DynamicThemeKey] ?: true,
                        themeMode = try {
                            ThemeMode.valueOf(prefs[ThemeModeKey] ?: ThemeMode.SYSTEM.name)
                        } catch (e: Exception) { ThemeMode.SYSTEM },
                        appTheme = try {
                            AppTheme.valueOf(prefs[AppThemeKey] ?: AppTheme.DEFAULT.name)
                        } catch (e: Exception) { AppTheme.DEFAULT },
                        logoVariant = try {
                            LogoVariant.valueOf(prefs[LogoVariantKey] ?: LogoVariant.DEFAULT.name)
                        } catch (e: Exception) { LogoVariant.DEFAULT },
                        playerStyle = try {
                            PlayerStyle.valueOf(prefs[PlayerStyleKey] ?: PlayerStyle.LIQUID_GLASS.name)
                        } catch (e: Exception) { PlayerStyle.LIQUID_GLASS },
                        miniPlayerStyle = try {
                            MiniPlayerStyle.valueOf(prefs[MiniPlayerStyleKey] ?: MiniPlayerStyle.LIQUID_GLASS.name)
                        } catch (e: Exception) { MiniPlayerStyle.LIQUID_GLASS },
                        artworkShape = try {
                            ArtworkShape.valueOf(prefs[ArtworkShapeKey] ?: ArtworkShape.ROUNDED_SQUARE.name)
                        } catch (e: Exception) { ArtworkShape.ROUNDED_SQUARE },
                        artworkSize = try {
                            ArtworkSize.valueOf(prefs[ArtworkSizeKey] ?: ArtworkSize.FULL.name)
                        } catch (e: Exception) { ArtworkSize.FULL },
                        seekbarStyle = try {
                            SeekbarStyle.valueOf(prefs[SeekbarStyleKey] ?: SeekbarStyle.M3E_WAVY.name)
                        } catch (e: Exception) { SeekbarStyle.M3E_WAVY },
                        iosLiquidGlassEnabled = prefs[LiquidGlassEnabledKey] ?: true,
                        forceMaxRefreshRateEnabled = prefs[ForceMaxRefreshRateKey] ?: false,
                        swipeDownToDismissEnabled = prefs[SwipeDownToDismissPlayerKey] ?: true,
                        playerAnimatedBackgroundEnabled = prefs[PlayerAnimatedBackgroundEnabledKey] ?: true,
                        albumArtDynamicColorsEnabled = prefs[AlbumArtDynamicColorsEnabledKey] ?: true,
                        rotatingVinylAnimationEnabled = prefs[RotatingVinylAnimationEnabledKey] ?: true,
                        navBarAlpha = prefs[NavBarAlphaKey] ?: 1.0f,
                        navBarBlur = prefs[NavBarBlurKey] ?: 60.0f,
                        miniPlayerAlpha = prefs[MiniPlayerAlphaKey] ?: 0.0f,
                        miniPlayerBlur = prefs[MiniPlayerBlurKey] ?: 50.0f,
                        lyricsAnimationType = try {
                            LyricsAnimationType.valueOf(prefs[LyricsAnimationTypeKey] ?: LyricsAnimationType.WORD.name)
                        } catch (e: Exception) { LyricsAnimationType.WORD },
                        lyricsTextPosition = try {
                            LyricsTextPosition.valueOf(prefs[LyricsTextPositionKey] ?: LyricsTextPosition.CENTER.name)
                        } catch (e: Exception) { LyricsTextPosition.CENTER },
                        lyricsBlur = prefs[LyricsBlurKey] ?: 2.5f,
                        preferredLyricsProvider = prefs[PreferredLyricsProviderKey] ?: PreferredLyricsProvider.BETTER_LYRICS.name,
                        sponsorBlockEnabled = prefs[SponsorBlockEnabledKey] ?: true,
                        lastFmUsername = prefs[LastFmUsernameKey],
                        lastFmScrobblingEnabled = prefs[LastFmScrobblingEnabledKey] ?: false,
                        openaiSecret = sensitiveSettings.openaiSecret,
                        openaiModel = prefs[OpenaiModelKey] ?: "gpt-4o",
                        anthropicSecret = sensitiveSettings.anthropicSecret,
                        anthropicModel = prefs[AnthropicModelKey] ?: "claude-3-5-sonnet-20240620",
                        geminiSecret = sensitiveSettings.geminiSecret,
                        geminiModel = prefs[GeminiModelKey] ?: "gemini-1.5-pro",
                        selectedAiProvider = prefs[SelectedAiProviderKey] ?: "gemini",
                        audioOffloadEnabled = prefs[AudioOffloadEnabledKey] ?: prefs[AudioOffload] ?: false,
                        wifiAudioQuality = prefs[WifiAudioQualityKey]
                            ?.let { runCatching { AudioQuality.valueOf(it) }.getOrNull() }
                            ?: prefs[AudioQualityKey]
                                ?.let { runCatching { AudioQuality.valueOf(it) }.getOrNull() }
                            ?: AudioQuality.HIGH,
                        mobileAudioQuality = prefs[MobileAudioQualityKey]
                            ?.let { runCatching { AudioQuality.valueOf(it) }.getOrNull() }
                            ?: prefs[AudioQualityKey]
                                ?.let { runCatching { AudioQuality.valueOf(it) }.getOrNull() }
                            ?: AudioQuality.MEDIUM,
                        downloadQuality = prefs[DownloadQualityKey]
                            ?.let { runCatching { AudioQuality.valueOf(it) }.getOrNull() }
                            ?: AudioQuality.HIGH,
                        doubleTapSeekSeconds = prefs[DoubleTapSeekSecondsKey] ?: 10,
                        volumeNormalizationEnabled = prefs[VolumeNormalizationKey] ?: prefs[AudioNormalizationKey] ?: true,
                        volumeSliderEnabled = prefs[VolumeSliderEnabledKey] ?: true,
                        pauseMusicOnMediaMuted = prefs[PauseOnDeviceMuteKey] ?: false,
                        pictureInPictureEnabled = prefs[PictureInPictureEnabledKey] ?: true,
                        keepScreenOn = prefs[KeepScreenOnKey] ?: false,
                        stopMusicOnTaskClear = prefs[StopMusicOnTaskClearKey] ?: false,
                        crossfadeMs = prefs[CrossfadeMsKey]
                            ?: prefs[AudioCrossfadeDurationKey]?.times(1_000)
                            ?: 0,
                        nextSongPreloadingEnabled = prefs[NextSongPreloadingKey] ?: true,
                        playerCacheLimit = prefs[PlayerCacheLimitKey] ?: -1L
                    )
                }
            }
        }
    }

    private suspend fun readSensitiveSettings(prefs: Preferences): SensitiveSettings =
        SensitiveSettings(
            openaiSecret = readSensitivePreference(prefs, OpenaiApiKey),
            anthropicSecret = readSensitivePreference(prefs, AnthropicApiKey),
            geminiSecret = readSensitivePreference(prefs, GeminiApiKey),
        )

    private suspend fun readSensitivePreference(
        prefs: Preferences,
        key: Preferences.Key<String>,
    ): String {
        val decoded = decodeSensitivePreference(prefs[key])
        decoded.migratedStorageValue?.let { migratedValue ->
            context.dataStore.edit { settings ->
                settings[key] = migratedValue
            }
        }
        return decoded.plainValue
    }

    private fun decodeSensitivePreference(rawValue: String?) =
        SensitivePreferenceCodec.decodeForRead(
            rawValue = rawValue,
            isEncrypted = SecurePreferenceCipher::isEncrypted,
            decryptOrPlain = SecurePreferenceCipher::decryptOrPlain,
            encrypt = SecurePreferenceCipher::encrypt,
        )

    private fun <T> setPreference(key: Preferences.Key<T>, value: T) {
        PreferenceStore.launchEdit(context.dataStore) {
            this[key] = value
        }
    }

    private fun setSensitivePreference(key: Preferences.Key<String>, value: String) {
        PreferenceStore.launchEdit(context.dataStore) {
            val encryptedValue = SensitivePreferenceCodec.encodeForStorage(
                plainValue = value,
                encrypt = SecurePreferenceCipher::encrypt,
            )
            if (encryptedValue == null) {
                remove(key)
            } else {
                this[key] = encryptedValue
            }
        }
    }

    fun setThemeMode(mode: ThemeMode) = setPreference(ThemeModeKey, mode.name)
    fun setPureBlackEnabled(enabled: Boolean) = setPreference(PureBlackKey, enabled)
    fun setDynamicColor(enabled: Boolean) = setPreference(DynamicThemeKey, enabled)
    fun setAppTheme(theme: AppTheme) = setPreference(AppThemeKey, theme.name)
    fun setLogoVariant(variant: LogoVariant) {
        viewModelScope.launch {
            withContext(Dispatchers.Default) {
                LauncherIconSwitcher(context).apply(variant)
            }
            setPreference(LogoVariantKey, variant.name)
        }
    }
    fun setPlayerStyle(style: PlayerStyle) = setPreference(PlayerStyleKey, style.name)
    fun setMiniPlayerStyle(style: MiniPlayerStyle) = setPreference(MiniPlayerStyleKey, style.name)
    fun setArtworkShape(shape: ArtworkShape) = setPreference(ArtworkShapeKey, shape.name)
    fun setArtworkSize(size: ArtworkSize) = setPreference(ArtworkSizeKey, size.name)
    fun setSeekbarStyle(style: SeekbarStyle) = setPreference(SeekbarStyleKey, style.name)
    fun setIosLiquidGlassEnabled(enabled: Boolean) = setPreference(LiquidGlassEnabledKey, enabled)
    fun setForceMaxRefreshRate(enabled: Boolean) = setPreference(ForceMaxRefreshRateKey, enabled)
    fun setSwipeDownToDismissEnabled(enabled: Boolean) = setPreference(SwipeDownToDismissPlayerKey, enabled)
    fun setNavBarAlpha(alpha: Float) = setPreference(NavBarAlphaKey, alpha.coerceIn(0f, 1f))
    fun setNavBarBlur(blur: Float) = setPreference(NavBarBlurKey, blur.coerceIn(0f, 100f))
    fun setMiniPlayerAlpha(alpha: Float) = setPreference(MiniPlayerAlphaKey, alpha.coerceIn(0f, 1f))
    fun setMiniPlayerBlur(blur: Float) = setPreference(MiniPlayerBlurKey, blur.coerceIn(0f, 100f))
    fun setLyricsTextPosition(position: LyricsTextPosition) = setPreference(LyricsTextPositionKey, position.name)
    fun setLyricsAnimationType(type: LyricsAnimationType) = setPreference(LyricsAnimationTypeKey, type.name)
    fun setLyricsBlur(blur: Float) = setPreference(LyricsBlurKey, blur)
    fun setPreferredLyricsProvider(provider: String) = setPreference(PreferredLyricsProviderKey, provider)
    fun setSponsorBlockEnabled(enabled: Boolean) = setPreference(SponsorBlockEnabledKey, enabled)
    fun setLastFmScrobblingEnabled(enabled: Boolean) = setPreference(LastFmScrobblingEnabledKey, enabled)
    fun setLastFmUsername(username: String) = setPreference(LastFmUsernameKey, username)
    fun setOpenaiSecret(secret: String) = setSensitivePreference(OpenaiApiKey, secret)
    fun setOpenaiModel(model: String) = setPreference(OpenaiModelKey, model)
    fun setAnthropicSecret(secret: String) = setSensitivePreference(AnthropicApiKey, secret)
    fun setAnthropicModel(model: String) = setPreference(AnthropicModelKey, model)
    fun setGeminiSecret(secret: String) = setSensitivePreference(GeminiApiKey, secret)
    fun setGeminiModel(model: String) = setPreference(GeminiModelKey, model)
    fun setSelectedAiProvider(provider: String) = setPreference(SelectedAiProviderKey, provider)
    fun setAudioOffloadEnabled(enabled: Boolean) = setPreference(AudioOffloadEnabledKey, enabled)
    fun setPauseMusicOnMediaMuted(enabled: Boolean) = setPreference(PauseOnDeviceMuteKey, enabled)
    fun setPictureInPictureEnabled(enabled: Boolean) = setPreference(PictureInPictureEnabledKey, enabled)
    fun setKeepScreenOn(enabled: Boolean) = setPreference(KeepScreenOnKey, enabled)
    fun setStopMusicOnTaskClear(enabled: Boolean) = setPreference(StopMusicOnTaskClearKey, enabled)
    fun setVolumeNormalizationEnabled(enabled: Boolean) = setPreference(VolumeNormalizationKey, enabled)
    fun setVolumeSliderEnabled(enabled: Boolean) = setPreference(VolumeSliderEnabledKey, enabled)
    fun setCrossfadeMs(ms: Int) = setPreference(CrossfadeMsKey, ms)
    fun setNextSongPreloadingEnabled(enabled: Boolean) = setPreference(NextSongPreloadingKey, enabled)
    fun setPlayerCacheLimit(limit: Long) = setPreference(PlayerCacheLimitKey, limit)

    fun setWifiAudioQuality(quality: AudioQuality) {
        setPreference(WifiAudioQualityKey, quality.name)
    }

    fun setMobileAudioQuality(quality: AudioQuality) {
        setPreference(MobileAudioQualityKey, quality.name)
    }

    fun setDownloadQuality(quality: AudioQuality) {
        setPreference(DownloadQualityKey, quality.name)
    }

    fun setDoubleTapSeekSeconds(seconds: Int) {
        setPreference(DoubleTapSeekSecondsKey, seconds)
    }

    fun setPlayerAnimatedBackgroundEnabled(enabled: Boolean) {
        setPreference(PlayerAnimatedBackgroundEnabledKey, enabled)
    }

    fun setAlbumArtDynamicColorsEnabled(enabled: Boolean) {
        setPreference(AlbumArtDynamicColorsEnabledKey, enabled)
    }

    fun setRotatingVinylAnimationEnabled(enabled: Boolean) {
        setPreference(RotatingVinylAnimationEnabledKey, enabled)
    }


    fun <T> updatePreference(ctx: Context, key: Preferences.Key<T>, value: T) {
        viewModelScope.launch(Dispatchers.IO) {
            ctx.dataStore.edit { it[key] = value }
        }
    }

    fun clearSearchHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            database.clearSearchHistory()
        }
    }

    fun clearListenHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            database.clearListenHistory()
        }
    }

    fun clearAppCache(ctx: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                ctx.cacheDir.deleteRecursively()
            } catch (e: Exception) {}
        }
    }

    fun clearArtworkCache(ctx: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                File(ctx.cacheDir, "image_cache").deleteRecursively()
            } catch (e: Exception) {}
        }
    }
}

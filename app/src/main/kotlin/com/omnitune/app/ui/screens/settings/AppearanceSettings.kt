package com.omnitune.app.ui.screens.settings

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import com.omnitune.app.R
import com.omnitune.app.constants.DynamicSongColorsKey
import com.omnitune.app.constants.HidePlayerThumbnailKey
import com.omnitune.app.constants.OmniLyricsPresentation
import com.omnitune.app.constants.OmniLyricsPresentationKey
import com.omnitune.app.constants.OmniLibraryDesign
import com.omnitune.app.constants.OmniLibraryDesignKey
import com.omnitune.app.constants.OmniMiniPlayerDesign
import com.omnitune.app.constants.OmniMiniPlayerDesignKey
import com.omnitune.app.constants.OmniPlayerBackgroundStyle
import com.omnitune.app.constants.OmniPlayerBackgroundStyleKey
import com.omnitune.app.constants.OmniPlayerButtonColorMode
import com.omnitune.app.constants.OmniPlayerButtonColorModeKey
import com.omnitune.app.constants.OmniPlayerDesignStyle
import com.omnitune.app.constants.OmniPlayerDesignStyleKey
import com.omnitune.app.constants.OmniSliderStyle
import com.omnitune.app.constants.OmniSliderStyleKey
import com.omnitune.app.constants.PureBlackKey
import com.omnitune.app.constants.ShowCachedPlaylistKey
import com.omnitune.app.constants.ShowDownloadedPlaylistKey
import com.omnitune.app.constants.ShowLikedPlaylistKey
import com.omnitune.app.constants.ShowTagsInLibraryKey
import com.omnitune.app.constants.ShowTopPlaylistKey
import com.omnitune.app.constants.SwipeSensitivityKey
import com.omnitune.app.constants.UseSystemFontKey
import com.omnitune.app.ui.theme.OmniColors
import com.omnitune.app.utils.dataStore
import com.omnitune.app.utils.rememberEnumPreference
import com.omnitune.app.utils.rememberPreference
import kotlinx.coroutines.launch

@Composable
fun AppearanceSettings() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var dynamicSongColors by rememberPreference(DynamicSongColorsKey, true)
    var playerBackgroundStyle by rememberEnumPreference(
        OmniPlayerBackgroundStyleKey,
        OmniPlayerBackgroundStyle.DYNAMIC_GRADIENT,
    )
    var pureBlack by rememberPreference(PureBlackKey, false)
    var useSystemFont by rememberPreference(UseSystemFontKey, false)
    var playerDesignStyle by rememberEnumPreference(
        OmniPlayerDesignStyleKey,
        OmniPlayerDesignStyle.DEFAULT,
    )
    var hidePlayerThumbnail by rememberPreference(HidePlayerThumbnailKey, false)
    var buttonColorMode by rememberEnumPreference(
        OmniPlayerButtonColorModeKey,
        OmniPlayerButtonColorMode.DYNAMIC,
    )
    var sliderStyle by rememberEnumPreference(
        OmniSliderStyleKey,
        OmniSliderStyle.DEFAULT,
    )
    var miniPlayerDesign by rememberEnumPreference(
        OmniMiniPlayerDesignKey,
        OmniMiniPlayerDesign.DEFAULT,
    )
    var swipeSensitivity by rememberPreference(SwipeSensitivityKey, 0.73f)
    var lyricsPresentation by rememberEnumPreference(
        OmniLyricsPresentationKey,
        OmniLyricsPresentation.DEFAULT,
    )
    var showLikedPlaylist by rememberPreference(ShowLikedPlaylistKey, true)
    var showDownloadedPlaylist by rememberPreference(ShowDownloadedPlaylistKey, true)
    var showTopPlaylist by rememberPreference(ShowTopPlaylistKey, true)
    var showCachedPlaylist by rememberPreference(ShowCachedPlaylistKey, true)
    var showPlaylistFolders by rememberPreference(ShowTagsInLibraryKey, true)
    var libraryDesign by rememberEnumPreference(
        OmniLibraryDesignKey,
        OmniLibraryDesign.DEFAULT,
    )

    OmniPreferenceCard(title = "APPEARANCE") {
        OmniSwitchPreference(
            title = "Dynamic song colors",
            description = "Adapt the app shell, player, mini player, and now-playing surfaces to the current artwork.",
            iconRes = R.drawable.ic_album,
            accent = OmniColors.OmniAccentSecondary,
            checked = dynamicSongColors,
            onCheckedChange = { dynamicSongColors = it },
        )
        OmniEnumPreference(
            title = "Player background",
            description = "Choose how the full player background is rendered.",
            iconRes = R.drawable.ic_settings,
            accent = OmniColors.OmniAccentSecondary,
            selectedValue = playerBackgroundStyle,
            values = OmniPlayerBackgroundStyle.entries,
            valueText = { it.label },
            onValueSelected = { playerBackgroundStyle = it },
        )
        OmniSwitchPreference(
            title = "Pure black mode",
            description = "Use deeper dark surfaces for OLED displays while preserving readable text and accents.",
            iconRes = R.drawable.ic_bedtime,
            accent = OmniColors.OmniAccentSecondary,
            checked = pureBlack,
            onCheckedChange = { pureBlack = it },
        )
        OmniSwitchPreference(
            title = "Use system font",
            description = "Use the device font instead of OmniTune's bundled typeface.",
            iconRes = R.drawable.ic_settings,
            accent = OmniColors.OmniAccentSecondary,
            checked = useSystemFont,
            onCheckedChange = { useSystemFont = it },
        )
    }

    Spacer(Modifier.height(12.dp))

    OmniPreferenceCard(title = "PLAYER") {
        OmniEnumPreference(
            title = "Player design style",
            description = "Switch between balanced, compact, and immersive full-player layouts.",
            iconRes = R.drawable.ic_play_arrow,
            accent = OmniColors.OmniAccentPrimary,
            selectedValue = playerDesignStyle,
            values = OmniPlayerDesignStyle.entries,
            valueText = { it.label },
            onValueSelected = { playerDesignStyle = it },
        )
        OmniSwitchPreference(
            title = "Hide player thumbnail",
            description = "Hide the large artwork card and rebalance the full player around metadata and controls.",
            iconRes = R.drawable.ic_album,
            accent = OmniColors.OmniAccentPrimary,
            checked = hidePlayerThumbnail,
            onCheckedChange = { hidePlayerThumbnail = it },
        )
        OmniEnumPreference(
            title = "Player button colors",
            description = "Choose dynamic, default, or monochrome playback control colors.",
            iconRes = R.drawable.ic_settings,
            accent = OmniColors.OmniAccentPrimary,
            selectedValue = buttonColorMode,
            values = OmniPlayerButtonColorMode.entries,
            valueText = { it.label },
            onValueSelected = { buttonColorMode = it },
        )
        OmniEnumPreference(
            title = "Player slider style",
            description = "Change the full player progress slider and mini player progress indicator.",
            iconRes = R.drawable.ic_settings,
            accent = OmniColors.OmniAccentPrimary,
            selectedValue = sliderStyle,
            values = OmniSliderStyle.entries,
            valueText = { it.label },
            onValueSelected = { sliderStyle = it },
        )
    }

    Spacer(Modifier.height(12.dp))

    OmniPreferenceCard(title = "MINI PLAYER") {
        OmniEnumPreference(
            title = "Mini player design",
            description = "Choose the normal dock or a denser compact dock treatment.",
            iconRes = R.drawable.ic_play_arrow,
            accent = OmniColors.OmniAccentWarm,
            selectedValue = miniPlayerDesign,
            values = OmniMiniPlayerDesign.entries,
            valueText = { it.label },
            onValueSelected = { miniPlayerDesign = it },
        )
        FloatPreferenceSliderRow(
            label = "Mini player swipe sensitivity",
            description = "Adjust the swipe distance and velocity needed to skip tracks from the mini player.",
            value = swipeSensitivity,
            onValueChange = { swipeSensitivity = it },
            valueRange = 0.30f..1.00f,
            steps = 6,
            valueFormat = { value -> "${(value * 100).toInt()}%" },
        )
    }

    Spacer(Modifier.height(12.dp))

    OmniPreferenceCard(title = "LIBRARY & HOME") {
        OmniEnumPreference(
            title = "Library design",
            description = "Choose the standard library hub or a denser compact list layout.",
            iconRes = R.drawable.ic_list,
            accent = OmniColors.OmniAccentWarm,
            selectedValue = libraryDesign,
            values = OmniLibraryDesign.entries,
            valueText = { it.label },
            onValueSelected = { libraryDesign = it },
        )
        OmniSwitchPreference(
            title = "Show liked shortcut",
            description = "Show or hide the Liked shortcut in supported library surfaces.",
            iconRes = R.drawable.ic_favorite,
            accent = OmniColors.Hot,
            checked = showLikedPlaylist,
            onCheckedChange = { showLikedPlaylist = it },
        )
        OmniSwitchPreference(
            title = "Show downloads shortcut",
            description = "Show or hide the Downloads shortcut in supported library surfaces.",
            iconRes = R.drawable.ic_download,
            accent = OmniColors.Downloaded,
            checked = showDownloadedPlaylist,
            onCheckedChange = { showDownloadedPlaylist = it },
        )
        OmniSwitchPreference(
            title = "Show top playlist shortcut",
            description = "Show or hide the generated top playlist shortcut where that library surface is available.",
            iconRes = R.drawable.ic_history,
            accent = OmniColors.OmniAccentSecondary,
            checked = showTopPlaylist,
            onCheckedChange = { showTopPlaylist = it },
        )
        OmniSwitchPreference(
            title = "Show cached playlist shortcut",
            description = "Show or hide the cached playlist shortcut where that library surface is available.",
            iconRes = R.drawable.ic_download,
            accent = OmniColors.OmniAccentTertiary,
            checked = showCachedPlaylist,
            onCheckedChange = { showCachedPlaylist = it },
        )
        OmniSwitchPreference(
            title = "Show playlist folders",
            description = "Show folder chips on the playlists screen.",
            iconRes = R.drawable.ic_list,
            accent = OmniColors.OmniAccentWarm,
            checked = showPlaylistFolders,
            onCheckedChange = { showPlaylistFolders = it },
        )
    }

    Spacer(Modifier.height(12.dp))

    OmniPreferenceCard(title = "LYRICS") {
        OmniEnumPreference(
            title = "Lyrics presentation",
            description = "Change lyrics size and spacing in the synced lyrics view.",
            iconRes = R.drawable.ic_lyrics,
            accent = OmniColors.OmniAccentSecondary,
            selectedValue = lyricsPresentation,
            values = OmniLyricsPresentation.entries,
            valueText = { it.label },
            onValueSelected = { lyricsPresentation = it },
        )
    }

    Spacer(Modifier.height(12.dp))

    OmniPreferenceCard(title = "ADVANCED") {
        OmniPreferenceEntry(
            title = "Reset appearance settings",
            description = "Restore Appearance defaults without changing playback, downloads, queue, or account settings.",
            iconRes = R.drawable.ic_settings,
            accent = OmniColors.Error,
            onClick = {
                scope.launch {
                    context.dataStore.edit { prefs ->
                        prefs.remove(DynamicSongColorsKey)
                        prefs.remove(OmniPlayerBackgroundStyleKey)
                        prefs.remove(PureBlackKey)
                        prefs.remove(UseSystemFontKey)
                        prefs.remove(OmniPlayerDesignStyleKey)
                        prefs.remove(HidePlayerThumbnailKey)
                        prefs.remove(OmniPlayerButtonColorModeKey)
                        prefs.remove(OmniSliderStyleKey)
                        prefs.remove(OmniMiniPlayerDesignKey)
                        prefs.remove(OmniLibraryDesignKey)
                        prefs.remove(SwipeSensitivityKey)
                        prefs.remove(ShowLikedPlaylistKey)
                        prefs.remove(ShowDownloadedPlaylistKey)
                        prefs.remove(ShowTopPlaylistKey)
                        prefs.remove(ShowCachedPlaylistKey)
                        prefs.remove(ShowTagsInLibraryKey)
                        prefs.remove(OmniLyricsPresentationKey)
                    }
                }
            },
        )
    }
}

private val OmniPlayerBackgroundStyle.label: String
    get() = when (this) {
        OmniPlayerBackgroundStyle.DYNAMIC_GRADIENT -> "Dynamic gradient"
        OmniPlayerBackgroundStyle.SOLID_DARK -> "Solid dark"
    }

private val OmniPlayerDesignStyle.label: String
    get() = when (this) {
        OmniPlayerDesignStyle.DEFAULT -> "Default"
        OmniPlayerDesignStyle.COMPACT -> "Compact"
        OmniPlayerDesignStyle.IMMERSIVE -> "Immersive"
    }

private val OmniMiniPlayerDesign.label: String
    get() = when (this) {
        OmniMiniPlayerDesign.DEFAULT -> "Default"
        OmniMiniPlayerDesign.COMPACT -> "Compact"
    }

private val OmniLibraryDesign.label: String
    get() = when (this) {
        OmniLibraryDesign.DEFAULT -> "Default"
        OmniLibraryDesign.COMPACT_LIST -> "Compact list"
    }

private val OmniPlayerButtonColorMode.label: String
    get() = when (this) {
        OmniPlayerButtonColorMode.DYNAMIC -> "Dynamic"
        OmniPlayerButtonColorMode.DEFAULT -> "Default"
        OmniPlayerButtonColorMode.MONOCHROME -> "Monochrome"
    }

private val OmniSliderStyle.label: String
    get() = when (this) {
        OmniSliderStyle.DEFAULT -> "Default"
        OmniSliderStyle.THIN -> "Thin"
        OmniSliderStyle.ROUNDED -> "Rounded"
    }

private val OmniLyricsPresentation.label: String
    get() = when (this) {
        OmniLyricsPresentation.DEFAULT -> "Default"
        OmniLyricsPresentation.COMPACT -> "Compact"
        OmniLyricsPresentation.LARGE -> "Large"
    }

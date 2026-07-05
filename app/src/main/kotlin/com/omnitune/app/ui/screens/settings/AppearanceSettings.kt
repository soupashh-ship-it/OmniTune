package com.omnitune.app.ui.screens.settings

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.omnitune.app.R
import com.omnitune.app.constants.*
import com.omnitune.app.ui.theme.OmniColors
import com.omnitune.app.utils.rememberPreference
import com.omnitune.app.ui.screens.settings.OmniPreferenceCard
import com.omnitune.app.ui.screens.settings.OmniSwitchPreference
import com.omnitune.app.ui.screens.settings.OmniEnumPreference
import com.omnitune.app.ui.screens.settings.FloatPreferenceSliderRow
import com.omnitune.app.ui.screens.settings.OmniPreferenceEntry

@Composable
fun AppearanceSettings(
    onNavigateToThemeCreator: () -> Unit = {},
    onNavigateToCustomizeBackground: () -> Unit = {}
) {
    // Theme
    var dynamicTheme by rememberPreference(DynamicThemeKey, true)
    var pureBlack by rememberPreference(PureBlackKey, false)
    var useSystemFont by rememberPreference(UseSystemFontKey, false)

    // Player
    var playerDesignStyle by rememberPreference(PlayerDesignStyleKey, com.omnitune.app.constants.PlayerDesignStyle.V1.name)
    var newMiniPlayer by rememberPreference(UseNewMiniPlayerDesignKey, true)
    var newLibrary by rememberPreference(UseNewLibraryDesignKey, false)
    var playerBackgroundStyle by rememberPreference(PlayerBackgroundStyleKey, com.omnitune.app.constants.PlayerBackgroundStyle.DEFAULT.name)
    var hidePlayerThumbnail by rememberPreference(HidePlayerThumbnailKey, false)
    var veluneCanvas by rememberPreference(OmniTuneCanvasKey, false)
    var cropThumbnailToSquare by rememberPreference(CropThumbnailToSquareKey, false)
    var playerButtonColors by rememberPreference(PlayerButtonsStyleKey, com.omnitune.app.constants.PlayerButtonsStyle.DEFAULT.name)
    var playerSliderStyle by rememberPreference(SliderStyleKey, com.omnitune.app.constants.SliderStyle.Standard.name)
    var enableSwipeToSong by rememberPreference(SwipeToSongKey, true)
    var swipeSensitivity by rememberPreference(SwipeSensitivityKey, 0.73f)

    // Lyrics
    var lyricsV2 by rememberPreference(UseLyricsV2Key, false)
    var lyricsTextPosition by rememberPreference(LyricsTextPositionKey, com.omnitune.app.constants.LyricsPosition.LEFT.name)
    var lyricsAnimationStyle by rememberPreference(LyricsAnimationStyleKey, com.omnitune.app.constants.LyricsAnimationStyle.FADE.name)
    var lyricsClick by rememberPreference(LyricsClickKey, true)
    var autoScrollLyrics by rememberPreference(LyricsScrollKey, true)
    var lyricsTextSize by rememberPreference(LyricsTextSizeKey, 26f)
    var lyricsLineSpacing by rememberPreference(LyricsLineSpacingKey, 1.3f)

    // Misc
    var defaultOpenTab by rememberPreference(DefaultOpenTabKey, "Home")
    var showHomeCategoryChips by rememberPreference(ShowHomeCategoryChipsKey, true)
    var showTagsInLibrary by rememberPreference(ShowTagsInLibraryKey, true)
    var slimBottomNav by rememberPreference(SlimNavBarKey, false)

    // Auto Playlists
    var showLikedPlaylist by rememberPreference(ShowLikedPlaylistKey, true)
    var showDownloadedPlaylist by rememberPreference(ShowDownloadedPlaylistKey, true)

    OmniPreferenceCard(title = "THEME") {
        OmniSwitchPreference(
            title = "Enable dynamic theme",
            iconRes = R.drawable.ic_settings,
            checked = dynamicTheme,
            onCheckedChange = { dynamicTheme = it }
        )
        OmniSwitchPreference(
            title = "Pure black",
            iconRes = R.drawable.ic_bedtime,
            checked = pureBlack,
            onCheckedChange = { pureBlack = it }
        )
        OmniSwitchPreference(
            title = "Use system font",
            description = "Use the device font instead of the app font",
            iconRes = R.drawable.ic_settings,
            checked = useSystemFont,
            onCheckedChange = { useSystemFont = it }
        )
    }

    Spacer(Modifier.height(12.dp))

    OmniPreferenceCard(title = "PLAYER") {
        OmniEnumPreference(
            title = "Player design style",
            iconRes = R.drawable.ic_settings,
            selectedValue = com.omnitune.app.constants.PlayerDesignStyle.valueOf(playerDesignStyle),
            values = com.omnitune.app.constants.PlayerDesignStyle.entries,
            valueText = { it.name },
            onValueSelected = { playerDesignStyle = it.name }
        )
        OmniSwitchPreference(
            title = "New mini player design",
            iconRes = R.drawable.ic_settings,
            checked = newMiniPlayer,
            onCheckedChange = { newMiniPlayer = it }
        )
        OmniSwitchPreference(
            title = "New library design",
            description = "Enable the new library design",
            iconRes = R.drawable.ic_list,
            checked = newLibrary,
            onCheckedChange = { newLibrary = it }
        )
        OmniEnumPreference(
            title = "Player background style",
            iconRes = R.drawable.ic_settings,
            selectedValue = com.omnitune.app.constants.PlayerBackgroundStyle.valueOf(playerBackgroundStyle),
            values = com.omnitune.app.constants.PlayerBackgroundStyle.entries,
            valueText = { it.name },
            onValueSelected = { playerBackgroundStyle = it.name }
        )
        OmniSwitchPreference(
            title = "Hide Player Thumbnail",
            description = "Replace album artwork with app logo in player",
            iconRes = R.drawable.ic_settings,
            checked = hidePlayerThumbnail,
            onCheckedChange = { hidePlayerThumbnail = it }
        )
        OmniSwitchPreference(
            title = "OmniTune Canvas",
            description = "Animate album artwork while playing",
            iconRes = R.drawable.ic_settings,
            checked = veluneCanvas,
            onCheckedChange = { veluneCanvas = it }
        )
        OmniSwitchPreference(
            title = "Crop thumbnails to 1:1",
            description = "Display YouTube thumbnails in square format",
            iconRes = R.drawable.ic_settings,
            checked = cropThumbnailToSquare,
            onCheckedChange = { cropThumbnailToSquare = it }
        )
        OmniEnumPreference(
            title = "Player button colors",
            iconRes = R.drawable.ic_settings,
            selectedValue = com.omnitune.app.constants.PlayerButtonsStyle.valueOf(playerButtonColors),
            values = com.omnitune.app.constants.PlayerButtonsStyle.entries,
            valueText = { it.name },
            onValueSelected = { playerButtonColors = it.name }
        )
        OmniEnumPreference(
            title = "Player slider style",
            iconRes = R.drawable.ic_settings,
            selectedValue = com.omnitune.app.constants.SliderStyle.valueOf(playerSliderStyle),
            values = com.omnitune.app.constants.SliderStyle.entries,
            valueText = { it.name },
            onValueSelected = { playerSliderStyle = it.name }
        )
        OmniSwitchPreference(
            title = "Enable swipe to change song",
            iconRes = R.drawable.ic_settings,
            checked = enableSwipeToSong,
            onCheckedChange = { enableSwipeToSong = it }
        )
        FloatPreferenceSliderRow(
            label = "Mini player swipe sensitivity",
            description = "Sensitivity of mini player swipe",
            value = swipeSensitivity,
            valueRange = 0.1f..1.0f,
            onValueChange = { swipeSensitivity = it }
        )
    }

    Spacer(Modifier.height(12.dp))

    OmniPreferenceCard(title = "LYRICS") {
        OmniSwitchPreference(
            title = "Lyrics V2 (Experimental)",
            description = "Use the new fluid word-synced lyrics engine",
            iconRes = R.drawable.ic_lyrics,
            checked = lyricsV2,
            onCheckedChange = { lyricsV2 = it }
        )
        OmniEnumPreference(
            title = "Lyrics text position",
            iconRes = R.drawable.ic_settings,
            selectedValue = com.omnitune.app.constants.LyricsPosition.valueOf(lyricsTextPosition),
            values = com.omnitune.app.constants.LyricsPosition.entries,
            valueText = { it.name },
            onValueSelected = { lyricsTextPosition = it.name }
        )
        OmniEnumPreference(
            title = "Lyrics animation style",
            iconRes = R.drawable.ic_settings,
            selectedValue = com.omnitune.app.constants.LyricsAnimationStyle.valueOf(lyricsAnimationStyle),
            values = com.omnitune.app.constants.LyricsAnimationStyle.entries,
            valueText = { it.name },
            onValueSelected = { lyricsAnimationStyle = it.name }
        )
        OmniSwitchPreference(
            title = "Change lyrics on click",
            iconRes = R.drawable.ic_settings,
            checked = lyricsClick,
            onCheckedChange = { lyricsClick = it }
        )
        OmniSwitchPreference(
            title = "Auto scroll lyrics",
            iconRes = R.drawable.ic_settings,
            checked = autoScrollLyrics,
            onCheckedChange = { autoScrollLyrics = it }
        )
        FloatPreferenceSliderRow(
            label = "Lyrics text size",
            description = "${lyricsTextSize.toInt()} sp",
            value = lyricsTextSize,
            valueRange = 14f..40f,
            onValueChange = { lyricsTextSize = it }
        )
        FloatPreferenceSliderRow(
            label = "Lyrics line spacing",
            description = "${lyricsLineSpacing}x",
            value = lyricsLineSpacing,
            valueRange = 1.0f..2.0f,
            onValueChange = { lyricsLineSpacing = it }
        )
    }

    Spacer(Modifier.height(12.dp))

    OmniPreferenceCard(title = "MISC") {
        OmniSwitchPreference(
            title = "Show home category chips",
            description = "Show category filter chips on the home screen",
            iconRes = R.drawable.ic_settings,
            checked = showHomeCategoryChips,
            onCheckedChange = { showHomeCategoryChips = it }
        )
        OmniSwitchPreference(
            title = "Show tags in library",
            description = "Show tag filter chips in the library",
            iconRes = R.drawable.ic_settings,
            checked = showTagsInLibrary,
            onCheckedChange = { showTagsInLibrary = it }
        )
        OmniSwitchPreference(
            title = "Slim bottom navigation bar",
            iconRes = R.drawable.ic_settings,
            checked = slimBottomNav,
            onCheckedChange = { slimBottomNav = it }
        )
    }

    Spacer(Modifier.height(12.dp))

    OmniPreferenceCard(title = "AUTO PLAYLISTS") {
        OmniSwitchPreference(
            title = "Show \"Liked\" playlist",
            iconRes = R.drawable.ic_favorite,
            checked = showLikedPlaylist,
            onCheckedChange = { showLikedPlaylist = it }
        )
        OmniSwitchPreference(
            title = "Show \"Downloaded\" playlist",
            iconRes = R.drawable.ic_download,
            checked = showDownloadedPlaylist,
            onCheckedChange = { showDownloadedPlaylist = it }
        )
    }
    
    Spacer(Modifier.height(12.dp))
    OmniPreferenceCard(title = "Customization") {
        OmniPreferenceEntry(
            title = "Theme Creator",
            description = "Customize OmniTune's colors and palettes",
            iconRes = R.drawable.ic_settings,
            accent = OmniColors.OmniAccentSecondary,
            onClick = onNavigateToThemeCreator
        )
        OmniPreferenceEntry(
            title = "Customize Background",
            description = "Change the background blur and effects",
            iconRes = R.drawable.ic_settings,
            accent = OmniColors.OmniAccentSecondary,
            onClick = onNavigateToCustomizeBackground
        )
    }
}

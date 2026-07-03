package com.omnitune.app.ui.screens.settings

import androidx.compose.runtime.Composable
import com.omnitune.app.constants.EnableBetterLyricsKey
import com.omnitune.app.constants.EnableKugouKey
import com.omnitune.app.constants.EnableLrcLibKey
import com.omnitune.app.constants.EnableSimpMusicLyricsKey

@Composable
fun LyricsSettings() {
    SettingsCategoryLabel("Lyrics providers")
    TogglePreferenceRow(
        label = "LrcLib",
        description = "Use the existing LrcLib provider",
        key = EnableLrcLibKey,
        defaultValue = true,
    )
    TogglePreferenceRow(
        label = "KuGou",
        description = "Use the existing KuGou provider",
        key = EnableKugouKey,
        defaultValue = true,
    )
    TogglePreferenceRow(
        label = "Better Lyrics",
        description = "Use the existing Better Lyrics provider",
        key = EnableBetterLyricsKey,
        defaultValue = true,
    )
    TogglePreferenceRow(
        label = "SimpMusic",
        description = "Use the existing SimpMusic provider",
        key = EnableSimpMusicLyricsKey,
        defaultValue = true,
    )
}

package com.omnitune.app.ui.screens.settings

import androidx.compose.runtime.Composable
import com.omnitune.app.ui.theme.OmniColors

@Composable
fun AppearanceSettings() {
    SettingsCategoryLabel("Display")
    SettingsInfoBlock(
        title = "OmniTune dark theme",
        body = "The app uses the current dark, music-first visual system across Home, Library, Stats, History, Search, Player, and Settings.",
        accent = OmniColors.OmniAccentMuted,
    )

    SettingsCategoryLabel("Library layout")
    SettingsInfoBlock(
        title = "Compact library browsing",
        body = "Library pages use fixed artwork sizes, readable rows, and the existing saved-content counts.",
        accent = OmniColors.OmniAccentMuted,
    )
}

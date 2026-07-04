package com.omnitune.app.ui.screens.settings

import androidx.compose.runtime.Composable
import com.omnitune.app.R
import com.omnitune.app.ui.theme.OmniColors

@Composable
fun AppearanceSettings() {
    OmniPreferenceCard(title = "Display") {
        OmniPreferenceEntry(
            title = "OmniTune dark theme",
            description = "The app uses the current dark, music-first visual system across Home, Library, Stats, History, Search, Player, and Settings.",
            iconRes = R.drawable.ic_settings,
            accent = OmniColors.OmniAccentMuted,
        )
    }

    OmniPreferenceCard(title = "Library layout") {
        OmniPreferenceEntry(
            title = "Compact library browsing",
            description = "Library pages use fixed artwork sizes, readable rows, and the existing saved-content counts.",
            iconRes = R.drawable.ic_list,
            accent = OmniColors.OmniAccentMuted,
        )
    }
}

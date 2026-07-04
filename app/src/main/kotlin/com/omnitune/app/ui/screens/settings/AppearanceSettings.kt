package com.omnitune.app.ui.screens.settings

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.omnitune.app.R
import com.omnitune.app.constants.PureBlackKey
import com.omnitune.app.ui.theme.OmniColors
import com.omnitune.app.utils.rememberPreference

@Composable
fun AppearanceSettings() {
    var pureBlack by rememberPreference(PureBlackKey, false)

    OmniPreferenceCard(title = "Display") {
        OmniPreferenceEntry(
            title = "OmniTune dark theme",
            description = "The app uses the current dark, music-first visual system across Home, Library, Stats, History, Search, Player, and Settings.",
            iconRes = R.drawable.ic_settings,
            accent = OmniColors.OmniAccentMuted,
        )
        OmniSwitchPreference(
            title = "Pure Black mode",
            description = "Use true black backgrounds for OLED screens — mini-player and navigation bar only",
            iconRes = R.drawable.ic_bedtime,
            accent = OmniColors.OmniAccentPrimary,
            checked = pureBlack,
            onCheckedChange = { pureBlack = it },
        )
    }

    Spacer(Modifier.height(12.dp))

    OmniPreferenceCard(title = "Library layout") {
        OmniPreferenceEntry(
            title = "Compact library browsing",
            description = "Library pages use fixed artwork sizes, readable rows, and the existing saved-content counts.",
            iconRes = R.drawable.ic_list,
            accent = OmniColors.OmniAccentMuted,
        )
    }
}

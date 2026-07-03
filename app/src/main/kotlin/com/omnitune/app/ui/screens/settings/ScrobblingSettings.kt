package com.omnitune.app.ui.screens.settings

import androidx.compose.runtime.Composable
import com.omnitune.app.ui.theme.OmniColors

@Composable
fun ScrobblingSettings() {
    SettingsCategoryLabel("Last.fm")
    SettingsInfoBlock(
        title = "Scrobbling controls are hidden from release settings",
        body = "This section is intentionally not linked from Settings until account and submission controls are fully wired.",
        accent = OmniColors.OmniAccentMuted,
    )
}

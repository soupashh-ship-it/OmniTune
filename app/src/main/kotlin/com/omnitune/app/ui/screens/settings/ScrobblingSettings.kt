package com.omnitune.app.ui.screens.settings

import androidx.compose.runtime.Composable
import com.omnitune.app.R
import com.omnitune.app.ui.theme.OmniColors

@Composable
fun ScrobblingSettings() {
    OmniPreferenceCard(title = "Last.fm") {
        OmniPreferenceEntry(
            title = "Scrobbling controls are hidden",
            description = "This section is intentionally not linked from Settings until account and submission controls are fully wired.",
            iconRes = R.drawable.ic_favorite,
            accent = OmniColors.OmniAccentMuted,
        )
    }
}

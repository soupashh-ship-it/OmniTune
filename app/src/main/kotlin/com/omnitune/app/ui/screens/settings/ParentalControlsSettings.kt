package com.omnitune.app.ui.screens.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.omnitune.app.R
import com.omnitune.app.constants.RestrictExplicitContentKey
import com.omnitune.app.constants.SafeSearchKey
import com.omnitune.app.ui.theme.OmniColors
import com.omnitune.app.utils.rememberPreference

@Composable
fun ParentalControlsSettings() {
    var restrictExplicit by rememberPreference(RestrictExplicitContentKey, false)
    var safeSearch by rememberPreference(SafeSearchKey, true)

    OmniPreferenceCard(title = "Content filtering") {
        OmniSwitchPreference(
            title = "Restrict explicit content",
            description = "Hide explicit tracks and prevent them from starting in new playback queues",
            iconRes = R.drawable.ic_warning,
            accent = OmniColors.OmniAccentPrimary,
            checked = restrictExplicit,
            onCheckedChange = { restrictExplicit = it },
        )
        OmniSwitchPreference(
            title = "Safe search",
            description = "Remove explicit songs, albums, artists, and playlists from search results",
            iconRes = R.drawable.ic_search,
            accent = OmniColors.OmniAccentSecondary,
            checked = safeSearch,
            onCheckedChange = { safeSearch = it },
        )
    }

    OmniPreferenceCard(title = "Protection") {
        OmniPreferenceEntry(
            title = "Controls apply on this device",
            description = "Filtering uses provider explicit-content labels and does not upload listening data",
            iconRes = R.drawable.ic_verified,
            accent = OmniColors.Downloaded,
        )
    }
}

package com.omnitune.app.ui.screens.settings

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.omnitune.app.R
import com.omnitune.app.constants.DynamicSongColorsKey
import com.omnitune.app.constants.OmniPlayerBackgroundStyle
import com.omnitune.app.constants.OmniPlayerBackgroundStyleKey
import com.omnitune.app.constants.PureBlackKey
import com.omnitune.app.constants.UseSystemFontKey
import com.omnitune.app.ui.theme.OmniColors
import com.omnitune.app.utils.rememberEnumPreference
import com.omnitune.app.utils.rememberPreference

@Composable
fun AppearanceSettings() {
    var dynamicSongColors by rememberPreference(DynamicSongColorsKey, true)
    var playerBackgroundStyle by rememberEnumPreference(
        OmniPlayerBackgroundStyleKey,
        OmniPlayerBackgroundStyle.DYNAMIC_GRADIENT,
    )
    var pureBlack by rememberPreference(PureBlackKey, false)
    var useSystemFont by rememberPreference(UseSystemFontKey, false)

    OmniPreferenceCard(title = "PLAYBACK COLOR") {
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
    }

    Spacer(Modifier.height(12.dp))

    OmniPreferenceCard(title = "DISPLAY") {
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
}

private val OmniPlayerBackgroundStyle.label: String
    get() = when (this) {
        OmniPlayerBackgroundStyle.DYNAMIC_GRADIENT -> "Dynamic gradient"
        OmniPlayerBackgroundStyle.SOLID_DARK -> "Solid dark"
    }

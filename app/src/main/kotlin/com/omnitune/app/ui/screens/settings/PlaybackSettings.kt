package com.omnitune.app.ui.screens.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.omnitune.app.R
import com.omnitune.app.constants.AutoSkipNextOnErrorKey
import com.omnitune.app.constants.AudioCrossfadeDurationKey
import com.omnitune.app.constants.PlaybackQualityModeKey
import com.omnitune.app.constants.SkipSilenceKey
import com.omnitune.app.models.PlaybackQualityMode
import com.omnitune.app.ui.theme.OmniColors
import com.omnitune.app.utils.rememberEnumPreference
import com.omnitune.app.utils.rememberPreference

@Composable
fun PlaybackSettings(onNavigateToEqualizer: () -> Unit) {
    var playbackQualityMode by rememberEnumPreference(PlaybackQualityModeKey, PlaybackQualityMode.AUTO)
    var skipSilence by rememberPreference(SkipSilenceKey, false)
    var autoSkip by rememberPreference(AutoSkipNextOnErrorKey, true)

    OmniPreferenceCard(title = "Audio quality") {
        OmniEnumPreference(
            title = "Stream quality",
            description = when (playbackQualityMode) {
                PlaybackQualityMode.AUTO -> "Let OmniTune choose the best available quality."
                PlaybackQualityMode.DATA_SAVER -> "Prefer lower data usage when possible."
                PlaybackQualityMode.BALANCED -> "Prefer stable playback and normal quality."
                PlaybackQualityMode.HIGH -> "Prefer higher quality streams when available."
            },
            iconRes = R.drawable.ic_settings,
            accent = OmniColors.ActivePlayback,
            selectedValue = playbackQualityMode,
            values = PlaybackQualityMode.entries.toList(),
            valueText = { it.displayName() },
            onValueSelected = { playbackQualityMode = it },
        )
    }

    OmniPreferenceCard(title = "Playback behavior") {
        OmniPreferenceEntry(
            title = "Equalizer",
            description = "Open the existing Android audio effects screen",
            iconRes = R.drawable.ic_settings,
            accent = OmniColors.ActivePlayback,
            onClick = onNavigateToEqualizer,
        )
        IntPreferenceSliderRow(
            label = "Crossfade duration",
            description = "Fade between songs without changing playback logic",
            key = AudioCrossfadeDurationKey,
            defaultValue = 0,
            valueRange = 0f..15f,
            steps = 14,
            valueFormat = { if (it == 0) "Off" else "${it}s" },
        )
        OmniSwitchPreference(
            title = "Skip silence",
            description = "Automatically skip silent parts",
            iconRes = R.drawable.ic_play_arrow,
            accent = OmniColors.ActivePlayback,
            checked = skipSilence,
            onCheckedChange = { skipSilence = it },
        )
        OmniSwitchPreference(
            title = "Auto-skip on error",
            description = "Skip to the next song if playback fails",
            iconRes = R.drawable.ic_play_arrow,
            accent = OmniColors.ActivePlayback,
            checked = autoSkip,
            onCheckedChange = { autoSkip = it },
        )
    }
}

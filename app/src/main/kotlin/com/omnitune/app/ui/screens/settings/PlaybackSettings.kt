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

@Composable
fun PlaybackSettings(onNavigateToEqualizer: () -> Unit) {
    val playbackQualityMode by rememberEnumPreference(PlaybackQualityModeKey, PlaybackQualityMode.AUTO)

    SettingsCategoryLabel("Audio quality")
    EnumPreferenceRow(
        label = "Stream quality",
        description = when (playbackQualityMode) {
            PlaybackQualityMode.AUTO -> "Let OmniTune choose the best available quality."
            PlaybackQualityMode.DATA_SAVER -> "Prefer lower data usage when possible."
            PlaybackQualityMode.BALANCED -> "Prefer stable playback and normal quality."
            PlaybackQualityMode.HIGH -> "Prefer higher quality streams when available."
        },
        options = PlaybackQualityMode.entries.toList(),
        current = playbackQualityMode,
        key = PlaybackQualityModeKey,
    )
    SettingsInfoBlock(
        title = "Applies when multiple stream qualities are available.",
        body = "Changes apply from the next song.",
        accent = OmniColors.OmniAccentMuted,
    )

    SettingsCategoryLabel("Playback behavior")
    SettingsActionRow(
        iconRes = R.drawable.ic_settings,
        label = "Equalizer",
        description = "Open the existing Android audio effects screen",
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
    TogglePreferenceRow(
        label = "Skip silence",
        description = "Automatically skip silent parts",
        key = SkipSilenceKey,
        defaultValue = false,
    )
    TogglePreferenceRow(
        label = "Auto-skip on error",
        description = "Skip to the next song if playback fails",
        key = AutoSkipNextOnErrorKey,
        defaultValue = true,
    )
}

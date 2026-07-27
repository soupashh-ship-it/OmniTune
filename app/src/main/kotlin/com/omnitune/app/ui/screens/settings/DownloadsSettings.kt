package com.omnitune.app.ui.screens.settings

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.omnitune.app.R
import com.omnitune.app.constants.AutoDownloadOnLikeKey
import com.omnitune.app.constants.AudioQuality
import com.omnitune.app.constants.DownloadMaxParallelKey
import com.omnitune.app.constants.DownloadQualityKey
import com.omnitune.app.constants.DownloadWifiOnlyKey
import com.omnitune.app.constants.RetryFailedDownloadsKey
import com.omnitune.app.constants.SmartTrimmerKey
import com.omnitune.app.ui.theme.OmniColors
import com.omnitune.app.utils.rememberPreference
import com.omnitune.app.utils.rememberEnumPreference

@Composable
fun DownloadsSettings(
    onOpenDownloads: () -> Unit,
) {
    var autoDownloadLikedSongs by rememberPreference(AutoDownloadOnLikeKey, false)
    var smartCacheTrimmer by rememberPreference(SmartTrimmerKey, true)
    var wifiOnly by rememberPreference(DownloadWifiOnlyKey, true)
    var retryFailed by rememberPreference(RetryFailedDownloadsKey, true)
    var maxParallel by rememberPreference(DownloadMaxParallelKey, 3)
    var downloadQuality by rememberEnumPreference(DownloadQualityKey, AudioQuality.HIGH)

    OmniPreferenceCard(title = "QUALITY") {
        OmniEnumPreference(
            title = "Download quality",
            description = "Choose the audio quality resolved for new downloads",
            iconRes = R.drawable.ic_volume_up,
            accent = OmniColors.OmniAccentPrimary,
            selectedValue = downloadQuality,
            values = AudioQuality.entries,
            valueText = { it.downloadLabel },
            onValueSelected = { downloadQuality = it },
        )
    }

    Spacer(Modifier.height(12.dp))

    OmniPreferenceCard(title = "AUTOMATION") {
        OmniSwitchPreference(
            title = "Wi-Fi only",
            description = "Start new downloads only while connected to Wi-Fi",
            iconRes = R.drawable.ic_sync,
            accent = OmniColors.OmniAccentPrimary,
            checked = wifiOnly,
            onCheckedChange = { wifiOnly = it },
        )
        OmniSwitchPreference(
            title = "Auto-download liked songs",
            description = "Save a song for offline playback when you like it",
            iconRes = R.drawable.ic_favorite,
            accent = OmniColors.OmniAccentPrimary,
            checked = autoDownloadLikedSongs,
            onCheckedChange = { autoDownloadLikedSongs = it },
        )
        OmniSwitchPreference(
            title = "Smart cache trimmer",
            description = "Remove old temporary stream files automatically",
            iconRes = R.drawable.ic_storage,
            accent = OmniColors.OmniAccentPrimary,
            checked = smartCacheTrimmer,
            onCheckedChange = { smartCacheTrimmer = it },
        )
        OmniSwitchPreference(
            title = "Retry failed downloads",
            description = "Resolve a fresh stream and retry a failed download twice",
            iconRes = R.drawable.ic_sync,
            accent = OmniColors.OmniAccentSecondary,
            checked = retryFailed,
            onCheckedChange = { retryFailed = it },
        )
        FloatPreferenceSliderRow(
            label = "Concurrent downloads",
            description = "Maximum number of downloads processed together",
            value = maxParallel.toFloat(),
            onValueChange = { maxParallel = it.toInt().coerceIn(1, 8) },
            valueRange = 1f..8f,
            steps = 6,
            valueFormat = { it.toInt().toString() },
        )
    }

    Spacer(Modifier.height(12.dp))

    OmniPreferenceCard(title = "OFFLINE MUSIC") {
        OmniPreferenceEntry(
            title = "Open Downloads",
            description = "View ready, active, and failed offline songs",
            iconRes = R.drawable.ic_download,
            accent = OmniColors.Downloaded,
            onClick = onOpenDownloads,
        )
    }
}

private val AudioQuality.downloadLabel: String
    get() = when (this) {
        AudioQuality.AUTO -> "Automatic"
        AudioQuality.LOW -> "Data saver"
        AudioQuality.HIGH -> "High"
        AudioQuality.HIGHEST -> "Highest"
    }

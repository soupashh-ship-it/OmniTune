package com.omnitune.app.ui.screens.settings
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

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
import com.omnitune.app.ui.screens.settings.OmniPreferenceCard
import com.omnitune.app.ui.screens.settings.OmniSwitchPreference
import com.omnitune.app.ui.screens.settings.OmniEnumPreference
import com.omnitune.app.ui.screens.settings.OmniPreferenceEntry
import com.omnitune.app.ui.screens.settings.FloatPreferenceSliderRow
import com.omnitune.app.ui.screens.settings.IntPreferenceSliderRow

@Composable
fun PlaybackSettings(onNavigateToEqualizer: () -> Unit) {
    var metered by rememberPreference(com.omnitune.app.constants.NetworkMeteredKey, false)
    var historyDuration by rememberPreference(com.omnitune.app.constants.HistoryDuration, 30f)
    var skipSilence by rememberPreference(com.omnitune.app.constants.SkipSilenceKey, false)
    var audioNorm by rememberPreference(com.omnitune.app.constants.AudioNormalizationKey, false)
    var audioOffload by rememberPreference(com.omnitune.app.constants.AudioOffload, false)
    var progressiveSeek by rememberPreference(com.omnitune.app.constants.SeekExtraSeconds, false)
    var pauseOnMute by rememberPreference(com.omnitune.app.constants.PauseOnDeviceMuteKey, false)
    
    var playerClient by rememberPreference(com.omnitune.app.constants.PlayerStreamClientKey, com.omnitune.app.constants.PlayerStreamClient.MOBILE.name)
    
    var autoBluetooth by rememberPreference(com.omnitune.app.constants.AutoStartOnBluetoothKey, false)
    var persistentQueue by rememberPreference(com.omnitune.app.constants.PersistentQueueKey, true)
    var permShuffle by rememberPreference(com.omnitune.app.constants.PermanentShuffleKey, false)
    var autoDownloadLike by rememberPreference(com.omnitune.app.constants.AutoDownloadOnLikeKey, false)
    var autoSkipError by rememberPreference(com.omnitune.app.constants.AutoSkipNextOnErrorKey, true)
    
    var stopOnClear by rememberPreference(com.omnitune.app.constants.StopMusicOnTaskClearKey, false)
    var artistSeparators by rememberPreference(com.omnitune.app.constants.ArtistSeparatorsKey, ", ; / &")

    var showTagsDialog by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

    OmniPreferenceCard(title = "PLAYER") {
        OmniSwitchPreference(
            title = "Treat network as metered",
            description = "When enabled, the app will prefer lower-quality streams on metered networks",
            iconRes = R.drawable.ic_settings,
            checked = metered,
            onCheckedChange = { metered = it }
        )
        FloatPreferenceSliderRow(
            label = "History duration",
            description = "${historyDuration.toInt()}",
            value = historyDuration,
            valueRange = 0f..100f,
            onValueChange = { historyDuration = it }
        )
        IntPreferenceSliderRow(
            label = "Crossfade",
            description = "Fade between songs without changing playback logic",
            key = com.omnitune.app.constants.AudioCrossfadeDurationKey,
            defaultValue = 0,
            valueRange = 0f..15f,
            steps = 14,
            valueFormat = { if (it == 0) "Off" else "${it}s" },
        )
        OmniSwitchPreference(
            title = "Skip silence",
            iconRes = R.drawable.ic_settings,
            checked = skipSilence,
            onCheckedChange = { skipSilence = it }
        )
        OmniSwitchPreference(
            title = "Audio normalization",
            iconRes = R.drawable.ic_settings,
            checked = audioNorm,
            onCheckedChange = { audioNorm = it }
        )
        OmniSwitchPreference(
            title = "Audio offload",
            description = "Let supported devices play audio using low-power hardware decoding.",
            iconRes = R.drawable.ic_settings,
            checked = audioOffload,
            onCheckedChange = { audioOffload = it }
        )
        OmniSwitchPreference(
            title = "Progressive seek",
            description = "Adds up 5 extra seconds incrementally on each seek skip",
            iconRes = R.drawable.ic_settings,
            checked = progressiveSeek,
            onCheckedChange = { progressiveSeek = it }
        )
        OmniSwitchPreference(
            title = "Pause when device volume is 0",
            description = "Automatically pause music when muted and resume when unmuted",
            iconRes = R.drawable.ic_settings,
            checked = pauseOnMute,
            onCheckedChange = { pauseOnMute = it }
        )
        OmniEnumPreference(
            title = "Player Client",
            description = "Select the YouTube stream client. (Android VR is fastest)",
            iconRes = R.drawable.ic_settings,
            selectedValue = com.omnitune.app.constants.PlayerStreamClient.valueOf(playerClient),
            values = com.omnitune.app.constants.PlayerStreamClient.entries,
            valueText = { it.name },
            onValueSelected = { playerClient = it.name }
        )
        OmniSwitchPreference(
            title = "Auto-start on Bluetooth connect",
            description = "Automatically resume playback when a Bluetooth audio device connects",
            iconRes = R.drawable.ic_settings,
            checked = autoBluetooth,
            onCheckedChange = { autoBluetooth = it }
        )
    }

    androidx.compose.foundation.layout.Spacer(androidx.compose.ui.Modifier.height(12.dp))

    OmniPreferenceCard(title = "QUEUE") {
        OmniSwitchPreference(
            title = "Persistent queue",
            description = "Restore your last queue when the app starts",
            iconRes = R.drawable.ic_settings,
            checked = persistentQueue,
            onCheckedChange = { persistentQueue = it }
        )
        OmniSwitchPreference(
            title = "Permanent shuffle mode",
            description = "Keep shuffle enabled when you start a new song",
            iconRes = R.drawable.ic_settings,
            checked = permShuffle,
            onCheckedChange = { permShuffle = it }
        )
        OmniSwitchPreference(
            title = "Auto download on like",
            description = "Automatically download songs when you like them",
            iconRes = R.drawable.ic_settings,
            checked = autoDownloadLike,
            onCheckedChange = { autoDownloadLike = it }
        )
        OmniSwitchPreference(
            title = "Auto skip to next song when error occurs",
            description = "Ensure your continuous playback experience",
            iconRes = R.drawable.ic_settings,
            checked = autoSkipError,
            onCheckedChange = { autoSkipError = it }
        )
    }

    androidx.compose.foundation.layout.Spacer(androidx.compose.ui.Modifier.height(12.dp))

    OmniPreferenceCard(title = "MISC") {
        OmniSwitchPreference(
            title = "Stop music on task clear",
            iconRes = R.drawable.ic_settings,
            checked = stopOnClear,
            onCheckedChange = { stopOnClear = it }
        )
        OmniPreferenceEntry(
            title = "Symbols to split artists",
            description = artistSeparators,
            iconRes = R.drawable.ic_settings,
        )
    }

    val context = androidx.compose.ui.platform.LocalContext.current

    
}

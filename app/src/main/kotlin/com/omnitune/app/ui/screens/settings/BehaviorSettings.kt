package com.omnitune.app.ui.screens.settings

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.omnitune.app.R
import com.omnitune.app.constants.AutoSkipNextOnErrorKey
import com.omnitune.app.constants.AutoStartOnBluetoothKey
import com.omnitune.app.constants.AutoplaySimilarSongsKey
import com.omnitune.app.constants.PauseOnDeviceMuteKey
import com.omnitune.app.constants.PermanentShuffleKey
import com.omnitune.app.constants.PersistentQueueKey
import com.omnitune.app.constants.StopMusicOnTaskClearKey
import com.omnitune.app.ui.theme.OmniColors
import com.omnitune.app.utils.rememberPreference

@Composable
fun BehaviorSettings() {
    var autoplaySimilarSongs by rememberPreference(AutoplaySimilarSongsKey, true)
    var persistentQueue by rememberPreference(PersistentQueueKey, true)
    var autoSkipOnError by rememberPreference(AutoSkipNextOnErrorKey, true)
    var autoStartOnBluetooth by rememberPreference(AutoStartOnBluetoothKey, false)
    var pauseOnMute by rememberPreference(PauseOnDeviceMuteKey, false)
    var permanentShuffle by rememberPreference(PermanentShuffleKey, false)
    var stopOnTaskClear by rememberPreference(StopMusicOnTaskClearKey, false)

    OmniPreferenceCard(title = "PLAYBACK BEHAVIOR") {
        OmniSwitchPreference(
            title = "Autoplay similar songs",
            description = "Continue with related music after the current queue ends",
            iconRes = R.drawable.ic_play_arrow,
            accent = OmniColors.OmniAccentPrimary,
            checked = autoplaySimilarSongs,
            onCheckedChange = { autoplaySimilarSongs = it },
        )
        OmniSwitchPreference(
            title = "Resume last session",
            description = "Restore your queue and position when OmniTune starts",
            iconRes = R.drawable.ic_history,
            accent = OmniColors.OmniAccentPrimary,
            checked = persistentQueue,
            onCheckedChange = { persistentQueue = it },
        )
        OmniSwitchPreference(
            title = "Skip playback errors",
            description = "Move to the next song when the current stream cannot play",
            iconRes = R.drawable.ic_skip_next,
            accent = OmniColors.OmniAccentPrimary,
            checked = autoSkipOnError,
            onCheckedChange = { autoSkipOnError = it },
        )
        OmniSwitchPreference(
            title = "Permanent shuffle",
            description = "Keep shuffle enabled when a new queue starts",
            iconRes = R.drawable.ic_shuffle,
            accent = OmniColors.OmniAccentPrimary,
            checked = permanentShuffle,
            onCheckedChange = { permanentShuffle = it },
        )
    }

    Spacer(Modifier.height(12.dp))

    OmniPreferenceCard(title = "DEVICES") {
        OmniSwitchPreference(
            title = "Resume on Bluetooth connect",
            description = "Resume playback when a Bluetooth audio device reconnects",
            iconRes = R.drawable.ic_settings,
            accent = OmniColors.OmniAccentSecondary,
            checked = autoStartOnBluetooth,
            onCheckedChange = { autoStartOnBluetooth = it },
        )
        OmniSwitchPreference(
            title = "Pause when device volume is zero",
            description = "Pause while muted and resume when volume returns",
            iconRes = R.drawable.ic_volume_off,
            accent = OmniColors.OmniAccentSecondary,
            checked = pauseOnMute,
            onCheckedChange = { pauseOnMute = it },
        )
    }

    Spacer(Modifier.height(12.dp))

    OmniPreferenceCard(title = "SESSION") {
        OmniSwitchPreference(
            title = "Stop music when app is cleared",
            description = "End playback when OmniTune is removed from recent apps",
            iconRes = R.drawable.ic_close,
            accent = OmniColors.Warning,
            checked = stopOnTaskClear,
            onCheckedChange = { stopOnTaskClear = it },
        )
    }
}

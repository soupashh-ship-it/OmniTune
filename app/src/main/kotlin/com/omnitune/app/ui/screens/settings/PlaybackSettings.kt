package com.omnitune.app.ui.screens.settings

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import android.widget.Toast
import androidx.compose.material3.TextButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.datastore.preferences.core.Preferences
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.omnitune.app.BuildConfig
import com.omnitune.app.R
import com.omnitune.app.constants.AudioCrossfadeDurationKey
import com.omnitune.app.constants.AudioQuality
import com.omnitune.app.constants.AudioQualityKey
import com.omnitune.app.constants.PlaybackQualityModeKey
import com.omnitune.app.models.PlaybackQualityMode
import com.omnitune.app.constants.AutoSkipNextOnErrorKey
import com.omnitune.app.constants.DisableBlurKey
import com.omnitune.app.constants.EnableBetterLyricsKey
import com.omnitune.app.constants.EnableKugouKey
import com.omnitune.app.constants.EnableLastFMScrobblingKey
import com.omnitune.app.constants.EnableLrcLibKey
import com.omnitune.app.constants.EnableSimpMusicLyricsKey
import com.omnitune.app.constants.GridItemSize
import com.omnitune.app.constants.GridItemsSizeKey
import com.omnitune.app.constants.HideExplicitKey
import com.omnitune.app.constants.HideVideoKey
import com.omnitune.app.constants.LastFMUseNowPlaying
import com.omnitune.app.constants.ListenBrainzEnabledKey
import com.omnitune.app.constants.LyricsAnimationStyle
import com.omnitune.app.constants.LyricsAnimationStyleKey
import com.omnitune.app.constants.PauseListenHistoryKey
import com.omnitune.app.constants.PauseOnDeviceMuteKey
import com.omnitune.app.constants.PauseSearchHistoryKey
import com.omnitune.app.constants.PureBlackKey
import com.omnitune.app.constants.SkipSilenceKey
import com.omnitune.app.constants.SmartTrimmerKey
import com.omnitune.app.diagnostics.DiagnosticReportExporter
import com.omnitune.app.playback.MusicService
import com.omnitune.app.ui.component.GlassCard
import com.omnitune.app.ui.theme.OmniColors
import com.omnitune.app.ui.theme.OmniShapes
import com.omnitune.app.ui.theme.OmniSpacing
import com.omnitune.app.update.ApkInstallLauncher
import com.omnitune.app.update.UpdateState
import com.omnitune.app.update.UpdateViewModel
import com.omnitune.app.utils.rememberEnumPreference
import com.omnitune.app.utils.rememberPreference

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



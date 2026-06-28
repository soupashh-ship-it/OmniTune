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
fun MediaControlsHelp() {
    val context = LocalContext.current
    var message by remember { mutableStateOf<String?>(null) }
    val notificationsEnabled = remember {
        NotificationManagerCompat.from(context).areNotificationsEnabled()
    }
    val channelStatus = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)
            when (manager.getNotificationChannel(MusicService.CHANNEL_ID)?.importance) {
                null -> "Playback channel will be created after playback starts."
                NotificationManager.IMPORTANCE_NONE -> "Playback notification channel is blocked."
                NotificationManager.IMPORTANCE_MIN -> "Playback channel is set very low; controls may be hidden."
                else -> "Playback notification channel is allowed."
            }
        } else {
            "Notification channels are not used on this Android version."
        }
    }
    val batteryStatus = remember {
        val powerManager = context.getSystemService(PowerManager::class.java)
        if (powerManager.isIgnoringBatteryOptimizations(context.packageName)) {
            "Battery optimization is unrestricted for OmniTune."
        } else {
            "Battery restrictions may affect background playback and media controls."
        }
    }

    SettingsInfoBlock(
        title = "Notification and lock-screen controls",
        body = "Behavior can vary by device and OEM. Battery restrictions may affect background playback.",
        accent = OmniColors.Warning,
    )
    UpdateMessage(
        "Notification permission: ${if (notificationsEnabled) "Allowed" else "Blocked"}\n" +
            "$channelStatus\n$batteryStatus"
    )
    UpdateMessage(
        "On Vivo/iQOO/Funtouch OS and similar Android skins, allow notifications, lock-screen notifications, background activity, unrestricted battery, and autostart if your device offers those options."
    )
    SettingsActionButton("Open notification settings") {
        openSettingsIntent(context, openNotificationSettingsIntent(context)) {
            message = "Could not open notification settings."
        }
    }
    SettingsActionButton("Open app settings") {
        openSettingsIntent(context, openAppDetailsIntent(context)) {
            message = "Could not open app settings."
        }
    }
    SettingsActionButton("Open battery settings") {
        openSettingsIntent(context, Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) {
            message = "Could not open battery settings."
        }
    }
    message?.let { UpdateMessage(it) }
}



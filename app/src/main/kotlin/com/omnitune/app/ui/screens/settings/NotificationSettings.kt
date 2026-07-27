package com.omnitune.app.ui.screens.settings

import android.app.NotificationManager
import android.content.Intent
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.omnitune.app.R
import com.omnitune.app.playback.MusicService
import com.omnitune.app.ui.theme.OmniColors

@Composable
fun MediaControlsHelp() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var refreshKey by remember { mutableIntStateOf(0) }
    var message by remember { mutableStateOf<String?>(null) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refreshKey += 1
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val notificationsEnabled = remember(refreshKey) {
        NotificationManagerCompat.from(context).areNotificationsEnabled()
    }
    val channelStatus = remember(refreshKey) {
        val manager = context.getSystemService(NotificationManager::class.java)
        when (manager.getNotificationChannel(MusicService.CHANNEL_ID)?.importance) {
            null -> "Created automatically when playback starts"
            NotificationManager.IMPORTANCE_NONE -> "Blocked in Android settings"
            NotificationManager.IMPORTANCE_MIN -> "Allowed at very low importance"
            else -> "Allowed and ready for playback controls"
        }
    }
    val batteryUnrestricted = remember(refreshKey) {
        context.getSystemService(PowerManager::class.java)
            .isIgnoringBatteryOptimizations(context.packageName)
    }

    OmniPreferenceCard(title = "Playback controls") {
        OmniPreferenceEntry(
            title = "Push notifications",
            description = if (notificationsEnabled) {
                "Allowed by Android"
            } else {
                "Blocked — tap to enable in Android settings"
            },
            iconRes = R.drawable.ic_notification_play,
            accent = if (notificationsEnabled) OmniColors.Downloaded else OmniColors.Warning,
            onClick = {
                openSettingsIntent(context, openNotificationSettingsIntent(context)) {
                    message = "Could not open notification settings."
                }
            },
        )
        OmniPreferenceEntry(
            title = "Player notification",
            description = channelStatus,
            iconRes = R.drawable.ic_play_arrow,
            accent = OmniColors.OmniAccentPrimary,
            onClick = {
                openSettingsIntent(context, openNotificationSettingsIntent(context)) {
                    message = "Could not open playback notification settings."
                }
            },
        )
        OmniPreferenceEntry(
            title = "Lock-screen controls",
            description = "Managed with the playback notification by Android",
            iconRes = R.drawable.ic_bedtime,
            accent = OmniColors.OmniAccentSecondary,
            onClick = {
                openSettingsIntent(context, openNotificationSettingsIntent(context)) {
                    message = "Could not open lock-screen notification settings."
                }
            },
        )
    }

    OmniPreferenceCard(title = "Background playback") {
        OmniPreferenceEntry(
            title = "Battery access",
            description = if (batteryUnrestricted) {
                "Unrestricted — background playback is protected"
            } else {
                "Battery restrictions may stop playback; tap to review"
            },
            iconRes = R.drawable.ic_bolt,
            accent = if (batteryUnrestricted) OmniColors.Downloaded else OmniColors.Warning,
            onClick = {
                openSettingsIntent(
                    context,
                    Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS),
                ) {
                    message = "Could not open battery settings."
                }
            },
        )
        OmniPreferenceEntry(
            title = "App notification settings",
            description = "Review lock-screen visibility, background activity, and autostart options",
            iconRes = R.drawable.ic_settings,
            accent = OmniColors.OmniAccentTertiary,
            onClick = {
                openSettingsIntent(context, openAppDetailsIntent(context)) {
                    message = "Could not open app settings."
                }
            },
        )
    }

    message?.let {
        Text(
            text = it,
            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
            color = OmniColors.TextSecondary,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        )
    }
}

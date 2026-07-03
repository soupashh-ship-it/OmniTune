package com.omnitune.app.ui.screens.settings

import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationManagerCompat
import com.omnitune.app.playback.MusicService
import com.omnitune.app.ui.theme.OmniColors

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



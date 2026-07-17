package com.omnitune.app.ui.screens.settings

import android.app.NotificationManager
import android.content.Intent
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import com.omnitune.app.R
import com.omnitune.app.playback.MusicService
import com.omnitune.app.ui.theme.OmniColors
import com.omnitune.app.ui.theme.OmniShapes

@Composable
fun MediaControlsHelp() {
    val context = LocalContext.current
    var message by remember { mutableStateOf<String?>(null) }
    val notificationsEnabled = remember {
        NotificationManagerCompat.from(context).areNotificationsEnabled()
    }
    val channelStatus = remember {
        val manager = context.getSystemService(NotificationManager::class.java)
        when (manager.getNotificationChannel(MusicService.CHANNEL_ID)?.importance) {
            null -> "Playback channel will be created after playback starts."
            NotificationManager.IMPORTANCE_NONE -> "Playback notification channel is blocked."
            NotificationManager.IMPORTANCE_MIN -> "Playback channel is set very low; controls may be hidden."
            else -> "Playback notification channel is allowed."
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

    OmniPreferenceCard(title = "Notification & lock-screen controls") {
        OmniPreferenceEntry(
            title = "Behavior varies by device",
            description = "Battery restrictions may affect background playback and media controls.",
            iconRes = R.drawable.ic_notification_play,
            accent = OmniColors.Warning,
        )
    }

    Text(
        text = "Notification permission: ${if (notificationsEnabled) "Allowed" else "Blocked"}\n$channelStatus\n$batteryStatus",
        style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
        color = OmniColors.TextSecondary,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
    )

    Text(
        text = "On Vivo/iQOO/Funtouch OS and similar Android skins, allow notifications, lock-screen notifications, background activity, unrestricted battery, and autostart if your device offers those options.",
        style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
        color = OmniColors.TextSecondary,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
    )

    Spacer(Modifier.height(8.dp))

    Button(
        onClick = {
            openSettingsIntent(context, openNotificationSettingsIntent(context)) {
                message = "Could not open notification settings."
            }
        },
        colors = ButtonDefaults.buttonColors(
            containerColor = OmniColors.OmniAccentPrimary,
            contentColor = OmniColors.OmniAccentOnPrimary,
        ),
        shape = OmniShapes.Pill,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(50.dp),
    ) {
        Text("Open notification settings", fontWeight = FontWeight.Bold)
    }

    Spacer(Modifier.height(8.dp))

    Button(
        onClick = {
            openSettingsIntent(context, openAppDetailsIntent(context)) {
                message = "Could not open app settings."
            }
        },
        colors = ButtonDefaults.buttonColors(
            containerColor = OmniColors.OmniAccentPrimary,
            contentColor = OmniColors.OmniAccentOnPrimary,
        ),
        shape = OmniShapes.Pill,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(50.dp),
    ) {
        Text("Open app settings", fontWeight = FontWeight.Bold)
    }

    Spacer(Modifier.height(8.dp))

    Button(
        onClick = {
            openSettingsIntent(context, Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) {
                message = "Could not open battery settings."
            }
        },
        colors = ButtonDefaults.buttonColors(
            containerColor = OmniColors.OmniAccentPrimary,
            contentColor = OmniColors.OmniAccentOnPrimary,
        ),
        shape = OmniShapes.Pill,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(50.dp),
    ) {
        Text("Open battery settings", fontWeight = FontWeight.Bold)
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

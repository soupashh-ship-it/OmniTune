/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 *
 * Based on Velune Discord settings
 */

package com.omnitune.app.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.omnitune.app.R
import com.omnitune.app.constants.DiscordActivityNameKey
import com.omnitune.app.constants.DiscordActivityDetailsKey
import com.omnitune.app.constants.DiscordActivityStateKey
import com.omnitune.app.constants.DiscordActivityTypeKey
import com.omnitune.app.constants.DiscordPresenceStatusKey
import com.omnitune.app.constants.DiscordActivityButton1EnabledKey
import com.omnitune.app.constants.DiscordActivityButton1LabelKey
import com.omnitune.app.constants.DiscordActivityButton1UrlSourceKey
import com.omnitune.app.constants.DiscordActivityButton1CustomUrlKey
import com.omnitune.app.constants.DiscordActivityButton2EnabledKey
import com.omnitune.app.constants.DiscordActivityButton2LabelKey
import com.omnitune.app.constants.DiscordActivityButton2UrlSourceKey
import com.omnitune.app.constants.DiscordActivityButton2CustomUrlKey
import com.omnitune.app.constants.DiscordLargeImageTypeKey
import com.omnitune.app.constants.DiscordLargeTextSourceKey
import com.omnitune.app.constants.DiscordLargeTextCustomKey
import com.omnitune.app.constants.DiscordLargeImageCustomUrlKey
import com.omnitune.app.constants.DiscordSmallImageTypeKey
import com.omnitune.app.constants.DiscordSmallImageCustomUrlKey
import com.omnitune.app.constants.DiscordTokenKey
import com.omnitune.app.constants.EnableDiscordRPCKey
import com.omnitune.app.constants.DiscordShowWhenPausedKey
import com.omnitune.app.constants.DiscordPresenceIntervalValueKey
import com.omnitune.app.constants.DiscordPresenceIntervalUnitKey
import com.omnitune.app.constants.DiscordActivityButton1EnabledKey
import com.omnitune.app.constants.DiscordActivityButton2EnabledKey
import com.omnitune.app.LocalPlayerConnection
import com.omnitune.app.ui.theme.OmniColors
import com.omnitune.app.utils.rememberPreference
import kotlinx.coroutines.launch

@Composable
fun DiscordSettingsScreen(
    onNavigateToLogin: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val playerConnection = LocalPlayerConnection.current
    val discordRunning by playerConnection?.discordPresenceRunning?.collectAsState() ?: remember { mutableStateOf(false) }

    // Account state
    var discordToken by rememberPreference(DiscordTokenKey, "")

    // Main toggle
    var discordEnabled by rememberPreference(EnableDiscordRPCKey, false)

    // Activity customization
    var activityName by rememberPreference(DiscordActivityNameKey, "OmniTune")
    var activityDetails by rememberPreference(DiscordActivityDetailsKey, "{song}")
    var activityState by rememberPreference(DiscordActivityStateKey, "{artist}")
    var activityType by rememberPreference(DiscordActivityTypeKey, "LISTENING")
    var presenceStatus by rememberPreference(DiscordPresenceStatusKey, "ONLINE")
    var showWhenPaused by rememberPreference(DiscordShowWhenPausedKey, true)

    // Images
    var largeImageType by rememberPreference(DiscordLargeImageTypeKey, "thumbnail")
    var largeTextSource by rememberPreference(DiscordLargeTextSourceKey, "song")
    var largeTextCustom by rememberPreference(DiscordLargeTextCustomKey, "")
    var largeImageCustomUrl by rememberPreference(DiscordLargeImageCustomUrlKey, "")
    var smallImageType by rememberPreference(DiscordSmallImageTypeKey, "none")
    var smallImageCustomUrl by rememberPreference(DiscordSmallImageCustomUrlKey, "")

    // Buttons
    var btn1Enabled by rememberPreference(DiscordActivityButton1EnabledKey, false)
    var btn1Label by rememberPreference(DiscordActivityButton1LabelKey, "Listen on OmniTune")
    var btn1UrlSource by rememberPreference(DiscordActivityButton1UrlSourceKey, "custom")
    var btn1CustomUrl by rememberPreference(DiscordActivityButton1CustomUrlKey, "")
    var btn2Enabled by rememberPreference(DiscordActivityButton2EnabledKey, false)
    var btn2Label by rememberPreference(DiscordActivityButton2LabelKey, "View on YouTube")
    var btn2UrlSource by rememberPreference(DiscordActivityButton2UrlSourceKey, "custom")
    var btn2CustomUrl by rememberPreference(DiscordActivityButton2CustomUrlKey, "")

    // Interval
    var intervalValue by rememberPreference(DiscordPresenceIntervalValueKey, 30)
    var intervalUnit by rememberPreference(DiscordPresenceIntervalUnitKey, "S")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp),
    ) {
        Spacer(Modifier.height(8.dp))

        // Account section
        OmniPreferenceCard(title = "Account") {
            if (discordToken.isNotBlank()) {
                OmniPreferenceEntry(
                    title = "Discord Connected",
                    description = "Logged in as ${
                        discordToken.take(20)
                    }...",
                    iconRes = R.drawable.ic_info,
                    accent = OmniColors.Downloaded,
                    onClick = {
                        discordToken = ""
                        discordEnabled = false
                    },
                )
                // Live connection status
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(
                                color = if (discordRunning) OmniColors.Downloaded else OmniColors.Warning,
                                shape = CircleShape,
                            )
                    )
                    Text(
                        text = if (discordRunning) "Connected to Discord" else "Disconnected",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (discordRunning) OmniColors.Downloaded else OmniColors.Warning,
                    )
                }
                OmniPreferenceEntry(
                    title = "Log out",
                    description = "Disconnect your Discord account",
                    iconRes = R.drawable.ic_close,
                    accent = OmniColors.Warning,
                    onClick = {
                        discordToken = ""
                        discordEnabled = false
                        playerConnection?.restartDiscordPresence()
                    },
                )
            } else {
                OmniPreferenceEntry(
                    title = "Connect Discord",
                    description = "Sign in to enable Rich Presence",
                    iconRes = R.drawable.ic_favorite,
                    accent = OmniColors.OmniAccentPrimary,
                    onClick = onNavigateToLogin,
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // Main toggle
        OmniPreferenceCard(title = "Rich Presence") {
            OmniSwitchPreference(
                title = "Enable Discord RPC",
                description = "Show now-playing status on Discord",
                iconRes = R.drawable.ic_info,
                accent = OmniColors.Hot,
                checked = discordEnabled && discordToken.isNotBlank(),
                onCheckedChange = {
                    discordEnabled = it
                    playerConnection?.restartDiscordPresence()
                },
            )
            OmniSwitchPreference(
                title = "Show when paused",
                description = "Display presence even when playback is paused",
                checked = showWhenPaused,
                onCheckedChange = { showWhenPaused = it },
            )
        }

        Spacer(Modifier.height(12.dp))

        // Activity customization
        OmniPreferenceCard(title = "Activity") {
            OmniPreferenceEntry(
                title = "Activity name",
                description = activityName.ifBlank { "OmniTune" },
                iconRes = R.drawable.ic_play_arrow,
                accent = OmniColors.OmniAccentSecondary,
                onClick = {
                    // Would open a text input dialog
                },
            )
            OmniPreferenceEntry(
                title = "Activity type",
                description = activityType.lowercase().replaceFirstChar { it.uppercase() },
                iconRes = R.drawable.ic_play_arrow,
                accent = OmniColors.OmniAccentSecondary,
                onClick = {
                    // Would open enum selection dialog
                },
            )
            OmniPreferenceEntry(
                title = "Status",
                description = presenceStatus.lowercase().replaceFirstChar { it.uppercase() },
                iconRes = R.drawable.ic_info,
                accent = OmniColors.OmniAccentSecondary,
                onClick = {
                    // Would open status selection dialog
                },
            )
        }

        Spacer(Modifier.height(12.dp))

        // Display / Images
        OmniPreferenceCard(title = "Display") {
            OmniPreferenceEntry(
                title = "Large image",
                description = largeImageType.replaceFirstChar { it.uppercase() },
                iconRes = R.drawable.ic_album,
                accent = OmniColors.OmniAccentPrimary,
            )
            OmniPreferenceEntry(
                title = "Large text",
                description = largeTextSource.replaceFirstChar { it.uppercase() },
                iconRes = R.drawable.ic_artist,
                accent = OmniColors.TextSecondary,
            )
            OmniPreferenceEntry(
                title = "Small image",
                description = smallImageType.replaceFirstChar { it.uppercase() },
                iconRes = R.drawable.ic_album,
                accent = OmniColors.OmniAccentPrimary,
            )
        }

        Spacer(Modifier.height(12.dp))

        // Buttons
        OmniPreferenceCard(title = "Action buttons") {
            OmniSwitchPreference(
                title = "Button 1",
                description = btn1Label.take(40),
                iconRes = R.drawable.ic_play_arrow,
                accent = OmniColors.Hot,
                checked = btn1Enabled,
                onCheckedChange = { btn1Enabled = it },
            )
            OmniSwitchPreference(
                title = "Button 2",
                description = btn2Label.take(40),
                iconRes = R.drawable.ic_play_arrow,
                accent = OmniColors.Hot,
                checked = btn2Enabled,
                onCheckedChange = { btn2Enabled = it },
            )
        }

        Spacer(Modifier.height(12.dp))

        // Update interval
        OmniPreferenceCard(title = "Update interval") {
            OmniPreferenceEntry(
                title = "Update frequency",
                description = when (intervalUnit) {
                    "S" -> "Every ${intervalValue}s"
                    "M" -> "Every ${intervalValue}m"
                    "H" -> "Every ${intervalValue}h"
                    else -> "Every ${intervalValue}s"
                },
                iconRes = R.drawable.ic_settings,
                accent = OmniColors.TextSecondary,
            )
        }

        Spacer(Modifier.height(12.dp))

        // Info
        OmniPreferenceCard(title = "About Discord RPC") {
            OmniPreferenceEntry(
                title = "How it works",
                description = "Discord Rich Presence shows your current song as a status on your Discord profile. Album art and song details are displayed in real time.",
                iconRes = R.drawable.ic_info,
                accent = OmniColors.TextSecondary,
            )
            OmniPreferenceEntry(
                title = "Privacy",
                description = "Only what you choose to display is shared. Your token is stored locally and never sent to third parties.",
                iconRes = R.drawable.ic_info,
                accent = OmniColors.TextSecondary,
            )
        }

        Spacer(Modifier.height(32.dp))
    }
}

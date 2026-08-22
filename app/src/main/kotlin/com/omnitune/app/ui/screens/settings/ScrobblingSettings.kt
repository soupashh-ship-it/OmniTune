/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.omnitune.app.R
import com.omnitune.app.constants.ListenBrainzEnabledKey
import com.omnitune.app.constants.ListenBrainzNowPlayingKey
import com.omnitune.app.constants.ListenBrainzTokenKey
import com.omnitune.app.constants.ScrobbleDelayPercentKey
import com.omnitune.app.constants.ScrobbleDelaySecondsKey
import com.omnitune.app.constants.ScrobbleMinSongDurationKey
import com.omnitune.app.ui.theme.OmniColors
import com.omnitune.app.utils.SecurePreferenceCipher
import com.omnitune.app.utils.rememberPreference

@Composable
fun ScrobblingSettings() {
    var enabled by rememberPreference(ListenBrainzEnabledKey, false)
    var nowPlaying by rememberPreference(ListenBrainzNowPlayingKey, true)
    var storedToken by rememberPreference(ListenBrainzTokenKey, "")
    var delayPercent by rememberPreference(ScrobbleDelayPercentKey, 50f)
    var delaySeconds by rememberPreference(ScrobbleDelaySecondsKey, 30)
    var minSongDuration by rememberPreference(ScrobbleMinSongDurationKey, 30)
    var showTokenDialog by remember { mutableStateOf(false) }
    val token = SecurePreferenceCipher.decryptOrPlain(storedToken)

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        OmniPreferenceCard(title = "ListenBrainz") {
            OmniSwitchPreference(
                title = "Enable scrobbling",
                description = if (token.isBlank()) "Add your token before enabling scrobbling" else "Send completed listens to ListenBrainz",
                iconRes = R.drawable.ic_history,
                accent = OmniColors.OmniAccentTertiary,
                checked = enabled,
                onCheckedChange = { checked -> if (token.isNotBlank()) enabled = checked },
            )
            OmniSwitchPreference(
                title = "Now playing updates",
                description = "Send the current track while playback is active",
                iconRes = R.drawable.ic_play_arrow,
                accent = OmniColors.OmniAccentSecondary,
                checked = nowPlaying,
                onCheckedChange = { nowPlaying = it },
            )
            OmniPreferenceEntry(
                title = "User token",
                description = if (token.isBlank()) "Tap to configure your token" else "Token configured and encrypted on this device",
                iconRes = R.drawable.ic_settings,
                accent = OmniColors.OmniAccentSecondary,
                onClick = { showTokenDialog = true },
            )
            if (token.isNotBlank()) {
                OmniPreferenceEntry(
                    title = "Remove token",
                    description = "Stops ListenBrainz scrobbling and removes the local credential",
                    iconRes = R.drawable.ic_close,
                    accent = OmniColors.Warning,
                    onClick = {
                        storedToken = ""
                        enabled = false
                    },
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        OmniPreferenceCard(title = "Scrobble threshold") {
            FloatPreferenceSliderRow(
                label = "Percent of song",
                description = "Scrobble after this percentage has played",
                value = delayPercent,
                onValueChange = { delayPercent = it },
                valueRange = 10f..100f,
                steps = 8,
                valueFormat = { "${it.toInt()}%" },
            )
            FloatPreferenceSliderRow(
                label = "Max delay (seconds)",
                description = "Scrobble after this many seconds, whichever comes first",
                value = delaySeconds.toFloat(),
                onValueChange = { delaySeconds = it.toInt() },
                valueRange = 10f..120f,
                steps = 10,
                valueFormat = { "${it.toInt()}s" },
            )
            FloatPreferenceSliderRow(
                label = "Minimum song duration",
                description = "Do not scrobble tracks shorter than this",
                value = minSongDuration.toFloat(),
                onValueChange = { minSongDuration = it.toInt() },
                valueRange = 10f..300f,
                steps = 28,
                valueFormat = { "${it.toInt()}s" },
            )
        }
        Spacer(Modifier.height(32.dp))
    }

    if (showTokenDialog) {
        var tokenInput by remember(showTokenDialog) { mutableStateOf(token) }
        AlertDialog(
            onDismissRequest = { showTokenDialog = false },
            containerColor = OmniColors.OmniBackgroundElevated,
            titleContentColor = OmniColors.TextPrimary,
            textContentColor = OmniColors.TextSecondary,
            title = { Text("ListenBrainz token", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = tokenInput,
                    onValueChange = { tokenInput = it },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    placeholder = { Text("Enter your token") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OmniColors.OmniAccentPrimary,
                        unfocusedBorderColor = OmniColors.OmniGlassBorderSubtle,
                        focusedTextColor = OmniColors.TextPrimary,
                        unfocusedTextColor = OmniColors.TextPrimary,
                    ),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    storedToken = tokenInput.trim().takeIf { it.isNotBlank() }
                        ?.let(SecurePreferenceCipher::encrypt)
                        .orEmpty()
                    if (storedToken.isBlank()) enabled = false
                    showTokenDialog = false
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showTokenDialog = false }) { Text("Cancel") } },
        )
    }
}

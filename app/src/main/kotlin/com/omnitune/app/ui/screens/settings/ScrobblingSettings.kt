@file:Suppress("UNCHECKED_CAST", "IMPLICIT_CAST_TO_ANY")

package com.omnitune.app.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.omnitune.app.R
import com.omnitune.app.constants.EnableLastFMScrobblingKey
import com.omnitune.app.constants.LastFMSessionKey
import com.omnitune.app.constants.LastFMUseNowPlaying
import com.omnitune.app.constants.LastFMUsernameKey
import com.omnitune.app.constants.ScrobbleDelayPercentKey
import com.omnitune.app.constants.ScrobbleDelaySecondsKey
import com.omnitune.app.constants.ScrobbleMinSongDurationKey
import com.omnitune.app.ui.theme.OmniColors
import com.omnitune.app.utils.rememberPreference
import com.omnitune.lastfm.LastFM
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ScrobblingSettings() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var username by rememberPreference(LastFMUsernameKey, "")
    var sessionKey by rememberPreference(LastFMSessionKey, "")
    var scrobblingEnabled by rememberPreference(EnableLastFMScrobblingKey, false)
    var useNowPlaying by rememberPreference(LastFMUseNowPlaying, true)
    var delayPercent by rememberPreference(ScrobbleDelayPercentKey, 50f)
    var delaySeconds by rememberPreference(ScrobbleDelaySecondsKey, 30)
    var minSongDuration by rememberPreference(ScrobbleMinSongDurationKey, 30)

    var showLoginDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var loginError by remember { mutableStateOf<String?>(null) }

    val isLoggedIn = sessionKey.isNotBlank()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
    ) {
        Spacer(Modifier.height(8.dp))

        // Account section
        OmniPreferenceCard(title = "Account") {
            if (isLoggedIn) {
                OmniPreferenceEntry(
                    title = "Logged in",
                    description = username.ifBlank { "Connected to Last.fm" },
                    iconRes = R.drawable.ic_favorite,
                    accent = OmniColors.Downloaded,
                )
                OmniPreferenceEntry(
                    title = "Log out",
                    description = "Disconnect your Last.fm account",
                    iconRes = R.drawable.ic_close,
                    accent = OmniColors.Warning,
                    onClick = { showLogoutDialog = true },
                )
            } else {
                OmniPreferenceEntry(
                    title = "Log in",
                    description = "Connect your Last.fm account to scrobble",
                    iconRes = R.drawable.ic_favorite,
                    accent = OmniColors.OmniAccentPrimary,
                    onClick = { showLoginDialog = true },
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // Scrobbling toggle
        OmniPreferenceCard(title = "Scrobbling") {
            OmniSwitchPreference(
                title = "Enable scrobbling",
                description = "Submit plays to Last.fm",
                iconRes = R.drawable.ic_favorite,
                accent = OmniColors.Hot,
                checked = scrobblingEnabled && isLoggedIn,
                onCheckedChange = { scrobblingEnabled = it },
            )
            OmniSwitchPreference(
                title = "Now playing updates",
                description = "Show currently playing track on Last.fm",
                iconRes = R.drawable.ic_play_arrow,
                accent = OmniColors.OmniAccentSecondary,
                checked = useNowPlaying,
                onCheckedChange = { useNowPlaying = it },
            )
        }

        Spacer(Modifier.height(12.dp))

        // Scrobble threshold
        OmniPreferenceCard(title = "Scrobble threshold") {
            FloatPreferenceSliderRow(
                label = "Percent of song",
                description = "Scrobble when this percentage has played",
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
                label = "Min song duration",
                description = "Only scrobble songs longer than this",
                value = minSongDuration.toFloat(),
                onValueChange = { minSongDuration = it.toInt() },
                valueRange = 10f..300f,
                steps = 28,
                valueFormat = { "${it.toInt()}s" },
            )
        }

        Spacer(Modifier.height(32.dp))
    }

    // ── Login dialog ──────────────────────────────────────────────────

    if (showLoginDialog) {
        LoginDialog(
            error = loginError,
            onDismiss = {
                showLoginDialog = false
                loginError = null
            },
            onLoginSuccess = { user, session ->
                username = user
                sessionKey = session
                scrobblingEnabled = true
                showLoginDialog = false
                loginError = null
            },
            onError = { loginError = it },
        )
    }

    // ── Logout dialog ──────────────────────────────────────────────────

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            containerColor = OmniColors.OmniBackgroundElevated,
            titleContentColor = OmniColors.TextPrimary,
            textContentColor = OmniColors.TextSecondary,
            title = { Text("Log out of Last.fm?", fontWeight = FontWeight.Bold) },
            text = { Text("Your session key will be removed. Scrobbling will stop.") },
            confirmButton = {
                TextButton(onClick = {
                    username = ""
                    sessionKey = ""
                    scrobblingEnabled = false
                    showLogoutDialog = false
                }) {
                    Text("Log out", fontWeight = FontWeight.Bold, color = OmniColors.Warning)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel", color = OmniColors.TextSecondary)
                }
            },
        )
    }

    // ── Error banner ──────────────────────────────────────────────────

    loginError?.let { error ->
        Text(
            text = error,
            style = MaterialTheme.typography.bodySmall,
            color = OmniColors.Warning,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun LoginDialog(
    error: String? = null,
    onDismiss: () -> Unit,
    onLoginSuccess: (username: String, sessionKey: String) -> Unit,
    onError: (String) -> Unit,
) {
    var login by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val performLogin: () -> Unit = {
        if (login.isNotBlank() && password.isNotBlank() && !isLoading) {
            isLoading = true
            scope.launch {
                try {
                    val session = withContext(Dispatchers.IO) {
                        LastFM.getMobileSession(login.trim(), password)
                    }
                    val sessionKey = session?.key
                    if (sessionKey != null) {
                        onLoginSuccess(login.trim(), sessionKey)
                    } else {
                        onError("Login failed. Check your credentials.")
                    }
                } catch (e: Exception) {
                    onError(e.message ?: "Login failed. Check your credentials.")
                } finally {
                    isLoading = false
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        containerColor = OmniColors.OmniBackgroundElevated,
        titleContentColor = OmniColors.TextPrimary,
        textContentColor = OmniColors.TextSecondary,
        title = { Text("Last.fm Login", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = login,
                    onValueChange = { login = it },
                    label = { Text("Username", color = OmniColors.TextTertiary) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    enabled = !isLoading,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = OmniColors.TextPrimary,
                        unfocusedTextColor = OmniColors.TextPrimary,
                        cursorColor = OmniColors.OmniAccentSecondary,
                        focusedBorderColor = OmniColors.OmniAccentSecondary,
                        unfocusedBorderColor = OmniColors.OmniGlassBorderSubtle,
                        focusedLabelColor = OmniColors.OmniAccentSecondary,
                        unfocusedLabelColor = OmniColors.TextTertiary,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password", color = OmniColors.TextTertiary) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = { performLogin() }
                    ),
                    enabled = !isLoading,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = OmniColors.TextPrimary,
                        unfocusedTextColor = OmniColors.TextPrimary,
                        cursorColor = OmniColors.OmniAccentSecondary,
                        focusedBorderColor = OmniColors.OmniAccentSecondary,
                        unfocusedBorderColor = OmniColors.OmniGlassBorderSubtle,
                        focusedLabelColor = OmniColors.OmniAccentSecondary,
                        unfocusedLabelColor = OmniColors.TextTertiary,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (error != null) {
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodySmall,
                        color = OmniColors.Warning,
                    )
                }
                if (isLoading) {
                    Text(
                        text = "Signing in...",
                        style = MaterialTheme.typography.bodySmall,
                        color = OmniColors.TextTertiary,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { performLogin() },
                enabled = login.isNotBlank() && password.isNotBlank() && !isLoading,
            ) {
                Text("Log in", fontWeight = FontWeight.Bold, color = OmniColors.OmniAccentSecondary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) {
                Text("Cancel", color = OmniColors.TextSecondary)
            }
        },
    )
}


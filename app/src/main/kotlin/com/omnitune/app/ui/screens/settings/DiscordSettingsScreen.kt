/*
 * This file was adapted from SuvMusic.
 * Original copyright follows:
 * 
 * Copyright (C) Suvojeet
 * Licensed under the GNU General Public License v3.0 (GPLv3)
 */

package com.omnitune.app.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.omnitune.app.constants.ListenBrainzEnabledKey
import com.omnitune.app.constants.ListenBrainzNowPlayingKey
import com.omnitune.app.constants.ListenBrainzTokenKey
import com.omnitune.app.constants.ScrobbleDelayPercentKey
import com.omnitune.app.constants.ScrobbleDelaySecondsKey
import com.omnitune.app.constants.ScrobbleMinSongDurationKey
import com.omnitune.app.ui.component.SettingsCard
import com.omnitune.app.ui.component.SettingsRowStyle
import com.omnitune.app.ui.component.SettingsSwitchRow
import com.omnitune.app.ui.theme.SquircleShape
import com.omnitune.app.utils.SecurePreferenceCipher
import com.omnitune.app.utils.rememberPreference
import com.omnitune.app.viewmodels.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscordSettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var token by remember(uiState.discordToken) { mutableStateOf(uiState.discordToken) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Discord Rich Presence", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                SettingsCard {
                    SettingsSwitchRow(
                        title = "Enable Discord RPC",
                        subtitle = "Display currently playing song as your Discord activity",
                        icon = Icons.Default.MusicNote,
                        checked = uiState.discordRpcEnabled,
                        onCheckedChange = viewModel::setDiscordRpcEnabled
                    )
                }
            }

            item {
                SettingsCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        SecretSettingsField(
                            value = token,
                            persistedValue = uiState.discordToken,
                            label = "Discord User Token (Optional)",
                            onValueChange = { token = it },
                            onSave = viewModel::setDiscordToken,
                            onClear = {
                                token = ""
                                viewModel.setDiscordToken("")
                            },
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LastFmSettingsScreen(
    onBackClick: () -> Unit,
) {
    var enabled by rememberPreference(ListenBrainzEnabledKey, false)
    var nowPlaying by rememberPreference(ListenBrainzNowPlayingKey, true)
    var storedToken by rememberPreference(ListenBrainzTokenKey, "")
    var delayPercent by rememberPreference(ScrobbleDelayPercentKey, 50f)
    var delaySeconds by rememberPreference(ScrobbleDelaySecondsKey, 30)
    var minSongDuration by rememberPreference(ScrobbleMinSongDurationKey, 30)
    var showTokenDialog by remember { mutableStateOf(false) }
    val token = SecurePreferenceCipher.decryptOrPlain(storedToken)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scrobbling", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                ScrobblingSectionTitle("Account")
                SettingsCard {
                    if (token.isNotBlank()) {
                        ListItem(
                            headlineContent = { Text("ListenBrainz token configured") },
                            supportingContent = { Text("Scrobbling is active on this device") },
                            leadingContent = {
                                Icon(
                                    imageVector = Icons.Default.Favorite,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            },
                            trailingContent = {
                                Button(
                                    onClick = {
                                        storedToken = ""
                                        enabled = false
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer,
                                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                                    ),
                                ) {
                                    Text("Remove")
                                }
                            },
                            colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
                        )
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            Text(
                                text = "Connect ListenBrainz to scrobble your music.",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Button(onClick = { showTokenDialog = true }) {
                                Text("Add Token")
                            }
                        }
                    }
                }
            }

            item {
                ScrobblingSectionTitle("Scrobbling")
                SettingsCard {
                    SettingsSwitchRow(
                        title = "Enable Scrobbling",
                        subtitle = "Submit completed listens to ListenBrainz",
                        icon = Icons.Default.MusicNote,
                        checked = enabled,
                        onCheckedChange = { checked -> if (token.isNotBlank()) enabled = checked },
                        enabled = token.isNotBlank(),
                        style = SettingsRowStyle.Plain,
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                    SettingsSwitchRow(
                        title = "Show Now Playing",
                        subtitle = "Update your status while listening",
                        icon = Icons.Default.PlayArrow,
                        checked = nowPlaying,
                        onCheckedChange = { nowPlaying = it },
                        style = SettingsRowStyle.Plain,
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                    ListItem(
                        headlineContent = { Text("ListenBrainz Token") },
                        supportingContent = {
                            Text(if (token.isBlank()) "Tap to configure your token" else "Token is encrypted locally")
                        },
                        leadingContent = {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                        trailingContent = {
                            TextButton(onClick = { showTokenDialog = true }) {
                                Text(if (token.isBlank()) "Add" else "Edit")
                            }
                        },
                        colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
                    )
                }
            }

            item {
                ScrobblingSectionTitle("Rules")
                SettingsCard {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Text(
                            "Minimum Track Duration: ${minSongDuration}s",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Slider(
                            value = minSongDuration.toFloat(),
                            onValueChange = { minSongDuration = it.toInt() },
                            valueRange = 30f..120f,
                            steps = 9,
                        )
                        Text(
                            "Tracks shorter than this will not be scrobbled.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                        Text(
                            "Scrobble Point: ${delayPercent.toInt()}%",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Slider(
                            value = delayPercent,
                            onValueChange = { delayPercent = it },
                            valueRange = 50f..100f,
                            steps = 4,
                        )
                        Text(
                            "Percentage of track played before a listen is submitted.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                        Text(
                            "Maximum Delay: ${delaySeconds}s",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Slider(
                            value = delaySeconds.toFloat(),
                            onValueChange = { delaySeconds = it.toInt() },
                            valueRange = 10f..120f,
                            steps = 10,
                        )
                        Text(
                            "Scrobble after this many seconds, whichever comes first.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }

    if (showTokenDialog) {
        var tokenInput by remember(showTokenDialog) { mutableStateOf(token) }
        var passwordVisible by remember(showTokenDialog) { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showTokenDialog = false },
            title = { Text("ListenBrainz Token", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = tokenInput,
                        onValueChange = { tokenInput = it },
                        label = { Text("User token") },
                        singleLine = true,
                        visualTransformation = if (passwordVisible) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(image, contentDescription = "Toggle token visibility")
                            }
                        },
                        shape = SquircleShape,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = "Generate a user token in ListenBrainz, then paste it here.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        storedToken = tokenInput.trim().takeIf { it.isNotBlank() }
                            ?.let(SecurePreferenceCipher::encrypt)
                            .orEmpty()
                        if (storedToken.isBlank()) enabled = false
                        showTokenDialog = false
                    },
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTokenDialog = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun ScrobblingSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
    )
}

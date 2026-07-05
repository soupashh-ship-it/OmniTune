package com.omnitune.app.ui.screens.settings

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.omnitune.app.R
import com.omnitune.app.constants.*
import com.omnitune.app.utils.rememberPreference
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun MusicTogetherScreen(
    navController: NavController,
    viewModel: MusicTogetherViewModel = hiltViewModel()
) {
    var displayName by rememberPreference(TogetherDisplayNameKey, "")
    var showDisplayNameDialog by remember { mutableStateOf(false) }
    var showJoinDialog by remember { mutableStateOf(false) }
    var joinInput by remember { mutableStateOf("") }
    var editDisplayName by remember { mutableStateOf(displayName) }
    val isSessionActive by viewModel.isSessionActive.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    var allowAddTracks by rememberPreference(TogetherAllowGuestsToAddTracksKey, true)
    var allowControlPlayback by rememberPreference(TogetherAllowGuestsToControlPlaybackKey, false)
    var requireApproval by rememberPreference(TogetherRequireHostApprovalToJoinKey, false)

    SettingsSubScreenScaffold(
        title = "Listen Together",
        onBack = { navController.popBackStack() }
    ) {
        OmniPreferenceCard(title = "SESSION") {
            SettingsActionButton(
                label = if (isSessionActive) "Stop session" else "Start session",
                onClick = { viewModel.toggleSession() }
            )
            if (isSessionActive) {
                OmniPreferenceEntry(
                    title = "Session is active",
                    description = "Waiting for participants...",
                    iconRes = R.drawable.ic_info,
                    accent = com.omnitune.app.ui.theme.OmniColors.ActivePlayback,
                )
            }
        }
        
        Spacer(Modifier.height(12.dp))

        OmniPreferenceCard(title = "JOIN") {
            OmniPreferenceEntry(
                title = "Join session",
                description = "Paste a join link or code",
                iconRes = R.drawable.ic_add,
                onClick = { showJoinDialog = true }
            )
        }

        Spacer(Modifier.height(12.dp))

        OmniPreferenceCard(title = "PREFERENCES") {
            OmniPreferenceEntry(
                title = "Display name",
                description = displayName.ifBlank { "Set your display name for sessions" },
                iconRes = R.drawable.ic_info,
                onClick = { 
                    editDisplayName = displayName
                    showDisplayNameDialog = true 
                }
            )
            OmniSwitchPreference(
                title = "Allow guests to add tracks",
                iconRes = R.drawable.ic_settings,
                checked = allowAddTracks,
                onCheckedChange = { 
                    allowAddTracks = it 
                    viewModel.updateSettings(it, allowControlPlayback, requireApproval)
                }
            )
            OmniSwitchPreference(
                title = "Allow guests to control playback",
                iconRes = R.drawable.ic_settings,
                checked = allowControlPlayback,
                onCheckedChange = { 
                    allowControlPlayback = it 
                    viewModel.updateSettings(allowAddTracks, it, requireApproval)
                }
            )
            OmniSwitchPreference(
                title = "Require host approval to join",
                iconRes = R.drawable.ic_settings,
                checked = requireApproval,
                onCheckedChange = { 
                    requireApproval = it 
                    viewModel.updateSettings(allowAddTracks, allowControlPlayback, it)
                }
            )

        }
    }

    if (showJoinDialog) {
        AlertDialog(
            onDismissRequest = { showJoinDialog = false },
            containerColor = com.omnitune.app.ui.theme.OmniColors.SurfaceRaised,
            titleContentColor = com.omnitune.app.ui.theme.OmniColors.TextPrimary,
            textContentColor = com.omnitune.app.ui.theme.OmniColors.TextSecondary,
            title = { Text("Join session", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = joinInput,
                    onValueChange = { joinInput = it },
                    label = { Text("Paste link or code") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = com.omnitune.app.ui.theme.OmniColors.OmniAccentPrimary,
                        focusedLabelColor = com.omnitune.app.ui.theme.OmniColors.OmniAccentPrimary,
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (joinInput.isNotBlank()) {
                            showJoinDialog = false
                            viewModel.joinSession(joinInput)
                            android.widget.Toast.makeText(context, "Connecting to session...", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Join", color = com.omnitune.app.ui.theme.OmniColors.OmniAccentPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showJoinDialog = false }) {
                    Text("Cancel", color = com.omnitune.app.ui.theme.OmniColors.TextSecondary)
                }
            }
        )
    }

    if (showDisplayNameDialog) {
        AlertDialog(
            onDismissRequest = { showDisplayNameDialog = false },
            containerColor = com.omnitune.app.ui.theme.OmniColors.SurfaceRaised,
            titleContentColor = com.omnitune.app.ui.theme.OmniColors.TextPrimary,
            textContentColor = com.omnitune.app.ui.theme.OmniColors.TextSecondary,
            title = { Text("Display name", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = editDisplayName,
                    onValueChange = { editDisplayName = it },
                    label = { Text("Name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = com.omnitune.app.ui.theme.OmniColors.OmniAccentPrimary,
                        focusedLabelColor = com.omnitune.app.ui.theme.OmniColors.OmniAccentPrimary,
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        displayName = editDisplayName
                        showDisplayNameDialog = false
                    }
                ) {
                    Text("Save", color = com.omnitune.app.ui.theme.OmniColors.OmniAccentPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDisplayNameDialog = false }) {
                    Text("Cancel", color = com.omnitune.app.ui.theme.OmniColors.TextSecondary)
                }
            }
        )
    }
}
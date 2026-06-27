package com.omnitune.app.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.omnitune.app.R
import com.omnitune.app.db.entities.Playlist
import com.omnitune.app.ui.theme.OmniColors
import com.omnitune.app.ui.theme.OmniSpacing

@Composable
fun AddToPlaylistDialog(
    playlists: List<Playlist>,
    onDismissRequest: () -> Unit,
    onPlaylistSelected: (Playlist) -> Unit,
    onCreatePlaylist: (String) -> Unit
) {
    var showCreateDialog by remember { mutableStateOf(false) }

    if (showCreateDialog) {
        var playlistName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("New Playlist", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = playlistName,
                    onValueChange = { playlistName = it },
                    singleLine = true,
                    placeholder = { Text("Playlist name") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OmniColors.OmniAccentPrimary,
                        unfocusedBorderColor = OmniColors.OmniGlassBorderSubtle,
                        focusedTextColor = OmniColors.TextPrimary,
                        unfocusedTextColor = OmniColors.TextPrimary
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (playlistName.isNotBlank()) {
                            onCreatePlaylist(playlistName.trim())
                            showCreateDialog = false
                            onDismissRequest()
                        }
                    },
                    enabled = playlistName.isNotBlank()
                ) {
                    Text("Create", color = if (playlistName.isNotBlank()) OmniColors.Hot else OmniColors.TextSecondary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel", color = OmniColors.TextPrimary)
                }
            },
            containerColor = OmniColors.OmniBackgroundElevated,
            titleContentColor = OmniColors.TextPrimary,
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismissRequest,
            title = { Text("Add to Playlist", fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)
                ) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showCreateDialog = true }
                                .padding(vertical = OmniSpacing.small),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_add),
                                contentDescription = null,
                                tint = OmniColors.OmniAccentPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(OmniSpacing.medium))
                            Text("New Playlist", color = OmniColors.OmniAccentPrimary, style = MaterialTheme.typography.bodyLarge)
                        }
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = OmniSpacing.small),
                            color = OmniColors.OmniGlassBorderSubtle
                        )
                    }
                    items(playlists) { playlist ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onPlaylistSelected(playlist)
                                    onDismissRequest()
                                }
                                .padding(vertical = OmniSpacing.small),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_list),
                                contentDescription = null,
                                tint = OmniColors.TextSecondary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(OmniSpacing.medium))
                            Column {
                                Text(
                                    text = playlist.playlist.name,
                                    color = OmniColors.TextPrimary,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = "${playlist.songCount} songs",
                                    color = OmniColors.TextSecondary,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onDismissRequest) {
                    Text("Cancel", color = OmniColors.TextPrimary)
                }
            },
            containerColor = OmniColors.OmniBackgroundElevated,
            titleContentColor = OmniColors.TextPrimary,
        )
    }
}

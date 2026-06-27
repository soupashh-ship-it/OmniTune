/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.omnitune.app.R
import com.omnitune.app.db.entities.Playlist
import com.omnitune.app.ui.component.EmptyPlaceholder
import com.omnitune.app.ui.theme.OmniColors
import com.omnitune.app.ui.theme.OmniShapes
import com.omnitune.app.ui.theme.OmniSpacing

@Composable
fun LibraryPlaylistsScreen(
    onBack: () -> Unit = {},
    onNavigateToPlaylist: (String) -> Unit = {},
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val playlists by viewModel.playlists.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OmniColors.OmniBackgroundBase)
            .background(OmniColors.BackgroundGradient)
            .statusBarsPadding()
            .padding(horizontal = OmniSpacing.section),
    ) {
        var showCreateDialog by remember { androidx.compose.runtime.mutableStateOf(false) }
        if (showCreateDialog) {
            var playlistName by remember { androidx.compose.runtime.mutableStateOf("") }
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showCreateDialog = false },
                title = { Text("New Playlist", fontWeight = FontWeight.Bold) },
                text = {
                    androidx.compose.material3.OutlinedTextField(
                        value = playlistName,
                        onValueChange = { playlistName = it },
                        singleLine = true,
                        placeholder = { Text("Playlist name") },
                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OmniColors.OmniAccentPrimary,
                            unfocusedBorderColor = OmniColors.OmniGlassBorderSubtle,
                            focusedTextColor = OmniColors.TextPrimary,
                            unfocusedTextColor = OmniColors.TextPrimary
                        )
                    )
                },
                confirmButton = {
                    androidx.compose.material3.TextButton(
                        onClick = {
                            if (playlistName.isNotBlank()) {
                                viewModel.createPlaylist(playlistName.trim())
                                showCreateDialog = false
                            }
                        },
                        enabled = playlistName.isNotBlank()
                    ) {
                        Text("Create", color = if (playlistName.isNotBlank()) OmniColors.Hot else OmniColors.TextSecondary)
                    }
                },
                dismissButton = {
                    androidx.compose.material3.TextButton(onClick = { showCreateDialog = false }) {
                        Text("Cancel", color = OmniColors.TextPrimary)
                    }
                },
                containerColor = OmniColors.OmniBackgroundElevated,
                titleContentColor = OmniColors.TextPrimary,
            )
        }

        LibraryListHeader(
            title = "Playlists",
            subtitle = countLabel(playlists.size, "playlist"),
            icon = R.drawable.ic_list,
            accent = OmniColors.Hot,
            actionIcon = R.drawable.ic_add,
            onAction = { showCreateDialog = true },
            onBack = onBack,
        )

        if (playlists.isEmpty()) {
            LibraryEmptyState(icon = R.drawable.ic_list, text = "No playlists in your library yet")
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(OmniSpacing.small),
            ) {
                items(
                    items = playlists,
                    key = { it.id },
                    contentType = { "playlist" },
                ) { playlist ->
                    PlaylistRow(playlist = playlist, onClick = { onNavigateToPlaylist(playlist.id) })
                }
                item { Spacer(modifier = Modifier.height(88.dp)) }
            }
        }
    }
}

@Composable
private fun PlaylistRow(playlist: Playlist, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(OmniShapes.Large)
            .background(OmniColors.OmniGlassSubtle)
            .border(BorderStroke(1.dp, OmniColors.OmniGlassBorderSubtle), OmniShapes.Large)
            .clickable(onClick = onClick)
            .padding(OmniSpacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(58.dp)
                .clip(OmniShapes.ArtworkSmall)
                .background(OmniColors.Hot.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painterResource(R.drawable.ic_list),
                contentDescription = null,
                tint = OmniColors.Hot,
                modifier = Modifier.size(24.dp),
            )
        }
        Spacer(modifier = Modifier.width(OmniSpacing.small))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = playlist.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = OmniColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = countLabel(playlist.songCount, "song"),
                style = MaterialTheme.typography.bodySmall,
                color = OmniColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun LibraryListHeader(
    title: String,
    subtitle: String,
    icon: Int,
    accent: androidx.compose.ui.graphics.Color,
    actionIcon: Int? = null,
    onAction: (() -> Unit)? = null,
    onBack: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = OmniSpacing.medium, bottom = OmniSpacing.large),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.size(48.dp).clip(CircleShape).background(OmniColors.OmniGlassMedium).border(BorderStroke(1.dp, OmniColors.OmniGlassBorderSubtle), CircleShape),
        ) {
            Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = "Back", tint = OmniColors.TextPrimary)
        }
        Spacer(modifier = Modifier.width(OmniSpacing.medium))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = OmniColors.TextPrimary)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = OmniColors.TextSecondary)
        }
        if (actionIcon != null && onAction != null) {
            IconButton(
                onClick = onAction,
                modifier = Modifier.size(44.dp).clip(CircleShape).background(accent.copy(alpha = 0.16f))
            ) {
                Icon(painterResource(actionIcon), contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
            }
        } else {
            Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(accent.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
                Icon(painterResource(icon), contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
            }
        }
    }
}

@Composable
private fun LibraryEmptyState(icon: Int, text: String) {
    Box(
        modifier = Modifier.fillMaxWidth().clip(OmniShapes.ExtraLarge).background(OmniColors.OmniGlassSubtle).border(BorderStroke(1.dp, OmniColors.OmniGlassBorderSubtle), OmniShapes.ExtraLarge).padding(OmniSpacing.screen),
        contentAlignment = Alignment.Center,
    ) {
        EmptyPlaceholder(icon = icon, text = text)
    }
}

private fun countLabel(count: Int, singular: String): String {
    val noun = if (count == 1) singular else "${singular}s"
    return "$count $noun"
}

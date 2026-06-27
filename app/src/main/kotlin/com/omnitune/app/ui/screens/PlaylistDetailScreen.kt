/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.omnitune.app.LocalPlayerConnection
import com.omnitune.app.extensions.toMediaItem
import com.omnitune.app.models.toMediaMetadata
import com.omnitune.app.ui.component.TrackMenuProvider

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.omnitune.app.R
import com.omnitune.app.db.entities.PlaylistSong
import com.omnitune.app.ui.component.EmptyPlaceholder
import com.omnitune.app.ui.theme.OmniColors
import com.omnitune.app.ui.theme.OmniShapes
import com.omnitune.app.ui.theme.OmniSpacing

@Composable
fun PlaylistDetailScreen(
    onBack: () -> Unit = {},
    onPlaySong: (com.omnitune.app.db.entities.Song) -> Unit = {},
    viewModel: PlaylistDetailViewModel = androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel(),
) {
    val playlist by viewModel.playlist.collectAsState()
    val songs by viewModel.songs.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OmniColors.OmniBackgroundBase)
            .background(OmniColors.BackgroundGradient)
            .padding(horizontal = OmniSpacing.section),
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = OmniSpacing.medium, bottom = OmniSpacing.large),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(OmniColors.OmniGlassMedium)
                    .border(BorderStroke(1.dp, OmniColors.OmniGlassBorderSubtle), CircleShape),
            ) {
                Icon(
                    painterResource(R.drawable.ic_arrow_back),
                    contentDescription = "Back",
                    tint = OmniColors.TextPrimary,
                )
            }
            Spacer(modifier = Modifier.width(OmniSpacing.medium))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = playlist?.playlist?.name ?: "Playlist",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = OmniColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${songs.size} songs",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OmniColors.TextSecondary,
                )
            }
            
            var showPlaylistMenu by remember { androidx.compose.runtime.mutableStateOf(false) }
            var showRenameDialog by remember { androidx.compose.runtime.mutableStateOf(false) }
            var showDeleteDialog by remember { androidx.compose.runtime.mutableStateOf(false) }
            
            if (playlist?.playlist?.isEditable == true) {
                Box {
                    IconButton(
                        onClick = { showPlaylistMenu = true },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(OmniColors.OmniGlassMedium),
                    ) {
                        Icon(
                            painterResource(R.drawable.ic_more_vert),
                            contentDescription = "More options",
                            tint = OmniColors.TextSecondary,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                    androidx.compose.material3.DropdownMenu(
                        expanded = showPlaylistMenu,
                        onDismissRequest = { showPlaylistMenu = false },
                        modifier = Modifier.background(OmniColors.OmniBackgroundElevated),
                    ) {
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text("Rename playlist") },
                            onClick = {
                                showPlaylistMenu = false
                                showRenameDialog = true
                            },
                        )
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text("Delete playlist", color = OmniColors.Error) },
                            onClick = {
                                showPlaylistMenu = false
                                showDeleteDialog = true
                            },
                        )
                    }
                }
                
                if (showRenameDialog) {
                    var newName by remember { androidx.compose.runtime.mutableStateOf(playlist?.playlist?.name ?: "") }
                    androidx.compose.material3.AlertDialog(
                        onDismissRequest = { showRenameDialog = false },
                        title = { Text("Rename Playlist", fontWeight = FontWeight.Bold) },
                        text = {
                            androidx.compose.material3.OutlinedTextField(
                                value = newName,
                                onValueChange = { newName = it },
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
                                    if (newName.isNotBlank()) {
                                        viewModel.renamePlaylist(newName.trim())
                                        showRenameDialog = false
                                    }
                                },
                                enabled = newName.isNotBlank()
                            ) {
                                Text("Rename", color = if (newName.isNotBlank()) OmniColors.Hot else OmniColors.TextSecondary)
                            }
                        },
                        dismissButton = {
                            androidx.compose.material3.TextButton(onClick = { showRenameDialog = false }) {
                                Text("Cancel", color = OmniColors.TextPrimary)
                            }
                        },
                        containerColor = OmniColors.OmniBackgroundElevated,
                        titleContentColor = OmniColors.TextPrimary,
                    )
                }
                
                if (showDeleteDialog) {
                    androidx.compose.material3.AlertDialog(
                        onDismissRequest = { showDeleteDialog = false },
                        title = { Text("Delete Playlist?", fontWeight = FontWeight.Bold) },
                        text = { Text("This removes the playlist only. Songs and downloads will not be deleted.") },
                        confirmButton = {
                            androidx.compose.material3.TextButton(
                                onClick = {
                                    viewModel.deletePlaylist()
                                    showDeleteDialog = false
                                    onBack()
                                }
                            ) {
                                Text("Delete", color = OmniColors.Error)
                            }
                        },
                        dismissButton = {
                            androidx.compose.material3.TextButton(onClick = { showDeleteDialog = false }) {
                                Text("Cancel", color = OmniColors.TextPrimary)
                            }
                        },
                        containerColor = OmniColors.OmniBackgroundElevated,
                        titleContentColor = OmniColors.TextPrimary,
                        textContentColor = OmniColors.TextSecondary,
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(OmniColors.Hot.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painterResource(R.drawable.ic_list),
                        contentDescription = null,
                        tint = OmniColors.Hot,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }

        if (songs.isEmpty()) {
            EmptyPlaceholder(
                icon = R.drawable.ic_list,
                text = "No songs in this playlist yet",
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                itemsIndexed(
                    items = songs,
                    key = { _, item -> item.map.songId },
                    contentType = { _, _ -> "playlist_song" },
                ) { index, playlistSong ->
                    PlaylistSongRow(
                        index = index,
                        playlistSong = playlistSong,
                        onClick = { onPlaySong(playlistSong.song) },
                        onRemove = { viewModel.removeSong(playlistSong.song.id) }
                    )
                }
                item { Spacer(modifier = Modifier.height(88.dp)) }
            }
        }
    }
}

@Composable
private fun PlaylistSongRow(
    index: Int,
    playlistSong: PlaylistSong,
    onClick: () -> Unit,
    onRemove: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(OmniShapes.SM)
            .clickable(
                remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "${index + 1}",
            style = MaterialTheme.typography.bodyMedium,
            color = OmniColors.TextMuted,
            modifier = Modifier.width(28.dp),
        )
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(OmniShapes.SM)
                .background(OmniColors.GlassSurface),
        ) {
            val thumbnailUrl = playlistSong.song.song.thumbnailUrl
            if (thumbnailUrl != null) {
                AsyncImage(
                    model = thumbnailUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = playlistSong.song.song.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = OmniColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = playlistSong.song.artists.joinToString(", ") { it.name },
                style = MaterialTheme.typography.bodySmall,
                color = OmniColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        
        var menuExpanded by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
        val playerConnection = LocalPlayerConnection.current
        
        Box {
            IconButton(
                onClick = { menuExpanded = true },
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_more_vert),
                    contentDescription = "More options",
                    tint = OmniColors.TextSecondary,
                    modifier = Modifier.size(20.dp),
                )
            }
            
            TrackMenuProvider(
                showMenu = menuExpanded,
                onDismissMenu = { menuExpanded = false },
                mediaMetadata = playlistSong.song.toMediaMetadata(),
                onPlayNext = { playerConnection?.playNext(playlistSong.song.toMediaItem()) },
                onAddToQueue = { playerConnection?.addToQueue(playlistSong.song.toMediaItem()) },
                onRemoveFromPlaylist = onRemove
            )
        }
    }
}

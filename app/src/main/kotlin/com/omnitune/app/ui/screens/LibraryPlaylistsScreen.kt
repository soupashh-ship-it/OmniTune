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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.omnitune.app.R
import com.omnitune.app.db.entities.Playlist
import com.omnitune.app.db.entities.TagEntity
import com.omnitune.app.ui.component.EmptyPlaceholder
import com.omnitune.app.ui.component.OmniChrome
import com.omnitune.app.ui.theme.OmniColors
import com.omnitune.app.ui.theme.OmniShapes
import com.omnitune.app.ui.theme.OmniSpacing
import java.time.format.DateTimeFormatter
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun LibraryPlaylistsScreen(
    onBack: () -> Unit = {},
    onNavigateToPlaylist: (String) -> Unit = {},
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val allPlaylists by viewModel.playlists.collectAsStateWithLifecycle()
    val allTags by viewModel.allTags.collectAsStateWithLifecycle(initialValue = emptyList())

    var selectedTagId by remember { mutableStateOf<String?>(null) }
    var downloadedOnly by remember { mutableStateOf(false) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showManageFolders by remember { mutableStateOf(false) }
    var showFolderPickerFor by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OmniColors.OmniBackgroundBase)
            .background(OmniColors.BackgroundGradient)
            .statusBarsPadding()
            .padding(horizontal = OmniSpacing.section),
    ) {
        // ── Create playlist dialog ──────────────────────────────────
        if (showCreateDialog) {
            var playlistName by remember { mutableStateOf("") }
            val trimmedName = playlistName.trim()
            val nameError = when {
                playlistName.isNotBlank() && trimmedName.isBlank() -> "Playlist name cannot be empty"
                trimmedName.length > 80 -> "Playlist name must be 80 characters or fewer"
                else -> null
            }
            AlertDialog(
                onDismissRequest = { showCreateDialog = false },
                title = { Text("New Playlist", fontWeight = FontWeight.Bold) },
                text = {
                    OutlinedTextField(
                        value = playlistName,
                        onValueChange = { playlistName = it },
                        singleLine = true,
                        isError = nameError != null,
                        placeholder = { Text("Playlist name") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OmniColors.OmniAccentPrimary,
                            unfocusedBorderColor = OmniColors.OmniGlassBorderSubtle,
                            focusedTextColor = OmniColors.TextPrimary,
                            unfocusedTextColor = OmniColors.TextPrimary
                        )
                    )
                    if (nameError != null) {
                        Text(
                            text = nameError,
                            style = MaterialTheme.typography.bodySmall,
                            color = OmniColors.Error,
                            modifier = Modifier.padding(top = OmniSpacing.compact),
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (trimmedName.isNotBlank() && trimmedName.length <= 80) {
                                viewModel.createPlaylist(trimmedName) { playlistId ->
                                    onNavigateToPlaylist(playlistId)
                                }
                                showCreateDialog = false
                            }
                        },
                        enabled = trimmedName.isNotBlank() && trimmedName.length <= 80
                    ) {
                        Text("Create", color = if (trimmedName.isNotBlank() && trimmedName.length <= 80) OmniColors.Hot else OmniColors.TextSecondary)
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
        }

        // ── Manage folders dialog ───────────────────────────────────
        if (showManageFolders) {
            ManageFoldersDialog(
                tags = allTags,
                onDismiss = { showManageFolders = false },
                viewModel = viewModel,
            )
        }

        // ── Folder picker dialog ────────────────────────────────────
        showFolderPickerFor?.let { playlistId ->
            FolderPickerDialog(
                tags = allTags,
                playlistId = playlistId,
                onDismiss = { showFolderPickerFor = null },
                viewModel = viewModel,
            )
        }

        LibraryListHeader(
            title = "Playlists",
            subtitle = countLabel(allPlaylists.size, "playlist"),
            icon = R.drawable.ic_list,
            accent = OmniColors.Hot,
            actionIcon = R.drawable.ic_add,
            onAction = { showCreateDialog = true },
            onBack = onBack,
        )

        val showTagsInLibrary by com.omnitune.app.utils.rememberPreference(com.omnitune.app.constants.ShowTagsInLibraryKey, true)
        run {
            // ── Folder chips + manage button ────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LazyRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(OmniSpacing.compact),
                ) {
                    item {
                        AssistChip(
                            onClick = {
                                selectedTagId = null
                                downloadedOnly = false
                            },
                            label = { Text("All") },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = if (selectedTagId == null && !downloadedOnly)
                                    OmniColors.OmniAccentPrimary.copy(alpha = 0.2f)
                                else OmniColors.OmniGlassSubtle,
                                labelColor = if (selectedTagId == null && !downloadedOnly)
                                    OmniColors.OmniAccentPrimary
                                else OmniColors.TextSecondary,
                            ),
                        )
                    }
                    item {
                        AssistChip(
                            onClick = {
                                selectedTagId = null
                                downloadedOnly = true
                            },
                            label = { Text("Downloaded") },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = if (downloadedOnly) OmniColors.Downloaded.copy(alpha = 0.2f) else OmniColors.OmniGlassSubtle,
                                labelColor = if (downloadedOnly) OmniColors.Downloaded else OmniColors.TextSecondary,
                            ),
                        )
                    }
                    if (showTagsInLibrary) items(allTags, key = { it.id }) { tag ->
                        val tagColor = android.graphics.Color.parseColor(tag.color).let { Color(it) }
                        AssistChip(
                            onClick = {
                                downloadedOnly = false
                                selectedTagId = if (selectedTagId == tag.id) null else tag.id
                            },
                            label = { Text(tag.name) },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(tagColor)
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = if (selectedTagId == tag.id)
                                    tagColor.copy(alpha = 0.2f)
                                else OmniColors.OmniGlassSubtle,
                                labelColor = if (selectedTagId == tag.id)
                                    tagColor
                                else OmniColors.TextSecondary,
                            ),
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(OmniSpacing.small))

        val visiblePlaylists = allPlaylists.filter { playlist ->
            !downloadedOnly || playlist.playlist.isDownloaded
        }

        // ── Playlist list ───────────────────────────────────────────
        if (visiblePlaylists.isEmpty()) {
            LibraryEmptyState(
                icon = R.drawable.ic_list,
                text = if (downloadedOnly) "No downloaded playlists yet" else "No playlists in your library yet",
            )
        } else {
            // Show all playlists with the folder picker available per row
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(OmniSpacing.small),
            ) {
                items(
                    items = visiblePlaylists,
                    key = { it.id },
                    contentType = { "playlist" },
                ) { playlist ->
                    PlaylistRow(
                        playlist = playlist,
                        onClick = { onNavigateToPlaylist(playlist.id) },
                        onAssignFolder = { showFolderPickerFor = playlist.id },
                    )
                }
                item(contentType = "bottom-spacer") { Spacer(modifier = Modifier.height(OmniChrome.BottomContentPadding)) }
            }
        }
    }
}

@Composable
private fun ManageFoldersDialog(
    tags: List<TagEntity>,
    onDismiss: () -> Unit,
    viewModel: LibraryViewModel,
) {
    var newFolderName by remember { mutableStateOf("") }
    var editingTag by remember { mutableStateOf<TagEntity?>(null) }
    var editingName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Manage Folders", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                if (editingTag == null) {
                    // Create new folder
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        OutlinedTextField(
                            value = newFolderName,
                            onValueChange = { newFolderName = it },
                            singleLine = true,
                            placeholder = { Text("Folder name") },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = OmniColors.OmniAccentPrimary,
                                unfocusedBorderColor = OmniColors.OmniGlassBorderSubtle,
                                focusedTextColor = OmniColors.TextPrimary,
                                unfocusedTextColor = OmniColors.TextPrimary,
                            ),
                        )
                        Spacer(modifier = Modifier.width(OmniSpacing.compact))
                        TextButton(
                            onClick = {
                                if (newFolderName.isNotBlank()) {
                                    val color = TagEntity.DEFAULT_COLORS[tags.size % TagEntity.DEFAULT_COLORS.size]
                                    viewModel.createTag(newFolderName.trim(), color)
                                    newFolderName = ""
                                }
                            },
                            enabled = newFolderName.isNotBlank(),
                        ) {
                            Text("Add", color = if (newFolderName.isNotBlank()) OmniColors.Hot else OmniColors.TextSecondary)
                        }
                    }
                    Spacer(modifier = Modifier.height(OmniSpacing.medium))
                } else {
                    // Edit folder
                    OutlinedTextField(
                        value = editingName,
                        onValueChange = { editingName = it },
                        singleLine = true,
                        label = { Text("Folder name") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OmniColors.OmniAccentPrimary,
                            unfocusedBorderColor = OmniColors.OmniGlassBorderSubtle,
                            focusedTextColor = OmniColors.TextPrimary,
                            unfocusedTextColor = OmniColors.TextPrimary,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(OmniSpacing.medium))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(OmniSpacing.compact),
                    ) {
                        TextButton(onClick = {
                            val tag = editingTag ?: return@TextButton
                            viewModel.updateTag(tag.copy(name = editingName.trim()))
                            editingTag = null
                        }) {
                            Text("Save", color = OmniColors.OmniAccentPrimary)
                        }
                        TextButton(onClick = {
                            val tag = editingTag ?: return@TextButton
                            viewModel.deleteTag(tag)
                            editingTag = null
                        }) {
                            Text("Delete", color = OmniColors.Error)
                        }
                        TextButton(onClick = { editingTag = null }) {
                            Text("Cancel", color = OmniColors.TextSecondary)
                        }
                    }
                    Spacer(modifier = Modifier.height(OmniSpacing.small))
                }

                // Existing folders list
                if (tags.isEmpty() && editingTag == null) {
                    Text(
                        text = "No folders yet. Create one above to organize your playlists.",
                        style = MaterialTheme.typography.bodySmall,
                        color = OmniColors.TextSecondary,
                    )
                } else {
                    tags.forEach { tag ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    editingTag = tag
                                    editingName = tag.name
                                }
                                .padding(vertical = OmniSpacing.small),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(parseColor(tag.color))
                            )
                            Spacer(modifier = Modifier.width(OmniSpacing.small))
                            Text(
                                text = tag.name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = OmniColors.TextPrimary,
                                modifier = Modifier.weight(1f),
                            )
                            Icon(
                                painterResource(R.drawable.ic_more_vert),
                                contentDescription = "Edit",
                                tint = OmniColors.TextTertiary,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Done", color = OmniColors.TextPrimary)
            }
        },
        containerColor = OmniColors.OmniBackgroundElevated,
        titleContentColor = OmniColors.TextPrimary,
    )
}

@Composable
private fun FolderPickerDialog(
    tags: List<TagEntity>,
    playlistId: String,
    onDismiss: () -> Unit,
    viewModel: LibraryViewModel,
) {
    var selectedTagIds by remember { mutableStateOf(setOf<String>()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Assign Folder", fontWeight = FontWeight.Bold) },
        text = {
            if (tags.isEmpty()) {
                Text(
                    text = "No folders yet. Create one in playlist settings first.",
                    style = MaterialTheme.typography.bodySmall,
                    color = OmniColors.TextSecondary,
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(OmniSpacing.small)) {
                    tags.forEach { tag ->
                        val isSelected = tag.id in selectedTagIds
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedTagIds = if (isSelected) {
                                        selectedTagIds - tag.id
                                    } else {
                                        selectedTagIds + tag.id
                                    }
                                }
                                .clip(OmniShapes.Medium)
                                .background(if (isSelected) parseColor(tag.color).copy(alpha = 0.12f) else Color.Transparent)
                                .padding(OmniSpacing.small),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(parseColor(tag.color))
                            )
                            Spacer(modifier = Modifier.width(OmniSpacing.medium))
                            Text(
                                text = tag.name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = OmniColors.TextPrimary,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                tags.forEach { tag ->
                    if (tag.id in selectedTagIds) {
                        viewModel.assignPlaylistTag(playlistId, tag.id)
                    } else {
                        viewModel.removePlaylistTag(playlistId, tag.id)
                    }
                }
                onDismiss()
            }) {
                Text("Save", color = OmniColors.Hot)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = OmniColors.TextPrimary)
            }
        },
        containerColor = OmniColors.OmniBackgroundElevated,
        titleContentColor = OmniColors.TextPrimary,
    )
}

@Composable
private fun PlaylistRow(
    playlist: Playlist,
    onClick: () -> Unit,
    onAssignFolder: () -> Unit,
) {
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
            PlaylistArtwork(playlist = playlist)
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
                text = playlist.playlist.lastUpdateTime?.let {
                    "${countLabel(playlist.songCount, "song")} • Updated ${it.format(DateTimeFormatter.ofPattern("MMM d"))}"
                } ?: countLabel(playlist.songCount, "song"),
                style = MaterialTheme.typography.bodySmall,
                color = OmniColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(modifier = Modifier.width(OmniSpacing.compact))
        IconButton(
            onClick = onAssignFolder,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(OmniColors.OmniGlassMedium),
        ) {
            Icon(
                painterResource(R.drawable.ic_more_vert),
                contentDescription = "Assign folder",
                tint = OmniColors.TextTertiary,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun LibraryListHeader(
    title: String,
    subtitle: String,
    icon: Int,
    accent: Color,
    actionIcon: Int? = null,
    onAction: (() -> Unit)? = null,
    onBack: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = OmniSpacing.medium, bottom = OmniSpacing.medium),
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
                title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = OmniColors.TextPrimary,
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = OmniColors.TextSecondary,
            )
        }
        if (actionIcon != null && onAction != null) {
            IconButton(
                onClick = onAction,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.16f)),
            ) {
                Icon(
                    painterResource(actionIcon),
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(22.dp),
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painterResource(icon),
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}

@Composable
private fun LibraryEmptyState(icon: Int, text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(OmniShapes.ExtraLarge)
            .background(OmniColors.OmniGlassSubtle)
            .border(
                BorderStroke(1.dp, OmniColors.OmniGlassBorderSubtle),
                OmniShapes.ExtraLarge,
            )
            .padding(OmniSpacing.screen),
        contentAlignment = Alignment.Center,
    ) {
        EmptyPlaceholder(icon = icon, text = text)
    }
}

@Composable
private fun PlaylistArtwork(playlist: Playlist) {
    val thumbnails = playlist.thumbnails.take(4)
    if (thumbnails.isEmpty()) {
        Icon(
            painterResource(R.drawable.ic_list),
            contentDescription = null,
            tint = OmniColors.Hot,
            modifier = Modifier.size(24.dp),
        )
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (thumbnails.size == 1) {
            AsyncImage(
                model = thumbnails.first(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            val positions = listOf(
                Alignment.TopStart,
                Alignment.TopEnd,
                Alignment.BottomStart,
                Alignment.BottomEnd,
            )
            thumbnails.forEachIndexed { index, thumbnail ->
                AsyncImage(
                    model = thumbnail,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .align(positions[index])
                        .size(29.dp),
                )
            }
        }
    }
}

private fun parseColor(hex: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (_: Exception) {
        Color(0xFFFF6B6B)
    }
}

private fun countLabel(count: Int, singular: String): String {
    val noun = if (count == 1) singular else "${singular}s"
    return "$count $noun"
}

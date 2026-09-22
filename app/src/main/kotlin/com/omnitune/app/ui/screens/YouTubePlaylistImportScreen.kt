package com.omnitune.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.omnitune.app.ui.theme.SquircleShape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YouTubePlaylistImportScreen(
    onBackClick: () -> Unit,
    viewModel: YouTubePlaylistImportViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val allSelected = state.playlists.isNotEmpty() && state.selectedIds.size == state.playlists.size

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("YouTube Music playlists", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick, enabled = !state.isImporting) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Text(
                    text = "Choose the playlists to copy into your OmniTune library. Existing imports are updated instead of duplicated.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            state.progressLabel?.let { label ->
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(label, style = MaterialTheme.typography.bodySmall)
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }
            }

            state.errorMessage?.let { message ->
                item {
                    Card(
                        shape = SquircleShape,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(message, color = MaterialTheme.colorScheme.onErrorContainer)
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(onClick = viewModel::loadPlaylists) { Text("Retry") }
                        }
                    }
                }
            }

            if (state.isLoading) {
                item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
            } else if (state.playlists.isEmpty() && state.errorMessage == null) {
                item {
                    Text(
                        text = "No saved YouTube Music playlists were found for this account.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = allSelected,
                            onCheckedChange = viewModel::selectAll,
                            enabled = !state.isImporting,
                        )
                        Text("Select all", modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                        Text(
                            text = "${state.selectedIds.size} selected",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                items(state.playlists, key = YouTubeLibraryPlaylist::id) { playlist ->
                    Card(
                        shape = SquircleShape,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 6.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = playlist.id in state.selectedIds,
                                onCheckedChange = { selected -> viewModel.setSelected(playlist.id, selected) },
                                enabled = !state.isImporting,
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = playlist.title,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                playlist.songCountText?.let { count ->
                                    Text(
                                        text = count,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Button(
                        onClick = viewModel::importSelected,
                        enabled = state.selectedIds.isNotEmpty() && !state.isImporting,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Import selected playlists")
                    }
                }
            }

            state.completedSummary?.let { summary ->
                item {
                    Text(
                        text = summary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

/*
 * Velune - by Nikhil
 * Nikhil
 * Licensed Under GPL-3.0
 */


package com.omnitune.app.ui.screens.playlist

import com.omnitune.app.ui.component.OmniTuneLoader
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.omnitune.app.LocalDatabase
import com.omnitune.app.LocalPlayerConnection
import com.omnitune.app.R
import com.omnitune.app.extensions.toMediaItem
import com.omnitune.app.extensions.togglePlayPause
import com.omnitune.innertube.models.SongItem
import com.omnitune.app.playback.queues.ListQueue
import com.omnitune.app.ui.component.DefaultDialog
import com.omnitune.app.ui.component.OmniSectionHeader
import com.omnitune.app.ui.component.OmniMusicRow
import com.omnitune.app.ui.screens.PlaylistDetailViewModel

@Composable
fun PlaylistSuggestionsSection(
    modifier: Modifier = Modifier,
    viewModel: PlaylistDetailViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val coroutineScope = rememberCoroutineScope()
    val playerConnection = LocalPlayerConnection.current
    val isPlaying by playerConnection?.isPlaying?.collectAsState() ?: androidx.compose.runtime.mutableStateOf(false)
    val mediaMetadata by playerConnection?.mediaMetadata?.collectAsState() ?: androidx.compose.runtime.mutableStateOf(null)
    
    val playlistSuggestions by viewModel.playlistSuggestions.collectAsState()
    val isLoading by viewModel.isLoadingSuggestions.collectAsState()
    
    // State for duplicate check dialog
    var showDuplicateDialog by remember { mutableStateOf(false) }
    var songToCheck by remember { mutableStateOf<SongItem?>(null) }
    
    val currentSuggestions = playlistSuggestions
    if (currentSuggestions == null && !isLoading) return
    if (currentSuggestions != null && currentSuggestions.items.isEmpty() && !isLoading) return

    // Duplicate Check Dialog
    if (showDuplicateDialog && songToCheck != null) {
        val song = songToCheck!!
        DefaultDialog(
            title = { Text("Duplicates") },
            buttons = {
                TextButton(
                    onClick = {
                        showDuplicateDialog = false
                        songToCheck = null
                    }
                ) {
                    Text(stringResource(android.R.string.cancel))
                }
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            // Add to current playlist anyway
                            val browseId = viewModel.playlist.value?.playlist?.browseId
                            viewModel.addSongToPlaylist(song, browseId)
                            
                            val playlistName = viewModel.playlist.value?.playlist?.name
                            val message = if (playlistName != null) {
                                "Added to $playlistName"
                            } else {
                                "Added to playlist"
                            }
                        }
                        showDuplicateDialog = false
                        songToCheck = null
                    }
                ) {
                    Text("Add Anyway")
                }
            },
            onDismiss = {
                showDuplicateDialog = false
                songToCheck = null
            }
        ) {
            Text(text = "This song is already in the playlist.")
        }
    }

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OmniSectionHeader(
                title = "You Might Like",
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        currentSuggestions?.let { suggestions ->
            suggestions.items.forEach { item ->
                val songItem = item as? SongItem ?: return@forEach
                OmniMusicRow(
                    title = songItem.title,
                    subtitle = songItem.artists.joinToString { it.name },
                    thumbnailUrl = songItem.thumbnail,
                    trailing = {
                        IconButton(
                            onClick = { 
                                // Check for duplicates in current playlist first
                                songToCheck = songItem
                                coroutineScope.launch {
                                    val isDuplicate = withContext(Dispatchers.IO) {
                                        val duplicates = database.playlistDuplicates(
                                            viewModel.getPlaylistId,
                                            listOf(songItem.id)
                                        )
                                        duplicates.isNotEmpty()
                                    }
                                    
                                    if (isDuplicate) {
                                        showDuplicateDialog = true
                                    } else {
                                        // No duplicate, add directly
                                        val browseId = viewModel.playlist.value?.playlist?.browseId
                                        val success = viewModel.addSongToPlaylist(
                                            song = songItem,
                                            browseId = browseId
                                        )
                                        
                                        if (success) {
                                            val playlistName = viewModel.playlist.value?.playlist?.name
                                            val message = if (playlistName != null) {
                                                "Added to $playlistName"
                                            } else {
                                                "Added to playlist"
                                            }
                                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Unknown error", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_add),
                                contentDescription = "Add to Playlist"
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium),
                    onClick = {
                        if (playerConnection != null) {
                            if (item.id == mediaMetadata?.id) {
                                playerConnection.player.togglePlayPause()
                            } else {
                                val songItems = suggestions.items.filterIsInstance<SongItem>()
                                val startIndex = songItems.indexOfFirst { it.id == item.id }
                                if (startIndex != -1) {
                                    playerConnection.playQueue(
                                        ListQueue(
                                            title = "You Might Like",
                                            items = songItems.map { it.toMediaItem() },
                                            startIndex = startIndex
                                        )
                                    )
                                }
                            }
                        }
                    }
                )
            }
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    OmniTuneLoader(size = 24.dp)
                } else {
                    TextButton(
                        onClick = { viewModel.resetAndLoadPlaylistSuggestions() }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "Refresh Suggestions")
                        }
                    }
                }
            }
        }
    }
}

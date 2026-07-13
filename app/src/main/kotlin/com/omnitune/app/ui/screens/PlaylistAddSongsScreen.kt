/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.omnitune.app.R
import com.omnitune.app.models.AddPlaylistSongResult
import com.omnitune.app.ui.component.EmptyPlaceholder
import com.omnitune.app.ui.component.OmniMusicRow
import com.omnitune.app.ui.component.OmniSectionHeader
import com.omnitune.app.ui.component.OmniTuneLoader
import com.omnitune.app.ui.screens.playlist.PlaylistSuggestionsSection
import com.omnitune.app.ui.theme.OmniColors
import com.omnitune.app.ui.theme.OmniShapes
import com.omnitune.app.ui.theme.OmniSpacing
import com.omnitune.innertube.YouTube
import com.omnitune.innertube.models.SongItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun PlaylistAddSongsScreen(
    onBack: () -> Unit = {},
    viewModel: PlaylistDetailViewModel = hiltViewModel(),
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val playlist by viewModel.playlist.collectAsState()
    val songs by viewModel.songs.collectAsState()
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<SongItem>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    val addingSongIds = remember { mutableStateMapOf<String, Boolean>() }
    val playlistName = playlist?.playlist?.name ?: "playlist"
    val browseId = playlist?.playlist?.browseId
    val existingIds = remember(songs) { songs.map { it.song.id }.toSet() }

    LaunchedEffect(query) {
        val searchQuery = query.trim()
        if (searchQuery.length < 2) {
            results = emptyList()
            loading = false
            return@LaunchedEffect
        }

        loading = true
        delay(350)
        results = withContext(Dispatchers.IO) {
            YouTube.search(searchQuery, YouTube.SearchFilter.FILTER_SONG)
                .getOrNull()
                ?.items
                ?.filterIsInstance<SongItem>()
                ?.distinctBy { it.id }
                ?.take(40)
                .orEmpty()
        }
        loading = false
    }

    fun showResult(result: AddPlaylistSongResult) {
        val message = when (result) {
            AddPlaylistSongResult.Added -> "Added to $playlistName"
            AddPlaylistSongResult.Duplicate -> "Already in playlist"
            is AddPlaylistSongResult.Failed -> result.message ?: "Could not add song"
        }
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OmniColors.OmniBackgroundBase)
            .background(OmniColors.BackgroundGradient)
            .padding(horizontal = OmniSpacing.section),
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
                    .background(OmniColors.OmniGlassMedium),
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
                    text = "Add songs",
                    style = MaterialTheme.typography.headlineMedium,
                    color = OmniColors.TextPrimary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
                Text(
                    text = playlistName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = OmniColors.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            singleLine = true,
            placeholder = { Text("Search songs") },
            leadingIcon = {
                Icon(
                    painterResource(R.drawable.ic_search),
                    contentDescription = null,
                )
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = OmniColors.OmniAccentPrimary,
                unfocusedBorderColor = OmniColors.OmniGlassBorderSubtle,
                focusedTextColor = OmniColors.TextPrimary,
                unfocusedTextColor = OmniColors.TextPrimary,
            ),
            shape = OmniShapes.Medium,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(OmniSpacing.medium))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(OmniSpacing.compact),
        ) {
            when {
                loading -> {
                    item(contentType = "loading") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            OmniTuneLoader(size = 28.dp)
                        }
                    }
                }

                query.trim().length >= 2 && results.isEmpty() -> {
                    item(contentType = "empty_results") {
                        EmptyPlaceholder(icon = R.drawable.ic_search, text = "No results found")
                    }
                }

                query.trim().length >= 2 -> {
                    item(contentType = "search_header") {
                        OmniSectionHeader(title = "Search results")
                    }
                    items(results, key = { it.id }, contentType = { "song_result" }) { song ->
                        val alreadyAdded = song.id in existingIds
                        val adding = addingSongIds[song.id] == true
                        OmniMusicRow(
                            title = song.title,
                            subtitle = song.artists.joinToString { it.name },
                            thumbnailUrl = song.thumbnail,
                            trailing = {
                                TextButton(
                                    onClick = {
                                        if (!adding && !alreadyAdded) {
                                            coroutineScope.launch {
                                                addingSongIds[song.id] = true
                                                val result = viewModel.addSongToPlaylist(song, browseId)
                                                addingSongIds.remove(song.id)
                                                showResult(result)
                                            }
                                        }
                                    },
                                    enabled = !adding && !alreadyAdded,
                                    colors = ButtonDefaults.textButtonColors(
                                        contentColor = if (alreadyAdded) {
                                            OmniColors.TextMuted
                                        } else {
                                            OmniColors.Hot
                                        },
                                    ),
                                ) {
                                    if (adding) {
                                        OmniTuneLoader(size = 18.dp)
                                    } else {
                                        Text(if (alreadyAdded) "Added" else "Add")
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                else -> {
                    item(contentType = "prompt") {
                        Text(
                            text = "Search for a song, then tap Add. Songs are appended to the end of this playlist.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = OmniColors.TextSecondary,
                            modifier = Modifier.padding(bottom = OmniSpacing.small),
                        )
                    }
                    item(contentType = "suggestions") {
                        PlaylistSuggestionsSection(viewModel = viewModel)
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(96.dp)) }
        }
    }
}

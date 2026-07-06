package com.omnitune.app.ui.screens

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.omnitune.innertube.YouTube
import com.omnitune.innertube.models.AlbumItem
import com.omnitune.innertube.models.SongItem
import com.omnitune.innertube.pages.ArtistPage
import com.omnitune.app.ui.component.EmptyPlaceholder
import com.omnitune.app.ui.component.OmniTuneLoader
import com.omnitune.app.utils.formatDurationSeconds

import com.omnitune.app.LocalPlayerConnection
import com.omnitune.app.extensions.toMediaItem
import com.omnitune.app.models.toMediaMetadata
import com.omnitune.app.ui.component.TrackMenuProvider
import androidx.hilt.navigation.compose.hiltViewModel
import com.omnitune.app.R
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistScreen(
    artistId: String,
    onBack: () -> Unit = {},
    onPlaySong: (SongItem) -> Unit = {},
    onNavigateToAlbum: (String) -> Unit = {},
    viewModel: ArtistDetailViewModel = hiltViewModel(),
) {
    var artistPage by remember { mutableStateOf<ArtistPage?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val isBookmarked by viewModel.isBookmarked.collectAsState()

    LaunchedEffect(artistId) {
        Timber.tag("ArtistNav").i("ArtistScreen LaunchedEffect: artistId=%s", artistId)
        isLoading = true
        viewModel.loadArtist(artistId)
        YouTube.artist(artistId).fold(
            onSuccess = { page -> artistPage = page },
            onFailure = { e -> error = e.message }
        )
        isLoading = false
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(artistPage?.artist?.title ?: "Artist") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        painter = painterResource(com.omnitune.app.R.drawable.ic_arrow_back),
                        contentDescription = "Back",
                    )
                }
            },
            actions = {
                IconButton(onClick = { viewModel.toggleBookmark(artistId) }) {
                    Icon(
                        painter = painterResource(
                            if (isBookmarked) R.drawable.ic_favorite
                            else R.drawable.ic_favorite_border
                        ),
                        contentDescription = if (isBookmarked) "Unfollow artist" else "Follow artist",
                        tint = if (isBookmarked) MaterialTheme.colorScheme.primary
                               else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
            ),
        )

        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    OmniTuneLoader(size = 48.dp)
                }
            }
            error != null -> {
                EmptyPlaceholder(
                    icon = com.omnitune.app.R.drawable.ic_artist,
                    text = error ?: "Failed to load artist",
                )
            }
            artistPage != null -> {
                val page = artistPage!!

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                ) {
                    item {
                        Spacer(modifier = Modifier.height(24.dp))

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            AsyncImage(
                                model = page.artist.thumbnail,
                                contentDescription = page.artist.title,
                                modifier = Modifier
                                    .size(180.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop,
                            )
                            
                            Spacer(modifier = Modifier.height(24.dp))

                            Text(
                                text = page.artist.title,
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center,
                            )
                            
                            page.description?.let { desc ->
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = desc,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                                Text(
                                    text = "more",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .padding(top = 4.dp)
                                        .clickable { /* TODO: show full bio */ }
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            // Stats Row
                            val songCount = page.sections.find { it.title.contains("Song", ignoreCase = true) }?.items?.size ?: 0
                            val albumCount = page.sections.find { it.title.contains("Album", ignoreCase = true) }?.items?.size ?: 0
                            
                            if (songCount > 0 || albumCount > 0) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    if (songCount > 0) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(text = "$songCount+", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                            Text(text = "Songs", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                    if (albumCount > 0) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(text = "$albumCount+", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                            Text(text = "Albums", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(24.dp))
                            }
                            
                            // Action Buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.toggleBookmark(artistId) },
                                    modifier = Modifier.weight(1f).height(50.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isBookmarked) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.errorContainer,
                                        contentColor = if (isBookmarked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onErrorContainer
                                    )
                                ) {
                                    Icon(painter = painterResource(if (isBookmarked) com.omnitune.app.R.drawable.ic_close else com.omnitune.app.R.drawable.ic_add), contentDescription = null, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(if (isBookmarked) "Subscribed" else "Subscribe")
                                }
                                
                                Button(
                                    onClick = { 
                                        val allSongs = page.sections.flatMap { it.items }.filterIsInstance<SongItem>()
                                        if (allSongs.isNotEmpty()) {
                                            val shuffled = allSongs.shuffled()
                                            // Actually would use playerConnection.playQueue here, handled by parent ideally
                                            // For now just play the first section
                                        }
                                    },
                                    modifier = Modifier.weight(1f).height(50.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                ) {
                                    Icon(painter = painterResource(com.omnitune.app.R.drawable.ic_shuffle), contentDescription = null, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Shuffle")
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            OutlinedButton(
                                onClick = { /* Start Radio */ },
                                modifier = Modifier.fillMaxWidth().height(50.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Icon(painter = painterResource(com.omnitune.app.R.drawable.ic_insights), contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurface)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Radio", color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(32.dp))
                    }

                    if (page.sections.isEmpty()) {
                        item {
                            EmptyPlaceholder(
                                icon = com.omnitune.app.R.drawable.ic_artist,
                                text = "No content available",
                            )
                        }
                    } else {
                        page.sections.forEach { section ->
                            item {
                                Spacer(modifier = Modifier.height(24.dp))
                                Text(
                                    text = section.title,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                            }

                            if (section.items.firstOrNull() is SongItem) {
                                items(
                                    items = section.items,
                                    key = { item -> "artist_section_${section.title}_${item.id}" },
                                    contentType = { "artist-song" },
                                ) { item ->
                                    if (item is SongItem) {
                                        ArtistSongRow(
                                            song = item,
                                            onClick = { onPlaySong(item) },
                                        )
                                        HorizontalDivider(
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                        )
                                    }
                                }
                            } else {
                                item {
                                    androidx.compose.foundation.lazy.LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 16.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        items(section.items) { item ->
                                            if (item is AlbumItem) {
                                                val subtitleText = item.year?.toString() ?: item.artists?.joinToString(", ") { it.name } ?: ""
                                                com.omnitune.app.ui.component.GridItem(
                                                    title = item.title,
                                                    subtitle = subtitleText,
                                                    thumbnailContent = {
                                                        AsyncImage(
                                                            model = item.thumbnail,
                                                            contentDescription = null,
                                                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)),
                                                            contentScale = ContentScale.Crop,
                                                        )
                                                    },
                                                    modifier = Modifier.width(160.dp).clickable { onNavigateToAlbum(item.browseId) }
                                                )
                                            } else if (item is com.omnitune.innertube.models.ArtistItem) {
                                                com.omnitune.app.ui.component.GridItem(
                                                    title = item.title,
                                                    subtitle = "Artist",
                                                    thumbnailContent = {
                                                        AsyncImage(
                                                            model = item.thumbnail,
                                                            contentDescription = null,
                                                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                                                            contentScale = ContentScale.Crop,
                                                        )
                                                    },
                                                    modifier = Modifier.width(160.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }
        }
    }
}

@Composable
private fun ArtistSongRow(
    song: SongItem,
    onClick: () -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = song.thumbnail,
            contentDescription = null,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(6.dp)),
            contentScale = ContentScale.Crop,
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = song.artists.joinToString(", ") { it.name },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        song.duration?.let { duration ->
            Text(
                text = formatDurationSeconds(duration.toLong()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        var menuExpanded by remember { mutableStateOf(false) }
        val playerConnection = LocalPlayerConnection.current
        
        Box {
            IconButton(
                onClick = { menuExpanded = true },
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    painter = painterResource(com.omnitune.app.R.drawable.ic_more_vert),
                    contentDescription = "More options",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
            
            TrackMenuProvider(
                showMenu = menuExpanded,
                onDismissMenu = { menuExpanded = false },
                mediaMetadata = song.toMediaMetadata(),
                onPlayNext = { playerConnection?.playNext(song.toMediaItem()) },
                onAddToQueue = { playerConnection?.addToQueue(song.toMediaItem()) }
            )
        }
    }
}

@Composable
private fun ArtistAlbumRow(
    album: AlbumItem,
    onClick: () -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = album.thumbnail,
            contentDescription = null,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(6.dp)),
            contentScale = ContentScale.Crop,
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = album.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            album.year?.let { year ->
                Text(
                    text = year.toString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}



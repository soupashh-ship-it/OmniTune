/*
 * Adapted from SuvMusic (https://github.com/SuvojeetDev/SuvMusic)
 * Copyright (c) SuvMusic contributors
 * Licensed under GPL-3.0
 */

package com.omnitune.app.ui.screens

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.omnitune.app.LocalDownloadUtil
import com.omnitune.app.LocalPlayerConnection
import com.omnitune.app.models.Album
import com.omnitune.app.models.Artist
import com.omnitune.app.models.ArtistPreview
import com.omnitune.app.models.Playlist
import com.omnitune.app.models.Song
import com.omnitune.app.models.toMediaItem
import com.omnitune.app.ui.component.AddToPlaylistSheet
import com.omnitune.app.ui.component.BounceButton
import com.omnitune.app.ui.component.CreatePlaylistDialog
import com.omnitune.app.ui.component.DominantColors
import com.omnitune.app.ui.component.MultipleArtistsDialog
import com.omnitune.app.ui.component.PremiumLoadingScreen
import com.omnitune.app.ui.component.SongMenuBottomSheet
import com.omnitune.app.ui.component.bounceClick
import com.omnitune.app.ui.component.rememberDominantColors
import com.omnitune.app.ui.navigation.LocalRouteChromeInsets
import com.omnitune.app.ui.theme.PillShape
import com.omnitune.app.ui.theme.SquircleShape
import com.omnitune.app.ui.utils.ImageUtils
import com.omnitune.app.ui.utils.dpadFocusable
import com.omnitune.app.utils.TimeUtil
import java.util.Locale
import kotlin.math.min

@Composable
fun ArtistScreen(
    onBackClick: () -> Unit,
    onSongClick: (List<Song>, Int) -> Unit,
    onAlbumClick: (Album) -> Unit,
    onSeeAllAlbumsClick: () -> Unit = {},
    onSeeAllSinglesClick: () -> Unit = {},
    onArtistClick: (ArtistPreview) -> Unit = {},
    onArtistIdClick: (String) -> Unit = {},
    onPlaylistClick: (Playlist) -> Unit = {},
    onStartRadio: (List<Song>) -> Unit = {},
    viewModel: ArtistViewModel = hiltViewModel(),
    playlistViewModel: PlaylistManagementViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val playlistMgmtState by playlistViewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberLazyListState()
    val isDark = isSystemInDarkTheme()
    val context = LocalContext.current
    val playerConnection = LocalPlayerConnection.current
    val downloadUtil = LocalDownloadUtil.current
    val chromeInsets = LocalRouteChromeInsets.current
    var showSongMenu by remember { mutableStateOf(false) }
    var selectedSong by remember { mutableStateOf<Song?>(null) }

    val artist = uiState.artist
    val dominantColors = rememberDominantColors(
        imageUrl = artist?.thumbnailUrl,
        isDarkTheme = isDark
    )

    if (uiState.showMultipleArtistsDialog) {
        MultipleArtistsDialog(
            artists = uiState.currentArtistCredits,
            onArtistClick = onArtistIdClick,
            onDismiss = { viewModel.toggleMultipleArtistsDialog(false) },
            dominantColors = dominantColors
        )
    }

    val headerAlpha by remember {
        derivedStateOf {
            val firstVisibleItemIndex = scrollState.firstVisibleItemIndex
            val firstVisibleItemScrollOffset = scrollState.firstVisibleItemScrollOffset
            if (firstVisibleItemIndex == 0) {
                min(1f, firstVisibleItemScrollOffset / 400f)
            } else {
                1f
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when {
            uiState.isLoading -> {
                PremiumLoadingScreen(
                    thumbnailUrl = null,
                    onBackClick = onBackClick
                )
            }
            uiState.error != null -> {
                ArtistErrorView(
                    error = uiState.error!!,
                    onRetry = { viewModel.loadArtist() },
                    onBackClick = onBackClick
                )
            }
            uiState.artist != null -> {
                val currentArtist = uiState.artist!!
                val surfaceColor = MaterialTheme.colorScheme.surface

                LazyColumn(
                    state = scrollState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = chromeInsets.contentBottomPadding)
                ) {
                    // Immersive Header
                    item {
                        ImmersiveArtistHeader(
                            artist = currentArtist,
                            dominantColors = dominantColors,
                            onPlayAll = {
                                if (currentArtist.songs.isNotEmpty()) {
                                    onSongClick(currentArtist.songs, 0)
                                }
                            },
                            onShuffle = {
                                if (currentArtist.songs.isNotEmpty()) {
                                    onSongClick(currentArtist.songs.shuffled(), 0)
                                }
                            },
                            onSubscribe = viewModel::toggleSubscribe,
                            isSubscribed = currentArtist.isSubscribed,
                            isSubscribing = uiState.isSubscribing,
                            onStartRadio = { viewModel.startRadio(onStartRadio) }
                        )
                    }

                    // Latest Release
                    val latestRelease = (currentArtist.albums + currentArtist.singles)
                        .maxByOrNull { it.year ?: "0" }
                    if (latestRelease != null) {
                        item {
                            Spacer(modifier = Modifier.height(20.dp))
                            LatestReleaseSection(
                                artistName = currentArtist.name,
                                album = latestRelease,
                                isSingle = currentArtist.singles.contains(latestRelease),
                                dominantColors = dominantColors,
                                onClick = { onAlbumClick(latestRelease) }
                            )
                        }
                    }

                    // Top Songs
                    if (currentArtist.songs.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(32.dp))
                            SectionHeader(title = "Top Songs", dominantColors = dominantColors)
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        itemsIndexed(currentArtist.songs.take(5), key = { index, song -> "${song.id}_$index" }) { index, song ->
                            TopSongRow(
                                index = index + 1,
                                song = song,
                                dominantColors = dominantColors,
                                onClick = { onSongClick(currentArtist.songs, index) },
                                onArtistClick = {
                                    viewModel.fetchArtistCreditsAndShow(song.artist)
                                },
                                onMoreClick = {
                                    selectedSong = song
                                    showSongMenu = true
                                }
                            )
                        }
                    }

                    // Discography - Albums
                    if (currentArtist.albums.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(32.dp))
                            SectionHeader(
                                title = "Albums",
                                showSeeAll = currentArtist.albums.size > 5,
                                dominantColors = dominantColors,
                                onSeeAllClick = onSeeAllAlbumsClick
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 20.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(currentArtist.albums, key = { it.id }) { album ->
                                    ArtistContentCard(
                                        title = album.title,
                                        subtitle = album.year,
                                        imageUrl = album.thumbnailUrl,
                                        dominantColors = dominantColors,
                                        onClick = { onAlbumClick(album) }
                                    )
                                }
                            }
                        }
                    }

                    // Discography - Singles & EPs
                    if (currentArtist.singles.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(32.dp))
                            SectionHeader(
                                title = "Singles & EPs",
                                showSeeAll = currentArtist.singles.size > 5,
                                dominantColors = dominantColors,
                                onSeeAllClick = onSeeAllSinglesClick
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 20.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(currentArtist.singles, key = { it.id }) { single ->
                                    ArtistContentCard(
                                        title = single.title,
                                        subtitle = single.year,
                                        imageUrl = single.thumbnailUrl,
                                        dominantColors = dominantColors,
                                        onClick = { onAlbumClick(single) }
                                    )
                                }
                            }
                        }
                    }

                    // Videos
                    if (currentArtist.videos.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(32.dp))
                            SectionHeader(title = "Videos", dominantColors = dominantColors)
                            Spacer(modifier = Modifier.height(16.dp))

                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 20.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                itemsIndexed(currentArtist.videos) { index, video ->
                                    ArtistVideoCard(
                                        video = video,
                                        dominantColors = dominantColors,
                                        onClick = { onSongClick(currentArtist.videos, index) }
                                    )
                                }
                            }
                        }
                    }

                    // Featured On
                    if (currentArtist.featuredPlaylists.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(32.dp))
                            SectionHeader(title = "Featured On", dominantColors = dominantColors)
                            Spacer(modifier = Modifier.height(16.dp))

                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 20.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(currentArtist.featuredPlaylists, key = { it.id }) { playlist ->
                                    ArtistContentCard(
                                        title = playlist.title,
                                        subtitle = "Playlist",
                                        imageUrl = playlist.thumbnailUrl,
                                        dominantColors = dominantColors,
                                        onClick = { onPlaylistClick(playlist) },
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Fans Also Like
                    if (currentArtist.relatedArtists.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(32.dp))
                            SectionHeader(title = "Fans Also Like", dominantColors = dominantColors)
                            Spacer(modifier = Modifier.height(16.dp))

                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 20.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(currentArtist.relatedArtists, key = { it.id }) { related ->
                                    ArtistCircleCard(
                                        artist = related,
                                        dominantColors = dominantColors,
                                        onClick = { onArtistClick(related) }
                                    )
                                }
                            }
                        }
                    }

                    // About
                    if (!currentArtist.description.isNullOrBlank()) {
                        item {
                            Spacer(modifier = Modifier.height(40.dp))
                            SectionHeader(title = "About ${currentArtist.name}", dominantColors = dominantColors)
                            Spacer(modifier = Modifier.height(16.dp))
                            AboutArtistCard(
                                artist = currentArtist,
                                dominantColors = dominantColors
                            )
                        }
                    }
                }

                // Glassy Sticky Top Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(84.dp)
                        .graphicsLayer { alpha = headerAlpha }
                        .blur(radius = 20.dp * headerAlpha)
                        .background(
                            lerp(
                                Color.Transparent,
                                surfaceColor.copy(alpha = 0.85f),
                                headerAlpha
                            )
                        )
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .dpadFocusable(onClick = onBackClick, shape = CircleShape)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.3f))
                            .padding(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    AnimatedVisibility(
                        visible = headerAlpha > 0.8f,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Text(
                            text = currentArtist.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Radio Loading Overlay
                AnimatedVisibility(
                    visible = uiState.isStartingRadio,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.7f))
                            .pointerInput(Unit) {
                                awaitPointerEventScope {
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        event.changes.forEach { it.consume() }
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator(color = dominantColors.accent)
                            Text(
                                text = uiState.radioStatus ?: "Starting Radio...",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }

    selectedSong?.let { song ->
        if (showSongMenu) {
            SongMenuBottomSheet(
                isVisible = true,
                onDismiss = {
                    showSongMenu = false
                    selectedSong = null
                },
                song = song,
                onPlayNext = playerConnection?.let { connection -> { connection.playNext(song.toMediaItem()) } },
                onAddToQueue = playerConnection?.let { connection -> { connection.addToQueue(song.toMediaItem()) } },
                onAddToPlaylist = { playlistViewModel.showAddToPlaylistSheet(song) },
                onDownload = {
                    downloadUtil.enqueue(song.id, song.title) { _, message ->
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    }
                },
                onShare = { shareArtistSong(context, song) },
                onViewArtist = { viewModel.fetchArtistCreditsAndShow(song.artist) }
            )
        }
    }

    AddToPlaylistSheet(
        songs = playlistMgmtState.selectedSongs,
        isVisible = playlistMgmtState.showAddToPlaylistSheet,
        playlists = playlistMgmtState.userPlaylists,
        isLoading = playlistMgmtState.isLoadingPlaylists || playlistMgmtState.isAddingSong,
        onDismiss = playlistViewModel::hideAddToPlaylistSheet,
        onAddToPlaylist = playlistViewModel::addSongsToPlaylist,
        onCreateNewPlaylist = {
            playlistViewModel.hideAddToPlaylistSheet()
            playlistViewModel.showCreatePlaylistDialog()
        }
    )

    CreatePlaylistDialog(
        isVisible = playlistMgmtState.showCreatePlaylistDialog,
        isCreating = playlistMgmtState.isCreatingPlaylist,
        onDismiss = playlistViewModel::hideCreatePlaylistDialog,
        onCreate = playlistViewModel::createPlaylist
    )

    LaunchedEffect(playlistMgmtState.successMessage, playlistMgmtState.errorMessage) {
        val message = playlistMgmtState.successMessage ?: playlistMgmtState.errorMessage
        if (!message.isNullOrBlank()) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            playlistViewModel.clearMessages()
        }
    }
}

private fun shareArtistSong(context: Context, song: Song) {
    val shareText = "Check out this song: ${song.title} by ${song.artist}\n\nhttps://music.youtube.com/watch?v=${song.id}"
    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, shareText)
        type = "text/plain"
    }
    context.startActivity(Intent.createChooser(sendIntent, "Share Song"))
}

@Composable
fun ImmersiveArtistHeader(
    artist: Artist,
    dominantColors: DominantColors,
    onPlayAll: () -> Unit,
    onShuffle: () -> Unit,
    onSubscribe: () -> Unit,
    isSubscribed: Boolean,
    isSubscribing: Boolean,
    onStartRadio: () -> Unit
) {
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(420.dp)
    ) {
        val highResThumbnail = ImageUtils.getHighResThumbnailUrl(artist.thumbnailUrl, size = 1200)

        // Background Image
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(highResThumbnail)
                .crossfade(true)
                .build(),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Standardized Gradient Overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            dominantColors.primary.copy(alpha = 0.2f),
                            dominantColors.primary.copy(alpha = 0.8f),
                            MaterialTheme.colorScheme.background
                        ),
                        startY = 100f
                    )
                )
        )

        // Artist Info & Actions
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 24.dp, vertical = 24.dp)
                .fillMaxWidth()
        ) {
            if (artist.subscribers != null) {
                Surface(
                    shape = PillShape,
                    color = dominantColors.accent.copy(alpha = 0.2f),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = null,
                            tint = dominantColors.accent,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "OFFICIAL ARTIST",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.9f),
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }

            Text(
                text = artist.name,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1.5).sp,
                    lineHeight = 44.sp
                ),
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (artist.subscribers != null) {
                Text(
                    text = "${artist.subscribers} listeners",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                BounceButton(
                    onClick = onPlayAll,
                    size = 56.dp,
                    shape = CircleShape,
                    modifier = Modifier.background(dominantColors.accent, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = if (dominantColors.accent.luminance() > 0.5f) Color.Black else Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Row(
                    modifier = Modifier
                        .height(48.dp)
                        .clip(PillShape)
                        .background(Color.White.copy(alpha = 0.15f)),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onShuffle,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shuffle,
                            contentDescription = "Shuffle",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    VerticalDivider(
                        modifier = Modifier.height(24.dp).width(1.dp),
                        color = Color.White.copy(alpha = 0.2f)
                    )

                    IconButton(
                        onClick = onStartRadio,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Radio,
                            contentDescription = "Radio",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                if (artist.id.startsWith("UC") || artist.id.startsWith("FE")) {
                    Button(
                        onClick = onSubscribe,
                        shape = PillShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSubscribed) Color.White.copy(alpha = 0.2f) else Color.White,
                            contentColor = if (isSubscribed) Color.White else Color.Black
                        ),
                        modifier = Modifier.height(48.dp).weight(1f)
                    ) {
                        if (isSubscribing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = if (isSubscribed) Color.White else Color.Black,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = if (isSubscribed) "FOLLOWING" else "FOLLOW",
                                fontWeight = FontWeight.ExtraBold,
                                style = MaterialTheme.typography.labelLarge,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LatestReleaseSection(
    artistName: String,
    album: Album,
    isSingle: Boolean,
    dominantColors: DominantColors,
    onClick: () -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Text(
            text = "LATEST FROM $artistName",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(12.dp))

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .bounceClick(onClick = onClick)
                .dpadFocusable(onClick = onClick, shape = SquircleShape),
            shape = SquircleShape,
            color = MaterialTheme.colorScheme.surfaceContainer,
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = ImageUtils.getHighResThumbnailUrl(album.thumbnailUrl, size = 160),
                    contentDescription = null,
                    modifier = Modifier
                        .size(80.dp)
                        .clip(SquircleShape),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = album.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = buildString {
                            append(if (isSingle) "Single" else "Album")
                            if (album.year != null) append(" • ${album.year}")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = dominantColors.accent,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun TopSongRow(
    index: Int,
    song: Song,
    dominantColors: DominantColors,
    onClick: () -> Unit,
    onArtistClick: () -> Unit = {},
    onMoreClick: () -> Unit
) {
    val context = LocalContext.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .bounceClick(onClick = onClick)
            .dpadFocusable(onClick = onClick, shape = RoundedCornerShape(12.dp)),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = index.toString(),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.width(32.dp),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Black
            )

            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(ImageUtils.getHighResThumbnailUrl(song.thumbnailUrl, size = 120))
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier
                    .size(52.dp)
                    .clip(SquircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = dominantColors.accent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.clickable { onArtistClick() },
                    fontWeight = FontWeight.SemiBold
                )
            }

            Text(
                text = TimeUtil.formatPosition(song.duration),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp),
                fontWeight = FontWeight.Medium
            )

            IconButton(
                onClick = onMoreClick,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun ArtistContentCard(
    title: String,
    subtitle: String?,
    imageUrl: String?,
    dominantColors: DominantColors,
    onClick: () -> Unit,
    shape: androidx.compose.ui.graphics.Shape = SquircleShape
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .width(150.dp)
            .bounceClick(onClick = onClick)
            .dpadFocusable(onClick = onClick, shape = SquircleShape)
    ) {
        Surface(
            modifier = Modifier.size(150.dp),
            shape = shape,
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 2.dp
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(ImageUtils.getHighResThumbnailUrl(imageUrl, size = 300))
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun ArtistCircleCard(
    artist: ArtistPreview,
    dominantColors: DominantColors,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .width(120.dp)
            .bounceClick(onClick = onClick)
            .dpadFocusable(onClick = onClick, shape = CircleShape),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(120.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 2.dp
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(ImageUtils.getHighResThumbnailUrl(artist.thumbnailUrl, size = 240))
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = artist.name,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun ArtistVideoCard(
    video: Song,
    dominantColors: DominantColors,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val playOverlayGradient = remember {
        Brush.verticalGradient(
            listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f))
        )
    }
    Column(
        modifier = Modifier
            .width(260.dp)
            .bounceClick(onClick = onClick)
            .dpadFocusable(onClick = onClick, shape = SquircleShape)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f),
            shape = SquircleShape,
            color = Color.Black,
            tonalElevation = 4.dp
        ) {
            Box {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(ImageUtils.getHighResThumbnailUrl(video.thumbnailUrl, size = 520))
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(playOverlayGradient),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayCircleFilled,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = video.title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = "MUSIC VIDEO",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
fun AboutArtistCard(
    artist: Artist,
    dominantColors: DominantColors
) {
    val context = LocalContext.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .height(300.dp),
        shape = SquircleShape,
        tonalElevation = 4.dp
    ) {
        Box {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(ImageUtils.getHighResThumbnailUrl(artist.thumbnailUrl, size = 800))
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.4f),
                                Color.Black.copy(alpha = 0.9f)
                            ),
                            startY = 100f
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(24.dp)
            ) {
                Surface(
                    color = dominantColors.accent,
                    shape = PillShape,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Text(
                        text = "${artist.views ?: "Millions"} Monthly Listeners".uppercase(Locale.getDefault()),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = if (dominantColors.accent.luminance() > 0.5f) Color.Black else Color.White,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        letterSpacing = 1.sp
                    )
                }

                Text(
                    text = artist.description ?: "Biography currently unavailable.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    dominantColors: DominantColors,
    showSeeAll: Boolean = false,
    onSeeAllClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        if (showSeeAll) {
            Text(
                text = "SEE ALL",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = dominantColors.accent,
                modifier = Modifier
                    .clickable(onClick = onSeeAllClick)
                    .padding(vertical = 4.dp)
            )
        }
    }
}

@Composable
fun ArtistErrorView(
    error: ArtistError,
    onRetry: () -> Unit,
    onBackClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.ErrorOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = when (error) {
                ArtistError.AUTH_REQUIRED -> "Authentication Required"
                ArtistError.NETWORK -> "Network Error"
                ArtistError.UNKNOWN -> "Something went wrong"
            },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Could not load artist information",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onRetry,
            shape = PillShape
        ) {
            Text("Retry")
        }
        TextButton(
            onClick = onBackClick,
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text("Go Back")
        }
    }
}

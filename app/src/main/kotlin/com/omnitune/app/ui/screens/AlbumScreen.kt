/*
 * Adapted from SuvMusic (https://github.com/SuvojeetDev/SuvMusic)
 * Copyright (c) SuvMusic contributors
 * Licensed under GPL-3.0
 */

package com.omnitune.app.ui.screens

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.omnitune.app.LocalPlayerConnection
import com.omnitune.app.models.Album
import com.omnitune.app.models.Song
import com.omnitune.app.ui.component.AddToPlaylistSheet
import com.omnitune.app.ui.component.CreatePlaylistDialog
import com.omnitune.app.ui.component.MediaMenuBottomSheet
import com.omnitune.app.ui.component.PremiumLoadingScreen
import com.omnitune.app.ui.component.ReorderableSongRow
import com.omnitune.app.ui.component.SelectionTopBar
import com.omnitune.app.ui.component.SongMenuBottomSheet
import com.omnitune.app.ui.navigation.LocalRouteChromeInsets
import com.omnitune.app.ui.theme.PillShape
import com.omnitune.app.ui.theme.SquircleShape
import com.omnitune.app.ui.utils.ImageUtils
import com.omnitune.app.ui.utils.dpadFocusable
import com.omnitune.app.utils.TimeUtil

@Composable
fun AlbumScreen(
    onBackClick: () -> Unit,
    onSongClick: (List<Song>, Int) -> Unit,
    onPlayAll: (List<Song>) -> Unit = {},
    onShufflePlay: (List<Song>) -> Unit = {},
    onArtistClick: (String) -> Unit = {},
    currentSong: Song? = null,
    viewModel: AlbumViewModel = hiltViewModel(),
    playlistViewModel: PlaylistManagementViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val batchProgress by viewModel.batchProgress.collectAsStateWithLifecycle()
    val album = uiState.album
    val focusedSongId = viewModel.selectedSongId ?: currentSong?.id
    val playerConnection = LocalPlayerConnection.current
    val chromeInsets = LocalRouteChromeInsets.current

    LaunchedEffect(playerConnection) {
        viewModel.setPlayerConnection(playerConnection)
    }

    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val backgroundColor = if (isDarkTheme) Color(0xFF0D0D0D) else Color.White
    val contentColor = if (isDarkTheme) Color.White else Color.Black
    val secondaryContentColor = if (isDarkTheme) Color.White.copy(alpha = 0.7f) else Color.Black.copy(alpha = 0.7f)

    val listState = rememberLazyListState()
    val isScrolled by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 100 }
    }

    var isScrollingDown by remember { mutableStateOf(false) }
    var previousIndex by remember { mutableIntStateOf(0) }
    var previousScrollOffset by remember { mutableIntStateOf(0) }

    LaunchedEffect(listState) {
        snapshotFlow {
            Pair(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset)
        }.collect { (currentIndex, currentOffset) ->
            if (currentIndex > previousIndex) {
                isScrollingDown = true
            } else if (currentIndex < previousIndex) {
                isScrollingDown = false
            } else {
                if (currentOffset > previousScrollOffset + 10) {
                    isScrollingDown = true
                } else if (currentOffset < previousScrollOffset - 10) {
                    isScrollingDown = false
                }
            }
            previousIndex = currentIndex
            previousScrollOffset = currentOffset
        }
    }

    LaunchedEffect(album?.id, focusedSongId, album?.songs?.size) {
        val currentIndex = album?.songs?.indexOfFirst { it.id == focusedSongId } ?: -1
        if (currentIndex >= 0) {
            listState.scrollToItem(currentIndex + 1)
        }
    }

    val isTopBarVisible = !isScrolled || !isScrollingDown

    var showMenu by remember { mutableStateOf(false) }
    var showSongMenu by remember { mutableStateOf(false) }
    var selectedSong: Song? by remember { mutableStateOf(null) }

    val context = LocalContext.current

    val shareAlbum: (Album) -> Unit = { albumToShare ->
        val shareText = "Check out this album: ${albumToShare.title} by ${albumToShare.artist}\n\nhttps://music.youtube.com/playlist?list=${albumToShare.id}"
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, shareText)
            type = "text/plain"
        }
        context.startActivity(Intent.createChooser(sendIntent, "Share Album"))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        // 720px blurred artwork backdrop
        if (album?.thumbnailUrl != null) {
            val blurredHighRes = remember(album.thumbnailUrl) {
                ImageUtils.getHighResThumbnailUrl(album.thumbnailUrl, size = 720) ?: album.thumbnailUrl
            }
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(blurredHighRes)
                    .crossfade(true)
                    .size(720)
                    .build(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(100.dp),
                contentScale = ContentScale.Crop,
                alpha = if (isDarkTheme) 0.4f else 0.3f
            )
        }

        // Gradient overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = if (isDarkTheme) {
                            listOf(
                                Color.Transparent,
                                Color(0xFF0D0D0D).copy(alpha = 0.8f),
                                Color(0xFF0D0D0D)
                            )
                        } else {
                            listOf(
                                Color.White.copy(alpha = 0.3f),
                                Color.White.copy(alpha = 0.8f),
                                Color.White
                            )
                        }
                    )
                )
        )

        when {
            uiState.isLoading -> {
                PremiumLoadingScreen(
                    thumbnailUrl = album?.thumbnailUrl,
                    onBackClick = onBackClick
                )
            }
            uiState.error != null && album == null -> {
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
                        tint = secondaryContentColor,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = uiState.error ?: "Could not load album",
                        style = MaterialTheme.typography.titleMedium,
                        color = contentColor,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { viewModel.retry() },
                        shape = PillShape
                    ) {
                        Text("Retry")
                    }
                    TextButton(
                        onClick = onBackClick,
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text("Go Back", color = secondaryContentColor)
                    }
                }
            }
            album != null -> {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(
                        top = 60.dp,
                        bottom = chromeInsets.contentBottomPadding,
                    ),
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Album Header
                    item {
                        AlbumHeader(
                            album = album,
                            batchProgress = batchProgress,
                            isSaved = uiState.isSaved,
                            onPlayAll = { onPlayAll(album.songs) },
                            onShufflePlay = { onShufflePlay(album.songs) },
                            onToggleSave = { viewModel.toggleSaveToLibrary() },
                            onDownload = { viewModel.downloadAlbum(album) },
                            onShare = { shareAlbum(album) },
                            onMoreClick = { showMenu = true },
                            onArtistClick = onArtistClick,
                            contentColor = contentColor,
                            secondaryContentColor = secondaryContentColor,
                            isDarkTheme = isDarkTheme
                        )
                    }

                    // Song List with ReorderableSongRow
                    itemsIndexed(album.songs, key = { index, song -> "album_song_${index}_${song.id}" }) { index, song ->
                        val isSelected = uiState.selectedSongIds.contains(song.id)
                        ReorderableSongRow(
                            song = song,
                            index = index,
                            totalSongs = album.songs.size,
                            isSelected = isSelected,
                            isCurrentlyPlaying = song.id == currentSong?.id,
                            isSelectionMode = uiState.isSelectionMode,
                            onReorder = { from, to -> viewModel.reorderSong(from, to) },
                            onClick = {
                                if (uiState.isSelectionMode) {
                                    viewModel.toggleSongSelection(song)
                                } else {
                                    onSongClick(album.songs, index)
                                }
                            },
                            onLongClick = {
                                if (!uiState.isSelectionMode) {
                                    viewModel.toggleSongSelection(song)
                                }
                            },
                            onMoreClick = {
                                selectedSong = song
                                showSongMenu = true
                            },
                            titleColor = contentColor,
                            subtitleColor = secondaryContentColor
                        )
                    }
                }

                // Sticky Top Bar
                AnimatedVisibility(
                    visible = isTopBarVisible,
                    enter = slideInVertically(initialOffsetY = { -it }),
                    exit = slideOutVertically(targetOffsetY = { -it })
                ) {
                    if (uiState.isSelectionMode) {
                        SelectionTopBar(
                            selectedCount = uiState.selectedSongIds.size,
                            onCloseClick = { viewModel.clearSelection() },
                            onDeleteClick = { /* Not applicable for album */ },
                            onPlayNextClick = { viewModel.playNextSelectedSongs() },
                            onAddToQueueClick = { viewModel.addToQueueSelectedSongs() },
                            onAddToPlaylistClick = {
                                val selectedSongs = album.songs.filter { it.id in uiState.selectedSongIds }
                                playlistViewModel.showAddToPlaylistSheet(selectedSongs)
                            },
                            onMoveToTopClick = { /* Not applicable for album */ },
                            contentColor = contentColor,
                            isDarkTheme = isDarkTheme
                        )
                    } else {
                        AlbumTopBar(
                            title = album.title,
                            isScrolled = isScrolled,
                            onBackClick = onBackClick,
                            isDarkTheme = isDarkTheme,
                            contentColor = contentColor
                        )
                    }
                }

                // Media Menu Bottom Sheet
                if (showMenu) {
                    MediaMenuBottomSheet(
                        isVisible = showMenu,
                        onDismiss = { showMenu = false },
                        title = album.title,
                        subtitle = "${album.songs.size} songs",
                        thumbnailUrl = album.thumbnailUrl,
                        onShuffle = { onShufflePlay(album.songs) },
                        onStartRadio = { onShufflePlay(album.songs) },
                        onPlayNext = { viewModel.playNext(album.songs) },
                        onAddToQueue = { viewModel.addToQueue(album.songs) },
                        onAddToPlaylist = {
                            selectedSong = album.songs.firstOrNull()
                            selectedSong?.let { song ->
                                playlistViewModel.showAddToPlaylistSheet(song)
                            }
                        },
                        onDownload = { viewModel.downloadAlbum(album) },
                        onShare = { shareAlbum(album) }
                    )
                }

                // Song Menu Bottom Sheet
                selectedSong?.let { song ->
                    if (showSongMenu) {
                        SongMenuBottomSheet(
                            isVisible = showSongMenu,
                            onDismiss = { showSongMenu = false },
                            song = song,
                            isCurrentlyPlaying = song.id == currentSong?.id,
                            onPlayNext = { viewModel.playNext(listOf(song)) },
                            onAddToQueue = { viewModel.addToQueue(listOf(song)) },
                            onAddToPlaylist = { playlistViewModel.showAddToPlaylistSheet(song) },
                            onDownload = { viewModel.downloadSong(song) },
                            onShare = {
                                val shareText = "Check out this song: ${song.title} by ${song.artist}\n\nhttps://music.youtube.com/watch?v=${song.id}"
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, shareText)
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Share Song"))
                            },
                            onViewArtist = { onArtistClick(song.artist) }
                        )
                    }
                }

                // Global Add to Playlist Sheet
                val playlistMgmtState by playlistViewModel.uiState.collectAsStateWithLifecycle()
                if (playlistMgmtState.showAddToPlaylistSheet && playlistMgmtState.selectedSongs.isNotEmpty()) {
                    AddToPlaylistSheet(
                        songs = playlistMgmtState.selectedSongs,
                        isVisible = true,
                        playlists = playlistMgmtState.userPlaylists,
                        isLoading = playlistMgmtState.isLoadingPlaylists || playlistMgmtState.isAddingSong,
                        onDismiss = { playlistViewModel.hideAddToPlaylistSheet() },
                        onAddToPlaylist = { playlistId -> playlistViewModel.addSongsToPlaylist(playlistId) },
                        onCreateNewPlaylist = { playlistViewModel.showCreatePlaylistDialog() }
                    )
                }

                // Create Playlist Dialog
                if (playlistMgmtState.showCreatePlaylistDialog) {
                    CreatePlaylistDialog(
                        isVisible = playlistMgmtState.showCreatePlaylistDialog,
                        isCreating = playlistMgmtState.isCreatingPlaylist,
                        onDismiss = { playlistViewModel.hideCreatePlaylistDialog() },
                        onCreate = playlistViewModel::createPlaylist,
                    )
                }
            }
        }
    }
}

@Composable
private fun AlbumTopBar(
    title: String,
    isScrolled: Boolean,
    onBackClick: () -> Unit,
    isDarkTheme: Boolean,
    contentColor: Color
) {
    val scrolledColor = if (isDarkTheme) Color(0xFF1D1D1D).copy(alpha = 0.9f) else Color.White.copy(alpha = 0.9f)
    val targetColor = if (isScrolled) scrolledColor else Color.Transparent
    val backgroundColor by animateColorAsState(targetValue = targetColor, label = "AlbumTopBarBackground")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .statusBarsPadding()
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .dpadFocusable(
                    onClick = onBackClick,
                    shape = CircleShape,
                )
                .padding(8.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = contentColor
            )
        }

        AnimatedVisibility(
            visible = isScrolled,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }

        if (!isScrolled) {
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun AlbumHeader(
    album: Album,
    batchProgress: Pair<Int, Int>,
    isSaved: Boolean,
    onPlayAll: () -> Unit,
    onShufflePlay: () -> Unit,
    onToggleSave: () -> Unit,
    onDownload: () -> Unit,
    onShare: () -> Unit,
    onMoreClick: () -> Unit,
    onArtistClick: (String) -> Unit,
    contentColor: Color,
    secondaryContentColor: Color,
    isDarkTheme: Boolean
) {
    val (current, total) = batchProgress
    val isDownloading = total > 0 && current < total
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Centered Artwork: 210dp squircle with elevation 24dp and spotColor primary 0.3f
        Box(
            modifier = Modifier
                .size(210.dp)
                .shadow(
                    elevation = 24.dp,
                    shape = SquircleShape,
                    spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                )
                .clip(SquircleShape)
                .background(if (isDarkTheme) Color(0xFF2A2A2A) else Color.LightGray)
        ) {
            if (album.thumbnailUrl != null) {
                val ctx = LocalContext.current
                val coverHighRes = remember(album.thumbnailUrl) {
                    ImageUtils.getHighResThumbnailUrl(album.thumbnailUrl, size = 720) ?: album.thumbnailUrl
                }
                AsyncImage(
                    model = ImageRequest.Builder(ctx)
                        .data(coverHighRes)
                        .crossfade(true)
                        .size(720)
                        .build(),
                    contentDescription = album.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Album Title
        Text(
            text = album.title,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold
            ),
            color = contentColor,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Artist
        Text(
            text = album.artist,
            style = MaterialTheme.typography.bodyMedium,
            color = secondaryContentColor,
            textAlign = TextAlign.Center,
            modifier = Modifier.clickable { onArtistClick(album.artist) }
        )

        // Year and song count
        Text(
            text = buildString {
                album.year?.let { append("$it • ") }
                append(TimeUtil.formatSongCountAndDuration(album.songs))
            },
            style = MaterialTheme.typography.bodySmall,
            color = secondaryContentColor.copy(alpha = 0.5f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp)
        )

        // Description (if available)
        val description = album.description
        if (!description.isNullOrBlank()) {
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = secondaryContentColor.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // Batch Download Progress
        if (isDownloading) {
            Spacer(modifier = Modifier.height(16.dp))
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
            ) {
                Text(
                    text = "Downloading $current / $total",
                    style = MaterialTheme.typography.labelMedium,
                    color = contentColor,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { if (total > 0) current.toFloat() / total.toFloat() else 0f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(PillShape),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 48dp PillShape Play/Shuffle Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onPlayAll,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = PillShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDarkTheme) Color.White else Color.Black,
                    contentColor = if (isDarkTheme) Color.Black else Color.White
                ),
                contentPadding = PaddingValues(0.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Play",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            Button(
                onClick = onShufflePlay,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = PillShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDarkTheme) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.05f),
                    contentColor = contentColor
                ),
                contentPadding = PaddingValues(0.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Shuffle",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Actions Row (Download, Save, Share, More)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ActionButton(
                icon = Icons.Default.Download,
                label = "Download",
                onClick = onDownload,
                contentColor = contentColor,
                isDarkTheme = isDarkTheme
            )

            ActionButton(
                icon = if (isSaved) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                label = "Library",
                onClick = onToggleSave,
                contentColor = if (isSaved) MaterialTheme.colorScheme.primary else contentColor,
                isDarkTheme = isDarkTheme
            )

            ActionButton(
                icon = Icons.Default.Share,
                label = "Share",
                onClick = onShare,
                contentColor = contentColor,
                isDarkTheme = isDarkTheme
            )

            ActionButton(
                icon = Icons.Default.MoreVert,
                label = "More",
                onClick = onMoreClick,
                contentColor = contentColor,
                isDarkTheme = isDarkTheme
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun ActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    contentColor: Color,
    isDarkTheme: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = contentColor,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor.copy(alpha = 0.7f)
        )
    }
}

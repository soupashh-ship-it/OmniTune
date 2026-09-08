package com.omnitune.app.ui.screens

import android.content.Context
import android.content.Intent
import android.widget.Toast

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.omnitune.app.LocalDownloadUtil
import com.omnitune.app.LocalPlayerConnection
import com.omnitune.app.models.Album
import com.omnitune.app.models.HomeItem
import com.omnitune.app.models.HomeSection
import com.omnitune.app.models.HomeSectionType
import com.omnitune.app.models.PlaylistDisplayItem
import com.omnitune.app.models.Song
import com.omnitune.app.models.toMediaItem
import com.omnitune.app.ui.component.*
import com.omnitune.app.ui.theme.SquircleShape
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import java.util.Calendar

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onSongClick: (List<Song>, Int) -> Unit,
    onPlaylistClick: (PlaylistDisplayItem) -> Unit,
    onAlbumClick: (Album) -> Unit,
    onArtistClick: (String) -> Unit = {},
    onHistoryClick: () -> Unit = {},
    onStartRadio: () -> Unit = {},
    onCreateMixClick: () -> Unit = {},
    currentSong: Song? = null,
    viewModel: HomeViewModel = hiltViewModel(),
    playlistViewModel: PlaylistManagementViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val playlistMgmtState by playlistViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val playerConnection = LocalPlayerConnection.current
    val downloadUtil = LocalDownloadUtil.current

    var showSongMenu by remember { mutableStateOf(false) }
    var selectedSong: Song? by remember { mutableStateOf(null) }

    val onSongMoreClickHandler = remember {
        { song: Song ->
            selectedSong = song
            showSongMenu = true
        }
    }

    val lazyListState = rememberLazyListState()

    val homeLifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(homeLifecycleOwner) {
        homeLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.events.collect { event ->
                when (event) {
                    is HomeEvent.ShowAddToPlaylistSheet -> {
                        playlistViewModel.showAddToPlaylistSheet(event.song)
                    }
                    is HomeEvent.ScrollToTop -> {
                        if (uiState.homeSections.isNotEmpty() || uiState.recommendations.isNotEmpty()) {
                            lazyListState.animateScrollToItem(0)
                        }
                    }
                    is HomeEvent.Refresh -> {
                        viewModel.refresh()
                    }
                }
            }
        }
    }

    val dominantColors = rememberDominantColors(
        imageUrl = currentSong?.thumbnailUrl ?: uiState.recommendations.firstOrNull()?.thumbnailUrl
    )

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        MeshGradientBackground(dominantColors = dominantColors)

        when {
            uiState.isLoading && uiState.homeSections.isEmpty() -> {
                HomeLoadingSkeleton()
            }
            uiState.error != null && uiState.homeSections.isEmpty() && uiState.recommendations.isEmpty() -> {
                HomeErrorState(
                    message = uiState.error ?: "Something went wrong",
                    onRetry = { viewModel.refresh() },
                    modifier = Modifier.fillMaxSize().statusBarsPadding()
                )
            }
            uiState.homeSections.isNotEmpty() || uiState.recommendations.isNotEmpty() -> {
                LaunchedEffect(lazyListState, uiState.isLoadingMore) {
                    snapshotFlow {
                        val layoutInfo = lazyListState.layoutInfo
                        val totalItems = layoutInfo.totalItemsCount
                        val lastVisibleIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                        lastVisibleIndex >= totalItems - 8 && totalItems > 0 && !uiState.isLoadingMore
                    }
                        .distinctUntilChanged()
                        .filter { it }
                        .collectLatest {
                            viewModel.loadMore()
                        }
                }

                val refreshState = rememberPullToRefreshState()

                PullToRefreshBox(
                    isRefreshing = uiState.isRefreshing,
                    onRefresh = { viewModel.refresh() },
                    state = refreshState,
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                ) {
                    LazyColumn(
                        state = lazyListState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 140.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        item(key = "header", contentType = "header") {
                            HomeProfileHeader(
                                modifier = Modifier.padding(start = 12.dp, end = 8.dp, top = 10.dp, bottom = 4.dp),
                                avatarUrl = uiState.userAvatarUrl,
                                onHistoryClick = onHistoryClick
                            )
                        }

                        item(key = "mood_chips", contentType = "mood_chips") {
                            MoodChipsSection(
                                selectedMood = uiState.selectedMood,
                                onMoodSelected = viewModel::onMoodSelected
                            )
                        }

                        if (uiState.isLoggedIn && uiState.isForYouBannerVisible) {
                            item(key = "for_you_banner", contentType = "for_you_banner") {
                                AnimatedVisibility(
                                    visible = uiState.isForYouBannerVisible,
                                    exit = fadeOut() + shrinkVertically()
                                ) {
                                    HomeForYouBanner(
                                        onStartRadio = onStartRadio,
                                        onDismiss = viewModel::onDismissForYouBanner,
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )
                                }
                            }
                        }

                        if (uiState.recommendations.isNotEmpty()) {
                            item(key = "speed_dial", contentType = "speed_dial") {
                                QuickAccessGrid(
                                    songs = uiState.recommendations,
                                    currentSong = currentSong,
                                    userName = uiState.userName,
                                    avatarUrl = uiState.userAvatarUrl,
                                    onSongClick = onSongClick,
                                    onShuffleClick = {
                                        val shuffled = uiState.recommendations.shuffled()
                                        if (shuffled.isNotEmpty()) {
                                            onSongClick(shuffled, 0)
                                        }
                                    }
                                )
                            }
                        }

                        itemsIndexed(
                            items = uiState.filteredSections,
                            key = { index, section -> "${section.title}_$index" },
                            contentType = { _, section -> section.type }
                        ) { _, section ->
                            when (section.type) {
                                HomeSectionType.LargeCardWithList -> {
                                    LargeCardWithListSection(
                                        section = section,
                                        onSongClick = onSongClick,
                                        onPlaylistClick = onPlaylistClick,
                                        onAlbumClick = onAlbumClick,
                                        onArtistClick = onArtistClick,
                                        onSongMoreClick = onSongMoreClickHandler,
                                    )
                                }
                                HomeSectionType.Grid -> {
                                    GridSection(
                                        section = section,
                                        onSongClick = onSongClick,
                                        onPlaylistClick = onPlaylistClick,
                                        onAlbumClick = onAlbumClick,
                                        onArtistClick = onArtistClick,
                                        onSongMoreClick = onSongMoreClickHandler,
                                    )
                                }
                                HomeSectionType.VerticalList -> {
                                    VerticalListSection(
                                        section = section,
                                        onSongClick = onSongClick,
                                        onPlaylistClick = onPlaylistClick,
                                        onAlbumClick = onAlbumClick,
                                        onArtistClick = onArtistClick,
                                        onSongMoreClick = onSongMoreClickHandler,
                                    )
                                }
                                HomeSectionType.HorizontalCarousel -> {
                                    HorizontalCarouselSection(
                                        section = section,
                                        onSongClick = onSongClick,
                                        onPlaylistClick = onPlaylistClick,
                                        onAlbumClick = onAlbumClick,
                                        onArtistClick = onArtistClick,
                                        onSongMoreClick = onSongMoreClickHandler,
                                    )
                                }
                                HomeSectionType.CommunityCarousel -> {
                                    CommunityCarouselSection(
                                        section = section,
                                        onSongClick = onSongClick,
                                        onPlaylistClick = onPlaylistClick,
                                        onAlbumClick = onAlbumClick,
                                        onStartRadio = onStartRadio,
                                        onSongMoreClick = onSongMoreClickHandler
                                    )
                                }

                                HomeSectionType.QuickPicks -> {
                                    QuickPicksSection(
                                        section = section,
                                        onSongClick = onSongClick,
                                        onPlaylistClick = onPlaylistClick,
                                        onAlbumClick = onAlbumClick,
                                        onArtistClick = onArtistClick,
                                        onSongMoreClick = onSongMoreClickHandler,
                                    )
                                }
                                HomeSectionType.ChartPodium -> {
                                    ChartPodiumSection(
                                        section = section,
                                        onSongClick = onSongClick,
                                        onPlaylistClick = onPlaylistClick,
                                        onAlbumClick = onAlbumClick,
                                        onArtistClick = onArtistClick,
                                        onSongMoreClick = onSongMoreClickHandler
                                    )
                                }
                                HomeSectionType.GenreCarousel -> {
                                    GenreCarousel(
                                        section = section,
                                        onSongClick = onSongClick,
                                        onPlaylistClick = onPlaylistClick,
                                        onAlbumClick = onAlbumClick,
                                        onArtistClick = onArtistClick
                                    )
                                }
                                HomeSectionType.PersonalizedMix -> {
                                    PersonalizedMixCarousel(
                                        section = section,
                                        onSongClick = onSongClick,
                                        onPlaylistClick = onPlaylistClick,
                                        onAlbumClick = onAlbumClick,
                                        onArtistClick = onArtistClick
                                    )
                                }

                                else -> {
                                    HorizontalCarouselSection(
                                        section = section,
                                        onSongClick = onSongClick,
                                        onPlaylistClick = onPlaylistClick,
                                        onAlbumClick = onAlbumClick,
                                        onArtistClick = onArtistClick,
                                        onSongMoreClick = onSongMoreClickHandler,
                                    )
                                }
                            }
                        }

                        item(key = "create_mix_card") {
                            CreateMixCard(onClick = onCreateMixClick)
                        }

                        item(key = "end_of_feed") {
                            EndOfFeedCard(onStartRadio = onStartRadio)
                        }

                        item(key = "footer") {
                            AppFooter()
                        }

                    }
                }
            }
        }

        selectedSong?.let { menuSong ->
            SongMenuBottomSheet(
                isVisible = showSongMenu,
                onDismiss = {
                    showSongMenu = false
                    selectedSong = null
                },
                song = menuSong,
                onPlayNext = playerConnection?.let { connection -> { connection.playNext(menuSong.toMediaItem()) } },
                onAddToQueue = playerConnection?.let { connection -> { connection.addToQueue(menuSong.toMediaItem()) } },
                onAddToPlaylist = { viewModel.addToPlaylist(menuSong) },
                onDownload = {
                    downloadUtil.enqueue(menuSong.id, menuSong.title) { _, message ->
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    }
                },
                onShare = { shareSong(context, menuSong) },
                isCurrentlyPlaying = currentSong?.id == menuSong.id
            )
        }

        AddToPlaylistSheet(
            songs = playlistMgmtState.selectedSongs,
            isVisible = playlistMgmtState.showAddToPlaylistSheet,
            playlists = playlistMgmtState.userPlaylists,
            isLoading = playlistMgmtState.isLoadingPlaylists,
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
            onCreate = { title, description, isPrivate, syncWithYt ->
                playlistViewModel.createPlaylist(title, description, isPrivate, syncWithYt)
            }
        )
    }
}

private fun shareSong(context: Context, song: Song) {
    val shareText = buildString {
        append(song.title)
        if (song.artist.isNotBlank()) {
            append('\n')
            append(song.artist)
        }
        if (song.album.isNotBlank()) {
            append('\n')
            append(song.album)
        }
        if (song.id.isNotBlank()) {
            append("\n\nhttps://music.youtube.com/watch?v=")
            append(song.id)
        }
    }
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        putExtra(Intent.EXTRA_TEXT, shareText)
        putExtra(Intent.EXTRA_SUBJECT, "${song.title} - ${song.artist}")
        type = "text/plain"
    }
    context.startActivity(Intent.createChooser(sendIntent, "Share Song"))
}

@Composable
private fun HomeProfileHeader(
    modifier: Modifier = Modifier,
    avatarUrl: String?,
    onHistoryClick: () -> Unit
) {
    val greeting = remember {
        when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 4..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            in 17..21 -> "Good evening"
            else -> "Good night"
        }
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (avatarUrl != null) {
                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = "Profile",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Column {
                Text(
                    text = greeting,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Welcome to OmniTune",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        IconButton(onClick = onHistoryClick) {
            Icon(
                imageVector = Icons.Default.History,
                contentDescription = "History",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun HomeForYouBanner(
    onStartRadio: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = SquircleShape,
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Made For You",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Listen to an endless mix tailored to your taste",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onStartRadio,
                    shape = SquircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Start Radio", fontWeight = FontWeight.Bold)
                }
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.Top)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun QuickAccessGrid(
    songs: List<Song>,
    currentSong: Song?,
    userName: String?,
    avatarUrl: String?,
    onSongClick: (List<Song>, Int) -> Unit,
    onShuffleClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Quick access",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            IconButton(onClick = onShuffleClick) {
                Icon(
                    Icons.Default.Shuffle,
                    contentDescription = "Shuffle",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        val displaySongs = songs.take(6)
        val chunked = displaySongs.chunked(2)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            chunked.forEach { rowSongs ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowSongs.forEach { song ->
                        val index = songs.indexOf(song)
                        val isPlaying = currentSong?.id == song.id
                        Surface(
                            onClick = { onSongClick(songs, index) },
                            shape = SquircleShape,
                            color = if (isPlaying) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = song.thumbnailUrl,
                                    contentDescription = song.title,
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(SquircleShape),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = song.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isPlaying) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    },
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                            }
                        }
                    }
                    if (rowSongs.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Couldn't load home feed",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onRetry,
            shape = SquircleShape
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Try again")
        }
    }
}

@Composable
private fun CreateMixCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val transition = androidx.compose.animation.core.rememberInfiniteTransition(label = "createMixShimmer")
    val shimmer by transition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(durationMillis = 2400, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Restart
        ),
        label = "shimmerOffset"
    )

    val borderColor = MaterialTheme.colorScheme.primary
    val shimmerStart = MaterialTheme.colorScheme.primary.copy(alpha = 0f)
    val shimmerMid = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(76.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.18f))
            .drawBehind {
                val stroke = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = 2.dp.toPx(),
                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                        floatArrayOf(12.dp.toPx(), 8.dp.toPx()), 0f
                    )
                )
                drawRoundRect(
                    color = borderColor,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx(), 16.dp.toPx()),
                    style = stroke
                )

                val sweepWidthPx = size.width * 0.4f
                val sweepX = (size.width + sweepWidthPx) * shimmer - sweepWidthPx
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(shimmerStart, shimmerMid, shimmerStart),
                        start = androidx.compose.ui.geometry.Offset(sweepX, 0f),
                        end = androidx.compose.ui.geometry.Offset(sweepX + sweepWidthPx, size.height)
                    )
                )
            }
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column {
                Text(
                    text = "Create your own mix",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Pick artists to get started",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun EndOfFeedCard(
    onStartRadio: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                        SquircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "You've explored it all",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Start a personal radio for endless music tailored to you",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onStartRadio,
                shape = com.omnitune.app.ui.theme.PillShape,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Start Your Radio",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun AppFooter(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 40.dp, bottom = 32.dp)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "OmniTune",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "v${com.omnitune.app.BuildConfig.VERSION_NAME}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        )
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "\u00A9 2026 OmniTune",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
        )
    }
}

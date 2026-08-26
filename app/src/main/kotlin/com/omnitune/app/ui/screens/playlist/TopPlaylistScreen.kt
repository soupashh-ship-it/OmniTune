/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */



package com.omnitune.app.ui.screens.playlist

import com.omnitune.app.ui.component.OmniTuneLoader
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastSumBy
import androidx.compose.ui.zIndex
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadService
import androidx.navigation.NavController
import androidx.palette.graphics.Palette
import coil3.compose.AsyncImage
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.omnitune.app.LocalDownloadUtil
import androidx.compose.foundation.layout.navigationBars
import com.omnitune.app.LocalPlayerConnection
import com.omnitune.app.R

import com.omnitune.app.constants.MyTopFilter
import com.omnitune.app.constants.TopPlaylistSortDescendingKey
import com.omnitune.app.db.entities.Song
import com.omnitune.app.extensions.toMediaItem
import com.omnitune.app.extensions.togglePlayPause
import com.omnitune.app.playback.ExoDownloadService
import com.omnitune.app.playback.enqueueCollection
import com.omnitune.app.models.toMediaMetadata
import com.omnitune.app.playback.queues.ListQueue
import com.omnitune.app.ui.component.DefaultDialog
import com.omnitune.app.ui.component.DraggableScrollbar
import com.omnitune.app.ui.component.EmptyPlaceholder
import androidx.compose.material3.IconButton
import com.omnitune.app.ui.component.LocalMenuState
import com.omnitune.app.ui.component.SongListItem
import com.omnitune.app.ui.component.SortHeader
import com.omnitune.app.ui.menu.SelectionSongMenu
import com.omnitune.app.ui.menu.SongMenu
import com.omnitune.app.ui.theme.OmniSpacing
import com.omnitune.app.ui.theme.PlayerColorExtractor
import com.omnitune.app.ui.utils.ItemWrapper
import com.omnitune.app.ui.utils.backToMain
import com.omnitune.app.utils.makeTimeString
import com.omnitune.app.utils.rememberPreference
import com.omnitune.app.ui.screens.LibraryViewModel
import com.omnitune.app.db.entities.PlaylistEntity
import com.omnitune.app.ui.component.AssignTagsDialog
import com.omnitune.app.ui.component.TagChip
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import com.omnitune.app.LocalDatabase
import com.omnitune.app.viewmodels.TopPlaylistViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TopPlaylistScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: TopPlaylistViewModel = hiltViewModel(),
    libraryViewModel: LibraryViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isPlaying.collectAsStateWithLifecycle()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsStateWithLifecycle()
    val maxSize = viewModel.top

    val songs by viewModel.topSongs.collectAsStateWithLifecycle(null)
    val mutableSongs = remember { mutableStateListOf<Song>() }

    val likeLength = remember(songs) {
        songs?.fastSumBy { it.song.duration } ?: 0
    }

    val wrappedSongs = remember(songs) {
        songs?.map { item -> ItemWrapper(item) }?.toMutableStateList() ?: mutableStateListOf()
    }

    var isSearching by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf(TextFieldValue()) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(isSearching) {
        if (isSearching) {
            focusRequester.requestFocus()
        }
    }

    var selection by remember { mutableStateOf(false) }


    if (isSearching) {
        BackHandler {
            isSearching = false
            query = TextFieldValue()
        }
    } else if (selection) {
        BackHandler {
            selection = false
        }
    }

    val sortType by viewModel.topPeriod.collectAsStateWithLifecycle()
    val (sortDescending, onSortDescendingChange) = rememberPreference(
        TopPlaylistSortDescendingKey,
        true,
    )
    val name = stringResource(R.string.my_top) + " $maxSize"

    val downloadUtil = LocalDownloadUtil.current
    val database = LocalDatabase.current
    val coroutineScope = rememberCoroutineScope()
    var downloadState by remember { mutableIntStateOf(Download.STATE_STOPPED) }
    // Tags
    val pid = "LP_TOP"
    val tags by libraryViewModel.playlistTags(pid).collectAsStateWithLifecycle(initialValue = emptyList())
    var showAssignTagsDialog by remember { mutableStateOf(false) }

    if (showAssignTagsDialog) {
        AssignTagsDialog(
            database = LocalDatabase.current,
            playlistId = pid,
            onDismiss = { showAssignTagsDialog = false }
        )
    }

    LaunchedEffect(songs) {
        mutableSongs.apply {
            clear()
            songs?.let { addAll(it) }
        }
        if (songs?.isEmpty() == true) return@LaunchedEffect
        kotlinx.coroutines.flow.flowOf(emptyMap<String, Download>()).collect { downloads ->
            downloadState =
                if (songs?.all { downloads[it.song.id]?.state == Download.STATE_COMPLETED } == true) {
                    Download.STATE_COMPLETED
                } else if (songs?.all {
                        downloads[it.song.id]?.state == Download.STATE_QUEUED ||
                                downloads[it.song.id]?.state == Download.STATE_DOWNLOADING ||
                                downloads[it.song.id]?.state == Download.STATE_COMPLETED
                    } == true
                ) {
                    Download.STATE_DOWNLOADING
                } else {
                    Download.STATE_STOPPED
                }
        }
    }

    var showRemoveDownloadDialog by remember { mutableStateOf(false) }

    if (showRemoveDownloadDialog) {
        DefaultDialog(
            onDismiss = { showRemoveDownloadDialog = false },
            content = {
                Text(
                    text = stringResource(R.string.remove_download_playlist_confirm, name),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = 18.dp),
                )
            },
            buttons = {
                TextButton(
                    onClick = { showRemoveDownloadDialog = false },
                ) {
                    Text(text = stringResource(android.R.string.cancel))
                }

                TextButton(
                    onClick = {
                        showRemoveDownloadDialog = false
                        songs.orEmpty().forEach { song ->
                            DownloadService.sendRemoveDownload(
                                context,
                                ExoDownloadService::class.java,
                                song.song.id,
                                false,
                            )
                        }
                    },
                ) {
                    Text(text = stringResource(android.R.string.ok))
                }
            },
        )
    }

    val filteredSongs = remember(wrappedSongs, query, sortDescending) {
        val matchingSongs = if (query.text.isEmpty()) wrappedSongs
        else wrappedSongs.filter { wrapper ->
            val song = wrapper.item
            song.song.title.contains(query.text, true) ||
                    song.artists.any { it.name.contains(query.text, true) }
        }
        if (sortDescending) matchingSongs else matchingSongs.asReversed()
    }

    val lazyListState = rememberLazyListState()

    // Gradient colors state for playlist cover
    var gradientColors by remember { mutableStateOf<List<Color>>(emptyList()) }

    // Capture fallback color in composable context
    val fallbackColor = MaterialTheme.colorScheme.surface.toArgb()
    val surfaceColor = MaterialTheme.colorScheme.surface

    // Extract gradient colors from playlist cover (first song thumbnail)
    LaunchedEffect(songs) {
        val thumbnailUrl = songs?.firstOrNull()?.song?.thumbnailUrl
        if (thumbnailUrl != null) {
            val request = ImageRequest.Builder(context)
                .data(thumbnailUrl)
                .size(128, 128)
                .allowHardware(false)
                .build()

            val result = runCatching {
                context.imageLoader.execute(request)
            }.getOrNull()

            if (result != null) {
                val bitmap = result.image?.toBitmap()
                if (bitmap != null) {
                    val palette = withContext(Dispatchers.Default) {
                        Palette.from(bitmap)
                            .maximumColorCount(PlayerColorExtractor.Config.MAX_COLOR_COUNT)
                            .resizeBitmapArea(PlayerColorExtractor.Config.BITMAP_AREA)
                            .generate()
                    }

                    val extractedColors = com.omnitune.app.ui.theme.PlayerColorExtractor.extractGradientColors(
                        palette = palette,
                        fallbackColor = fallbackColor
                    )
                    gradientColors = extractedColors
                }
            }
        } else {
            gradientColors = emptyList()
        }
    }

    // Calculate gradient opacity based on scroll position
    val gradientAlpha by remember {
        derivedStateOf {
            if (lazyListState.firstVisibleItemIndex == 0) {
                val offset = lazyListState.firstVisibleItemScrollOffset
                (1f - (offset / 600f)).coerceIn(0f, 1f)
            } else {
                0f
            }
        }
    }

    val showTopBarTitle by remember {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex > 0
        }
    }

    val transparentAppBar by remember {
        derivedStateOf {
            !selection && !showTopBarTitle
        }
    }

    val headerItems by remember {
        derivedStateOf {
            val currentSongs = songs
            if (currentSongs != null && currentSongs.isNotEmpty() && !isSearching) 2 else 0
        }
    }

    // System bars padding
    val systemBarsTopPadding = WindowInsets.systemBars.asPaddingValues().calculateTopPadding()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(surfaceColor),
    ) {
        // Mesh gradient background layer
        if (gradientColors.isNotEmpty() && gradientAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        val width = size.width
                        val height = size.height * 0.55f

                        if (gradientColors.size >= 3) {
                            val c0 = gradientColors[0]
                            val c1 = gradientColors[1]
                            val c2 = gradientColors[2]
                            val c3 = gradientColors.getOrElse(3) { c0 }
                            val c4 = gradientColors.getOrElse(4) { c1 }
                            // Primary color blob - top center
                            drawRect(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        c0.copy(alpha = gradientAlpha * 0.75f),
                                        c0.copy(alpha = gradientAlpha * 0.4f),
                                        Color.Transparent
                                    ),
                                    center = Offset(width * 0.5f, height * 0.15f),
                                    radius = width * 0.8f
                                )
                            )

                            // Secondary color blob - left side
                            drawRect(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        c1.copy(alpha = gradientAlpha * 0.55f),
                                        c1.copy(alpha = gradientAlpha * 0.3f),
                                        Color.Transparent
                                    ),
                                    center = Offset(width * 0.1f, height * 0.4f),
                                    radius = width * 0.6f
                                )
                            )

                            // Third color blob - right side
                            drawRect(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        c2.copy(alpha = gradientAlpha * 0.5f),
                                        c2.copy(alpha = gradientAlpha * 0.25f),
                                        Color.Transparent
                                    ),
                                    center = Offset(width * 0.9f, height * 0.35f),
                                    radius = width * 0.55f
                                )
                            )

                            drawRect(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        c3.copy(alpha = gradientAlpha * 0.35f),
                                        c3.copy(alpha = gradientAlpha * 0.18f),
                                        Color.Transparent
                                    ),
                                    center = Offset(width * 0.25f, height * 0.65f),
                                    radius = width * 0.75f
                                )
                            )

                            drawRect(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        c4.copy(alpha = gradientAlpha * 0.3f),
                                        c4.copy(alpha = gradientAlpha * 0.15f),
                                        Color.Transparent
                                    ),
                                    center = Offset(width * 0.55f, height * 0.85f),
                                    radius = width * 0.9f
                                )
                            )
                        } else if (gradientColors.isNotEmpty()) {
                            drawRect(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        gradientColors[0].copy(alpha = gradientAlpha * 0.7f),
                                        gradientColors[0].copy(alpha = gradientAlpha * 0.35f),
                                        Color.Transparent
                                    ),
                                    center = Offset(width * 0.5f, height * 0.25f),
                                    radius = width * 0.85f
                                )
                            )
                        }

                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Transparent,
                                    surfaceColor.copy(alpha = gradientAlpha * 0.22f),
                                    surfaceColor.copy(alpha = gradientAlpha * 0.55f),
                                    surfaceColor
                                ),
                                startY = size.height * 0.35f,
                                endY = size.height
                            )
                        )
                    }
            )
        }

        LazyColumn(
            state = lazyListState,
            contentPadding = WindowInsets.navigationBars.asPaddingValues(),
        ) {
            if (songs != null) {
                if (songs.orEmpty().isEmpty()) {
                    item {
                        EmptyPlaceholder(
                            icon = R.drawable.ic_album,
                            text = stringResource(R.string.playlist_is_empty),
                        )
                    }
                } else {
                    if (!isSearching) {
                        // Hero Header Item
                        item(key = "header") {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = systemBarsTopPadding + 48.dp)
                                    .padding(horizontal = OmniSpacing.section)
                                    .padding(bottom = OmniSpacing.medium)
                            ) {
                                // Large centered artwork with shadow
                                Box(
                                    modifier = Modifier
                                        .size(240.dp)
                                        .shadow(
                                            elevation = 24.dp,
                                            shape = RoundedCornerShape(16.dp),
                                            ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                            spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                        )
                                ) {
                                    AsyncImage(
                                        model = songs.orEmpty().firstOrNull()?.song?.thumbnailUrl,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(16.dp))
                                    )
                                }

                                Spacer(modifier = Modifier.height(24.dp))

                                // Playlist title
                                Text(
                                    text = name,
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // Metadata chips row
                                @OptIn(ExperimentalLayoutApi::class)
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(OmniSpacing.compact, Alignment.CenterHorizontally),
                                ) {
                                    // Song count chip
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                                    ) {
                                        Text(
                                            text = pluralStringResource(
                                                R.plurals.songs,
                                                songs.orEmpty().size,
                                                songs.orEmpty().size
                                            ),
                                            style = MaterialTheme.typography.labelMedium,
                                            modifier = Modifier.padding(horizontal = OmniSpacing.small, vertical = 6.dp)
                                        )
                                    }

                                    // Duration chip
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                                    ) {
                                        Text(
                                            text = makeTimeString(likeLength * 1000L),
                                            style = MaterialTheme.typography.labelMedium,
                                            modifier = Modifier.padding(horizontal = OmniSpacing.small, vertical = 6.dp)
                                        )
                                    }

                                    tags.forEach { tag ->
                                        TagChip(
                                            tag = tag,
                                            editable = true,
                                            onRemove = { libraryViewModel.removePlaylistTag(pid, tag.id) }
                                        )
                                    }
                                    // Add Tag chip
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        modifier = Modifier.clickable { showAssignTagsDialog = true }
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = OmniSpacing.compact, vertical = 6.dp)
                                        ) {
                                            Icon(
                                                painter = androidx.compose.ui.res.painterResource(com.omnitune.app.R.drawable.ic_add),
                                                contentDescription = "Add Tag",
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Tag",
                                                style = MaterialTheme.typography.labelMedium
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(20.dp))

                                // Action buttons row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(OmniSpacing.small, Alignment.CenterHorizontally),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Download Button
                                    Surface(
                                        onClick = {
                                            when (downloadState) {
                                                Download.STATE_COMPLETED -> {
                                                    showRemoveDownloadDialog = true
                                                }
                                                Download.STATE_DOWNLOADING -> {
                                                    songs.orEmpty().forEach { song ->
                                                        DownloadService.sendRemoveDownload(
                                                            context,
                                                            ExoDownloadService::class.java,
                                                            song.song.id,
                                                            false,
                                                        )
                                                    }
                                                }
                                                else -> {
                                                    coroutineScope.launch {
                                                        downloadUtil.enqueueCollection(
                                                            database = database,
                                                            playlistId = pid,
                                                            name = name,
                                                            thumbnailUrl = songs.orEmpty().firstOrNull()?.song?.thumbnailUrl,
                                                            songs = songs.orEmpty().map { it.toMediaMetadata() },
                                                        )
                                                    }
                                                }
                                            }
                                        },
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            when (downloadState) {
                                                Download.STATE_COMPLETED -> {
                                                    Icon(
                                                        painter = painterResource(R.drawable.ic_download),
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                }
                                                Download.STATE_DOWNLOADING -> {
                                                    OmniTuneLoader(size = 24.dp)
                                                }
                                                else -> {
                                                    Icon(
                                                        painter = painterResource(R.drawable.ic_download),
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // Play Button
                                    Button(
                                        onClick = {
                                            playerConnection.playQueue(
                                                ListQueue(
                                                    title = name,
                                                    items = songs.orEmpty().map { it.toMediaItem() },
                                                ),
                                            )
                                        },
                                        shape = RoundedCornerShape(24.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_play_arrow),
                                            contentDescription = stringResource(R.string.play),
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }

                                    // Shuffle Button
                                    Button(
                                        onClick = {
                                            playerConnection.playQueue(
                                                ListQueue(
                                                    title = name,
                                                    items = songs.orEmpty().shuffled().map { it.toMediaItem() },
                                                ),
                                            )
                                        },
                                        shape = RoundedCornerShape(24.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_shuffle),
                                            contentDescription = stringResource(R.string.shuffle),
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }

                                    // Add to Queue Button
                                    Surface(
                                        onClick = {
                                            playerConnection.addToQueue(
                                                items = songs.orEmpty().map { it.toMediaItem() },
                                            )
                                        },
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.ic_list),
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(24.dp))
                            }
                        }
                    }

                    // Sort Header (Period Filter)
                    item(key = "sortHeader") {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(start = OmniSpacing.medium),
                        ) {
                            SortHeader(
                                sortType = sortType,
                                sortDescending = sortDescending,
                                onSortTypeChange = {
                                    viewModel.topPeriod.value = it
                                },
                                onSortDescendingChange = onSortDescendingChange,
                                sortTypeText = { sortType ->
                                    when (sortType) {
                                        MyTopFilter.ALL_TIME -> stringResource(R.string.all_time)
                                        MyTopFilter.DAY -> stringResource(R.string.past_24_hours)
                                        MyTopFilter.WEEK -> stringResource(R.string.past_week)
                                        MyTopFilter.MONTH -> stringResource(R.string.past_month)
                                        MyTopFilter.YEAR -> stringResource(R.string.past_year)
                                    }
                                }
                            )
                        }
                    }

                    // Song items
                    itemsIndexed(
                        items = filteredSongs,
                        key = { _, song -> song.item.id },
                    ) { index, songWrapper ->
                        SongListItem(
                            song = songWrapper.item,
                            isActive = songWrapper.item.song.id == mediaMetadata?.id,
                            isPlaying = isPlaying,
                            trailingContent = {
                                androidx.compose.material3.IconButton(
                                    onClick = {
                                        menuState.show {
                                            SongMenu(
                                                originalSong = songWrapper.item,
                                                navController = navController,
                                                onDismiss = menuState::dismiss,
                                            )
                                        }
                                    },
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_more_vert),
                                        contentDescription = null,
                                    )
                                }
                            },
                            isSelected = songWrapper.isSelected && selection,
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = {
                                        if (!selection) {
                                            if (songWrapper.item.song.id == mediaMetadata?.id) {
                                                playerConnection.player.togglePlayPause()
                                            } else {
                                                playerConnection.playQueue(
                                                    ListQueue(
                                                        title = name,
                                                        items = songs.orEmpty().map { it.toMediaItem() },
                                                        startIndex = songs.orEmpty().indexOfFirst { it.id == songWrapper.item.id }
                                                    ),
                                                )
                                            }
                                        } else {
                                            songWrapper.isSelected = !songWrapper.isSelected
                                        }
                                    },
                                    onLongClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        if (!selection) {
                                            selection = true
                                            wrappedSongs.forEach { it.isSelected = false }
                                            songWrapper.isSelected = true
                                        }
                                    },
                                )
                                .animateItem()
                        )
                    }
                }
            }
        }

        DraggableScrollbar(
            modifier = Modifier
                .padding(
                    WindowInsets.navigationBars.union(WindowInsets.ime)
                        .asPaddingValues()
                )
                .align(Alignment.CenterEnd),
            scrollState = lazyListState,
            headerItems = headerItems
        )

        TopAppBar(
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = if (transparentAppBar) Color.Transparent else MaterialTheme.colorScheme.surface,
                scrolledContainerColor = MaterialTheme.colorScheme.surface
            ),
            title = {
                when {
                    selection -> {
                        val count = wrappedSongs.count { it.isSelected }
                        Text(
                            text = pluralStringResource(R.plurals.songs, count, count),
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                    isSearching -> {
                        TextField(
                            value = query,
                            onValueChange = { query = it },
                            placeholder = {
                                Text(
                                    text = "Search",
                                    style = MaterialTheme.typography.titleLarge
                                )
                            },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.titleLarge,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                disabledIndicatorColor = Color.Transparent,
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester)
                        )
                    }
                    showTopBarTitle -> {
                        Text(
                            text = name,
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                }
            },
            navigationIcon = {
                IconButton(
                    onClick = {
                        when {
                            isSearching -> {
                                isSearching = false
                                query = TextFieldValue()
                                focusManager.clearFocus()
                            }
                            selection -> {
                                selection = false
                            }
                            else -> {
                                navController.navigateUp()
                            }
                        }
                    },
                ) {
                    Icon(
                        painter = painterResource(
                            if (selection) R.drawable.ic_close else R.drawable.ic_arrow_back
                        ),
                        contentDescription = null
                    )
                }
            },
            actions = {
                if (selection) {
                    val count = wrappedSongs.count { it.isSelected }
                    androidx.compose.material3.IconButton(
                        onClick = {
                            if (count == wrappedSongs.size) {
                                wrappedSongs.forEach { it.isSelected = false }
                            } else {
                                wrappedSongs.forEach { it.isSelected = true }
                            }
                        },
                    ) {
                        Icon(
                            painter = painterResource(
                                if (count == wrappedSongs.size) R.drawable.ic_close else R.drawable.ic_list
                            ),
                            contentDescription = null
                        )
                    }

                    androidx.compose.material3.IconButton(
                        onClick = {
                            menuState.show {
                                SelectionSongMenu(
                                    songSelection = wrappedSongs.filter { it.isSelected }
                                        .map { it.item },
                                    onDismiss = menuState::dismiss,
                                    clearAction = { selection = false },
                                )
                            }
                        },
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_more_vert),
                            contentDescription = null
                        )
                    }
                } else if (!isSearching) {
                    androidx.compose.material3.IconButton(
                        onClick = { isSearching = true }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_search),
                            contentDescription = null
                        )
                    }
                }
            }
        )
    }
}
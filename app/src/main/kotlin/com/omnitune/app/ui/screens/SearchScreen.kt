package com.omnitune.app.ui.screens

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.omnitune.app.LocalDownloadUtil
import com.omnitune.app.LocalPlayerConnection
import com.omnitune.app.models.*
import com.omnitune.app.models.toMediaItem
import com.omnitune.app.ui.component.*
import com.omnitune.app.ui.navigation.LocalRouteChromeInsets
import com.omnitune.app.ui.theme.SquircleShape
import com.omnitune.app.ui.utils.dpadFocusable
import kotlinx.coroutines.flow.distinctUntilChanged

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onSongClick: (List<Song>, Int) -> Unit,
    onArtistClick: (String) -> Unit = {},
    onPlaylistClick: (String) -> Unit = {},
    onAlbumClick: (Album) -> Unit = {},
    onBrowseClick: (String, String?, String) -> Unit = { _, _, _ -> },
    currentSong: Song? = null,
    viewModel: SearchViewModel = hiltViewModel(),
    playlistViewModel: PlaylistManagementViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val effectiveSelectedTab = SearchSourcePolicy.normalize(uiState.selectedTab)
    val playlistMgmtState by playlistViewModel.uiState.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val playerConnection = LocalPlayerConnection.current
    val downloadUtil = LocalDownloadUtil.current
    val listState = rememberLazyGridState()
    val chromeInsets = LocalRouteChromeInsets.current

    var isSearchActive by remember { mutableStateOf(false) }
    var isHeaderVisible by remember { mutableStateOf(true) }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                val delta = available.y
                if (delta < -20 && isHeaderVisible) {
                    isHeaderVisible = false
                } else if (delta > 20 && !isHeaderVisible) {
                    isHeaderVisible = true
                }
                return Offset.Zero
            }
        }
    }

    // Always show header at the very top
    LaunchedEffect(listState) {
        snapshotFlow {
            listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0
        }
            .distinctUntilChanged()
            .collect { isAtTop ->
                if (isAtTop) {
                    isHeaderVisible = true
                }
            }
        }

    // Always show headers when search is expanded/active
    val effectiveHeaderVisibility = isHeaderVisible || isSearchActive

    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) {
            focusManager.clearFocus()
        }
    }

    val accentColor = MaterialTheme.colorScheme.primary

    var showSongMenu by remember { mutableStateOf(false) }
    var selectedSong: Song? by remember { mutableStateOf(null) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is SearchEvent.ShowAddToPlaylistSheet -> {
                    playlistViewModel.showAddToPlaylistSheet(event.song)
                }
            }
        }
    }

    val voiceSearchLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                viewModel.onQueryChange(spokenText)
                viewModel.search()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            AnimatedVisibility(
                visible = effectiveHeaderVisibility,
                enter = fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) +
                        expandVertically(animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)),
                exit = fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) +
                       shrinkVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
            ) {
                Column {
                    SearchBar(
                        inputField = {
                            SearchBarDefaults.InputField(
                                query = uiState.query,
                                onQueryChange = { viewModel.onQueryChange(it) },
                                onSearch = {
                                    viewModel.search()
                                    isSearchActive = false
                                    focusManager.clearFocus()
                                },
                                expanded = isSearchActive,
                                onExpandedChange = { isSearchActive = it },
                                placeholder = {
                                    Text(
                                        "Search for songs, artists, or albums",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                leadingIcon = {
                                    if (isSearchActive) {
                                        IconButton(onClick = {
                                            isSearchActive = false
                                            viewModel.onBackPressed()
                                            focusManager.clearFocus()
                                        }) {
                                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                        }
                                    } else {
                                        Icon(Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                },
                                trailingIcon = {
                                    if (uiState.query.isNotEmpty()) {
                                        IconButton(onClick = { viewModel.onQueryChange("") }) {
                                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                                        }
                                    } else {
                                        IconButton(onClick = {
                                            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                                putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to search")
                                            }
                                            try { voiceSearchLauncher.launch(intent) }
                                            catch (e: Exception) {
                                                Toast.makeText(context, "Voice search not supported", Toast.LENGTH_SHORT).show()
                                            }
                                        }) {
                                            Icon(Icons.Default.Mic, contentDescription = "Voice Search")
                                        }
                                    }
                                }
                            )
                        },
                        expanded = isSearchActive,
                        onExpandedChange = { isSearchActive = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        colors = SearchBarDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        ),
                        shape = SquircleShape
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(bottom = 16.dp)
                        ) {
                            if (uiState.showSuggestions && uiState.query.isNotBlank() && uiState.suggestions.isNotEmpty()) {
                                items(
                                    items = uiState.suggestions.take(5),
                                    key = { it },
                                    contentType = { "suggestion" }
                                ) { suggestion ->
                                    SuggestionItem(
                                        suggestion = suggestion,
                                        accentColor = accentColor,
                                        onClick = {
                                            viewModel.onSuggestionClick(suggestion)
                                            isSearchActive = false
                                        }
                                    )
                                }
                            }

                            if (uiState.query.isBlank() && effectiveSelectedTab == SearchTab.YOUTUBE_MUSIC) {
                                item {
                                    Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                                        Text(
                                            text = "Browse by mood",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                                        )
                                        MoodChipsSection(
                                            selectedMood = null,
                                            onMoodSelected = { mood ->
                                                viewModel.onQueryChange("$mood songs")
                                                viewModel.search(saveToHistory = false)
                                            }
                                        )
                                    }
                                }
                            }

                            if (uiState.query.isBlank() && uiState.recentSearches.isNotEmpty()) {
                                item {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 20.dp, vertical = 12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Recently Searched",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )

                                        Text(
                                            text = "Clear",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = accentColor,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.clickable { viewModel.clearRecentSearches() }
                                        )
                                    }
                                }

                                items(
                                    items = uiState.recentSearches,
                                    key = { it.id },
                                    contentType = { it.javaClass.simpleName }
                                ) { item ->
                                    RecentSearchItemRow(
                                        item = item,
                                        onSongClick = onSongClick,
                                        onArtistClick = onArtistClick,
                                        onAlbumClick = onAlbumClick,
                                        onPlaylistClick = onPlaylistClick,
                                        onMoreClick = { song ->
                                            selectedSong = song
                                            showSongMenu = true
                                        },
                                        viewModel = viewModel
                                    )
                                }
                            }
                        }
                    }

                    val visibleTabs = SearchSourcePolicy.visibleTabs
                    if (visibleTabs.size > 1) {
                        val visibleSelectedIdx = visibleTabs.indexOf(effectiveSelectedTab).coerceAtLeast(0)
                        SecondaryTabRow(
                            selectedTabIndex = visibleSelectedIdx,
                            containerColor = Color.Transparent,
                            contentColor = accentColor,
                            divider = {},
                            indicator = {
                                TabRowDefaults.SecondaryIndicator(
                                    Modifier.tabIndicatorOffset(visibleSelectedIdx, matchContentSize = false),
                                    color = accentColor
                                )
                            },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            visibleTabs.forEach { tab ->
                                val label = when (tab) {
                                    SearchTab.YOUTUBE_MUSIC -> "YouTube Music"
                                    SearchTab.REMOTE -> "HQ Audio"
                                }
                                Tab(
                                    selected = effectiveSelectedTab == tab,
                                    onClick = { viewModel.onTabChange(tab) },
                                    text = { Text(label, style = MaterialTheme.typography.titleSmall) }
                                )
                            }
                        }
                    }

                    // Filter chips / segmented buttons
                    if (effectiveSelectedTab in visibleTabs) {
                        AnimatedVisibility(
                            visible = uiState.query.isNotBlank(),
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            val filters = SearchSourcePolicy.filtersFor(effectiveSelectedTab)

                            SingleChoiceSegmentedButtonRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                                    .horizontalScroll(rememberScrollState())
                            ) {
                                filters.forEachIndexed { index, (filter, label) ->
                                    SegmentedButton(
                                        selected = uiState.resultFilter == filter,
                                        onClick = { viewModel.setResultFilter(filter) },
                                        shape = SegmentedButtonDefaults.itemShape(index = index, count = filters.size),
                                        colors = SegmentedButtonDefaults.colors(
                                            activeContainerColor = accentColor.copy(alpha = 0.15f),
                                            activeContentColor = accentColor,
                                            activeBorderColor = accentColor,
                                            inactiveContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                            inactiveContentColor = MaterialTheme.colorScheme.onSurface,
                                            inactiveBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                        ),
                                        icon = {}
                                    ) {
                                        Text(text = label, style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Results Content Area
            LazyVerticalGrid(
                state = listState,
                columns = GridCells.Fixed(1),
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(nestedScrollConnection),
                contentPadding = PaddingValues(bottom = chromeInsets.contentBottomPadding)
            ) {
                if (uiState.isLoading) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        SearchResultsSkeleton(modifier = Modifier.padding(top = 8.dp))
                    }
                }

                if (effectiveSelectedTab == SearchTab.YOUTUBE_MUSIC) {
                    if (uiState.resultFilter != ResultFilter.ALL && !uiState.isLoading) {
                        when (uiState.resultFilter) {
                            ResultFilter.SONGS, ResultFilter.VIDEOS -> {
                                itemsIndexed(uiState.results, key = { index, song -> "song_${index}_${song.id}" }) { index, song ->
                                    SearchResultItem(
                                        song = song,
                                        isPlaying = currentSong?.id == song.id,
                                        onClick = {
                                            viewModel.addToRecentSearches(RecentSearchItem.SongItem(song))
                                            onSongClick(uiState.results, index)
                                        },
                                        onArtistClick = onArtistClick,
                                        onMoreClick = {
                                            selectedSong = song
                                            showSongMenu = true
                                        }
                                    )
                                }
                            }
                            ResultFilter.ARTISTS -> {
                                items(uiState.artistResults, key = { it.id }) { artist ->
                                    ArtistSearchListItem(artist = artist, onClick = { onArtistClick(artist.id) })
                                }
                            }
                            ResultFilter.ALBUMS -> {
                                items(uiState.albumResults, key = { it.id }) { album ->
                                    AlbumSearchListItem(
                                        album = album,
                                        onClick = {
                                            viewModel.addToRecentSearches(RecentSearchItem.AlbumItem(album))
                                            onAlbumClick(album)
                                        }
                                    )
                                }
                            }
                            ResultFilter.COMMUNITY_PLAYLISTS, ResultFilter.FEATURED_PLAYLISTS -> {
                                items(uiState.playlistResults, key = { it.id }) { playlist ->
                                    PlaylistSearchListItem(
                                        playlist = playlist,
                                        onClick = {
                                            viewModel.addToRecentSearches(RecentSearchItem.PlaylistItem(playlist))
                                            onPlaylistClick(playlist.id)
                                        }
                                    )
                                }
                            }
                            else -> {}
                        }
                    } else if (uiState.resultFilter == ResultFilter.ALL && !uiState.isLoading && uiState.query.isNotBlank()) {
                        if (uiState.artistResults.isNotEmpty()) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                Column {
                                    Text(
                                        "Artists",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp)
                                    )
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 20.dp),
                                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                                        modifier = Modifier.padding(vertical = 12.dp)
                                    ) {
                                        items(uiState.artistResults, key = { it.id }) { artist ->
                                            ArtistSearchCard(artist = artist, onClick = { onArtistClick(artist.id) })
                                        }
                                    }
                                    HorizontalDivider(
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }
                        if (uiState.playlistResults.isNotEmpty()) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                Column {
                                    Text(
                                        "Playlists",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp)
                                    )
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 20.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier.padding(vertical = 12.dp)
                                    ) {
                                        items(uiState.playlistResults, key = { it.id }) { playlist ->
                                            PlaylistSearchCard(
                                                playlist = playlist,
                                                onClick = {
                                                    viewModel.addToRecentSearches(RecentSearchItem.PlaylistItem(playlist))
                                                    onPlaylistClick(playlist.id)
                                                }
                                            )
                                        }
                                    }
                                    HorizontalDivider(
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }
                        if (uiState.albumResults.isNotEmpty()) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                Column {
                                    Text(
                                        "Albums",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp)
                                    )
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 20.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier.padding(vertical = 12.dp)
                                    ) {
                                        items(uiState.albumResults, key = { it.id }) { album ->
                                            AlbumSearchCard(
                                                album = album,
                                                onClick = {
                                                    viewModel.addToRecentSearches(RecentSearchItem.AlbumItem(album))
                                                    onAlbumClick(album)
                                                }
                                            )
                                        }
                                    }
                                    HorizontalDivider(
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }
                        if (uiState.results.isNotEmpty()) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                Text(
                                    "Songs",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 8.dp)
                                )
                            }
                            itemsIndexed(uiState.results, key = { index, song -> "main_song_${index}_${song.id}" }) { index, song ->
                                SearchResultItem(
                                    song = song,
                                    isPlaying = currentSong?.id == song.id,
                                    onClick = {
                                        viewModel.addToRecentSearches(RecentSearchItem.SongItem(song))
                                        onSongClick(uiState.results, index)
                                    },
                                    onArtistClick = onArtistClick,
                                    onMoreClick = {
                                        selectedSong = song
                                        showSongMenu = true
                                    }
                                )
                            }
                        }
                    }
                }

                // Zero-state view when query is blank
                if (uiState.query.isBlank() && !uiState.isLoading && uiState.results.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                            Text(
                                text = "Browse by mood",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                            )
                            MoodChipsSection(
                                selectedMood = null,
                                onMoodSelected = { mood ->
                                    viewModel.onQueryChange("$mood songs")
                                    viewModel.search(saveToHistory = false)
                                }
                            )
                        }
                    }

                    if (uiState.recentSearches.isNotEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Recently Searched",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Text(
                                    text = "Clear",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = accentColor,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.clickable { viewModel.clearRecentSearches() }
                                )
                            }
                        }

                        items(uiState.recentSearches, key = { it.id }) { item ->
                            RecentSearchItemRow(
                                item = item,
                                onSongClick = onSongClick,
                                onArtistClick = onArtistClick,
                                onAlbumClick = onAlbumClick,
                                onPlaylistClick = onPlaylistClick,
                                onMoreClick = { song ->
                                    selectedSong = song
                                    showSongMenu = true
                                },
                                viewModel = viewModel
                            )
                        }
                    }

                    if (uiState.browseCategories.isNotEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Text(
                                text = "Browse all",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 12.dp)
                            )
                        }

                        val chunked = uiState.browseCategories.chunked(2)
                        items(chunked) { rowItems ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                rowItems.forEach { category ->
                                    BrowseCategoryCard(
                                        category = category,
                                        onClick = { onBrowseClick(category.id, category.params, category.title) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                if (rowItems.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }

                    if (uiState.trendingSearches.isNotEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
                                Text(
                                    text = "Trending Searches",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )
                                uiState.trendingSearches.forEach { term ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(SquircleShape)
                                            .clickable { viewModel.onTrendingSearchClick(term) }
                                            .padding(vertical = 12.dp, horizontal = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Text(term, style = MaterialTheme.typography.bodyLarge)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showSongMenu && selectedSong != null) {
            val song = selectedSong!!
            SongMenuBottomSheet(
                isVisible = showSongMenu,
                onDismiss = {
                    showSongMenu = false
                    selectedSong = null
                },
                song = song,
                onPlayNext = playerConnection?.let { connection -> { connection.playNext(song.toMediaItem()) } },
                onAddToQueue = playerConnection?.let { connection -> { connection.addToQueue(song.toMediaItem()) } },
                onAddToPlaylist = { viewModel.addToPlaylist(song) },
                onDownload = {
                    downloadUtil.enqueue(song.id, song.title) { _, message ->
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    }
                },
                onShare = {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, "Check out this song: ${song.title} by ${song.artist}\n\nhttps://music.youtube.com/watch?v=${song.id}")
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share Song"))
                },
                onViewArtist = song.artistId?.let { id -> { onArtistClick(id) } },
                isCurrentlyPlaying = currentSong?.id == song.id
            )
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
            onCreate = { title, description, isPrivate, syncWithYt ->
                playlistViewModel.createPlaylist(title, description, isPrivate, syncWithYt)
            }
        )
    }
}

@Composable
private fun BrowseCategoryCard(
    category: BrowseCategory,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val baseColor = category.color?.let { Color(it) } ?: run {
        val hue = (((category.title.hashCode() % 360) + 360) % 360).toFloat()
        Color.hsv(hue, 0.5f, 0.62f)
    }
    Box(
        modifier = modifier
            .height(96.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(baseColor)
            .clickable(onClick = onClick)
    ) {
        Text(
            text = category.title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(12.dp).align(Alignment.TopStart)
        )
        if (!category.thumbnailUrl.isNullOrBlank()) {
            AsyncImage(
                model = category.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(56.dp)
                    .clip(RoundedCornerShape(topStart = 10.dp))
            )
        }
    }
}

@Composable
private fun SuggestionItem(suggestion: String, accentColor: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .dpadFocusable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Search, null, tint = accentColor, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(suggestion, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun SearchResultItem(
    song: Song,
    isPlaying: Boolean = false,
    onClick: () -> Unit,
    onArtistClick: (String) -> Unit = {},
    onMoreClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .dpadFocusable(onClick = onClick, shape = SquircleShape)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = song.thumbnailUrl,
            contentDescription = null,
            modifier = Modifier
                .size(50.dp)
                .clip(SquircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.SemiBold,
                color = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (song.isVideo) "Video" else "Song",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(" • ", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (song.artistId != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = if (song.artistId != null) Modifier.clickable { onArtistClick(song.artistId) } else Modifier
                )
            }
        }
        Box(modifier = Modifier.dpadFocusable(onClick = onMoreClick, shape = CircleShape).padding(8.dp)) {
            Icon(Icons.Default.MoreVert, "More options", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun ArtistSearchListItem(artist: Artist, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .dpadFocusable(onClick = onClick, shape = SquircleShape)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = artist.thumbnailUrl,
            contentDescription = null,
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(artist.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            artist.subscribers?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

@Composable
fun AlbumSearchListItem(album: Album, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .dpadFocusable(onClick = onClick, shape = SquircleShape)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = album.thumbnailUrl,
            contentDescription = null,
            modifier = Modifier
                .size(50.dp)
                .clip(SquircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(album.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold)
            Text("Album • ${album.artist}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun PlaylistSearchListItem(playlist: Playlist, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .dpadFocusable(onClick = onClick, shape = SquircleShape)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = playlist.thumbnailUrl,
            contentDescription = null,
            modifier = Modifier
                .size(50.dp)
                .clip(SquircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(playlist.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold)
            Text("Playlist • ${playlist.author}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun ArtistSearchCard(artist: Artist, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(100.dp).dpadFocusable(onClick = onClick, shape = SquircleShape)
    ) {
        AsyncImage(
            model = artist.thumbnailUrl,
            contentDescription = artist.name,
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(artist.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(artist.subscribers ?: "Artist", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
    }
}

@Composable
private fun PlaylistSearchCard(playlist: Playlist, onClick: () -> Unit) {
    Column(modifier = Modifier.width(140.dp).dpadFocusable(onClick = onClick, shape = SquircleShape)) {
        AsyncImage(
            model = playlist.thumbnailUrl,
            contentDescription = playlist.title,
            modifier = Modifier
                .size(140.dp)
                .clip(SquircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(playlist.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
        if (playlist.author.isNotBlank()) {
            Text(playlist.author, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun AlbumSearchCard(album: Album, onClick: () -> Unit) {
    Column(modifier = Modifier.width(140.dp).dpadFocusable(onClick = onClick, shape = SquircleShape)) {
        AsyncImage(
            model = album.thumbnailUrl,
            contentDescription = album.title,
            modifier = Modifier
                .size(140.dp)
                .clip(SquircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(album.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
        val subtitle = (if (album.artist.isNotBlank()) album.artist else "") + (if (album.year != null) " • ${album.year}" else "")
        if (subtitle.isNotBlank()) {
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun RecentSearchItemRow(
    item: RecentSearchItem,
    onSongClick: (List<Song>, Int) -> Unit,
    onArtistClick: (String) -> Unit,
    onAlbumClick: (Album) -> Unit,
    onPlaylistClick: (String) -> Unit,
    onMoreClick: (Song) -> Unit,
    viewModel: SearchViewModel
) {
    val accentColor = MaterialTheme.colorScheme.primary

    when (item) {
        is RecentSearchItem.SongItem -> SearchResultItem(
            song = item.song,
            onClick = {
                viewModel.addToRecentSearches(item)
                onSongClick(listOf(item.song), 0)
            },
            onArtistClick = onArtistClick,
            onMoreClick = { onMoreClick(item.song) }
        )
        is RecentSearchItem.AlbumItem -> AlbumSearchListItem(
            album = item.album,
            onClick = {
                viewModel.addToRecentSearches(item)
                onAlbumClick(item.album)
            }
        )
        is RecentSearchItem.PlaylistItem -> PlaylistSearchListItem(
            playlist = item.playlist,
            onClick = {
                viewModel.addToRecentSearches(item)
                onPlaylistClick(item.playlist.id)
            }
        )
        is RecentSearchItem.QueryItem -> QuerySearchItem(
            query = item.query,
            accentColor = accentColor,
            onClick = {
                viewModel.onSuggestionClick(item.query)
            }
        )
    }
}

@Composable
private fun QuerySearchItem(query: String, accentColor: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.History,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = query,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

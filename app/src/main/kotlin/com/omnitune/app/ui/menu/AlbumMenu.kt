/*
 * Velune - by Nikhil
 * Nikhil
 * Licensed Under GPL-3.0
 */



package com.omnitune.app.ui.menu

import com.omnitune.app.ui.component.OmniTuneLoader
import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ListItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.exoplayer.offline.Download.STATE_COMPLETED
import androidx.media3.exoplayer.offline.Download.STATE_DOWNLOADING
import androidx.media3.exoplayer.offline.Download.STATE_QUEUED
import androidx.media3.exoplayer.offline.Download.STATE_STOPPED
import androidx.media3.exoplayer.offline.DownloadService
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.omnitune.innertube.YouTube
import com.omnitune.app.LocalDatabase
import com.omnitune.app.LocalDownloadUtil
import com.omnitune.app.playback.downloads
import com.omnitune.app.ui.component.AddToPlaylistDialog
import com.omnitune.app.LocalPlayerConnection
import com.omnitune.app.R
import com.omnitune.app.constants.ArtistSeparatorsKey
import com.omnitune.app.constants.ListItemHeight
import com.omnitune.app.constants.ListThumbnailSize
import com.omnitune.app.db.entities.Album
import com.omnitune.app.db.entities.Song
import com.omnitune.app.extensions.toMediaItem
import com.omnitune.app.playback.ExoDownloadService
import com.omnitune.app.playback.downloadCollectionId
import com.omnitune.app.playback.enqueueCollection
import com.omnitune.app.models.toMediaMetadata
import com.omnitune.app.playback.queues.ListQueue
import com.omnitune.app.playback.queues.LocalAlbumRadio
import com.omnitune.app.sync.YouTubeLibrarySync
import com.omnitune.app.ui.component.AlbumListItem
import com.omnitune.app.ui.component.ListDialog
import com.omnitune.app.ui.component.ListItem
import com.omnitune.app.ui.component.NewAction
import com.omnitune.app.ui.component.NewActionGrid
import com.omnitune.app.ui.component.SongListItem
import com.omnitune.app.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@SuppressLint("MutableCollectionMutableState")
@Composable
fun AlbumMenu(
    originalAlbum: Album,
    navController: NavController,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val database = LocalDatabase.current
    val downloadUtil = LocalDownloadUtil.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val scope = rememberCoroutineScope()
    val libraryAlbum by database.album(originalAlbum.id).collectAsStateWithLifecycle(initialValue = originalAlbum)
    val album = libraryAlbum ?: originalAlbum
    var songs by remember {
        mutableStateOf(emptyList<Song>())
    }

    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        database.albumSongs(album.id).collect {
            songs = it
        }
    }

    var downloadState by remember { mutableIntStateOf(STATE_STOPPED) }

    LaunchedEffect(songs) {
        if (songs.isEmpty()) return@LaunchedEffect
        downloadUtil.downloads.collect { downloads ->
            downloadState =
                if (songs.all { downloads[it.id]?.state == STATE_COMPLETED }) {
                    STATE_COMPLETED
                } else if (songs.all {
                        downloads[it.id]?.state == STATE_QUEUED ||
                                downloads[it.id]?.state == STATE_DOWNLOADING ||
                                downloads[it.id]?.state == STATE_COMPLETED
                    }
                ) {
                    STATE_DOWNLOADING
                } else {
                    STATE_STOPPED
                }
        }
    }

    var refetchIconDegree by remember { mutableFloatStateOf(0f) }

    val rotationAnimation by animateFloatAsState(
        targetValue = refetchIconDegree,
        animationSpec = tween(durationMillis = 800),
        label = "",
    )

    // Artist separators for splitting artist names
    val (artistSeparators) = rememberPreference(ArtistSeparatorsKey, defaultValue = ",;/&")

    // Split artists by configured separators
    data class SplitArtist(
        val name: String,
        val originalArtist: com.omnitune.app.db.entities.ArtistEntity?
    )

    val splitArtists = remember(album.artists, artistSeparators) {
        if (artistSeparators.isEmpty()) {
            album.artists.map { SplitArtist(it.name, it) }
        } else {
            val separatorRegex = "[${Regex.escape(artistSeparators)}]".toRegex()
            album.artists.flatMap { artist ->
                val parts = artist.name.split(separatorRegex).map { it.trim() }.filter { it.isNotEmpty() }
                if (parts.size > 1) {
                    parts.mapIndexed { index, name ->
                        SplitArtist(name, if (index == 0) artist else null)
                    }
                } else {
                    listOf(SplitArtist(artist.name, artist))
                }
            }
        }
    }

    var showChoosePlaylistDialog by rememberSaveable {
        mutableStateOf(false)
    }

    var showSelectArtistDialog by rememberSaveable {
        mutableStateOf(false)
    }

    var showErrorPlaylistAddDialog by rememberSaveable {
        mutableStateOf(false)
    }

    val notAddedList by remember {
        mutableStateOf(mutableListOf<Song>())
    }

    AddToPlaylistDialog(
        isVisible = showChoosePlaylistDialog,
        onGetSong = { playlist ->
            coroutineScope.launch(Dispatchers.IO) {
                playlist.playlist.browseId?.let { playlistId ->
                    album.album.playlistId?.let { addPlaylistId ->
                        YouTube.addPlaylistToPlaylist(playlistId, addPlaylistId)
                    }
                }
            }
            songs.map { it.id }
        },
        onDismiss = {
            showChoosePlaylistDialog = false
        },
        onAddComplete = { songCount, playlistNames ->
            val message = playlistAddMessage(resources, songCount, playlistNames)
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        },
    )

    if (showErrorPlaylistAddDialog) {
        ListDialog(
            onDismiss = {
                showErrorPlaylistAddDialog = false
                onDismiss()
            },
        ) {
            item {
                ListItem(
                    title = stringResource(R.string.already_in_playlist),
                    thumbnailContent = {
                        Image(
                            painter = painterResource(R.drawable.ic_close),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onBackground),
                            modifier = Modifier.size(ListThumbnailSize),
                        )
                    },
                    modifier =
                    Modifier
                        .clickable { showErrorPlaylistAddDialog = false },
                )
            }

            items(notAddedList) { song ->
                SongListItem(song = song)
            }
        }
    }

    if (showSelectArtistDialog) {
        ListDialog(
            onDismiss = { showSelectArtistDialog = false },
        ) {
            items(
                items = splitArtists.distinctBy { it.name },
                key = { it.name },
            ) { splitArtist ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier =
                    Modifier
                        .height(ListItemHeight)
                        .clickable {
                            splitArtist.originalArtist?.let { artist ->
                                navController.navigate("artist/${artist.id}")
                                showSelectArtistDialog = false
                                onDismiss()
                            }
                        }
                        .padding(horizontal = 12.dp),
                ) {
                    Box(
                        modifier = Modifier.padding(8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        AsyncImage(
                            model = splitArtist.originalArtist?.thumbnailUrl,
                            contentDescription = null,
                            modifier =
                            Modifier
                                .size(ListThumbnailSize)
                                .clip(CircleShape),
                        )
                    }
                    Text(
                        text = splitArtist.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier =
                        Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp),
                    )
                }
            }
        }
    }

    AlbumListItem(
        album = album,
        showLikedIcon = false,
        badges = {},
        trailingContent = {
            IconButton(
                onClick = {
                    database.query {
                        val updatedAlbum = album.album.toggleLike()
                        update(updatedAlbum)
                        coroutineScope.launch(Dispatchers.IO) {
                            YouTubeLibrarySync.syncAlbumBookmark(
                                updatedAlbum,
                                bookmarked = updatedAlbum.bookmarkedAt != null
                            )
                        }
                    }
                },
            ) {
                Icon(
                    painter = painterResource(if (album.album.bookmarkedAt != null) R.drawable.ic_favorite else R.drawable.ic_favorite_border),
                    tint = if (album.album.bookmarkedAt != null) MaterialTheme.colorScheme.error else LocalContentColor.current,
                    contentDescription = null,
                )
            }
        },
    )

    HorizontalDivider()

    Spacer(modifier = Modifier.height(12.dp))

    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT

    LazyColumn(
        userScrollEnabled = !isPortrait,
        contentPadding = PaddingValues(
            start = 0.dp,
            top = 0.dp,
            end = 0.dp,
            bottom = 8.dp + WindowInsets.systemBars.asPaddingValues().calculateBottomPadding(),
        ),
    ) {
        item {
            // Enhanced Action Grid using NewMenuComponents
            NewActionGrid(
                actions = listOf(
                    NewAction(
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.ic_play_arrow),
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        text = stringResource(R.string.play),
                        onClick = {
                            onDismiss()
                            if (songs.isNotEmpty()) {
                                playerConnection.playQueue(
                                    ListQueue(
                                        title = album.album.title,
                                        items = songs.map(Song::toMediaItem)
                                    )
                                )
                            }
                        }
                    ),
                    NewAction(
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.ic_shuffle),
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        text = stringResource(R.string.shuffle),
                        onClick = {
                            onDismiss()
                            if (songs.isNotEmpty()) {
                                playerConnection.playQueue(
                                    ListQueue(
                                        title = album.album.title,
                                        items = songs.shuffled().map(Song::toMediaItem)
                                    )
                                )
                            }
                        }
                    ),
                    NewAction(
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.ic_share),
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        text = stringResource(R.string.share),
                        onClick = {
                            onDismiss()
                            val intent = Intent().apply {
                                action = Intent.ACTION_SEND
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, "https://music.youtube.com/playlist?list=${album.album.playlistId}")
                            }
                            context.startActivity(Intent.createChooser(intent, null))
                        }
                    )
                ),
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 16.dp)
            )
        }
        item {
            ListItem(
                headlineContent = { Text(text = stringResource(R.string.play_next)) },
                leadingContent = {
                    Icon(
                        painter = painterResource(R.drawable.ic_play_arrow),
                        contentDescription = null,
                    )
                },
                modifier = Modifier.clickable {
                    onDismiss()
                    playerConnection.playNext(songs.map { it.toMediaItem() })
                }
            )
        }
        item {
            ListItem(
                headlineContent = { Text(text = stringResource(R.string.add_to_queue)) },
                leadingContent = {
                    Icon(
                        painter = painterResource(R.drawable.ic_list),
                        contentDescription = null,
                    )
                },
                modifier = Modifier.clickable {
                    onDismiss()
                    playerConnection.addToQueue(songs.map { it.toMediaItem() })
                }
            )
        }
        item {
            ListItem(
                headlineContent = { Text(text = stringResource(R.string.add_to_playlist)) },
                leadingContent = {
                    Icon(
                        painter = painterResource(R.drawable.ic_list),
                        contentDescription = null,
                    )
                },
                modifier = Modifier.clickable {
                    showChoosePlaylistDialog = true
                }
            )
        }
        item {
            when (downloadState) {
                STATE_COMPLETED -> {
                    ListItem(
                        headlineContent = {
                            Text(
                                text = stringResource(R.string.remove_download),
                                color = MaterialTheme.colorScheme.error
                            )
                        },
                        leadingContent = {
                            Icon(
                                painter = painterResource(R.drawable.ic_download),
                                contentDescription = null,
                            )
                        },
                        modifier = Modifier.clickable {
                            songs.forEach { song ->
                                DownloadService.sendRemoveDownload(
                                    context,
                                    ExoDownloadService::class.java,
                                    song.id,
                                    false,
                                )
                            }
                        }
                    )
                }
                STATE_QUEUED, STATE_DOWNLOADING -> {
                    ListItem(
                        headlineContent = { Text(text = stringResource(R.string.downloading)) },
                        leadingContent = {
                            OmniTuneLoader(size = 24.dp)
                        },
                        modifier = Modifier.clickable {
                            songs.forEach { song ->
                                DownloadService.sendRemoveDownload(
                                    context,
                                    ExoDownloadService::class.java,
                                    song.id,
                                    false,
                                )
                            }
                        }
                    )
                }
                else -> {
                    ListItem(
                        headlineContent = { Text(text = stringResource(R.string.action_download)) },
                        leadingContent = {
                            Icon(
                                painter = painterResource(R.drawable.ic_download),
                                contentDescription = null,
                            )
                        },
                        modifier = Modifier.clickable {
                            scope.launch {
                                downloadUtil.enqueueCollection(
                                    database = database,
                                    playlistId = downloadCollectionId("album", album.id),
                                    name = album.album.title,
                                    thumbnailUrl = album.album.thumbnailUrl,
                                    songs = songs.map { it.toMediaMetadata() },
                                )
                            }
                        }
                    )
                }
            }
        }
        item {
            ListItem(
                headlineContent = { Text(text = stringResource(R.string.view_artist)) },
                leadingContent = {
                    Icon(
                        painter = painterResource(R.drawable.ic_artist),
                        contentDescription = null,
                    )
                },
                modifier = Modifier.clickable {
                    if (splitArtists.size == 1 && splitArtists[0].originalArtist != null) {
                        val artistId = splitArtists.getOrNull(0)?.originalArtist?.id ?: return@clickable
                        navController.navigate("artist/$artistId")
                        onDismiss()
                    } else {
                        showSelectArtistDialog = true
                    }
                }
            )
        }
        item {
            ListItem(
                headlineContent = { Text(text = stringResource(R.string.refetch)) },
                leadingContent = {
                    Icon(
                        painter = painterResource(R.drawable.ic_repeat),
                        contentDescription = null,
                        modifier = Modifier.graphicsLayer(rotationZ = rotationAnimation),
                    )
                },
                modifier = Modifier.clickable {
                    refetchIconDegree -= 360
                    scope.launch(Dispatchers.IO) {
                        YouTube.album(album.id).onSuccess {
                            database.transaction {
                                update(album.album, it, album.artists)
                            }
                        }
                    }
                }
            )
        }

    }
}

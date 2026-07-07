/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.omnitune.app.LocalPlayerConnection
import com.omnitune.app.extensions.toMediaItem
import com.omnitune.app.models.toMediaMetadata
import com.omnitune.app.ui.component.TrackMenuProvider

import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.omnitune.app.R
import com.omnitune.app.db.entities.Song
import com.omnitune.app.ui.component.EmptyPlaceholder
import com.omnitune.app.ui.component.OmniChrome
import com.omnitune.app.ui.theme.OmniColors
import com.omnitune.app.ui.theme.OmniShapes
import com.omnitune.app.ui.theme.OmniSpacing

@Composable
fun LikedSongsScreen(
    onBack: () -> Unit = {},
    onPlaySong: (Song) -> Unit = {},
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val likedSongs by viewModel.likedSongs.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OmniColors.OmniBackgroundBase)
            .background(OmniColors.BackgroundGradient)
            .statusBarsPadding()
            .padding(horizontal = OmniSpacing.section),
    ) {
        LibraryListHeader(
            title = "Liked Songs",
            subtitle = countLabel(likedSongs.size, "song"),
            icon = R.drawable.ic_favorite,
            onBack = onBack,
        )

        if (likedSongs.isEmpty()) {
            LibraryEmptyState(
                icon = R.drawable.ic_favorite,
                text = "No liked songs yet",
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(OmniSpacing.small),
            ) {
                items(
                    items = likedSongs,
                    key = { it.song.id },
                    contentType = { "likedSong" },
                ) { song ->
                    LibrarySongRow(
                        song = song,
                        onClick = { onPlaySong(song) },
                    )
                }
                item(contentType = "bottom-spacer") { Spacer(modifier = Modifier.height(OmniChrome.BottomContentPadding)) }
            }
        }
    }
}

@Composable
private fun LibraryListHeader(
    title: String,
    subtitle: String,
    icon: Int,
    onBack: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = OmniSpacing.medium, bottom = OmniSpacing.large),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(OmniColors.OmniGlassMedium)
                .border(BorderStroke(1.dp, OmniColors.OmniGlassBorderSubtle), CircleShape),
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
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = OmniColors.TextPrimary,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = OmniColors.TextSecondary,
            )
        }
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(OmniColors.Hot.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painterResource(icon),
                contentDescription = null,
                tint = OmniColors.Hot,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
private fun LibrarySongRow(
    song: com.omnitune.app.db.entities.Song,
    onClick: () -> Unit,
) {
    val title = song.song.title
    val artists = song.artists.joinToString(", ") { it.name }.ifBlank { "Unknown artist" }
    val thumbnail = song.song.thumbnailUrl
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(OmniShapes.Large)
            .background(OmniColors.OmniGlassSubtle)
            .border(BorderStroke(1.dp, OmniColors.OmniGlassBorderSubtle), OmniShapes.Large)
            .clickable(onClick = onClick)
            .padding(OmniSpacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(58.dp)
                .clip(OmniShapes.ArtworkSmall)
                .background(OmniColors.OmniGlassStrong),
            contentAlignment = Alignment.Center,
        ) {
            if (thumbnail.isNullOrBlank()) {
                Icon(
                    painterResource(R.drawable.ic_album),
                    contentDescription = null,
                    tint = OmniColors.TextTertiary,
                    modifier = Modifier.size(24.dp),
                )
            } else {
                AsyncImage(
                    model = thumbnail,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
        }
        Spacer(modifier = Modifier.width(OmniSpacing.small))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = OmniColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = artists,
                style = MaterialTheme.typography.bodySmall,
                color = OmniColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        
        var menuExpanded by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
        val playerConnection = LocalPlayerConnection.current
        
        Box {
            IconButton(
                onClick = { menuExpanded = true },
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_more_vert),
                    contentDescription = "More options",
                    tint = OmniColors.TextSecondary,
                    modifier = Modifier.size(20.dp),
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
private fun LibraryEmptyState(
    icon: Int,
    text: String,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(OmniShapes.ExtraLarge)
            .background(OmniColors.OmniGlassSubtle)
            .border(BorderStroke(1.dp, OmniColors.OmniGlassBorderSubtle), OmniShapes.ExtraLarge)
            .padding(OmniSpacing.screen),
        contentAlignment = Alignment.Center,
    ) {
        EmptyPlaceholder(
            icon = icon,
            text = text,
        )
    }
}

private fun countLabel(count: Int, singular: String): String {
    val noun = if (count == 1) singular else "${singular}s"
    return "$count $noun"
}

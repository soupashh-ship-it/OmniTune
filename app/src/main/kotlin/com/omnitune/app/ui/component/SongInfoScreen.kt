/*
 * This file was adapted from SuvMusic.
 * Original copyright follows:
 * 
 * Copyright (C) Suvojeet
 * Licensed under the GNU General Public License v3.0 (GPLv3)
 */

package com.omnitune.app.ui.component

import java.util.Locale

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.omnitune.app.models.Song
import com.omnitune.app.ui.theme.SquircleShape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongInfoScreen(
    song: Song,
    onBack: () -> Unit,
    onArtistClick: (String) -> Unit = {},
    onAlbumClick: (String, String?) -> Unit = { _, _ -> },
    audioCodec: String? = null,
    audioBitrate: Int? = null,
    dominantColors: DominantColors? = null,
    isDarkTheme: Boolean = isSystemInDarkTheme()
) {
    val contentColor = if (isDarkTheme) Color.White else Color.Black
    val accentColor = dominantColors?.accent ?: MaterialTheme.colorScheme.primary
    val backgroundColor = if (isDarkTheme) {
        dominantColors?.primary?.copy(alpha = 0.95f) ?: MaterialTheme.colorScheme.background
    } else {
        MaterialTheme.colorScheme.background
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Song Info", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = contentColor,
                    navigationIconContentColor = contentColor
                )
            )
        },
        containerColor = backgroundColor
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Artwork
            AsyncImage(
                model = song.thumbnailUrl,
                contentDescription = song.title,
                modifier = Modifier
                    .size(180.dp)
                    .clip(RoundedCornerShape(20.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = song.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = contentColor
            )

            Text(
                text = song.artist,
                style = MaterialTheme.typography.titleMedium,
                color = accentColor,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable { onArtistClick(song.artist) }
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Metadata card
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (song.album.isNotBlank()) {
                        InfoRow(
                            icon = Icons.Default.Album,
                            label = "Album",
                            value = song.album,
                            onClick = { onAlbumClick(song.album, null) }
                        )
                        HorizontalDivider(
                            color = contentColor.copy(alpha = 0.06f),
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    }

                    InfoRow(
                        icon = Icons.Default.Person,
                        label = "Artist",
                        value = song.artist,
                        onClick = { onArtistClick(song.artist) }
                    )

                    HorizontalDivider(
                        color = contentColor.copy(alpha = 0.06f),
                        modifier = Modifier.padding(vertical = 12.dp)
                    )

                    InfoRow(
                        icon = Icons.Default.Timer,
                        label = "Duration",
                        value = formatDuration(song.duration)
                    )

                    if (audioCodec != null || audioBitrate != null) {
                        HorizontalDivider(
                            color = contentColor.copy(alpha = 0.06f),
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                        InfoRow(
                            icon = Icons.Default.Audiotrack,
                            label = "Audio Quality",
                            value = "${audioCodec ?: "AAC"} • ${audioBitrate ?: 256} kbps"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%d:%02d", minutes, seconds)
}

/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.omnitune.app.R
import com.omnitune.app.db.entities.Song
import com.omnitune.app.ui.component.EmptyPlaceholder
import com.omnitune.app.ui.component.GlassCard
import com.omnitune.app.ui.theme.OmniColors
import com.omnitune.app.ui.theme.OmniShapes

@Composable
fun LikedSongsScreen(
    onBack: () -> Unit = {},
    onPlaySong: (Song) -> Unit = {},
    viewModel: LibraryViewModel = androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel(),
) {
    val likedSongs by viewModel.likedSongs.collectAsState()

    Column(modifier = Modifier.fillMaxSize().background(OmniColors.Background)) {
        // Top bar
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.size(40.dp).clip(OmniShapes.SM).background(OmniColors.GlassSurface)) {
                Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = "Back", tint = OmniColors.TextPrimary, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text("Liked Songs", style = androidx.compose.material3.MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = OmniColors.TextPrimary)
            Spacer(modifier = Modifier.weight(1f))
            Text("${likedSongs.size} songs", style = androidx.compose.material3.MaterialTheme.typography.bodyMedium, color = OmniColors.TextMuted)
        }

        if (likedSongs.isEmpty()) {
            EmptyPlaceholder(icon = R.drawable.ic_favorite, text = "No liked songs yet\nTap the heart icon on any song to add it here")
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(likedSongs) { song ->
                    SongRow(song = song, onClick = { onPlaySong(song) })
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
private fun SongRow(song: Song, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clip(OmniShapes.SM).clickable(remember { MutableInteractionSource() }, indication = androidx.compose.material3.ripple(bounded = true, color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.1f)), onClick = onClick).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(48.dp).clip(OmniShapes.SM).background(OmniColors.GlassSurface)) {
            if (song.song.thumbnailUrl != null) {
                AsyncImage(model = song.song.thumbnailUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(song.song.title, style = androidx.compose.material3.MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = OmniColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(song.artists.joinToString(", ") { it.name }, style = androidx.compose.material3.MaterialTheme.typography.bodySmall, color = OmniColors.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

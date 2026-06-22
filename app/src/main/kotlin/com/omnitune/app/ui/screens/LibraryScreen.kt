/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.omnitune.app.R
import com.omnitune.app.ui.component.GlassCard
import com.omnitune.app.ui.component.OmniSectionHeader
import com.omnitune.app.ui.theme.OmniColors
import com.omnitune.app.ui.theme.OmniShapes
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

@Composable
fun LibraryScreen(
    onNavigateToSearch: () -> Unit = {},
    onNavigateToLiked: () -> Unit = {},
    onNavigateToDownloads: () -> Unit = {},
    onNavigateToRecentlyPlayed: () -> Unit = {},
    onNavigateToArtists: () -> Unit = {},
    onNavigateToAlbums: () -> Unit = {},
    onNavigateToPlaylists: () -> Unit = {},
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LazyColumn(modifier = Modifier.fillMaxSize().background(OmniColors.Background).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Spacer(modifier = Modifier.statusBarsPadding())
            Spacer(modifier = Modifier.height(12.dp))
            Text("Library", style = androidx.compose.material3.MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold, color = OmniColors.TextPrimary)
            Spacer(modifier = Modifier.height(2.dp))
            Text("Your music collection", style = androidx.compose.material3.MaterialTheme.typography.bodyLarge, color = OmniColors.TextMuted)
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LibrarySmartCard(painterResource(R.drawable.ic_favorite), "Liked", "${uiState.likedCount} songs", listOf(OmniColors.Hot, OmniColors.Primary), onNavigateToLiked, Modifier.weight(1f))
                LibrarySmartCard(painterResource(com.omnitune.app.R.drawable.ic_download), "Downloads", "", listOf(OmniColors.Secondary, OmniColors.Primary), onNavigateToDownloads, Modifier.weight(1f))
            }
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LibrarySmartCard(painterResource(R.drawable.ic_list), "Recently Played", "${uiState.recentlyPlayed.size} songs", listOf(OmniColors.Primary, OmniColors.Secondary), onNavigateToRecentlyPlayed, Modifier.weight(1f))
                LibrarySmartCard(painterResource(R.drawable.ic_repeat), "Your Mixes", "${uiState.playlistCount} playlists", listOf(OmniColors.Primary, OmniColors.Hot), onNavigateToPlaylists, Modifier.weight(1f))
            }
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LibrarySmartCard(painterResource(R.drawable.ic_play_arrow), "All Songs", "${uiState.librarySongCount} songs", listOf(OmniColors.Secondary, OmniColors.Hot), onNavigateToSearch, Modifier.weight(1f))
            }
        }
        item { Spacer(modifier = Modifier.height(4.dp)); OmniSectionHeader(title = "Browse") }
        item {
            GlassCard(modifier = Modifier.fillMaxWidth(), onClick = onNavigateToArtists) {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(OmniColors.Primary.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                        Icon(painterResource(com.omnitune.app.R.drawable.ic_artist), contentDescription = null, tint = OmniColors.Primary, modifier = Modifier.size(22.dp))
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Text("Artists", style = androidx.compose.material3.MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = OmniColors.TextPrimary)
                }
            }
        }
        item {
            GlassCard(modifier = Modifier.fillMaxWidth(), onClick = onNavigateToAlbums) {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(OmniColors.Secondary.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                        Icon(painterResource(com.omnitune.app.R.drawable.ic_album), contentDescription = null, tint = OmniColors.Secondary, modifier = Modifier.size(22.dp))
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Text("Albums", style = androidx.compose.material3.MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = OmniColors.TextPrimary)
                }
            }
        }
        item {
            GlassCard(modifier = Modifier.fillMaxWidth(), onClick = onNavigateToPlaylists) {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(OmniColors.Hot.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                        Icon(painterResource(R.drawable.ic_list), contentDescription = null, tint = OmniColors.Hot, modifier = Modifier.size(22.dp))
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Text("Playlists", style = androidx.compose.material3.MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = OmniColors.TextPrimary)
                }
            }
        }
        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
private fun LibrarySmartCard(painter: Painter, label: String, count: String, gradient: List<Color>, onClick: () -> Unit, modifier: Modifier = Modifier) {
    GlassCard(modifier = modifier, onClick = onClick) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(Brush.linearGradient(gradient)), contentAlignment = Alignment.Center) {
                Icon(painter, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(label, style = androidx.compose.material3.MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = OmniColors.TextPrimary, textAlign = TextAlign.Center)
            if (count.isNotEmpty()) {
                Text(count, style = androidx.compose.material3.MaterialTheme.typography.bodySmall, color = OmniColors.TextMuted, textAlign = TextAlign.Center)
            }
        }
    }
}

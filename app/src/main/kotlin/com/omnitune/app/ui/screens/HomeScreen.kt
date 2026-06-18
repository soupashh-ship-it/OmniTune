/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.omnitune.app.R
import com.omnitune.app.db.entities.Song
import com.omnitune.app.ui.component.GlassCard
import com.omnitune.app.ui.component.OmniSectionHeader
import com.omnitune.app.ui.component.ShimmerBar
import com.omnitune.app.ui.theme.OmniColors
import com.omnitune.app.ui.theme.OmniShapes

@Composable
fun HomeScreen(
    onNavigateToSearch: () -> Unit = {},
    onNavigateToLibrary: () -> Unit = {},
    onResumePlayback: () -> Unit = {},
    onPlaySong: (Song) -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    LazyColumn(modifier = Modifier.fillMaxSize().background(OmniColors.Background).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        item {
            Spacer(modifier = Modifier.height(12.dp))
            Text("OmniTune", style = androidx.compose.material3.MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold, color = OmniColors.TextPrimary)
            Spacer(modifier = Modifier.height(2.dp))
            Text("Your music, your universe", style = androidx.compose.material3.MaterialTheme.typography.bodyLarge, color = OmniColors.TextMuted)
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                QuickGlassAction(painterResource(R.drawable.ic_play_arrow), "Play", listOf(OmniColors.Primary, OmniColors.Secondary), onResumePlayback, Modifier.weight(1f))
                QuickGlassAction(painterResource(android.R.drawable.ic_menu_search), "Search", listOf(OmniColors.Secondary, OmniColors.Primary), onNavigateToSearch, Modifier.weight(1f))
                QuickGlassAction(painterResource(R.drawable.ic_list), "Library", listOf(OmniColors.Hot, OmniColors.Primary), onNavigateToLibrary, Modifier.weight(1f))
            }
        }
        item { OmniSectionHeader(title = "Recently Played", action = if (uiState.recentSongs.isNotEmpty()) "See all" else null, onAction = onNavigateToLibrary) }
        if (uiState.isLoading) {
            items(3) {
                GlassCard(modifier = Modifier.fillMaxWidth().height(72.dp)) {
                    Row(modifier = Modifier.fillMaxSize().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        ShimmerBar(modifier = Modifier.size(48.dp).clip(OmniShapes.SM))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            ShimmerBar(modifier = Modifier.fillMaxWidth(0.7f).height(14.dp).clip(OmniShapes.XS))
                            Spacer(modifier = Modifier.height(6.dp))
                            ShimmerBar(modifier = Modifier.fillMaxWidth(0.45f).height(11.dp).clip(OmniShapes.XS))
                        }
                    }
                }
            }
        } else if (uiState.recentSongs.isEmpty()) {
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(painterResource(R.drawable.ic_list), contentDescription = null, tint = OmniColors.TextMuted, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No recently played songs", style = androidx.compose.material3.MaterialTheme.typography.bodyMedium, color = OmniColors.TextMuted, textAlign = TextAlign.Center)
                    }
                }
            }
        } else {
            items(uiState.recentSongs) { event ->
                GlassCard(modifier = Modifier.fillMaxWidth(), onClick = { onPlaySong(event.song) }) {
                    Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(48.dp).clip(OmniShapes.SM).background(OmniColors.GlassSurface)) {
                            if (event.song.thumbnailUrl != null) AsyncImage(model = event.song.thumbnailUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(event.song.title, style = androidx.compose.material3.MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = OmniColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(event.song.artists.joinToString(", ") { it.name }, style = androidx.compose.material3.MaterialTheme.typography.bodySmall, color = OmniColors.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Icon(painterResource(R.drawable.ic_play_arrow), contentDescription = "Play", tint = OmniColors.Primary, modifier = Modifier.size(24.dp))
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
private fun QuickGlassAction(painter: androidx.compose.ui.graphics.painter.Painter, title: String, gradient: List<Color>, onClick: () -> Unit, modifier: Modifier = Modifier) {
    GlassCard(modifier = modifier, onClick = onClick, cornerRadius = OmniShapes.LG) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = Modifier.size(40.dp).shadow(8.dp, RoundedCornerShape(50)).clip(RoundedCornerShape(50)).background(Brush.linearGradient(gradient)), contentAlignment = Alignment.Center) {
                Icon(painter, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(title, style = androidx.compose.material3.MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = OmniColors.TextPrimary)
        }
    }
}

/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.omnitune.app.R
import com.omnitune.app.db.entities.Album
import com.omnitune.app.ui.component.EmptyPlaceholder
import com.omnitune.app.ui.theme.OmniColors
import com.omnitune.app.ui.theme.OmniShapes
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

@Composable
fun LibraryAlbumsScreen(
    onBack: () -> Unit = {},
    onNavigateToAlbum: (String) -> Unit = {},
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val albums by viewModel.libraryAlbums.collectAsState()

    Column(modifier = Modifier.fillMaxSize().background(OmniColors.Background).statusBarsPadding()) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.size(40.dp).clip(OmniShapes.SM).background(OmniColors.GlassSurface)) {
                Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = "Back", tint = OmniColors.TextPrimary, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text("Albums", style = androidx.compose.material3.MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = OmniColors.TextPrimary)
            Spacer(modifier = Modifier.weight(1f))
            Text("${albums.size} albums", style = androidx.compose.material3.MaterialTheme.typography.bodyMedium, color = OmniColors.TextMuted)
        }

        if (albums.isEmpty()) {
            EmptyPlaceholder(icon = android.R.drawable.ic_menu_gallery, text = "No albums in your library yet")
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                items(albums) { album ->
                    AlbumRow(album = album, onClick = { onNavigateToAlbum(album.id) })
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
private fun AlbumRow(album: Album, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(OmniShapes.MD)
            .background(OmniColors.GlassSurface)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = album.thumbnailUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(OmniColors.Secondary.copy(alpha = 0.1f))
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = album.title,
                style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = OmniColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = album.artists.joinToString { it.name },
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                color = OmniColors.TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

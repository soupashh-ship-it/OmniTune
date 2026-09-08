/*
 * This file was adapted from SuvMusic.
 * Original copyright follows:
 * 
 * Copyright (C) Suvojeet
 * Licensed under the GNU General Public License v3.0 (GPLv3)
 */

package com.omnitune.app.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.VerticalAlignTop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun SelectionTopBar(
    selectedCount: Int,
    onCloseClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onPlayNextClick: () -> Unit = {},
    onAddToQueueClick: () -> Unit = {},
    onAddToPlaylistClick: () -> Unit = {},
    onMoveToTopClick: () -> Unit = {},
    contentColor: Color,
    isDarkTheme: Boolean
) {
    val scrolledColor = if (isDarkTheme) Color(0xFF1D1D1D).copy(alpha = 0.9f) else Color.White.copy(alpha = 0.9f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(scrolledColor)
            .statusBarsPadding()
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onCloseClick) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = contentColor)
        }

        Text(
            text = "$selectedCount",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = contentColor,
            modifier = Modifier.padding(start = 8.dp)
        )

        Spacer(modifier = Modifier.weight(1f))

        IconButton(onClick = onPlayNextClick) {
            Icon(Icons.AutoMirrored.Filled.PlaylistPlay, "Play Next", tint = contentColor)
        }

        IconButton(onClick = onAddToQueueClick) {
            Icon(Icons.AutoMirrored.Filled.QueueMusic, "Add to Queue", tint = contentColor)
        }

        IconButton(onClick = onAddToPlaylistClick) {
            Icon(Icons.AutoMirrored.Filled.PlaylistAdd, "Add to Playlist", tint = contentColor)
        }

        IconButton(onClick = onMoveToTopClick) {
            Icon(
                imageVector = Icons.Default.VerticalAlignTop,
                contentDescription = "Move to top",
                tint = contentColor
            )
        }

        IconButton(onClick = onDeleteClick) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete selected",
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}

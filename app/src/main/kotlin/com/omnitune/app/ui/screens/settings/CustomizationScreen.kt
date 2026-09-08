/*
 * This file was adapted from SuvMusic.
 * Original copyright follows:
 * 
 * Copyright (C) Suvojeet
 * Licensed under the GNU General Public License v3.0 (GPLv3)
 */

package com.omnitune.app.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.RoundedCorner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.omnitune.app.models.MiniPlayerStyle
import com.omnitune.app.models.PlayerStyle
import com.omnitune.app.viewmodels.SettingsViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomizationScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onSeekbarStyleClick: () -> Unit = {},
    onArtworkShapeClick: () -> Unit = {},
    onArtworkSizeClick: () -> Unit = {},
    showStyleSheet: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showMiniPlayerSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Player Customization", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Player Art & Progress
            item {
                Text(
                    text = "ARTWORK & CONTROLS",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
                SettingsCard {
                    SettingsNavRow(
                        title = "Artwork Shape",
                        subtitle = uiState.artworkShape.name.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() },
                        icon = Icons.Rounded.RoundedCorner,
                        onClick = onArtworkShapeClick
                    )
                    SettingsNavRow(
                        title = "Artwork Size",
                        subtitle = uiState.artworkSize.label,
                        icon = Icons.Default.AspectRatio,
                        onClick = onArtworkSizeClick
                    )
                    SettingsNavRow(
                        title = "Seekbar Style",
                        subtitle = uiState.seekbarStyle.name.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() },
                        icon = Icons.Default.LinearScale,
                        onClick = onSeekbarStyleClick
                    )
                }
            }

            // MiniPlayer & Layout
            item {
                Text(
                    text = "MINIPLAYER & DOCK",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
                SettingsCard {
                    SettingsNavRow(
                        title = "MiniPlayer Style",
                        subtitle = uiState.miniPlayerStyle.label,
                        icon = Icons.Default.PictureInPicture,
                        onClick = { showMiniPlayerSheet = true }
                    )
                    SettingsSwitchRow(
                        title = "Swipe Down to Dismiss Player",
                        subtitle = "Collapse full-screen player with a downward gesture",
                        icon = Icons.Default.SwipeDown,
                        checked = uiState.swipeDownToDismissEnabled,
                        onCheckedChange = viewModel::setSwipeDownToDismissEnabled
                    )
                }
            }
        }
    }

    if (showMiniPlayerSheet) {
        ModalBottomSheet(onDismissRequest = { showMiniPlayerSheet = false }) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Select MiniPlayer Style", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                MiniPlayerStyle.entries.forEach { style ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.setMiniPlayerStyle(style)
                                showMiniPlayerSheet = false
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = uiState.miniPlayerStyle == style, onClick = null)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(style.label)
                    }
                }
            }
        }
    }
}

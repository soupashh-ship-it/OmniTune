/*
 * This file was adapted from SuvMusic.
 * Original copyright follows:
 * 
 * Copyright (C) Suvojeet
 * Licensed under the GNU General Public License v3.0 (GPLv3)
 */

package com.omnitune.app.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.RoundedCorner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.omnitune.app.models.MiniPlayerStyle
import com.omnitune.app.models.PlayerStyle
import com.omnitune.app.ui.utils.displayLabel
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
                        subtitle = uiState.artworkShape.displayLabel(),
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
                        subtitle = uiState.seekbarStyle.displayLabel(),
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
                    CustomizationSliderRow(
                        title = "MiniPlayer Transparency",
                        subtitle = "${(uiState.miniPlayerAlpha * 100).toInt()}%",
                        icon = Icons.Default.Tune,
                        value = uiState.miniPlayerAlpha,
                        onValueChange = viewModel::setMiniPlayerAlpha,
                        valueRange = 0f..1f
                    )
                    CustomizationSliderRow(
                        title = "MiniPlayer Blur",
                        subtitle = "${uiState.miniPlayerBlur.toInt()} dp",
                        icon = Icons.Default.BlurOn,
                        value = uiState.miniPlayerBlur,
                        onValueChange = viewModel::setMiniPlayerBlur,
                        valueRange = 0f..100f
                    )
                    CustomizationSliderRow(
                        title = "Dock Opacity",
                        subtitle = "${(uiState.navBarAlpha * 100).toInt()}%",
                        icon = Icons.Default.Tune,
                        value = uiState.navBarAlpha,
                        onValueChange = viewModel::setNavBarAlpha,
                        valueRange = 0f..1f
                    )
                    CustomizationSliderRow(
                        title = "Dock Blur",
                        subtitle = "${uiState.navBarBlur.toInt()} dp",
                        icon = Icons.Default.BlurOn,
                        value = uiState.navBarBlur,
                        onValueChange = viewModel::setNavBarBlur,
                        valueRange = 0f..100f
                    )
                    SettingsSwitchRow(
                        title = "Swipe Down to Dismiss Player",
                        subtitle = "Collapse full-screen player with a downward gesture",
                        icon = Icons.Default.SwipeDown,
                        checked = uiState.swipeDownToDismissEnabled,
                        onCheckedChange = viewModel::setSwipeDownToDismissEnabled
                    )
                    SettingsSwitchRow(
                        title = "Picture-in-Picture",
                        subtitle = "Keep video playback visible when leaving OmniTune",
                        icon = Icons.Default.PictureInPictureAlt,
                        checked = uiState.pictureInPictureEnabled,
                        onCheckedChange = viewModel::setPictureInPictureEnabled
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
                    val selected = uiState.miniPlayerStyle == style
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = selected,
                                role = Role.RadioButton,
                                onClick = {
                                    viewModel.setMiniPlayerStyle(style)
                                    showMiniPlayerSheet = false
                                },
                            )
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = selected, onClick = null)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(style.label)
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomizationSliderRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Slider(
            value = value.coerceIn(valueRange.start, valueRange.endInclusive),
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 40.dp, top = 4.dp)
        )
    }
}

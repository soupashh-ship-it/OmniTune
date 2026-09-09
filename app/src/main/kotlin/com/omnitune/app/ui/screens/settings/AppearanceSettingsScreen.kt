/*
 * This file was adapted from SuvMusic.
 * Original copyright follows:
 * 
 * Copyright (C) Suvojeet
 * Licensed under the GNU General Public License v3.0 (GPLv3)
 */

package com.omnitune.app.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.FormatAlignLeft
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.omnitune.app.models.*
import com.omnitune.app.ui.component.LogoPickerSection
import com.omnitune.app.ui.theme.SquircleShape
import com.omnitune.app.viewmodels.SettingsViewModel

import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    var showThemeModeSheet by remember { mutableStateOf(false) }
    var showAppThemeSheet by remember { mutableStateOf(false) }
    var showPlayerStyleSheet by remember { mutableStateOf(false) }
    var showLyricsPositionSheet by remember { mutableStateOf(false) }
    var showLyricsAnimationSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Appearance", fontWeight = FontWeight.Bold) },
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
            // Theme Section
            item {
                Text(
                    text = "THEME",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
                SettingsCard {
                    SettingsNavRow(
                        title = "Theme Mode",
                        subtitle = uiState.themeMode.name.lowercase().replaceFirstChar { it.uppercase() },
                        icon = Icons.Default.DarkMode,
                        onClick = { showThemeModeSheet = true }
                    )
                    SettingsNavRow(
                        title = "Color Scheme",
                        subtitle = uiState.appTheme.name.lowercase().replaceFirstChar { it.uppercase() },
                        icon = Icons.Default.Palette,
                        onClick = { showAppThemeSheet = true }
                    )
                    SettingsSwitchRow(
                        title = "Dynamic Colors",
                        subtitle = "Use Material You wallpaper colors (Android 12+)",
                        icon = Icons.Default.ColorLens,
                        checked = uiState.dynamicColorEnabled,
                        onCheckedChange = viewModel::setDynamicColor
                    )
                    SettingsSwitchRow(
                        title = "Pure Black (AMOLED)",
                        subtitle = "True pitch black backgrounds in dark mode",
                        icon = Icons.Default.Brightness2,
                        checked = uiState.pureBlackEnabled,
                        onCheckedChange = viewModel::setPureBlackEnabled
                    )
                }
            }

            // Liquid Glass & Visuals
            item {
                Text(
                    text = "LIQUID GLASS & HARDWARE",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
                SettingsCard {
                    SettingsSwitchRow(
                        title = "iOS Liquid Glass Effect",
                        subtitle = "Hardware blurred translucent glass surfaces",
                        icon = Icons.Default.BlurOn,
                        checked = uiState.iosLiquidGlassEnabled,
                        onCheckedChange = viewModel::setIosLiquidGlassEnabled
                    )
                    SettingsSwitchRow(
                        title = "Force High Refresh Rate",
                        subtitle = "Request the highest refresh rate this display supports",
                        icon = Icons.Default.Speed,
                        checked = uiState.forceMaxRefreshRateEnabled,
                        onCheckedChange = viewModel::setForceMaxRefreshRate
                    )
                }
            }

            // Player Styling
            item {
                Text(
                    text = "NOW PLAYING",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
                SettingsCard {
                    SettingsNavRow(
                        title = "Player UI Style",
                        subtitle = when (uiState.playerStyle) {
                            PlayerStyle.LIQUID_GLASS -> "Liquid Glass (iOS)"
                            PlayerStyle.YT_MUSIC -> "YouTube Music Expressive"
                            PlayerStyle.CLASSIC -> "Classic OmniTune"
                        },
                        icon = Icons.Default.Album,
                        onClick = { showPlayerStyleSheet = true }
                    )
                    SettingsSwitchRow(
                        title = "Animated Mesh Gradient",
                        subtitle = "Dynamic multi-blob animated background in player",
                        icon = Icons.Default.Animation,
                        checked = uiState.playerAnimatedBackgroundEnabled,
                        onCheckedChange = viewModel::setPlayerAnimatedBackgroundEnabled
                    )
                    SettingsSwitchRow(
                        title = "Dynamic Artwork Palette",
                        subtitle = "Adapt now-playing colors to album art dominant tone",
                        icon = Icons.Default.Palette,
                        checked = uiState.albumArtDynamicColorsEnabled,
                        onCheckedChange = viewModel::setAlbumArtDynamicColorsEnabled
                    )
                    SettingsSwitchRow(
                        title = "Vinyl Rotation Animation",
                        subtitle = "Spin album art when Vinyl shape is selected",
                        icon = Icons.Default.Album,
                        checked = uiState.rotatingVinylAnimationEnabled,
                        onCheckedChange = viewModel::setRotatingVinylAnimationEnabled
                    )
                }
            }

            // Lyrics Customization
            item {
                Text(
                    text = "LYRICS PRESENTATION",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
                SettingsCard {
                    SettingsNavRow(
                        title = "Text Alignment",
                        subtitle = uiState.lyricsTextPosition.name.lowercase().replaceFirstChar { it.uppercase() },
                        icon = Icons.AutoMirrored.Filled.FormatAlignLeft,
                        onClick = { showLyricsPositionSheet = true }
                    )
                    SettingsNavRow(
                        title = "Animation Type",
                        subtitle = uiState.lyricsAnimationType.name.lowercase().replaceFirstChar { it.uppercase() },
                        icon = Icons.Default.Animation,
                        onClick = { showLyricsAnimationSheet = true }
                    )
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Lyrics Backdrop Blur", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                text = "${uiState.lyricsBlur.toInt()} dp",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Slider(
                            value = uiState.lyricsBlur,
                            onValueChange = viewModel::setLyricsBlur,
                            valueRange = 0f..10f,
                            colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary)
                        )
                    }
                }
            }

            // App Logo section
            item {
                LogoPickerSection(
                    selected = uiState.logoVariant,
                    onSelect = viewModel::setLogoVariant
                )
            }
        }
    }

    // Pickers Bottom Sheets
    if (showThemeModeSheet) {
        ModalBottomSheet(onDismissRequest = { showThemeModeSheet = false }) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Select Theme Mode", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                ThemeMode.entries.forEach { mode ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.setThemeMode(mode)
                                showThemeModeSheet = false
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = uiState.themeMode == mode, onClick = null)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(mode.name.lowercase().replaceFirstChar { it.uppercase() })
                    }
                }
            }
        }
    }

    if (showAppThemeSheet) {
        ModalBottomSheet(onDismissRequest = { showAppThemeSheet = false }) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Select Color Scheme", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                AppTheme.entries.forEach { theme ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.setAppTheme(theme)
                                showAppThemeSheet = false
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = uiState.appTheme == theme, onClick = null)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(theme.name.lowercase().replaceFirstChar { it.uppercase() })
                    }
                }
            }
        }
    }

    if (showPlayerStyleSheet) {
        ModalBottomSheet(onDismissRequest = { showPlayerStyleSheet = false }) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Select Player Style", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                PlayerStyle.entries.forEach { style ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.setPlayerStyle(style)
                                showPlayerStyleSheet = false
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = uiState.playerStyle == style, onClick = null)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            when (style) {
                                PlayerStyle.LIQUID_GLASS -> "Liquid Glass (iOS)"
                                PlayerStyle.YT_MUSIC -> "YouTube Music Expressive"
                                PlayerStyle.CLASSIC -> "Classic OmniTune"
                            }
                        )
                    }
                }
            }
        }
    }

    if (showLyricsPositionSheet) {
        ModalBottomSheet(onDismissRequest = { showLyricsPositionSheet = false }) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Select Lyrics Text Position", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                LyricsTextPosition.entries.forEach { pos ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.setLyricsTextPosition(pos)
                                showLyricsPositionSheet = false
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = uiState.lyricsTextPosition == pos, onClick = null)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(pos.name.lowercase().replaceFirstChar { it.uppercase() })
                    }
                }
            }
        }
    }

    if (showLyricsAnimationSheet) {
        ModalBottomSheet(onDismissRequest = { showLyricsAnimationSheet = false }) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Select Lyrics Animation", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                LyricsAnimationType.entries.forEach { anim ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.setLyricsAnimationType(anim)
                                showLyricsAnimationSheet = false
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = uiState.lyricsAnimationType == anim, onClick = null)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(anim.name.lowercase().replaceFirstChar { it.uppercase() })
                    }
                }
            }
        }
    }
}

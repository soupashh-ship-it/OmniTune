/*
 * This file was adapted from SuvMusic.
 * Original copyright follows:
 * 
 * Copyright (C) Suvojeet
 * Licensed under the GNU General Public License v3.0 (GPLv3)
 */

package com.omnitune.app.ui.screens.settings

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.omnitune.app.LocalDownloadUtil
import com.omnitune.app.ui.theme.SquircleShape
import com.omnitune.app.viewmodels.SettingsViewModel

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onBackClick: () -> Unit,
    onPlayerCacheClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var cacheSizeBytes by remember { mutableLongStateOf(0L) }
    var isClearing by remember { mutableStateOf(false) }

    fun calculateCache() {
        scope.launch(Dispatchers.IO) {
            val cacheDir = context.cacheDir
            val size = cacheDir.walkTopDown().filter { it.isFile }.map { it.length() }.sum()
            withContext(Dispatchers.Main) {
                cacheSizeBytes = size
            }
        }
    }

    LaunchedEffect(Unit) {
        calculateCache()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Storage Manager", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
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
            // Overview card
            item {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.SdStorage,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = formatStorageSize(cacheSizeBytes),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Total App Cached Data",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            // Cache options
            item {
                Text(
                    text = "CACHE MANAGEMENT",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
                SettingsCard {
                    SettingsNavRow(
                        title = "Player Streaming Cache",
                        subtitle = "Inspect and configure audio buffer cache",
                        icon = Icons.Default.Cached,
                        onClick = onPlayerCacheClick
                    )
                    SettingsNavRow(
                        title = "Clear Image & Artwork Cache",
                        subtitle = "Free memory used by cached album thumbnails",
                        icon = Icons.Default.Image,
                        onClick = {
                            viewModel.clearArtworkCache(context)
                            calculateCache()
                        }
                    )
                    SettingsNavRow(
                        title = "Clear All App Cache",
                        subtitle = "Purge temporary offline data and logs",
                        icon = Icons.Default.Delete,
                        onClick = {
                            viewModel.clearAppCache(context)
                            calculateCache()
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerCacheScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val downloadUtil = LocalDownloadUtil.current
    val scope = rememberCoroutineScope()

    var storageInfo by remember { mutableStateOf<com.omnitune.app.playback.DownloadUtil.StorageInfo?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var showClearCacheDialog by remember { mutableStateOf(false) }
    var isClearing by remember { mutableStateOf(false) }

    fun refreshStorageInfo() {
        scope.launch {
            storageInfo = withContext(Dispatchers.IO) {
                downloadUtil.getStorageInfo()
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        refreshStorageInfo()
    }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text("Player Cache") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Cached,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    val usedBytes = storageInfo?.progressiveCacheBytes ?: 0L
                    val limitBytes = uiState.playerCacheLimit
                    val isUnlimited = limitBytes == -1L

                    Text(
                        text = if (isUnlimited) "Unlimited Cache" else "Cache Usage",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = formatStorageSize(usedBytes) + if (isUnlimited) "" else " / " + formatStorageSize(limitBytes),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    if (isLoading) {
                        Spacer(modifier = Modifier.height(16.dp))
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            strokeWidth = 2.dp
                        )
                    } else if (!isUnlimited && limitBytes > 0L) {
                        Spacer(modifier = Modifier.height(16.dp))
                        val progress = (usedBytes.toFloat() / limitBytes).coerceIn(0f, 1f)
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f),
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .background(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Player cache stores songs you stream so they do not need to be re-downloaded. This allows instant playback and offline access to recently played songs.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "SETTINGS",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 8.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (uiState.playerCacheLimit == -1L) {
                            viewModel.setPlayerCacheLimit(500L * 1024L * 1024L)
                        } else {
                            viewModel.setPlayerCacheLimit(-1L)
                        }
                    }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Unlimited Cache",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Allow cache to grow up to OmniTune's playback cache safety cap",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = uiState.playerCacheLimit == -1L,
                    onCheckedChange = { checked ->
                        if (checked) {
                            viewModel.setPlayerCacheLimit(-1L)
                        } else {
                            viewModel.setPlayerCacheLimit(500L * 1024L * 1024L)
                        }
                    }
                )
            }

            if (uiState.playerCacheLimit != -1L) {
                val currentLimit = uiState.playerCacheLimit
                val options = listOf(
                    250L * 1024L * 1024L to "250 MB",
                    500L * 1024L * 1024L to "500 MB",
                    1024L * 1024L * 1024L to "1 GB",
                    2L * 1024L * 1024L * 1024L to "2 GB",
                    5L * 1024L * 1024L * 1024L to "5 GB",
                    10L * 1024L * 1024L * 1024L to "10 GB"
                )

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Maximum Cache Size",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 8.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                options.forEach { (size, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.setPlayerCacheLimit(size) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentLimit == size,
                            onClick = { viewModel.setPlayerCacheLimit(size) }
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text(
                        text = "Note: changes to cache limit may require an app restart to take full effect.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedButton(
                onClick = { showClearCacheDialog = true },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading && !isClearing && (storageInfo?.progressiveCacheBytes ?: 0L) > 0L,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isClearing) "Clearing..." else "Clear Player Cache")
            }

            Spacer(modifier = Modifier.height(50.dp))
        }
    }

    if (showClearCacheDialog) {
        AlertDialog(
            onDismissRequest = { showClearCacheDialog = false },
            title = { Text("Clear Player Cache?") },
            text = {
                Text("This will remove all cached streaming data. Downloaded songs will not be affected.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearCacheDialog = false
                        isClearing = true
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                downloadUtil.clearProgressiveCache()
                                downloadUtil.getStorageInfo()
                            }.also { updatedInfo ->
                                storageInfo = updatedInfo
                                isClearing = false
                            }
                        }
                    }
                ) {
                    Text("Clear", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearCacheDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

private fun formatStorageSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0))
        .toInt()
        .coerceIn(0, units.lastIndex)
    return String.format(
        Locale.US,
        "%.1f %s",
        bytes / Math.pow(1024.0, digitGroups.toDouble()),
        units[digitGroups],
    )
}

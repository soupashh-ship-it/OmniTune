package com.omnitune.app.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.media3.exoplayer.offline.Download
import com.omnitune.app.R
import com.omnitune.app.ui.component.EmptyPlaceholder
import com.omnitune.app.ui.component.OmniChrome
import com.omnitune.app.ui.theme.OmniColors
import com.omnitune.app.ui.theme.OmniShapes
import com.omnitune.app.ui.theme.OmniSpacing

import androidx.compose.ui.platform.LocalContext
import com.omnitune.app.LocalPlayerConnection
import com.omnitune.app.db.entities.Song
import coil3.compose.AsyncImage
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@androidx.media3.common.util.UnstableApi
@Composable
fun DownloadsScreen(
    onBack: () -> Unit,
    onNavigateToSearch: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onPlayDownload: (Download) -> Unit = {},
    viewModel: DownloadsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val playerConnection = LocalPlayerConnection.current
    
    var downloadToRemove by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<Download?>(null) }
    
    val completedCount = uiState.downloads.count { it.state == Download.STATE_COMPLETED }
    val activeCount = uiState.downloads.count {
        it.state == Download.STATE_DOWNLOADING || it.state == Download.STATE_QUEUED || it.state == Download.STATE_STOPPED || it.state == Download.STATE_REMOVING
    }
    val failedCount = uiState.downloads.count { it.state == Download.STATE_FAILED }
    val queuedCount = uiState.downloads.count { it.state == Download.STATE_QUEUED }
    val downloadedBytes = uiState.downloads.sumOf { it.bytesDownloaded.coerceAtLeast(0L) }
    var selectedTab by remember { mutableIntStateOf(0) }
    val filteredDownloads = when (selectedTab) {
        0 -> uiState.downloads.filter { it.state == Download.STATE_COMPLETED }
        1 -> uiState.downloads.filter {
            it.state == Download.STATE_DOWNLOADING || it.state == Download.STATE_QUEUED ||
                it.state == Download.STATE_STOPPED || it.state == Download.STATE_REMOVING
        }
        else -> uiState.downloads.filter { it.state == Download.STATE_FAILED }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OmniColors.OmniBackgroundBase)
            .background(OmniColors.BackgroundGradient)
            .statusBarsPadding()
            .padding(horizontal = OmniSpacing.section),
    ) {
        DownloadsHeader(
            completedCount = completedCount,
            activeCount = activeCount,
            failedCount = failedCount,
            queuedCount = queuedCount,
            downloadedBytes = downloadedBytes,
            onSettings = onNavigateToSettings,
            onClearFailed = viewModel::clearFailedDownloads,
            onClearQueued = viewModel::clearQueuedDownloads,
        )

        DownloadTabRow(selectedTab = selectedTab, onSelect = { selectedTab = it })

        if (filteredDownloads.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(OmniShapes.ExtraLarge)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                OmniColors.SurfaceRaised,
                                OmniColors.OmniAccentPrimary.copy(alpha = 0.12f),
                                OmniColors.SurfaceRaised,
                            ),
                        ),
                    )
                    .border(
                        BorderStroke(1.dp, OmniColors.OmniAccentPrimary.copy(alpha = 0.28f)),
                        OmniShapes.ExtraLarge,
                    )
                    .padding(OmniSpacing.section),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    DownloadEmptyState(
                        tab = selectedTab,
                        onFindMusic = onNavigateToSearch,
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(OmniSpacing.small),
                contentPadding = PaddingValues(bottom = OmniChrome.BottomContentPadding),
            ) {
                items(
                    items = filteredDownloads,
                    key = { it.request.id },
                    contentType = { "downloadRow" },
                ) { download ->
                    DownloadItemRow(
                        download = download,
                        song = uiState.songs[download.request.id],
                        onPlay = { viewModel.playDownload(download, playerConnection, context) },
                        onRetry = { viewModel.retryDownload(download.request.id) },
                        onRemove = { downloadToRemove = download },
                    )
                }
            }
        }
    }

    if (downloadToRemove != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { downloadToRemove = null },
            title = { Text("Remove Download", color = OmniColors.TextPrimary) },
            text = { Text("Are you sure you want to delete this downloaded song?", color = OmniColors.TextSecondary) },
            containerColor = OmniColors.OmniBackgroundBase,
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        downloadToRemove?.let { viewModel.removeDownload(it.request.id) }
                        downloadToRemove = null
                    }
                ) {
                    Text("Delete", color = OmniColors.Error)
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(
                    onClick = { downloadToRemove = null }
                ) {
                    Text("Cancel", color = OmniColors.TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun DownloadsHeader(
    completedCount: Int,
    activeCount: Int,
    failedCount: Int,
    queuedCount: Int,
    downloadedBytes: Long,
    onSettings: () -> Unit,
    onClearFailed: () -> Unit,
    onClearQueued: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = OmniSpacing.medium, bottom = OmniSpacing.large),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Downloads",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = OmniColors.TextPrimary,
            )
            Text(
                text = "Your music, available offline.",
                style = MaterialTheme.typography.bodyMedium,
                color = OmniColors.TextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(
            onClick = onSettings,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(OmniColors.OmniGlassMedium)
                .border(BorderStroke(1.dp, OmniColors.OmniGlassBorderSubtle), CircleShape),
        ) {
            Icon(
                painterResource(R.drawable.ic_settings),
                contentDescription = "Download settings",
                tint = OmniColors.TextPrimary,
                modifier = Modifier.size(22.dp),
            )
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = OmniSpacing.medium),
        horizontalArrangement = Arrangement.spacedBy(OmniSpacing.small),
    ) {
        DownloadMetric("Ready", completedCount, R.drawable.ic_check_circle, OmniColors.Downloaded, Modifier.weight(1f))
        DownloadMetric("Active", activeCount, R.drawable.ic_download, OmniColors.Warning, Modifier.weight(1f))
        DownloadMetric("Failed", failedCount, R.drawable.ic_warning, OmniColors.Error, Modifier.weight(1f))
        DownloadMetric("Used", formatDownloadSize(downloadedBytes), R.drawable.ic_storage, OmniColors.Downloaded, Modifier.weight(1f))
    }

    if (failedCount > 0) {
        Button(
            onClick = onClearFailed,
            colors = ButtonDefaults.buttonColors(
                containerColor = OmniColors.Error.copy(alpha = 0.18f),
                contentColor = OmniColors.Error,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = OmniSpacing.medium),
        ) {
            Text("Clear all failed", fontWeight = FontWeight.Bold)
        }
    }

    if (queuedCount > 0) {
        Button(
            onClick = onClearQueued,
            colors = ButtonDefaults.buttonColors(
                containerColor = OmniColors.Warning.copy(alpha = 0.18f),
                contentColor = OmniColors.Warning,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = OmniSpacing.medium),
        ) {
            Text("Remove all queued", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun DownloadMetric(
    label: String,
    value: Any,
    icon: Int,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .height(96.dp)
            .clip(OmniShapes.Large)
            .background(OmniColors.SurfaceRaised)
            .border(
                BorderStroke(1.dp, accent.copy(alpha = 0.36f)),
                OmniShapes.Large,
            )
            .padding(OmniSpacing.compact),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(21.dp),
        )
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = accent,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = OmniColors.TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun DownloadTabRow(
    selectedTab: Int,
    onSelect: (Int) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = OmniSpacing.compact)
            .clip(OmniShapes.Pill)
            .background(OmniColors.SurfaceQuiet)
            .border(1.dp, OmniColors.SurfaceHairline, OmniShapes.Pill)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        listOf("Ready", "Active", "Failed").forEachIndexed { index, title ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(OmniShapes.Pill)
                    .background(if (index == selectedTab) OmniColors.OmniAccentPrimary else Color.Transparent)
                    .clickable { onSelect(index) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (index == selectedTab) OmniColors.TextOnAccent else OmniColors.TextPrimary,
                )
            }
        }
    }
}

@Composable
private fun DownloadEmptyState(tab: Int, onFindMusic: () -> Unit) {
    val message = when (tab) {
        0 -> "Nothing downloaded yet"
        1 -> "No active downloads"
        else -> "No failed downloads"
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(R.drawable.ic_cloud),
                contentDescription = null,
                tint = OmniColors.OmniAccentPrimary.copy(alpha = 0.82f),
                modifier = Modifier.size(88.dp),
            )
            Icon(
                painter = painterResource(R.drawable.ic_download),
                contentDescription = null,
                tint = OmniColors.SurfaceFloating,
                modifier = Modifier.size(40.dp),
            )
        }
        Spacer(modifier = Modifier.height(OmniSpacing.large))
        Text(
            text = message,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = OmniColors.TextPrimary,
        )
        Spacer(modifier = Modifier.height(OmniSpacing.small))
        Text(
            text = if (tab == 0) "Download your favorite music to listen anytime, anywhere." else "Downloads will appear here when their state changes.",
            style = MaterialTheme.typography.bodyLarge,
            color = OmniColors.TextSecondary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        if (tab == 0) {
            Spacer(modifier = Modifier.height(OmniSpacing.large))
            Button(
                onClick = onFindMusic,
                colors = ButtonDefaults.buttonColors(
                    containerColor = OmniColors.OmniAccentPrimary,
                    contentColor = OmniColors.TextOnAccent,
                ),
            ) {
                Icon(painterResource(R.drawable.ic_download), contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(OmniSpacing.small))
                Text("Find music to download", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

private fun formatDownloadSize(bytes: Long): String = when {
    bytes >= 1_073_741_824L -> "%.1f GB".format(bytes / 1_073_741_824.0)
    bytes >= 1_048_576L -> "%.1f MB".format(bytes / 1_048_576.0)
    bytes >= 1024L -> "%.1f KB".format(bytes / 1024.0)
    else -> "$bytes B"
}

@androidx.media3.common.util.UnstableApi
@Composable
private fun DownloadItemRow(
    download: Download,
    song: Song?,
    onPlay: () -> Unit,
    onRetry: () -> Unit,
    onRemove: () -> Unit,
) {
    val title = song?.title ?: String(download.request.data, Charsets.UTF_8).ifBlank { download.request.id }
    val state = downloadPresentation(download)
    val rowModifier = Modifier
        .fillMaxWidth()
        .clip(OmniShapes.Large)
        .background(
            if (state.playable) {
                Brush.linearGradient(
                    listOf(
                        OmniColors.Downloaded.copy(alpha = 0.18f),
                        OmniColors.OmniGlassMedium,
                    )
                )
            } else {
                Brush.linearGradient(
                    listOf(
                        OmniColors.OmniGlassSubtle,
                        OmniColors.OmniGlassMedium,
                    )
                )
            }
        )
        .border(
            BorderStroke(
                1.dp,
                if (state.playable) OmniColors.Downloaded.copy(alpha = 0.36f) else OmniColors.OmniGlassBorderSubtle,
            ),
            OmniShapes.Large,
        )
        .then(if (state.playable) Modifier.clickable(onClick = onPlay) else Modifier)
        .animateContentSize()
        .defaultMinSize(minHeight = 82.dp)
        .padding(OmniSpacing.medium)

    Row(
        modifier = rowModifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(OmniShapes.ArtworkSmall)
                .background(state.accent.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            if (!song?.thumbnailUrl.isNullOrBlank()) {
                AsyncImage(
                    model = song.thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Icon(
                    painterResource(if (state.playable) R.drawable.ic_play_arrow else R.drawable.ic_download),
                    contentDescription = null,
                    tint = state.accent,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
        Spacer(modifier = Modifier.width(OmniSpacing.medium))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = OmniColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(OmniSpacing.micro))
            Text(
                text = song?.artists?.joinToString(", ") { it.name }?.ifBlank { state.label } ?: state.label,
                style = MaterialTheme.typography.bodySmall,
                color = OmniColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (song != null) {
                Text(
                    text = state.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = state.accent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (state.progress != null) {
                Spacer(modifier = Modifier.height(OmniSpacing.compact))
                val animatedProgress by androidx.compose.animation.core.animateFloatAsState(
                    targetValue = state.progress,
                    animationSpec = androidx.compose.animation.core.tween(durationMillis = 300, easing = androidx.compose.animation.core.LinearEasing),
                    label = "progressAnim"
                )
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(OmniShapes.Pill),
                    color = state.accent,
                    trackColor = OmniColors.OmniGlassStrong,
                )
            }
        }

        if (download.state == Download.STATE_FAILED) {
            Spacer(modifier = Modifier.width(OmniSpacing.compact))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = OmniColors.OmniAccentPrimary,
                    contentColor = OmniColors.TextOnAccent,
                ),
                contentPadding = PaddingValues(horizontal = OmniSpacing.small, vertical = OmniSpacing.compact),
                modifier = Modifier.defaultMinSize(minHeight = 48.dp),
            ) {
                Text(
                    text = "Retry",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        Spacer(modifier = Modifier.width(OmniSpacing.compact))
        IconButton(
            onClick = onRemove,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(OmniColors.OmniGlassStrong),
        ) {
            Icon(
                painterResource(R.drawable.ic_close),
                contentDescription = "Remove download",
                tint = OmniColors.TextSecondary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

private data class DownloadPresentation(
    val label: String,
    val accent: Color,
    val progress: Float?,
    val playable: Boolean,
)

@androidx.media3.common.util.UnstableApi
private fun downloadPresentation(download: Download): DownloadPresentation {
    return when (download.state) {
        Download.STATE_COMPLETED -> DownloadPresentation(
            label = "Ready offline",
            accent = OmniColors.Downloaded,
            progress = null,
            playable = true,
        )
        Download.STATE_DOWNLOADING -> {
            val percent = download.percentDownloaded
            val progress = percent.takeIf { !it.isNaN() && it >= 0f }
                ?.coerceIn(0f, 100f)
            DownloadPresentation(
                label = if (progress == null) "Downloading" else "Downloading ${progress.toInt()}%",
                accent = OmniColors.Warning,
                progress = progress?.div(100f),
                playable = false,
            )
        }
        Download.STATE_QUEUED -> DownloadPresentation(
            label = "Queued",
            accent = OmniColors.Warning,
            progress = null,
            playable = false,
        )
        Download.STATE_STOPPED -> DownloadPresentation(
            label = "Paused",
            accent = OmniColors.Offline,
            progress = null,
            playable = false,
        )
        Download.STATE_FAILED -> DownloadPresentation(
            label = "Failed - retry available",
            accent = OmniColors.Error,
            progress = null,
            playable = false,
        )
        Download.STATE_REMOVING -> DownloadPresentation(
            label = "Removing...",
            accent = OmniColors.TextTertiary,
            progress = null,
            playable = false,
        )
        else -> DownloadPresentation(
            label = "Download state unknown",
            accent = OmniColors.TextTertiary,
            progress = null,
            playable = false,
        )
    }
}

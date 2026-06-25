package com.omnitune.app.ui.screens

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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.media3.exoplayer.offline.Download
import com.omnitune.app.R
import com.omnitune.app.ui.component.EmptyPlaceholder
import com.omnitune.app.ui.theme.OmniColors
import com.omnitune.app.ui.theme.OmniShapes
import com.omnitune.app.ui.theme.OmniSpacing

@androidx.media3.common.util.UnstableApi
@Composable
fun DownloadsScreen(
    onBack: () -> Unit,
    onPlayDownload: (Download) -> Unit = {},
    viewModel: DownloadsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val completedCount = uiState.downloads.count { it.state == Download.STATE_COMPLETED }
    val activeCount = uiState.downloads.count {
        it.state == Download.STATE_DOWNLOADING || it.state == Download.STATE_QUEUED || it.state == Download.STATE_STOPPED
    }
    val failedCount = uiState.downloads.count { it.state == Download.STATE_FAILED }

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
            onBack = onBack,
        )

        if (uiState.downloads.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(OmniShapes.ExtraLarge)
                    .background(OmniColors.OmniGlassSubtle)
                    .border(
                        BorderStroke(1.dp, OmniColors.OmniGlassBorderSubtle),
                        OmniShapes.ExtraLarge,
                    )
                    .padding(OmniSpacing.section),
                contentAlignment = Alignment.Center,
            ) {
                EmptyPlaceholder(
                    icon = R.drawable.ic_download,
                    text = "No downloaded songs yet",
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(OmniSpacing.small),
            ) {
                items(
                    items = uiState.downloads,
                    key = { it.request.id },
                    contentType = { "downloadRow" },
                ) { download ->
                    DownloadItemRow(
                        download = download,
                        onPlay = { onPlayDownload(download) },
                        onRetry = { viewModel.retryDownload(download.request.id) },
                        onRemove = { viewModel.removeDownload(download.request.id) },
                    )
                }
                item { Spacer(modifier = Modifier.height(88.dp)) }
            }
        }
    }
}

@Composable
private fun DownloadsHeader(
    completedCount: Int,
    activeCount: Int,
    failedCount: Int,
    onBack: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = OmniSpacing.medium, bottom = OmniSpacing.large),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(OmniColors.OmniGlassMedium)
                .border(
                    BorderStroke(1.dp, OmniColors.OmniGlassBorderSubtle),
                    CircleShape,
                ),
        ) {
            Icon(
                painterResource(R.drawable.ic_arrow_back),
                contentDescription = "Back",
                tint = OmniColors.TextPrimary,
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(modifier = Modifier.width(OmniSpacing.medium))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Downloads",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = OmniColors.TextPrimary,
            )
            Text(
                text = "Completed songs are ready for offline playback.",
                style = MaterialTheme.typography.bodyMedium,
                color = OmniColors.TextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = OmniSpacing.medium),
        horizontalArrangement = Arrangement.spacedBy(OmniSpacing.small),
    ) {
        DownloadMetric("Ready", completedCount, OmniColors.Downloaded, Modifier.weight(1f))
        DownloadMetric("Active", activeCount, OmniColors.Warning, Modifier.weight(1f))
        DownloadMetric("Failed", failedCount, OmniColors.Error, Modifier.weight(1f))
    }
}

@Composable
private fun DownloadMetric(
    label: String,
    count: Int,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(OmniShapes.Large)
            .background(OmniColors.OmniGlassSubtle)
            .border(
                BorderStroke(1.dp, OmniColors.OmniGlassBorderSubtle),
                OmniShapes.Large,
            )
            .padding(OmniSpacing.small),
    ) {
        Text(
            text = count.toString(),
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

@androidx.media3.common.util.UnstableApi
@Composable
private fun DownloadItemRow(
    download: Download,
    onPlay: () -> Unit,
    onRetry: () -> Unit,
    onRemove: () -> Unit,
) {
    val title = String(download.request.data, Charsets.UTF_8).ifBlank { download.request.id }
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
            Icon(
                painterResource(if (state.playable) R.drawable.ic_play_arrow else R.drawable.ic_download),
                contentDescription = null,
                tint = state.accent,
                modifier = Modifier.size(24.dp),
            )
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
                text = state.label,
                style = MaterialTheme.typography.bodySmall,
                color = state.accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (state.progress != null) {
                Spacer(modifier = Modifier.height(OmniSpacing.compact))
                LinearProgressIndicator(
                    progress = { state.progress },
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
            val progress = download.percentDownloaded
                .takeIf { it >= 0f }
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
        else -> DownloadPresentation(
            label = "Download state unknown",
            accent = OmniColors.TextTertiary,
            progress = null,
            playable = false,
        )
    }
}

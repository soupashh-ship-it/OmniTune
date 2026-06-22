package com.omnitune.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.media3.exoplayer.offline.Download
import com.omnitune.app.R
import com.omnitune.app.ui.component.EmptyPlaceholder
import com.omnitune.app.ui.theme.OmniColors
import com.omnitune.app.ui.theme.OmniShapes

@androidx.media3.common.util.UnstableApi
@Composable
fun DownloadsScreen(
    onBack: () -> Unit,
    onPlayDownload: (Download) -> Unit = {},
    viewModel: DownloadsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OmniColors.Background)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(40.dp)
                    .clip(OmniShapes.SM)
                    .background(OmniColors.GlassSurface)
            ) {
                Icon(
                    painterResource(R.drawable.ic_arrow_back),
                    contentDescription = "Back",
                    tint = OmniColors.TextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "Downloads",
                style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = OmniColors.TextPrimary
            )
        }

        if (uiState.downloads.isEmpty()) {
            EmptyPlaceholder(
                icon = com.omnitune.app.R.drawable.ic_download,
                text = "No downloaded songs yet"
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.downloads) { download ->
                    DownloadItemRow(
                        download = download,
                        onPlay = { onPlayDownload(download) },
                        onRetry = { viewModel.retryDownload(download.request.id) },
                        onRemove = { viewModel.removeDownload(download.request.id) }
                    )
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}

@androidx.media3.common.util.UnstableApi
@Composable
private fun DownloadItemRow(
    download: Download,
    onPlay: () -> Unit,
    onRetry: () -> Unit,
    onRemove: () -> Unit
) {
    val title = String(download.request.data, Charsets.UTF_8)
    
    val (statusText, statusColor) = when (download.state) {
        Download.STATE_COMPLETED -> "Downloaded" to OmniColors.Primary
        Download.STATE_DOWNLOADING -> "Downloading... ${download.percentDownloaded.toInt()}%" to OmniColors.Secondary
        Download.STATE_FAILED -> "Failed" to OmniColors.Hot
        Download.STATE_QUEUED -> "Queued" to OmniColors.TextMuted
        Download.STATE_STOPPED -> "Stopped" to OmniColors.TextMuted
        else -> "Unknown" to OmniColors.TextMuted
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(OmniShapes.SM)
            .then(
                if (download.state == Download.STATE_COMPLETED) {
                    Modifier.clickable(onClick = onPlay)
                } else {
                    Modifier
                }
            )
            .background(OmniColors.GlassSurface)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(OmniShapes.SM)
                .background(OmniColors.GlassSurfaceStrong),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painterResource(com.omnitune.app.R.drawable.ic_download),
                contentDescription = null,
                tint = OmniColors.TextMuted,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = OmniColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = statusText,
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                color = statusColor
            )
        }
        
        if (download.state == Download.STATE_FAILED) {
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = OmniColors.Primary),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text("Retry", color = OmniColors.Background, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        IconButton(
            onClick = onRemove,
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(OmniColors.GlassSurfaceStrong)
        ) {
            Icon(
                painterResource(com.omnitune.app.R.drawable.ic_close),
                contentDescription = "Remove",
                tint = OmniColors.TextMuted,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

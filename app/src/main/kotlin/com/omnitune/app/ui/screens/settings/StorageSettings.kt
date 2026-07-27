package com.omnitune.app.ui.screens.settings

import android.os.StatFs
import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.omnitune.app.R
import com.omnitune.app.backup.OfflineDownloadArchive
import com.omnitune.app.constants.SmartTrimmerKey
import com.omnitune.app.ui.screens.SettingsViewModel
import com.omnitune.app.ui.theme.OmniColors
import com.omnitune.app.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.ln
import kotlin.math.pow

@Composable
fun StorageSettings(
    viewModel: SettingsViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    val context = LocalContext.current
    var showClearCacheDialog by remember { mutableStateOf(false) }
    var showClearArtworkDialog by remember { mutableStateOf(false) }
    var showClearSearchDialog by remember { mutableStateOf(false) }
    var smartTrimmer by rememberPreference(SmartTrimmerKey, true)
    var refreshToken by remember { mutableIntStateOf(0) }
    var storage by remember { mutableStateOf(StorageSnapshot()) }

    LaunchedEffect(refreshToken) {
        storage = withContext(Dispatchers.IO) {
            readStorageSnapshot(
                filesDir = context.filesDir,
                cacheDir = context.cacheDir,
                downloadsDir = OfflineDownloadArchive.downloadDirectory(context),
            )
        }
    }

    if (showClearCacheDialog) {
        AlertDialog(
            onDismissRequest = { showClearCacheDialog = false },
            containerColor = OmniColors.OmniBackgroundElevated,
            titleContentColor = OmniColors.TextPrimary,
            textContentColor = OmniColors.TextSecondary,
            title = { Text("Clear cache?", fontWeight = FontWeight.Bold) },
            text = { Text("This clears stream cache, image cache, and temporary resolver cache. It does NOT delete completed downloads.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearAppCache(context)
                    Toast.makeText(context, "Cache cleared", Toast.LENGTH_SHORT).show()
                    refreshToken += 1
                    showClearCacheDialog = false
                }) { Text("Clear") }
            },
            dismissButton = {
                TextButton(onClick = { showClearCacheDialog = false }) { Text("Cancel") }
            },
        )
    }

    if (showClearArtworkDialog) {
        AlertDialog(
            onDismissRequest = { showClearArtworkDialog = false },
            containerColor = OmniColors.OmniBackgroundElevated,
            titleContentColor = OmniColors.TextPrimary,
            textContentColor = OmniColors.TextSecondary,
            title = { Text("Clear artwork cache?", fontWeight = FontWeight.Bold) },
            text = { Text("Album artwork will be downloaded again when it is needed.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearArtworkCache(context)
                    Toast.makeText(context, "Artwork cache cleared", Toast.LENGTH_SHORT).show()
                    refreshToken += 1
                    showClearArtworkDialog = false
                }) { Text("Clear") }
            },
            dismissButton = {
                TextButton(onClick = { showClearArtworkDialog = false }) { Text("Cancel") }
            },
        )
    }

    if (showClearSearchDialog) {
        AlertDialog(
            onDismissRequest = { showClearSearchDialog = false },
            containerColor = OmniColors.OmniBackgroundElevated,
            titleContentColor = OmniColors.TextPrimary,
            textContentColor = OmniColors.TextSecondary,
            title = { Text("Clear search history?", fontWeight = FontWeight.Bold) },
            text = { Text("This removes the searches shown in recent search suggestions.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearSearchHistory()
                    Toast.makeText(context, "Search history cleared", Toast.LENGTH_SHORT).show()
                    showClearSearchDialog = false
                }) { Text("Clear") }
            },
            dismissButton = {
                TextButton(onClick = { showClearSearchDialog = false }) { Text("Cancel") }
            },
        )
    }

    OmniPreferenceCard(title = "Overview") {
        OmniPreferenceEntry(
            title = "Device storage",
            description = "${formatStorage(storage.usedDeviceBytes)} used of ${formatStorage(storage.totalDeviceBytes)}",
            iconRes = R.drawable.ic_storage,
            accent = OmniColors.OmniAccentPrimary,
        )
        LinearProgressIndicator(
            progress = {
                if (storage.totalDeviceBytes == 0L) 0f
                else (storage.usedDeviceBytes.toFloat() / storage.totalDeviceBytes).coerceIn(0f, 1f)
            },
            color = OmniColors.OmniAccentPrimary,
            trackColor = OmniColors.OmniGlassSubtle,
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 4.dp)
                .fillMaxWidth(),
        )
        OmniPreferenceEntry(
            title = "Downloads",
            description = formatStorage(storage.downloadBytes),
            iconRes = R.drawable.ic_download,
            accent = OmniColors.OmniAccentSecondary,
        )
        OmniPreferenceEntry(
            title = "Artwork",
            description = formatStorage(storage.artworkBytes),
            iconRes = R.drawable.ic_album,
            accent = OmniColors.OmniAccentTertiary,
        )
        OmniPreferenceEntry(
            title = "Temporary cache",
            description = formatStorage(storage.cacheBytes),
            iconRes = R.drawable.ic_storage,
            accent = OmniColors.Warning,
        )
    }

    OmniPreferenceCard(title = "Cleanup") {
        OmniPreferenceEntry(
            title = "Clear cache",
            description = "Remove ${formatStorage(storage.cacheBytes)} of temporary stream and image files",
            iconRes = R.drawable.ic_trash,
            accent = OmniColors.Warning,
            onClick = { showClearCacheDialog = true },
        )
        OmniPreferenceEntry(
            title = "Clear artwork cache",
            description = "Remove ${formatStorage(storage.artworkBytes)} of cached album art",
            iconRes = R.drawable.ic_album,
            accent = OmniColors.Warning,
            onClick = { showClearArtworkDialog = true },
        )
        OmniPreferenceEntry(
            title = "Clear search history",
            description = "Remove recent search suggestions",
            iconRes = R.drawable.ic_search,
            accent = OmniColors.Warning,
            onClick = { showClearSearchDialog = true },
        )
        OmniSwitchPreference(
            title = "Auto-clear temporary files",
            description = "Automatically trim old cached playback files",
            iconRes = R.drawable.ic_sparkle,
            accent = OmniColors.OmniAccentSecondary,
            checked = smartTrimmer,
            onCheckedChange = { smartTrimmer = it },
        )
    }

    OmniPreferenceCard(title = "Files") {
        OmniPreferenceEntry(
            title = "Downloads folder",
            description = OfflineDownloadArchive.downloadDirectory(context).absolutePath,
            iconRes = R.drawable.ic_download,
            accent = OmniColors.Downloaded,
            onClick = {
                openSettingsIntent(context, openAppDetailsIntent(context)) {
                    Toast.makeText(context, "Could not open app storage settings", Toast.LENGTH_SHORT).show()
                }
            },
        )
    }
}

private data class StorageSnapshot(
    val totalDeviceBytes: Long = 0L,
    val usedDeviceBytes: Long = 0L,
    val cacheBytes: Long = 0L,
    val artworkBytes: Long = 0L,
    val downloadBytes: Long = 0L,
)

private fun readStorageSnapshot(
    filesDir: File,
    cacheDir: File,
    downloadsDir: File,
): StorageSnapshot {
    val stat = StatFs(filesDir.absolutePath)
    val total = stat.totalBytes.coerceAtLeast(0L)
    val available = stat.availableBytes.coerceAtLeast(0L)
    return StorageSnapshot(
        totalDeviceBytes = total,
        usedDeviceBytes = (total - available).coerceAtLeast(0L),
        cacheBytes = cacheDir.safeRecursiveSize(),
        artworkBytes = File(cacheDir, "coil").safeRecursiveSize(),
        downloadBytes = downloadsDir.safeRecursiveSize(),
    )
}

private fun File.safeRecursiveSize(): Long = runCatching {
    if (!exists()) 0L
    else walkTopDown().filter { it.isFile }.sumOf { it.length() }
}.getOrDefault(0L)

private fun formatStorage(bytes: Long): String {
    if (bytes <= 0L) return "0 B"
    val unit = 1024.0
    val exponent = (ln(bytes.toDouble()) / ln(unit)).toInt().coerceIn(0, 4)
    val value = bytes / unit.pow(exponent.toDouble())
    val suffix = listOf("B", "KB", "MB", "GB", "TB")[exponent]
    return if (value >= 10 || exponent == 0) {
        "${value.toInt()} $suffix"
    } else {
        "%.1f %s".format(value, suffix)
    }
}
